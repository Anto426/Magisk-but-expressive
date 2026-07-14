from __future__ import annotations

import hashlib
import io
import json
import os
import tempfile
import unittest
from contextlib import redirect_stdout
from pathlib import Path
from unittest.mock import patch

from scripts import release_bot


class NativeReleaseFixture:
    def __init__(self, base: Path, release_id: str, version_code: int) -> None:
        self.root = base / release_id
        self.root.mkdir()
        abi = "arm64-v8a"
        files = []
        abi_dir = self.root / abi
        abi_dir.mkdir()
        for name in release_bot.REQUIRED_BINARIES:
            payload = f"{release_id}:{name}".encode()
            (abi_dir / name).write_bytes(payload)
            files.append({
                "abi": abi,
                "name": name,
                "sha256": hashlib.sha256(payload).hexdigest(),
                "size": len(payload),
            })
        self.manifest = {
            "version": f"core-{version_code}",
            "versionCode": version_code,
            "stubVersion": 40,
            "releaseId": release_id,
            "abiList": [abi],
            "files": files,
        }
        self.write_manifest()

    def write_manifest(self) -> None:
        (self.root / "manifest.json").write_text(
            json.dumps(self.manifest), encoding="utf-8"
        )


class ReleaseBotTest(unittest.TestCase):
    def test_checked_in_release_hashes_and_sizes_are_valid(self) -> None:
        root = release_bot.ROOT / release_bot.DEFAULT_NATIVE_RELEASES
        selected = release_bot.select_native_dir(root, None)
        manifest, abis = release_bot.read_native_binaries(selected)
        self.assertEqual(30700, manifest["versionCode"])
        self.assertEqual(["armeabi-v7a", "arm64-v8a", "x86", "x86_64"], abis)

    def test_selects_unique_highest_valid_manifest_version(self) -> None:
        with tempfile.TemporaryDirectory() as tmp:
            base = Path(tmp)
            NativeReleaseFixture(base, "older", 10)
            newest = NativeReleaseFixture(base, "newest", 11)
            self.assertEqual(newest.root, release_bot.select_native_dir(base, None))

    def test_equal_highest_version_codes_are_rejected(self) -> None:
        with tempfile.TemporaryDirectory() as tmp:
            base = Path(tmp)
            NativeReleaseFixture(base, "one", 11)
            NativeReleaseFixture(base, "two", 11)
            with self.assertRaisesRegex(release_bot.NativeReleaseError, "multiple valid"):
                release_bot.select_native_dir(base, None)

    def test_corrupt_binary_and_extra_file_are_rejected(self) -> None:
        with tempfile.TemporaryDirectory() as tmp:
            fixture = NativeReleaseFixture(Path(tmp), "corrupt", 12)
            binary = fixture.root / "arm64-v8a" / release_bot.REQUIRED_BINARIES[0]
            binary.write_bytes(b"corrupt")
            with self.assertRaisesRegex(release_bot.NativeReleaseError, "size mismatch"):
                release_bot.read_native_binaries(fixture.root)

            fixture = NativeReleaseFixture(Path(tmp), "extra", 13)
            (fixture.root / "arm64-v8a" / "unexpected").write_bytes(b"x")
            with self.assertRaisesRegex(release_bot.NativeReleaseError, "exactly"):
                release_bot.read_native_binaries(fixture.root)

    def test_mbe_version_auto_increments_update_json_monotonically(self) -> None:
        with tempfile.TemporaryDirectory() as tmp:
            update = Path(tmp) / "update.json"
            update.write_text(json.dumps({
                "magisk": {"version": "core-mbe.3", "versionCode": 3}
            }), encoding="utf-8")
            published_name, published_code = release_bot.read_published_mbe_version(update)
            version, code, suffix, resumed = release_bot.resolve_mbe_version(
                "next-core", "", "", published_name, published_code
            )
            self.assertEqual("next-core-mbe.4", version)
            self.assertEqual("00004", code)
            self.assertEqual("mbe.4", suffix)
            self.assertFalse(resumed)

            with self.assertRaisesRegex(release_bot.NativeReleaseError, "must be greater"):
                release_bot.resolve_mbe_version(
                    "next-core", "mbe.3", "", published_name, published_code
                )

            update.write_text(json.dumps({
                "channels": {
                    "stable": {
                        "release": {"version": "core-mbe.5", "versionCode": 5}
                    }
                }
            }), encoding="utf-8")
            self.assertEqual(
                ("core-mbe.5", 5), release_bot.read_published_mbe_version(update)
            )

    def test_rerun_reuses_release_key_without_incrementing(self) -> None:
        version, code, suffix, resumed = release_bot.resolve_mbe_version(
            "core", "", "", "core-mbe.4", 4, "run-42", "run-42"
        )
        self.assertEqual(("core-mbe.4", "00004", "mbe.4", True), (
            version, code, suffix, resumed
        ))
        with self.assertRaisesRegex(release_bot.NativeReleaseError, "suffix"):
            release_bot.resolve_mbe_version(
                "core", "mbe.5", "", "core-mbe.4", 4, "run-42", "run-42"
            )

    def test_android_package_version_tracks_mbe_counter(self) -> None:
        self.assertEqual(1_000_004, release_bot.android_package_version_code(4))
        with self.assertRaises(release_bot.ReleaseBotError):
            release_bot.android_package_version_code(0)

    def test_build_config_uses_manifest_for_native_base_values(self) -> None:
        with tempfile.TemporaryDirectory() as tmp:
            root = Path(tmp)
            releases = root / release_bot.DEFAULT_NATIVE_RELEASES
            releases.mkdir(parents=True)
            fixture = NativeReleaseFixture(releases, "selected", 42)
            (root / "app").mkdir()
            (root / "app" / "gradle.properties").write_text(
                "magisk.versionCode=1\nmagisk.stubVersion=1\n", encoding="utf-8"
            )
            original_root = release_bot.ROOT
            release_bot.ROOT = root
            try:
                manifest, abis = release_bot.read_native_binaries(fixture.root)
                output = release_bot.update_build_props(
                    fixture.root, manifest, abis, "core-42-mbe.4", "00004"
                )
            finally:
                release_bot.ROOT = original_root

            props = (root / "app" / "gradle.properties").read_text(encoding="utf-8")
            config = (root / "config.prop").read_text(encoding="utf-8")
            self.assertNotIn("magisk.versionCode=", props)
            self.assertNotIn("magisk.stubVersion=", props)
            self.assertIn("magisk.nativeReleaseId=selected", props)
            self.assertIn("versionCode=42", config)
            self.assertEqual("42", output["native_version_code"])

    def test_prepare_uses_authoritative_deploy_and_resumes_same_run(self) -> None:
        with tempfile.TemporaryDirectory() as tmp:
            root = Path(tmp)
            releases = root / release_bot.DEFAULT_NATIVE_RELEASES
            releases.mkdir(parents=True)
            NativeReleaseFixture(releases, "selected", 42)
            (root / "app").mkdir()
            (root / "app" / "gradle.properties").write_text("", encoding="utf-8")
            (root / "update.json").write_text(json.dumps({
                "magisk": {"version": "stale-mbe.3", "versionCode": 3}
            }), encoding="utf-8")
            deploy = root / "deploy.json"
            deploy.write_text(json.dumps({
                "magisk": {"version": "core-42-mbe.7", "versionCode": 7}
            }), encoding="utf-8")
            args = type("Args", (), {
                "native_dir": release_bot.DEFAULT_NATIVE_RELEASES.as_posix(),
                "native_release_id": "",
                "release_suffix": "",
                "version_code_offset": "",
                "published_manifest": str(deploy),
                "release_key": "run-42",
            })()
            original_root = release_bot.ROOT
            release_bot.ROOT = root
            try:
                with redirect_stdout(io.StringIO()):
                    release_bot.prepare_command(args)
                metadata = json.loads(
                    (root / "release-bot" / "metadata.json").read_text(encoding="utf-8")
                )
                self.assertEqual("00008", metadata["version_code"])
                self.assertEqual("core-42-mbe.8", metadata["version"])

                deploy.write_text(json.dumps({
                    "magisk": {
                        "version": "core-42-mbe.8",
                        "versionCode": 8,
                        "releaseKey": "run-42",
                    }
                }), encoding="utf-8")
                with redirect_stdout(io.StringIO()):
                    release_bot.prepare_command(args)
                resumed = json.loads(
                    (root / "release-bot" / "metadata.json").read_text(encoding="utf-8")
                )
                self.assertEqual("00008", resumed["version_code"])
                self.assertEqual("true", resumed["resumed"])
            finally:
                release_bot.ROOT = original_root

    def test_telegram_text_is_plain_and_bounded(self) -> None:
        arbitrary = "Changelog [_* with arbitrary Markdown\n" + ("APK_x" * 1000)
        message = arbitrary + "\n📲 Download APK: https://example.invalid/app.apk"
        result = release_bot.truncate_telegram_text(message, 1024)
        self.assertLessEqual(len(result), 1024)
        self.assertIn("Download APK", result)

    def test_deploy_directory_allows_only_update_json(self) -> None:
        with tempfile.TemporaryDirectory() as tmp:
            root = Path(tmp)
            (root / "update.json").write_text("{}", encoding="utf-8")
            release_bot.validate_manifest_only_deploy(root)
            (root / "README.md").write_text("unexpected", encoding="utf-8")
            with self.assertRaisesRegex(release_bot.ReleaseBotError, "update.json only"):
                release_bot.validate_manifest_only_deploy(root)

    def test_deploy_preserves_idempotency_and_telegram_delivery(self) -> None:
        with tempfile.TemporaryDirectory() as tmp:
            base = Path(tmp)
            deploy = base / "deploy"
            deploy.mkdir()
            metadata = base / "metadata.json"
            metadata.write_text(json.dumps({
                "version": "core-mbe.4",
                "version_code": "00004",
                "native_version_code": "30700",
                "release_key": "run-42",
            }), encoding="utf-8")
            args = type("Args", (), {
                "metadata": str(metadata),
                "upstream_dir": str(deploy),
                "release_repo": "owner/repo",
                "tag": "vcore-mbe.4",
                "apk_asset": "Magisk-vcore-mbe.4.apk",
                "channel": "stable",
                "legacy_manifest": "",
            })()
            release_bot.upstream_command(args)
            first = json.loads((deploy / "update.json").read_text(encoding="utf-8"))
            self.assertEqual({"magisk"}, set(first))
            release = first["magisk"]
            self.assertEqual("run-42", release["releaseKey"])
            self.assertFalse(release["telegramClaimed"])
            self.assertFalse(release["telegramNotified"])

            claim_args = type("Args", (), {
                "upstream_dir": str(deploy),
                "release_key": "run-42",
                "channel": "stable",
            })()
            release_bot.claim_telegram_command(claim_args)
            release_bot.mark_notified_command(type("Args", (), {
                "upstream_dir": str(deploy),
                "release_key": "run-42",
                "channel": "stable",
            })())
            release_bot.upstream_command(args)
            resumed = json.loads((deploy / "update.json").read_text(encoding="utf-8"))
            resumed_release = resumed["magisk"]
            self.assertEqual(release["publishedAt"], resumed_release["publishedAt"])
            self.assertTrue(resumed_release["telegramClaimed"])
            self.assertTrue(resumed_release["telegramNotified"])

    def test_claimed_but_unconfirmed_telegram_is_not_retried(self) -> None:
        with tempfile.TemporaryDirectory() as tmp:
            base = Path(tmp)
            deploy = base / "deploy"
            deploy.mkdir()
            metadata = base / "metadata.json"
            metadata.write_text(json.dumps({
                "version": "core-mbe.4",
                "version_code": "00004",
                "native_version_code": "30700",
                "release_key": "run-42",
            }), encoding="utf-8")
            upstream_args = type("Args", (), {
                "metadata": str(metadata),
                "upstream_dir": str(deploy),
                "release_repo": "owner/repo",
                "tag": "vcore-mbe.4",
                "apk_asset": "Magisk-vcore-mbe.4.apk",
                "channel": "stable",
                "legacy_manifest": "",
            })()
            delivery_args = type("Args", (), {
                "upstream_dir": str(deploy),
                "release_key": "run-42",
                "channel": "stable",
            })()

            release_bot.upstream_command(upstream_args)
            with self.assertRaisesRegex(release_bot.ReleaseBotError, "before it is claimed"):
                release_bot.mark_notified_command(delivery_args)
            release_bot.claim_telegram_command(delivery_args)

            github_output = base / "github-output.txt"
            with patch.dict(os.environ, {"GITHUB_OUTPUT": str(github_output)}):
                release_bot.upstream_command(upstream_args)

            outputs = dict(
                line.split("=", 1)
                for line in github_output.read_text(encoding="utf-8").splitlines()
            )
            self.assertEqual("false", outputs["telegram_required"])
            self.assertEqual("true", outputs["telegram_incomplete"])
            resumed = json.loads((deploy / "update.json").read_text(encoding="utf-8"))
            self.assertTrue(resumed["magisk"]["telegramClaimed"])
            self.assertFalse(resumed["magisk"]["telegramNotified"])

            wrong_key = type("Args", (), {
                "upstream_dir": str(deploy),
                "release_key": "run-other",
                "channel": "stable",
            })()
            with self.assertRaisesRegex(release_bot.ReleaseBotError, "different release key"):
                release_bot.claim_telegram_command(wrong_key)

    def test_legacy_notified_release_implicitly_owns_telegram_claim(self) -> None:
        with tempfile.TemporaryDirectory() as tmp:
            base = Path(tmp)
            deploy = base / "deploy"
            deploy.mkdir()
            (deploy / "update.json").write_text(json.dumps({
                "magisk": {
                    "version": "core-mbe.4",
                    "versionCode": 4,
                    "releaseKey": "run-42",
                    "telegramNotified": True,
                }
            }), encoding="utf-8")
            metadata = base / "metadata.json"
            metadata.write_text(json.dumps({
                "version": "core-mbe.4",
                "version_code": "00004",
                "native_version_code": "30700",
                "release_key": "run-42",
            }), encoding="utf-8")
            args = type("Args", (), {
                "metadata": str(metadata),
                "upstream_dir": str(deploy),
                "release_repo": "owner/repo",
                "tag": "vcore-mbe.4",
                "apk_asset": "Magisk-vcore-mbe.4.apk",
                "channel": "stable",
                "legacy_manifest": "",
            })()
            github_output = base / "github-output.txt"

            with patch.dict(os.environ, {"GITHUB_OUTPUT": str(github_output)}):
                release_bot.upstream_command(args)

            release = json.loads(
                (deploy / "update.json").read_text(encoding="utf-8")
            )["magisk"]
            self.assertTrue(release["telegramClaimed"])
            self.assertTrue(release["telegramNotified"])
            outputs = dict(
                line.split("=", 1)
                for line in github_output.read_text(encoding="utf-8").splitlines()
            )
            self.assertEqual("false", outputs["telegram_required"])
            self.assertEqual("false", outputs["telegram_incomplete"])


if __name__ == "__main__":
    unittest.main()
