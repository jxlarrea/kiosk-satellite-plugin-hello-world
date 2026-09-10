# Plugins

Plugins add optional features to Kiosk Satellite. The first SDK supports a floating window over the dashboard, plugin settings and plugin commands. Each plugin lives in its own public GitHub repository. This repository contains the Hello World template, SDK, build tools and plugin documentation.

## Install Hello World

1. Open **Settings > Plugins** on the kiosk or **Plugins** in remote admin and turn on **Enable Plugins**.
2. Choose **Add plugin**, paste its public GitHub repository URL and choose **Preview**.
3. Review the latest stable release's manifest, author, license, capabilities and README. Compatibility errors prevent installation.
4. Choose **Trust and install**. Installation downloads and verifies the reviewed release without running its code.
5. Enable **Hello World** using the switch on its entry row. Return to the dashboard to see its window.

Tap an installed plugin's entry row to open its subpage. Its settings, commands and saved README appear there. Enable, disable and uninstall controls stay on the entry row. Remote admin uses the same layout and supports direct links to plugin subpages.

The master switch explains that plugins add community developed features. A warning about plugin access appears below **Add plugin**. Plugin status refreshes when you open the page.

Drag the window by its title bar. **Say hello** updates its greeting count. Close dismisses the window without disabling the plugin. **Show window** in plugin settings opens it again. The Greeting setting changes its message. **Show window when enabled** controls whether it opens automatically.

The window stays inside Kiosk Satellite. It needs no Android permission to draw over other apps. The drawer, media player, screensaver, notifications and lockdown shield keep their existing priority above it. Touches outside the window reach the dashboard.

## Enable Plugins

The master **Enable Plugins** switch controls plugin execution and saves its state across app restarts. Turning it off closes plugin windows, revokes host callbacks and stops active sessions. Each plugin keeps its own enabled choice and settings. Selected plugins show **Paused** until the master switch is on again, then resume. Per-plugin switches and actions are unavailable while the master switch is off. You can still install, configure or uninstall plugins.

Plugins start off on kiosks with no installed plugins. Upgrading a kiosk that already has plugins preserves its existing behavior. Newly installed plugins still start individually disabled even when the master switch is on.

## Developer Tools

Choose **Install from ZIP** in the **Developer Tools** group to test a local plugin build. On the kiosk, select a file through Android's file picker. In remote admin, select a file from your computer. Review the filename and trust warning, then choose **Trust and install**. The package is validated before installation and starts disabled. Enable it from its entry row when ready.

ZIPs must be at most 4 MB and contain `kiosk-satellite-plugin.json`, `plugin.jar` and `LICENSE`. Local installs do not need a GitHub release or a separate checksum file. They do not have a repository README. To replace a local build that has run, disable the plugin and restart Kiosk first. Compatible settings are retained. Uninstall a repository-installed plugin before switching to a local build. Uninstalling removes its settings.

## Lifecycle and updates

Installed plugins start disabled. Enabling a plugin runs it immediately and saves that choice for the next app start. Disabling removes its window, revokes its host callbacks and asks it to stop. Removing also deletes its settings.

To replace a plugin that has run in the current process, disable it and restart Kiosk Satellite first. Then preview the same repository again and install the replacement release. The replacement starts disabled. Compatible settings are retained. A replacement with incompatible saved setting types is rejected, leaving the old package installed. Remove and reinstall if you want to discard those settings.

Each callback has a three-second deadline. A failure disables that plugin and records the error in settings. Threads that ignore interruption cannot be forcibly stopped safely, so a misbehaving native plugin may require an app restart. An unfinished automatic plugin startup causes enabled plugins to be disabled on the next launch. This recovery can also trigger if Android kills the process during the startup grace period.

## Trust and package checks

Native plugins run inside Kiosk Satellite with its app identity. They are trusted code and are not sandboxed. A plugin can access app data and Android permissions already granted to Kiosk. Its capability list describes the supported SDK features it uses and is not a security boundary.

The installer accepts ZIPs up to 4 MB and limits expanded content, including nested DEX content. It rejects unexpected paths, duplicate files, unsupported SDK versions and unsupported capabilities. Code is stored in private app storage with read-only DEX containers. Stored digests are checked before loading. A required SHA-256 from the release checksum file checks the downloaded package against the reviewed release. A hash does not establish who wrote a package.

Repository installation discovers the latest stable GitHub release. It reads the attached `kiosk-satellite-plugin.json` and package checksum, then reads `README.md` from the release tag's commit. Drafts and prereleases are excluded. The ZIP contains the same `kiosk-satellite-plugin.json`. Default-branch edits do not change the released documentation. Previews expire after 15 minutes. Installation checks the downloaded bytes and packaged manifest against the preview. Another repository cannot replace an installed plugin with the same ID without uninstalling it first. The reviewed README, release tag and source revision are saved for offline viewing.

Publisher signatures, private repositories, automatic updates, native library packaging, hardware APIs, ESPHome plugin entities and voice lifecycle subscriptions are not part of SDK 1. Plugin packages and settings are not included in Kiosk configuration export or fleet sync yet.

## Remote API

The existing authenticated command API exposes these commands:

| Command | Parameters |
| --- | --- |
| `getPluginState` | None. Returns the master `enabled` flag and `plugins` list |
| `setPluginsEnabled` | `enabled`: boolean master switch |
| `listPlugins` | None. Returns the installed plugin list |
| `previewPluginRepository` | `url`: public GitHub repository URL |
| `installPlugin` | `data`: base64-encoded plugin ZIP up to 4 MB, `trusted`: true |
| `installPluginRepository` | `previewId`: returned by preview, `trusted`: true |
| `enablePlugin` | `id` |
| `disablePlugin` | `id` |
| `removePlugin` | `id` |
| `configurePlugin` | `id`, `values`: complete settings object |
| `runPluginCommand` | `id`, `command`: manifest command ID |

Use `POST /api/commands/<command>` with the existing remote admin authentication. Each plugin's commands are addressed by its stable ID and command ID. They cannot replace core commands. Plugin management is not exposed to dashboard JavaScript.

## Licensing

The SDK and Hello World template use Apache-2.0. The application retains its license with an [additional permission for independent plugins](https://github.com/jxlarrea/kiosk-satellite/blob/main/PLUGIN-EXCEPTION.md). Each plugin must include a license for its own code and respect the licenses of its dependencies.
