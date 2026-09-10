# Creating plugins

SDK 1 lets a plugin display one floating window, receive window events, expose commands and save settings. Start with Hello World and build a ZIP with `tools/build.py`.

## Build and check the SDK

The template includes the SDK interfaces, build tool and lifecycle tests. Copy the template into a separate repository for your plugin. Install Python 3, JDK 17 or newer and an Android SDK with a platform and build-tools. Set `ANDROID_HOME` and `JAVA_HOME` as needed.

```sh
python3 tools/test.py
python3 tools/build.py
python3 tools/check-sdk.py /path/to/kiosk-satellite
```

The SDK check compares this repository's interfaces and license with the application's copy. Keep the SDK as a compile-time dependency. The application supplies those interfaces at runtime.

The SDK, template, tooling and documentation use [Apache-2.0](../LICENSE). Contributors retain copyright to their work and contribute under the license of the component they change. Plugins may choose their own license and must retain any required third-party notices.

## Test a local build

Use **Plugin Manager > Developer Tools > Install from ZIP** on the kiosk or remote admin. Select the built ZIP from `dist/`, confirm that you trust the code and enable the installed plugin. The ZIP contains `kiosk-satellite-plugin.json`, `plugin.jar` and `LICENSE`. The standalone release manifest and checksum file are only needed when publishing to GitHub.

Local packages use the same 4 MB size limit, manifest validation and DEX checks as release packages. New plugins start disabled. To test another build, install the replacement ZIP directly. KS automatically stops running plugins and restores their enabled state after the update. A normal update does not require an app restart. Compatible settings are retained. A local ZIP cannot replace a plugin installed from GitHub. Uninstall that plugin first, which also deletes its settings.

## Repository and release

Each public GitHub repository contains one plugin and these root files:

```text
README.md                     Documentation shown before installation
kiosk-satellite-plugin.json   The single plugin manifest
LICENSE                       Plugin license included in the package
src/                          Plugin source
```

Build the plugin and attach these three files from `dist/` to a GitHub release:

```text
kiosk-satellite-plugin.json
<id>-<version>.zip
<id>-<version>.zip.sha256
```

The attached manifest is an exact copy of the manifest inside the ZIP. It describes the plugin without release URLs or checksums. The ZIP filename comes from its `id` and `version`. The checksum file contains one line in `sha256sum` format: the 64 lowercase hexadecimal hash, two spaces and the ZIP filename.

Commit the source, manifest and README before creating the release tag. Tags start with a letter or digit and contain only letters, digits, periods, underscores or hyphens, up to 151 characters. Publish the release as the latest stable release. KS uses GitHub's latest release endpoint. Drafts and prereleases are excluded.

KS reads the manifest and checksum attached to that release. It resolves the release tag to a commit and reads `README.md` at that commit, including the base for relative documentation links. Edits on the default branch do not change a released version's preview. The preview displays the manifest, compatibility and README without downloading or running plugin code.

A preview lasts 15 minutes. Installation downloads the selected release's ZIP, checks its required SHA-256 and compares every field of the packaged manifest with the reviewed manifest. A newer release published during review does not change the approved download. Repository metadata, release tag, commit and reviewed README are saved for offline use. README HTML is sanitized in remote admin. The device renders Markdown without executable HTML. Links and images require HTTPS.

The manifest is limited to 32 KB, the checksum file to 1 KB and the README to 128 KB. Private repositories, GitHub tokens, automatic updates and custom registries are not supported yet. A repository cannot silently replace an installed plugin with the same ID from another repository. Uninstall first to change its source.

Rebuilds use fixed ZIP timestamps, but toolchain changes can still change DEX output. Upload the manifest, ZIP and checksum from the same build. Keep the checksum outside the ZIP to avoid a circular checksum. Publish a new version when changing a release package.

## Package

A ZIP contains exactly these files at its root:

```text
kiosk-satellite-plugin.json
plugin.jar
LICENSE
```

`plugin.jar` contains `classes.dex` and optional additional `classesN.dex` files. It must not contain a copy of the SDK, Java class files or native libraries. The build tool compiles against the SDK and packages only your plugin classes. External dependencies need their own build integration and license review.

The complete ZIP, expanded files and expanded DEX content each have a 4 MB limit. The manifest has a 32 KB limit. SDK 1 permits up to eight installed plugins per kiosk.

## Manifest

