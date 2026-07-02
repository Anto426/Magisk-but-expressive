# Release binaries

This directory stores extracted Magisk/Giada native binaries by release.

Each release uses this layout:

```text
<version>-<versionCode>/
  manifest.json
  arm64-v8a/
    busybox
    init-ld
    magisk
    magiskboot
    magiskinit
    magiskpolicy
  armeabi-v7a/
  x86/
  x86_64/
```

The `manifest.json` file records the source APK metadata and SHA-256 for every extracted binary.
The build cache remains `native/out`, while this directory is intended to be tracked in Git.
