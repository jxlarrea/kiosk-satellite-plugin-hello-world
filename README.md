# Hello World for Kiosk Satellite

A simple plugin template that opens a draggable greeting window over your Home Assistant dashboard. Change the greeting, count button presses and reopen the window from the plugin's settings page.

## Install

This plugin requires a Kiosk Satellite build with SDK 1 plugin support and Android 7.0 or newer.

1. Open **Settings > Plugin Manager** on the kiosk or **Plugin Manager** in remote admin and turn on **Enable Plugins**.
2. Choose **Add plugin**, paste this repository's GitHub URL and choose **Preview**.
3. Read the manifest and this README. Choose **Trust and install** if you trust the code.
4. Enable **Hello World** from its entry row. Return to the dashboard to see the window.

The master **Enable Plugins** switch pauses all plugins and closes their windows. It keeps each plugin's enabled choice and settings, so turning it back on resumes the selected plugins.

Tap the Hello World entry to open its subpage. **Greeting** changes the message. **Show window when enabled** controls whether it opens automatically. Save settings to apply changes. **Show window** reopens a dismissed window and **Hide window** closes it. The floating **Say hello** button increments its counter. Disable or uninstall using the controls on its entry row.

The plugin runs trusted code inside Kiosk Satellite. It can access app data and granted Android permissions. Review the source before enabling it.

## Build and test

Install Python 3, JDK 17 or newer and an Android SDK with build-tools. Set `ANDROID_HOME` and `JAVA_HOME` as needed.

```sh
python3 tools/test.py
python3 tools/build.py
```

The build creates `dist/hello-world-1.0.1.zip`, its `.zip.sha256` checksum file and a copy of `kiosk-satellite-plugin.json` in `dist/`. The ZIP contains that same manifest. The SDK sources in `sdk/` are compile-time dependencies. Their classes are not included in the package.

To test a local build, open **Plugin Manager > Developer Tools > Install from ZIP** on the kiosk or remote admin and select the ZIP from `dist/`. Confirm that you trust the code, then enable the plugin from its entry row. No GitHub release is needed. Install the replacement ZIP directly. KS stops the plugin automatically and restores its enabled state after the update. A normal update does not require an app restart. Compatible settings are retained. If the existing plugin was installed from GitHub, uninstall it before switching to a local build. Uninstalling deletes its settings.

## Publish your own plugin

Create a separate repository from this template. Change `kiosk-satellite-plugin.json`, the Java class/package and this README. Keep the plugin ID stable after publication.

1. Build and test the release.
2. Commit the source, README and `kiosk-satellite-plugin.json`.
3. Create a GitHub release tagged at that commit. Attach `kiosk-satellite-plugin.json`, `<id>-<version>.zip` and `<id>-<version>.zip.sha256` from `dist/`. Publish it as the latest stable release.
4. Share the public repository URL. Each repository contains one plugin.

KS discovers the latest stable GitHub release, previews its attached manifest and reads the README from the tagged commit. It downloads the ZIP only after confirmation and verifies the checksum and packaged manifest. Drafts and prereleases are excluded.

Rebuilding with different toolchains can change the package hash. Upload the three files from the same build. The checksum stays outside the ZIP to avoid a circular checksum.

## Plugin documentation

This repository is the home for all Kiosk Satellite plugin documentation and the self-contained Hello World template.

- [Installing and managing plugins](docs/installing-plugins.md): installation, settings, trust, updates and the remote API.
- [Creating plugins](docs/creating-plugins.md): SDK interfaces, lifecycle, package format, build tools and publishing.

Use `python3 tools/check-sdk.py /path/to/kiosk-satellite` to verify that the template's SDK interfaces match the application.

## License

[Apache-2.0](LICENSE). Kiosk Satellite has its own application license.