See [kiosk-satellite-plugin.json](../kiosk-satellite-plugin.json) for a complete example.

| Field | Meaning |
| --- | --- |
| `schemaVersion` | Must be `1` |
| `apiVersion` | Must be `1` |
| `id` | Stable lowercase ID with optional hyphens, at most 64 characters |
| `name` | Display name, at most 80 characters |
| `version` | `major.minor.patch` with optional prerelease suffix |
| `minAndroidSdk` | Android API level, at least `24` |
| `entryClass` | Public class implementing `KioskPlugin` with a public no-argument constructor |
| `description` | Plain text, at most 1000 characters |
| `author` | Author name, at most 120 characters |
| `license` | License identifier, at most 120 characters |
| `capabilities` | Array of required SDK capabilities. Only `overlay` is supported |
| `settings` | Up to 20 settings |
| `commands` | Up to 20 named commands |

A setting declares `key`, `title`, `type` and `default`. Types are `string` and `boolean`. Strings allow up to 512 characters. Keys start with a letter and contain letters, digits or underscores. A command declares `id` and `title`. Command IDs start with a lowercase letter and contain letters or digits. IDs must be unique within their respective lists.

Settings and commands are scoped to the plugin ID. Do not change that ID after publication. Increasing a plugin's own version does not increase the SDK version.

## Lifecycle

Implement `me.jxl.kiosk.plugins.KioskPlugin`:

| Callback | Responsibility |
| --- | --- |
| `start(host, settings)` | Save the host handle and acquire resources |
| `configure(settings)` | Apply the complete validated configuration |
| `execute(command, arguments)` | Run a declared command. SDK 1 passes an empty arguments map |
| `onEvent(event, payload)` | Handle a window event. SDK 1 passes an empty payload map |
| `stop()` | Release timers, threads and resources |

Callbacks run serially on a worker dedicated to the plugin. They must finish within three seconds. The host disables a plugin after a callback error or timeout. Keep long work asynchronous and honor interruption. Host calls are ignored after the plugin stops. A timed-out thread can keep running if it ignores interruption, since this runtime does not isolate plugin code.

The host rejects settings with unknown keys or incorrect types. Defaults fill missing keys. First-time installation does not run plugin code and leaves the plugin disabled. Updates automatically stop the old session and restart the replacement if the plugin was enabled and the master switch is on. Disabled plugins stay disabled. Updates while the master switch is off retain the enabled choice without running code. The host calls `start` after explicit enable or at app startup for an enabled plugin while the master **Enable Plugins** switch is on. Turning the master switch off calls `stop` and revokes host callbacks without changing the plugin's saved enabled choice or settings. Turning it on starts the selected plugins again. An off master switch prevents startup and execution across app restarts.

To replace a loaded plugin, preview the same repository again and install its replacement release. KS stops the old session and restores its enabled state automatically. A normal update does not require an app restart. Compatible settings are retained. Removing a plugin deletes its saved settings.

## Floating window

The host exposes these methods:

```java
host.showWindow("Hello World", "Your message", "Say hello");
host.hideWindow();
host.log("A short diagnostic message");
```

`showWindow` creates or updates the plugin's one window. Updating its text keeps its drag position. Titles and button labels allow up to 80 characters. Messages allow up to 4096 characters. An empty button label hides the action button. All text is rendered as plain text by Flutter.

The action button sends `window.action`. Closing the window sends `window.closed`. Closing does not disable the plugin. Calling `hideWindow` does not send a close event, so plugins can manage visibility without a callback loop.

The window floats over the dashboard and is draggable by its title bar. Other kiosk surfaces, including the drawer and screensaver, can cover it. It does not create an Android system overlay. No dashboard DOM access, custom HTML or arbitrary Flutter widget loading is provided.

## Trust and compatibility

A plugin executes inside Kiosk Satellite with the application's identity. This is not a sandbox. The capabilities array identifies SDK requirements and does not restrict arbitrary Java code. Do not copy app implementation classes into your plugin or rely on internal classes discovered through reflection.

SDK 1 does not expose app commands, Home Assistant credentials, hardware services, ESPHome entities or voice events through its API. Those APIs can be designed as later versioned capabilities. Native library packaging and automatic updates are future work.

Publish source code alongside release packages. A checksum detects changed bytes but does not authenticate a publisher. Users must trust the repository author. Plugin authors remain responsible for all licenses and notices included in their packages.
