#!/usr/bin/env python3
from __future__ import annotations

import argparse
import hashlib
import json
import os
import re
import sys
import urllib.parse
import urllib.request
from datetime import date
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
KNOWN_ABIS = ("armeabi-v7a", "arm64-v8a", "x86", "x86_64", "riscv64")
REQUIRED_BINARIES = ("magiskboot", "magiskinit", "magiskpolicy", "magisk", "init-ld", "busybox")
DEFAULT_NATIVE_RELEASES = Path("prebuilt/native/releases")
ANDROID_PACKAGE_VERSION_BASE = 1_000_000
MAX_ANDROID_VERSION_CODE = 2_147_483_647


class ReleaseBotError(ValueError):
    """Raised for a release configuration that must abort publication."""


class NativeReleaseError(ReleaseBotError):
    """Raised when a prebuilt native release is incomplete or untrusted."""


def die(message: str) -> None:
    print(f"release-bot: {message}", file=sys.stderr)
    raise SystemExit(1)


def read_json(path: Path) -> dict:
    return json.loads(path.read_text(encoding="utf-8-sig"))


def write_json(path: Path, data: dict) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(data, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")


def set_property(path: Path, key: str, value: str) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    lines = path.read_text(encoding="utf-8").splitlines() if path.exists() else []
    prefix = f"{key}="
    replaced = False
    next_lines: list[str] = []
    for line in lines:
        if line.startswith(prefix):
            next_lines.append(f"{key}={value}")
            replaced = True
        else:
            next_lines.append(line)
    if not replaced:
        next_lines.append(f"{key}={value}")
    path.write_text("\n".join(next_lines).rstrip() + "\n", encoding="utf-8")


def remove_property(path: Path, key: str) -> None:
    if not path.exists():
        return
    prefix = f"{key}="
    lines = [line for line in path.read_text(encoding="utf-8").splitlines()
             if not line.startswith(prefix)]
    path.write_text("\n".join(lines).rstrip() + "\n", encoding="utf-8")


def github_output(values: dict[str, str]) -> None:
    output = os.environ.get("GITHUB_OUTPUT")
    if not output:
        return
    with open(output, "a", encoding="utf-8") as fp:
        for key, value in values.items():
            fp.write(f"{key}={value}\n")


def safe_slug(value: str) -> str:
    value = re.sub(r"[^A-Za-z0-9._-]+", "-", value.strip())
    return value.strip("-") or "native"


def normalize_release_key(value: str) -> str:
    release_key = value.strip()
    if release_key and not re.fullmatch(r"[A-Za-z0-9._:-]{1,128}", release_key):
        raise ReleaseBotError("release key contains unsupported characters")
    return release_key


def android_package_version_code(mbe_version_code: int) -> int:
    """Map the monotonic MBE counter to an Android-upgrade-safe package code."""
    if isinstance(mbe_version_code, bool) or mbe_version_code <= 0:
        raise ReleaseBotError("MBE versionCode must be a positive integer")
    package_code = ANDROID_PACKAGE_VERSION_BASE + mbe_version_code
    if package_code > MAX_ANDROID_VERSION_CODE:
        raise ReleaseBotError("MBE versionCode exceeds the Android package limit")
    return package_code


def sha256_file(path: Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as stream:
        for chunk in iter(lambda: stream.read(1024 * 1024), b""):
            digest.update(chunk)
    return digest.hexdigest()


def read_native_binaries(root: Path) -> tuple[dict, list[str]]:
    manifest_path = root / "manifest.json"
    if not manifest_path.is_file():
        raise NativeReleaseError(f"missing manifest: {manifest_path}")
    try:
        manifest = read_json(manifest_path)
    except (OSError, UnicodeError, json.JSONDecodeError) as exc:
        raise NativeReleaseError(f"invalid JSON manifest {manifest_path}: {exc}") from exc
    if not isinstance(manifest, dict):
        raise NativeReleaseError(f"manifest must be an object: {manifest_path}")
    if "sourceApk" in manifest:
        raise NativeReleaseError(f"sourceApk is not allowed in {manifest_path}")

    version = manifest.get("version")
    release_id = manifest.get("releaseId")
    version_code = manifest.get("versionCode")
    stub_version = manifest.get("stubVersion")
    if not isinstance(version, str) or not version.strip():
        raise NativeReleaseError(f"missing non-empty version in {manifest_path}")
    if not isinstance(release_id, str) or not release_id.strip():
        raise NativeReleaseError(f"missing non-empty releaseId in {manifest_path}")
    if release_id != root.name:
        raise NativeReleaseError(
            f"releaseId {release_id!r} must match directory {root.name!r}"
        )
    if isinstance(version_code, bool) or not isinstance(version_code, int) or version_code <= 0:
        raise NativeReleaseError(f"versionCode must be a positive integer in {manifest_path}")
    if isinstance(stub_version, bool) or not isinstance(stub_version, int) or stub_version <= 0:
        raise NativeReleaseError(f"stubVersion must be a positive integer in {manifest_path}")

    abis_value = manifest.get("abiList")
    if not isinstance(abis_value, list) or not abis_value:
        raise NativeReleaseError(f"abiList must be a non-empty array in {manifest_path}")
    if any(not isinstance(abi, str) or not abi.strip() for abi in abis_value):
        raise NativeReleaseError(f"abiList contains an invalid ABI in {manifest_path}")
    abis = [abi.strip() for abi in abis_value]
    if len(abis) != len(set(abis)):
        raise NativeReleaseError(f"abiList contains duplicates in {manifest_path}")
    unsupported = sorted(set(abis) - set(KNOWN_ABIS))
    if unsupported:
        raise NativeReleaseError(f"unsupported ABIs in {manifest_path}: {', '.join(unsupported)}")

    expected_root_entries = {"manifest.json", *abis}
    actual_root_entries = {entry.name for entry in root.iterdir()}
    if actual_root_entries != expected_root_entries:
        missing = sorted(expected_root_entries - actual_root_entries)
        extra = sorted(actual_root_entries - expected_root_entries)
        raise NativeReleaseError(
            f"release root mismatch in {root}; missing={missing}, extra={extra}"
        )

    expected_keys = {(abi, name) for abi in abis for name in REQUIRED_BINARIES}
    for abi in abis:
        abi_dir = root / abi
        if not abi_dir.is_dir() or abi_dir.is_symlink():
            raise NativeReleaseError(f"missing real ABI directory: {abi_dir}")
        entries = list(abi_dir.iterdir())
        actual_names = {entry.name for entry in entries if entry.is_file()}
        if (actual_names != set(REQUIRED_BINARIES)
                or any(not entry.is_file() or entry.is_symlink() for entry in entries)):
            raise NativeReleaseError(
                f"{abi_dir} must contain exactly: {', '.join(sorted(REQUIRED_BINARIES))}"
            )

    files = manifest.get("files")
    if not isinstance(files, list):
        raise NativeReleaseError(f"files must be an array in {manifest_path}")
    declared_keys: set[tuple[str, str]] = set()
    for entry in files:
        if not isinstance(entry, dict):
            raise NativeReleaseError(f"files contains a non-object entry in {manifest_path}")
        if "path" in entry:
            raise NativeReleaseError(f"path fields are not allowed in {manifest_path}")
        abi = entry.get("abi")
        name = entry.get("name")
        key = (abi, name)
        if not isinstance(abi, str) or not isinstance(name, str) or key not in expected_keys:
            raise NativeReleaseError(f"unexpected native file entry {abi!r}/{name!r}")
        if key in declared_keys:
            raise NativeReleaseError(f"duplicate native file entry {abi}/{name}")
        declared_keys.add(key)

        expected_hash = entry.get("sha256")
        expected_size = entry.get("size")
        if (not isinstance(expected_hash, str)
                or not re.fullmatch(r"[0-9a-fA-F]{64}", expected_hash)):
            raise NativeReleaseError(f"invalid SHA-256 for {abi}/{name}")
        if isinstance(expected_size, bool) or not isinstance(expected_size, int) or expected_size < 0:
            raise NativeReleaseError(f"invalid size for {abi}/{name}")
        binary = root / abi / name
        if binary.stat().st_size != expected_size:
            raise NativeReleaseError(f"size mismatch for {abi}/{name}")
        if sha256_file(binary) != expected_hash.lower():
            raise NativeReleaseError(f"SHA-256 mismatch for {abi}/{name}")

    if declared_keys != expected_keys:
        missing = sorted(f"{abi}/{name}" for abi, name in expected_keys - declared_keys)
        raise NativeReleaseError(f"missing manifest file entries: {', '.join(missing)}")
    return manifest, abis


def select_native_dir(base_dir: Path, release_id: str | None) -> Path:
    base_dir = base_dir if base_dir.is_absolute() else ROOT / base_dir
    if release_id:
        native_dir = base_dir / release_id
        if not native_dir.is_dir():
            raise NativeReleaseError(f"native release not found: {native_dir}")
        read_native_binaries(native_dir)
        return native_dir
    if not base_dir.is_dir():
        raise NativeReleaseError(f"native release root not found: {base_dir}")

    candidates: list[tuple[Path, int]] = []
    rejected: list[str] = []
    for path in sorted((path for path in base_dir.iterdir() if path.is_dir()),
                       key=lambda candidate: candidate.name):
        try:
            manifest, _ = read_native_binaries(path)
        except NativeReleaseError as exc:
            rejected.append(f"{path.name}: {exc}")
            continue
        candidates.append((path, int(manifest["versionCode"])))
    if not candidates:
        details = f" ({'; '.join(rejected)})" if rejected else ""
        raise NativeReleaseError(f"no valid native releases found in {base_dir}{details}")

    max_code = max(code for _, code in candidates)
    latest = [path for path, code in candidates if code == max_code]
    if len(latest) != 1:
        names = ", ".join(path.name for path in latest)
        raise NativeReleaseError(
            f"multiple valid native releases have versionCode {max_code}: {names}"
        )
    for warning in rejected:
        print(f"release-bot: ignoring invalid native release: {warning}", file=sys.stderr)
    return latest[0]


def compose_release_version(native_version: str, suffix: str) -> str:
    suffix = suffix.strip()
    if not suffix:
        return native_version
    if suffix.startswith(("-", "+", ".")):
        return f"{native_version}{suffix}"
    return f"{native_version}-{suffix}"


def read_published_release(path: Path) -> dict:
    if not path.exists():
        return {}
    try:
        data = read_json(path)
    except (OSError, UnicodeError, json.JSONDecodeError) as exc:
        raise NativeReleaseError(f"cannot read published update manifest {path}: {exc}") from exc
    if not isinstance(data, dict):
        raise NativeReleaseError(f"published update manifest must be an object: {path}")

    releases: list[dict] = []
    legacy = data.get("magisk")
    if isinstance(legacy, dict):
        releases.append(legacy)
    channels = data.get("channels")
    if isinstance(channels, dict):
        for channel in channels.values():
            if isinstance(channel, dict) and isinstance(channel.get("release"), dict):
                releases.append(channel["release"])
    if not releases:
        raise NativeReleaseError(f"no published release found in {path}")

    parsed: list[dict] = []
    for release in releases:
        name = release.get("version")
        code = release.get("versionCode")
        if not isinstance(name, str) or not name.strip():
            raise NativeReleaseError(f"published release has an invalid version in {path}")
        if isinstance(code, bool) or not isinstance(code, int) or code < 0:
            raise NativeReleaseError(f"published release has an invalid versionCode in {path}")
        release_key = release.get("releaseKey", "")
        if not isinstance(release_key, str):
            raise NativeReleaseError(f"published release has an invalid releaseKey in {path}")
        telegram_notified = release.get("telegramNotified", False)
        if not isinstance(telegram_notified, bool):
            raise NativeReleaseError(
                f"published release has an invalid telegramNotified flag in {path}"
            )
        telegram_claimed = release.get("telegramClaimed", False)
        if not isinstance(telegram_claimed, bool):
            raise NativeReleaseError(
                f"published release has an invalid telegramClaimed flag in {path}"
            )
        parsed.append({**release, "version": name.strip(), "releaseKey": release_key.strip()})
    return max(parsed, key=lambda item: int(item["versionCode"]))


def read_published_mbe_version(path: Path) -> tuple[str, int]:
    release = read_published_release(path)
    if not release:
        return "", 0
    return str(release["version"]), int(release["versionCode"])


def release_suffix_from_version(native_version: str, release_version: str) -> str:
    if release_version == native_version:
        return ""
    if not release_version.startswith(native_version):
        raise NativeReleaseError(
            f"published version {release_version!r} does not use native base {native_version!r}"
        )
    return release_version[len(native_version):].lstrip("-+.")


def resolve_mbe_version(
    native_version: str,
    release_suffix: str,
    version_code_override: str,
    published_name: str,
    published_code: int,
    release_key: str = "",
    published_release_key: str = "",
) -> tuple[str, str, str, bool]:
    suffix = release_suffix.strip()
    override = version_code_override.strip()
    if release_key and release_key == published_release_key:
        if not published_name or published_code <= 0:
            raise NativeReleaseError("matching releaseKey has no valid published release")
        if suffix and compose_release_version(native_version, suffix) != published_name:
            raise NativeReleaseError("rerun release suffix does not match the published release")
        if override:
            try:
                requested_code = int(override)
            except ValueError as exc:
                raise NativeReleaseError("version-code-offset must be an integer") from exc
            if requested_code != published_code:
                raise NativeReleaseError("rerun versionCode does not match the published release")
        return (
            published_name,
            f"{published_code:05d}",
            release_suffix_from_version(native_version, published_name),
            True,
        )
    if override:
        try:
            next_code = int(override)
        except ValueError as exc:
            raise NativeReleaseError("version-code-offset must be an integer") from exc
    elif suffix:
        match = re.search(r"(\d+)$", suffix)
        next_code = int(match.group(1)) if match else published_code + 1
    else:
        next_code = published_code + 1
        suffix = f"mbe.{next_code}"
    if next_code <= published_code:
        raise NativeReleaseError(
            f"MBE versionCode {next_code} must be greater than published "
            f"{published_code} ({published_name or 'no version'})"
        )
    if not suffix:
        suffix = f"mbe.{next_code}"
    return compose_release_version(native_version, suffix), f"{next_code:05d}", suffix, False


def update_build_props(
    native_dir: Path,
    manifest: dict,
    abis: list[str],
    release_version: str,
    release_code: str,
) -> dict[str, str]:
    resolved_native_dir = native_dir.resolve()
    try:
        rel_native_dir = resolved_native_dir.relative_to(ROOT.resolve()).as_posix()
    except ValueError:
        rel_native_dir = resolved_native_dir.as_posix()
    native_version = str(manifest["version"])
    native_code = str(manifest["versionCode"])
    stub_version = str(manifest["stubVersion"])
    abi_list = ",".join(abis)
    package_version_code = android_package_version_code(int(release_code))

    gradle_props = ROOT / "app" / "gradle.properties"
    remove_property(gradle_props, "magisk.versionCode")
    remove_property(gradle_props, "magisk.stubVersion")
    default_root = (ROOT / DEFAULT_NATIVE_RELEASES).resolve()
    if resolved_native_dir.parent == default_root:
        remove_property(gradle_props, "magisk.nativeBinariesDir")
        remove_property(gradle_props, "magisk.nativeBinariesRoot")
        set_property(gradle_props, "magisk.nativeReleaseId", str(manifest["releaseId"]))
    else:
        remove_property(gradle_props, "magisk.nativeReleaseId")
        set_property(gradle_props, "magisk.nativeBinariesDir", rel_native_dir)
    set_property(gradle_props, "magisk.mbeVersionName", release_version)
    set_property(gradle_props, "magisk.mbeVersionCode", release_code)
    set_property(ROOT / "config.prop", "version", native_version)
    set_property(ROOT / "config.prop", "versionCode", native_code)
    set_property(ROOT / "config.prop", "abiList", abi_list)

    return {
        "native_dir": rel_native_dir,
        "release_id": str(manifest["releaseId"]),
        "native_version": native_version,
        "native_version_code": native_code,
        "version": release_version,
        "version_code": release_code,
        "package_version_name": release_version,
        "package_version_code": str(package_version_code),
        "stub_version": stub_version,
        "abis": abi_list,
    }


def extract_changelog(changelog: Path, version: str) -> str:
    if not changelog.exists():
        return f"Build {version}."
    text = changelog.read_text(encoding="utf-8", errors="replace").strip()
    if not text:
        return f"Build {version}."

    lines = text.splitlines()
    heading = re.compile(r"^(#{1,6})\s+(.+?)\s*$")
    version_re = re.compile(rf"(^|\b)v?{re.escape(version)}(\b|$)", re.IGNORECASE)
    for idx, line in enumerate(lines):
        match = heading.match(line)
        if not match or not version_re.search(match.group(2)):
            continue
        level = len(match.group(1))
        end = len(lines)
        for next_idx in range(idx + 1, len(lines)):
            next_match = heading.match(lines[next_idx])
            if next_match and len(next_match.group(1)) <= level:
                end = next_idx
                break
        section = "\n".join(lines[idx + 1 : end]).strip()
        if section:
            return section

    first_heading = next((i for i, line in enumerate(lines) if heading.match(line)), None)
    if first_heading is None:
        return text
    level = len(heading.match(lines[first_heading]).group(1))
    end = len(lines)
    for idx in range(first_heading + 1, len(lines)):
        match = heading.match(lines[idx])
        if match and len(match.group(1)) <= level:
            end = idx
            break
    return "\n".join(lines[first_heading + 1 : end]).strip() or text


def notes_command(args: argparse.Namespace) -> None:
    metadata = read_json(Path(args.metadata))
    out_dir = Path(args.out_dir)
    out_dir.mkdir(parents=True, exist_ok=True)

    version = metadata["version"]
    version_code = metadata.get("version_code") or ""
    notes = extract_changelog(ROOT / args.changelog, version)
    apk_url = f"https://github.com/{args.repo}/releases/download/{args.tag}/{args.apk_asset}"
    note_url = f"https://github.com/{args.repo}/releases/download/{args.tag}/release.md"
    release_title = f"Magisk {version}"

    release_md = [
        f"# 🚀 Magisk-but-Expressive {version}",
        "",
        "Magisk-but-Expressive brings a modern Material 3 Expressive design to the trusted Magisk application.",
        "",
        "## 📝 Changelog",
        notes,
        "",
        "---",
        "",
        "## 🛠️ Build Details",
        f"* **MBE Version**: `{version}` (Version Code: `{version_code}`)",
        f"* **Android Package**: `{metadata.get('package_version_name', version)}` "
        f"(Version Code: `{metadata.get('package_version_code', '')}`)",
        f"* **Native Core Version**: `{metadata.get('native_version', '')} ({metadata.get('native_version_code', '')})`",
        f"* **Supported ABIs**: `{metadata.get('abis', '')}`",
        f"* **Native Binaries Folder**: `{metadata.get('release_id', 'unknown')}`",
        "",
        "---",
        "",
        "## 📢 Stay Connected",
        "* Join the [Magisk-but-Expressive Telegram Channel](https://t.me/magiskBe) for updates.",
        "* Chat with us in the [Telegram Group](https://t.me/magiskBe)!",
    ]
    (out_dir / "release.md").write_text("\n".join(release_md).rstrip() + "\n", encoding="utf-8")

    native_code = metadata.get("native_version_code", "")
    update = {
        "magisk": {
            "version": version,
            "versionCode": int(version_code) if str(version_code).isdigit() else -1,
            "magiskVersionCode": int(native_code) if str(native_code).isdigit() else -1,
            "clientVersionCode": int(native_code) if str(native_code).isdigit() else -1,
            "link": apk_url,
            "note": note_url,
        }
    }
    write_json(out_dir / "update.json", update)

    telegram_lines = [
        f"🚀 Magisk-but-Expressive {version} is now available!",
        "",
        "📝 Changelog:",
        notes,
        "",
        f"📲 Download APK: {apk_url}",
    ]
    telegram = "\n".join(telegram_lines)
    (out_dir / "telegram.md").write_text(telegram, encoding="utf-8")

    try:
        vc_num = int(version_code)
    except ValueError:
        vc_num = 1
    
    photo_file = "release_image2.png" if vc_num % 2 == 0 else "release_image1.jpg"
    src_photo = ROOT / "scripts" / photo_file
    photo_output_path = ""
    if src_photo.exists():
        import shutil
        shutil.copy(src_photo, out_dir / photo_file)
        photo_output_path = f"release-bot/{photo_file}"

    github_output({
        "title": release_title,
        "update_url": note_url,
        "release_photo": photo_output_path
    })


def upstream_command(args: argparse.Namespace) -> None:
    """Write the only file consumed by the manifest-only upstream repository."""
    metadata = read_json(Path(args.metadata))
    root = Path(args.upstream_dir)
    deploy_manifest = root / "update.json"
    version = str(metadata["version"])
    version_code = int(metadata.get("version_code") or -1)
    client_version_code = int(metadata.get("native_version_code") or -1)
    release_key = normalize_release_key(str(metadata.get("release_key") or ""))
    tag = args.tag or f"v{version}"
    apk_asset = args.apk_asset or f"Magisk-v{version}.apk"
    release_base = f"https://github.com/{args.release_repo}/releases/download/{tag}"

    current = read_published_release(deploy_manifest)
    is_resume = bool(release_key and current.get("releaseKey") == release_key)
    if is_resume and (
        current.get("version") != version or int(current.get("versionCode", -1)) != version_code
    ):
        raise ReleaseBotError("release key already belongs to different published metadata")
    published_at = (
        str(current.get("publishedAt"))
        if is_resume and current.get("publishedAt")
        else date.today().isoformat()
    )
    telegram_notified = current.get("telegramNotified") is True if is_resume else False
    # A recorded delivery from manifests produced before telegramClaimed existed
    # implicitly owns the claim and must never be sent again.
    telegram_claimed = (
        current.get("telegramClaimed") is True or telegram_notified
    ) if is_resume else False

    public_release = {
        "version": version,
        "versionCode": version_code,
        "magiskVersionCode": client_version_code,
        # Kept while already-published clients still understand this name.
        "clientVersionCode": client_version_code,
        "link": f"{release_base}/{apk_asset}",
        "note": f"{release_base}/release.md",
        "publishedAt": published_at,
    }
    deploy_release = {
        **public_release,
        "releaseKey": release_key,
        "telegramClaimed": telegram_claimed,
        "telegramNotified": telegram_notified,
    }
    # Keep the public client schema stable: installed clients consume top-level `magisk`.
    write_json(deploy_manifest, {"magisk": deploy_release})
    if args.legacy_manifest:
        write_json(Path(args.legacy_manifest), {"magisk": public_release})
    validate_manifest_only_deploy(root)
    github_output({
        "release_key": release_key,
        "telegram_required": str(not telegram_claimed).lower(),
        "telegram_incomplete": str(telegram_claimed and not telegram_notified).lower(),
    })


def deploy_release(data: dict, channel_name: str) -> dict:
    release = data.get("magisk")
    if not isinstance(release, dict):
        channels = data.get("channels")
        channel = channels.get(channel_name) if isinstance(channels, dict) else None
        release = channel.get("release") if isinstance(channel, dict) else None
    if not isinstance(release, dict):
        raise ReleaseBotError(f"missing deploy channel {channel_name!r}")
    return release


def claim_telegram_command(args: argparse.Namespace) -> None:
    root = Path(args.upstream_dir)
    validate_manifest_only_deploy(root)
    manifest_path = root / "update.json"
    data = read_json(manifest_path)
    release = deploy_release(data, args.channel)
    expected_key = normalize_release_key(args.release_key)
    if not expected_key or release.get("releaseKey") != expected_key:
        raise ReleaseBotError("cannot claim Telegram delivery for a different release key")
    changed = release.get("telegramClaimed") is not True
    if changed:
        release["telegramClaimed"] = True
        write_json(manifest_path, data)
    github_output({"changed": str(changed).lower()})


def mark_notified_command(args: argparse.Namespace) -> None:
    root = Path(args.upstream_dir)
    validate_manifest_only_deploy(root)
    manifest_path = root / "update.json"
    data = read_json(manifest_path)
    release = deploy_release(data, args.channel)
    expected_key = normalize_release_key(args.release_key)
    if not expected_key or release.get("releaseKey") != expected_key:
        raise ReleaseBotError("cannot mark Telegram delivery for a different release key")
    if release.get("telegramClaimed") is not True:
        raise ReleaseBotError("cannot mark Telegram delivery before it is claimed")
    changed = release.get("telegramNotified") is not True
    if changed:
        release["telegramNotified"] = True
        write_json(manifest_path, data)
    github_output({"changed": str(changed).lower()})


def validate_manifest_only_deploy(root: Path) -> None:
    allowed = {".git", "update.json"}
    unexpected = sorted(entry.name for entry in root.iterdir() if entry.name not in allowed)
    if unexpected:
        raise ReleaseBotError(
            f"deploy repository must contain update.json only; unexpected: {', '.join(unexpected)}"
        )


def prepare_command(args: argparse.Namespace) -> None:
    native_dir = select_native_dir(Path(args.native_dir), args.native_release_id or None)
    manifest, abis = read_native_binaries(native_dir)
    native_version = str(manifest.get("version") or manifest.get("releaseId"))
    published_manifest = Path(args.published_manifest)
    if not published_manifest.is_absolute():
        published_manifest = ROOT / published_manifest
    published = read_published_release(published_manifest)
    published_name = str(published.get("version") or "")
    published_code = int(published.get("versionCode") or 0)
    published_release_key = str(published.get("releaseKey") or "")
    release_key = normalize_release_key(args.release_key)
    release_version, release_code, release_suffix, resumed = resolve_mbe_version(
        native_version,
        args.release_suffix,
        args.version_code_offset,
        published_name,
        published_code,
        release_key,
        published_release_key,
    )

    outputs = update_build_props(native_dir, manifest, abis, release_version, release_code)
    outputs["release_suffix"] = release_suffix
    outputs["release_key"] = release_key
    outputs["resumed"] = str(resumed).lower()
    metadata_path = ROOT / "release-bot" / "metadata.json"
    write_json(metadata_path, outputs)
    outputs["metadata"] = str(metadata_path.relative_to(ROOT).as_posix())
    github_output(outputs)
    print(json.dumps(outputs, indent=2))


def truncate_telegram_text(text: str, limit: int) -> str:
    if len(text) <= limit:
        return text
    footer_lines = [
        line for line in text.splitlines()
        if any(keyword in line for keyword in ("Download", "APK", "Group", "Channel"))
    ]
    footer = "\n".join(footer_lines[-2:])
    if len(footer) > limit // 2:
        footer = footer[-(limit // 2):]
    separator = "\n\n" if footer else ""
    available = limit - len(separator) - len(footer) - 1
    return text[:available].rstrip() + "…" + separator + footer


def send_photo_telegram(token: str, chat_id: str, photo_path: Path, caption: str) -> None:
    import uuid
    import mimetypes
    import urllib.request
    
    caption = truncate_telegram_text(caption, 1024)

    boundary = f"----WebKitFormBoundary{uuid.uuid4().hex}"
    parts = []
    
    parts.append(f"--{boundary}".encode())
    parts.append(b'Content-Disposition: form-data; name="chat_id"')
    parts.append(b'')
    parts.append(chat_id.encode())
    
    parts.append(f"--{boundary}".encode())
    parts.append(b'Content-Disposition: form-data; name="caption"')
    parts.append(b'')
    parts.append(caption.encode('utf-8'))
    
    parts.append(f"--{boundary}".encode())
    mime_type = mimetypes.guess_type(str(photo_path))[0] or 'application/octet-stream'
    parts.append(f'Content-Disposition: form-data; name="photo"; filename="{photo_path.name}"'.encode())
    parts.append(f'Content-Type: {mime_type}'.encode())
    parts.append(b'')
    parts.append(photo_path.read_bytes())
    
    parts.append(f"--{boundary}--".encode())
    parts.append(b'')
    
    body = b'\r\n'.join(parts)
    
    url = f"https://api.telegram.org/bot{token}/sendPhoto"
    req = urllib.request.Request(url, data=body)
    req.add_header('Content-Type', f'multipart/form-data; boundary={boundary}')
    req.add_header('Content-Length', str(len(body)))
    
    with urllib.request.urlopen(req, timeout=30) as response:
        if response.status >= 300:
            die(f"telegram returned HTTP {response.status}")


def telegram_command(args: argparse.Namespace) -> None:
    token = os.environ.get("TELEGRAM_BOT_TOKEN")
    chat_id = os.environ.get("TELEGRAM_CHAT_ID")
    if not token or not chat_id:
        die("TELEGRAM_BOT_TOKEN and TELEGRAM_CHAT_ID are required")
    text = Path(args.message).read_text(encoding="utf-8")
    
    if args.photo:
        photo_path = Path(args.photo)
        if photo_path.exists():
            send_photo_telegram(token, chat_id, photo_path, text)
            return
        else:
            print(f"Warning: Photo path {args.photo} not found, falling back to sendMessage")

    data = urllib.parse.urlencode({
        "chat_id": chat_id,
        "text": truncate_telegram_text(text, 4096),
        "disable_web_page_preview": "false",
    }).encode()
    url = f"https://api.telegram.org/bot{token}/sendMessage"
    with urllib.request.urlopen(url, data=data, timeout=30) as response:
        if response.status >= 300:
            die(f"telegram returned HTTP {response.status}")


def main() -> None:
    parser = argparse.ArgumentParser(description="Prepare and publish Magisk release builds")
    sub = parser.add_subparsers(dest="cmd", required=True)

    prepare = sub.add_parser("prepare", help="select local native binaries and update build config")
    prepare.add_argument("--native-dir", default=DEFAULT_NATIVE_RELEASES.as_posix())
    prepare.add_argument("--native-release-id", default="")
    prepare.add_argument("--release-suffix", default="")
    prepare.add_argument("--version-code-offset", default="")
    prepare.add_argument("--published-manifest", default="update.json")
    prepare.add_argument("--release-key", default="")
    prepare.set_defaults(func=prepare_command)

    notes = sub.add_parser("notes", help="generate release notes, update.json and telegram message")
    notes.add_argument("--metadata", required=True)
    notes.add_argument("--repo", required=True)
    notes.add_argument("--tag", required=True)
    notes.add_argument("--apk-asset", required=True)
    notes.add_argument("--changelog", default="docs/app_changes.md")
    notes.add_argument("--out-dir", default="release-bot")
    notes.set_defaults(func=notes_command)

    upstream = sub.add_parser("upstream", help="write files for the static upstream repository")
    upstream.add_argument("--metadata", required=True)
    upstream.add_argument("--upstream-dir", required=True)
    upstream.add_argument("--release-repo", required=True)
    upstream.add_argument("--tag", default="")
    upstream.add_argument("--apk-asset", default="")
    upstream.add_argument("--channel", default="stable")
    upstream.add_argument("--legacy-manifest", default="")
    upstream.set_defaults(func=upstream_command)

    claimed = sub.add_parser("claim-telegram", help="claim Telegram delivery before sending")
    claimed.add_argument("--upstream-dir", required=True)
    claimed.add_argument("--release-key", required=True)
    claimed.add_argument("--channel", default="stable")
    claimed.set_defaults(func=claim_telegram_command)

    notified = sub.add_parser("mark-notified", help="record successful Telegram delivery")
    notified.add_argument("--upstream-dir", required=True)
    notified.add_argument("--release-key", required=True)
    notified.add_argument("--channel", default="stable")
    notified.set_defaults(func=mark_notified_command)

    telegram = sub.add_parser("telegram", help="send generated release message to Telegram")
    telegram.add_argument("--message", required=True)
    telegram.add_argument("--photo", default="")
    telegram.set_defaults(func=telegram_command)

    args = parser.parse_args()
    try:
        args.func(args)
    except ReleaseBotError as exc:
        die(str(exc))


if __name__ == "__main__":
    main()
