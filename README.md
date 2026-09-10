# Hello World for Kiosk Satellite

A simple plugin template that opens a draggable greeting window over your Home Assistant dashboard. Change the greeting, count button presses and reopen the window from the plugin's settings page.

## Install

This plugin requires a Kiosk Satellite build with SDK 1 plugin support and Android 7.0 or newer.

1. Open **Settings > Plugins** on the kiosk or **Plugins** in remote admin.
2. Choose **Add plugin**, paste this repository's GitHub URL and choose **Preview**.
3. Read the manifest and this README. Choose **Trust and install** if you trust the code.
4. Enable **Hello World** from its entry row. Return to the dashboard to see the window.

Tap the Hello World entry to open its subpage. **Greeting** changes the message. **Show window when enabled** controls whether it opens automatically. Save settings to apply changes. **Show window** reopens a dismissed window and **Hide window** closes it. The floating **Say hello** button increments its counter. Disable or uninstall using the controls on its entry row.

The plugin runs trusted code inside Kiosk Satellite. It can access app data and granted Android permissions. Review the source before enabling it.

## Build and test

Install Python 3, JDK 17 or newer and an Android SDK with build-tools. Set `ANDROID_HOME` and `JAVA_HOME` as needed.

```sh
python3 tools/test.py
python3 tools/build.py
```

The build creates `dist/hello-world-1.0.0.zip`, its SHA-256 file and the root `kiosk-plugin.json` repository descriptor. The SDK sources in `sdk/` are compile-time dependencies. Their classes are not included in the package.

To test an installable build, publish a release in your plugin repository and install it through **Plugins > Add plugin**. To replace a plugin that has run, disable it and restart Kiosk before installing the replacement.

## Publish your own plugin

Create a separate repository from this template. Change `manifest.json`, the Java class/package and this README. Keep the plugin ID stable after publication.

1. Build and test the release.
2. Commit the source, README and generated `kiosk-plugin.json` to the default branch.
3. Create the release tag named in `download.tag` and upload the exact ZIP from `dist/` as the asset named in `download.asset`.
4. Share the public repository URL. Each repository contains one plugin.

Rebuilding with different toolchains can change the package hash. Publish the exact ZIP used to generate the descriptor. The descriptor contains the runtime manifest and package checksum. Keeping it outside the ZIP avoids a circular checksum.

See the [SDK documentation](https://github.com/jxlarrea/kiosk-satellite-plugins/blob/main/docs/creating-plugins.md) for the API and package contract.

## License

[Apache-2.0](LICENSE). Kiosk Satellite has its own application license.
