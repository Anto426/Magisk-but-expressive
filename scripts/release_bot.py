#!/usr/bin/env python3
from __future__ import annotations

import argparse
import json
import os
import re
import sys
import urllib.parse
import urllib.request
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
KNOWN_ABIS = ("armeabi-v7a", "arm64-v8a", "x86", "x86_64", "riscv64")
REQUIRED_BINARIES = ("magiskboot", "magiskinit", "magiskpolicy", "magisk", "init-ld", "busybox")


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


def has_required_bins(abi_dir: Path) -> bool:
    return all((abi_dir / name).exists() for name in REQUIRED_BINARIES)


def read_native_binaries(root: Path) -> tuple[dict, list[str]]:
    manifest_path = root / "manifest.json"
    manifest = read_json(manifest_path) if manifest_path.exists() else {}
    version = str(manifest.get("version") or root.name)
    version_code = str(manifest.get("versionCode") or manifest.get("version_code") or "")
    release_id = str(manifest.get("releaseId") or manifest.get("release_id") or "")
    if not release_id:
        release_id = root.name

    abis = [abi for abi in KNOWN_ABIS if (root / abi).is_dir()]
    missing = [abi for abi in abis if not has_required_bins(root / abi)]
    if not abis:
        die(f"no ABI folders found in {root}")
    if missing:
        die(f"missing required binaries in ABI folders: {', '.join(missing)}")

    manifest.setdefault("version", version)
    if version_code:
        manifest.setdefault("versionCode", int(version_code) if version_code.isdigit() else version_code)
    manifest.setdefault("releaseId", release_id)
    manifest.setdefault("abiList", abis)
    return manifest, abis


def select_native_dir(base_dir: Path, release_id: str | None) -> Path:
    base_dir = base_dir if base_dir.is_absolute() else ROOT / base_dir
    if release_id:
        native_dir = base_dir / release_id
        if not native_dir.is_dir():
            die(f"native release not found: {native_dir}")
        return native_dir

    candidates = [path for path in base_dir.iterdir() if path.is_dir()]
    candidates = [path for path in candidates if any((path / abi).is_dir() for abi in KNOWN_ABIS)]
    if not candidates:
        die(f"no local native releases found in {base_dir}")

    def sort_key(path: Path) -> tuple[int, float, str]:
        manifest_path = path / "manifest.json"
        manifest = read_json(manifest_path) if manifest_path.exists() else {}
        code = manifest.get("versionCode") or manifest.get("version_code") or 0
        try:
            code_int = int(code)
        except (TypeError, ValueError):
            code_int = 0
        return code_int, path.stat().st_mtime, path.name

    return max(candidates, key=sort_key)


def compose_release_version(native_version: str, suffix: str) -> str:
    suffix = suffix.strip()
    if not suffix:
        return native_version
    if suffix.startswith(("-", "+", ".")):
        return f"{native_version}{suffix}"
    return f"{native_version}-{suffix}"


def release_version_code(native_code: str, suffix: str, offset: str) -> str:
    if not suffix.strip() and not offset.strip():
        return native_code
    if offset.strip():
        bump = int(offset)
    else:
        match = re.search(r"(\d+)$", suffix)
        bump = int(match.group(1)) if match else 0
    return f"{bump:05d}"


def update_build_props(
    native_dir: Path,
    manifest: dict,
    abis: list[str],
    release_version: str,
    release_code: str,
) -> dict[str, str]:
    rel_native_dir = native_dir.relative_to(ROOT).as_posix()
    native_version = str(manifest.get("version") or manifest.get("releaseId"))
    native_code = str(manifest.get("versionCode") or manifest.get("version_code") or "")
    stub_version = str(manifest.get("stubVersion") or manifest.get("stub_version") or "")
    abi_list = ",".join(abis)

    set_property(ROOT / "app" / "gradle.properties", "magisk.nativeBinariesDir", rel_native_dir)
    if native_code:
        set_property(ROOT / "app" / "gradle.properties", "magisk.versionCode", native_code)
    if stub_version:
        set_property(ROOT / "app" / "gradle.properties", "magisk.stubVersion", stub_version)
    set_property(ROOT / "app" / "gradle.properties", "magisk.mbeVersionName", release_version)
    if release_code:
        set_property(ROOT / "app" / "gradle.properties", "magisk.mbeVersionCode", release_code)
    set_property(ROOT / "config.prop", "version", native_version)
    set_property(ROOT / "config.prop", "abiList", abi_list)

    return {
        "native_dir": rel_native_dir,
        "release_id": str(manifest["releaseId"]),
        "native_version": native_version,
        "native_version_code": native_code,
        "version": release_version,
        "version_code": release_code,
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
        f"# {release_title}",
        "",
        notes,
        "",
        "## Build",
        f"- MBE version: `{version}`",
    ]
    if version_code:
        release_md.append(f"- MBE version code: `{version_code}`")
    release_md.extend([
        f"- Native binaries: `{metadata.get('release_id', 'unknown')}`",
        f"- Magisk core version: `{metadata.get('native_version', '')} ({metadata.get('native_version_code', '')})`",
        f"- ABI: `{metadata.get('abis', '')}`",
    ])
    (out_dir / "release.md").write_text("\n".join(release_md).rstrip() + "\n", encoding="utf-8")

    native_code = metadata.get("native_version_code", "")
    update = {
        "magisk": {
            "version": version,
            "versionCode": int(version_code) if str(version_code).isdigit() else -1,
            "clientVersionCode": int(native_code) if str(native_code).isdigit() else -1,
            "link": apk_url,
            "note": note_url,
        }
    }
    write_json(out_dir / "update.json", update)

    telegram = f"{release_title}\n\n{notes}\n\nAPK: {apk_url}"
    if len(telegram) > 3900:
        telegram = telegram[:3800].rstrip() + "\n\n... continua nelle release notes.\n" + apk_url
    (out_dir / "telegram.md").write_text(telegram, encoding="utf-8")
    github_output({"title": release_title, "update_url": note_url})


