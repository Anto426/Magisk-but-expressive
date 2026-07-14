# Prebuilt native releases

This directory stores immutable Magisk Alpha native binaries outside the Android
assets tree. Gradle selects the valid release with the highest manifest
`versionCode` automatically.

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

`manifest.json` is the source of truth for the Magisk version, version code,
stub version, supported ABIs, file sizes and SHA-256 hashes. Every ABI directory
must contain exactly the six files shown above. A release can be pinned with
`-Pmagisk.nativeReleaseId=<directory>` or
`-Pmagisk.nativeBinariesDir=<release-directory>`.
