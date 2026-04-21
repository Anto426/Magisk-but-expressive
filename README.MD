## <img src="./asset/icon.gif" width="44px" /> About

This repository is a fork of the official Magisk project.

Upstream: https://github.com/topjohnwu/Magisk

```sh
magisk@android:~$ whoami
Magisk Android customization suite

magisk@android:~$ echo "platform"
Android 6.0+ (Marshmallow and above)

magisk@android:~$ echo "repo"
https://github.com/Anto426/Magisk
```

Magisk is an open-source suite for Android customization with systemless root, module support, boot image tooling, and process-level runtime extensions.

<p align="center">
  <img src="./asset/divider.gif" width="440" height="40" />
</p>

## <img src="./asset/icon2.gif" width="48px" /> Highlights

- **MagiskSU**: root access management for apps.
- **Magisk Modules**: systemless modifications on read-only partitions.
- **MagiskBoot**: advanced unpack/repack support for Android boot images.
- **Zygisk**: code injection in app processes via Zygote.

<p align="center">
  <img src="./asset/divider.gif" width="440" height="40" />
</p>

## Downloads

[GitHub Releases](https://github.com/anto426/Magisk/releases).

<p align="center">
  <img src="./asset/divider.gif" width="440" height="40" />
</p>

## Useful Links

- [Installation Instructions](https://topjohnwu.github.io/Magisk/install.html)
- [Building and Development Docs](https://topjohnwu.github.io/Magisk/build.html)
- [Magisk Documentation](https://topjohnwu.github.io/Magisk/)
- [Zygisk Module Sample](https://github.com/topjohnwu/zygisk-module-sample)

<p align="center">
  <img src="./asset/divider.gif" width="440" height="40" />
</p>

## <img src="./asset/icon3.gif" width="48px" /> Building And Development

### Setup Environment

Supported platforms:

- Linux x64
- macOS x64 (Intel)
- macOS arm64 (Apple Silicon)
- Windows x64

Windows only: enable Developer Mode. Symbolic link support is required.

Install Python 3.8+:

- On Unix, install `python3` with your preferred package manager.
- On Windows, install the latest Python from the official website.
- On Windows, make sure `Add Python to PATH` is enabled during installation.
- Optional on Windows: install `colorama` with `pip install colorama`.

Install Git:

- On Unix, install `git` with your preferred package manager.
- On Windows, install the latest Git from the official website.
- On Windows, enable symbolic links during Git installation.

Install Android Studio and complete the initial setup wizard.

Set `ANDROID_HOME` to your Android SDK path (available in Android Studio settings).

Set up JDK:

- Recommended: set `ANDROID_STUDIO` to your Android Studio install path; build scripts will auto-detect and use the bundled JDK.
- Alternative: set up JDK 21 manually.

Clone sources:

```sh
git clone --recurse-submodules https://github.com/Anto426/Magisk.git
```

Install NDK via build script:

```sh
./build.py ndk
```

### Building

Build everything and generate the final Magisk APK:

```sh
./build.py all
```

Build specific components by running:

```sh
./build.py
```

Use `-h` for action-level help:

```sh
./build.py binary -h
```

Configure build behavior with `config.prop` (`config.prop.sample` is provided).

### IDE Support

Kotlin, Java, C++, and C sources are supported by Android Studio out of the box. Open this repository directly as an Android Studio project.

For Rust development, see the next section.

Before touching native code, run:

```sh
./build.py binary
```

Some generated native files are created during the build process.

### Developing Rust

Install `rustup` (official Rust toolchain manager). The Magisk NDK package (ONDK), installed via `./build.py ndk`, already includes a complete Rust toolchain for normal Magisk builds.

For a better Rust development workflow, link ONDK's Rust toolchain in rustup:

```sh
# Link the ONDK toolchain as "magisk"
rustup toolchain link magisk "$ANDROID_HOME/ndk/magisk/toolchains/rust"

# Set it as default
rustup default magisk
```

For VS Code: install the `rust-analyzer` extension.

For JetBrains IDEs (RustRover or IntelliJ Rust plugin), do this extra setup:

```sh
# Install nightly (used for IDE compatibility only)
rustup toolchain install nightly

# Add components that are also bundled in ONDK
rustup +nightly component add rust-src clippy
```

Create a wrapper Cargo bin directory:

```sh
# Example path: ~/.cargo/wrapper
./build.py rustup ~/.cargo/wrapper
```

Then set **Settings > Rust > Toolchain location** to that wrapper path.

### Signing And Distribution

In release builds, the certificate used to sign the Magisk APK is used by Magisk's root daemon as a trusted reference. If another Magisk app is signed with a different key, it can be rejected and force-uninstalled to protect users from unverified packages.

For Magisk development, switch to an official debug build and reinstall Magisk to disable signature enforcement during development.

To distribute your own Magisk builds signed with your own keys, set signing values in `config.prop`.

For key generation details, see Google's Android app signing documentation.

<p align="center">
  <img src="./asset/divider.gif" width="440" height="40" />
</p>

## Bug Reports

Only bug reports from debug builds are accepted.

- For installation issues, upload both the boot image and install logs.
- For Magisk issues, upload boot `logcat` or `dmesg`.
- For app crashes, capture and upload `logcat` at crash time.

<p align="center">
  <img src="./asset/divider.gif" width="440" height="40" />
</p>

## Translation Contributions

Default string resources for the Magisk app and stub APK:

- `app/core/src/main/res/values/strings.xml`
- `app/stub-res/src/main/res/values/strings.xml`

Add translations in:

`[module]/src/main/res/values-[lang]/strings.xml`

<p align="center">
  <img src="./asset/divider.gif" width="440" height="40" />
</p>

## Contributors

- [topjohnwu](https://github.com/topjohnwu) - original Magisk author.
- [Anto426](https://github.com/Anto426) - fork maintainer.

<p align="center">
  <img src="./asset/divider.gif" width="440" height="40" />
</p>

## <img src="./asset/icon4.gif" width="48px" /> License

```text
Magisk, including all git submodules are free software:
you can redistribute it and/or modify it under the terms of the
GNU General Public License as published by the Free Software Foundation,
either version 3 of the License, or (at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program. If not, see <http://www.gnu.org/licenses/>.
```