def get_current_mbe_version() -> tuple[str, str]:
    props_path = ROOT / "app" / "gradle.properties"
    name = ""
    code = ""
    if props_path.exists():
        content = props_path.read_text(encoding="utf-8")
        for line in content.splitlines():
            line = line.strip()
            if line.startswith("magisk.mbeVersionName="):
                name = line.split("=", 1)[1].strip()
            elif line.startswith("magisk.mbeVersionCode="):
                code = line.split("=", 1)[1].strip()
    return name, code


def prepare_command(args: argparse.Namespace) -> None:
    native_dir = select_native_dir(Path(args.native_dir), args.native_release_id or None)
    manifest, abis = read_native_binaries(native_dir)
    native_version = str(manifest.get("version") or manifest.get("releaseId"))
    native_code = str(manifest.get("versionCode") or manifest.get("version_code") or "")

    release_suffix = args.release_suffix.strip()
    version_code_offset = args.version_code_offset.strip()

    if not release_suffix:
        current_name, current_code = get_current_mbe_version()
        if current_name and current_code:
            release_version = current_name
            release_code = current_code
            prefix = f"{native_version}-"
            if current_name.startswith(prefix):
                release_suffix = current_name[len(prefix):]
            else:
                release_suffix = current_name
        else:
            release_suffix = "mbe.1"
            release_version = compose_release_version(native_version, release_suffix)
            release_code = release_version_code(native_code, release_suffix, version_code_offset)
    else:
        release_version = compose_release_version(native_version, release_suffix)
        release_code = release_version_code(native_code, release_suffix, version_code_offset)
    
    args.release_suffix = release_suffix
    args.version_code_offset = version_code_offset

    outputs = update_build_props(native_dir, manifest, abis, release_version, release_code)
    outputs["release_suffix"] = args.release_suffix
    metadata_path = ROOT / "release-bot" / "metadata.json"
    write_json(metadata_path, outputs)
    outputs["metadata"] = str(metadata_path.relative_to(ROOT).as_posix())
    github_output(outputs)
    print(json.dumps(outputs, indent=2))


def telegram_command(args: argparse.Namespace) -> None:
    token = os.environ.get("TELEGRAM_BOT_TOKEN")
    chat_id = os.environ.get("TELEGRAM_CHAT_ID")
    if not token or not chat_id:
        die("TELEGRAM_BOT_TOKEN and TELEGRAM_CHAT_ID are required")
    text = Path(args.message).read_text(encoding="utf-8")
    data = urllib.parse.urlencode({
        "chat_id": chat_id,
        "text": text,
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
    prepare.add_argument("--native-dir", default="asset/binaries/releases")
    prepare.add_argument("--native-release-id", default="")
    prepare.add_argument("--release-suffix", default="")
    prepare.add_argument("--version-code-offset", default="")
    prepare.set_defaults(func=prepare_command)

    notes = sub.add_parser("notes", help="generate release notes, update.json and telegram message")
    notes.add_argument("--metadata", required=True)
    notes.add_argument("--repo", required=True)
    notes.add_argument("--tag", required=True)
    notes.add_argument("--apk-asset", required=True)
    notes.add_argument("--changelog", default="docs/app_changes.md")
    notes.add_argument("--out-dir", default="release-bot")
    notes.set_defaults(func=notes_command)

    telegram = sub.add_parser("telegram", help="send generated release message to Telegram")
    telegram.add_argument("--message", required=True)
    telegram.set_defaults(func=telegram_command)

    args = parser.parse_args()
    args.func(args)


if __name__ == "__main__":
    main()
