# Changelog

## Unreleased

### Added

- Add SDK 1 native overlays: with the `overlay` capability, `host.showOverlay(key, spec, factory)` shows a plugin's own Android view over the kiosk, from a small anchored bar to a full screen panel, and `hideOverlay(key)` removes it. `OverlaySpec` sets the anchor, size, inset, whether back closes it, whether it draws on top in the voice overlay's slot over the screensaver and camera views and whether touches pass through. `KsTheme` passes the KS colors, radii, spacing and Rubik typeface, with card and pill button helpers, and restyles live overlays when the theme changes. Back sends `overlay.closed`. Add Show top bar, Hide top bar, Show full screen overlay, Show edge glow and Hide edge glow actions that demonstrate a bar, a full screen card and a visual-only animated glow on top, and an overlay guide.

- Document the new limit of 50 settings per plugin, up from 20.

- Compile the SDK against the Android platform in `tools/build.py` and `tools/test.py`, since the overlay classes use Android views.

- Add the SDK 1 `getIntercomState` read and `intercom.state` event, which follow this kiosk's intercom with the same state, other kiosk and Do not disturb values as its ESPHome entities. Add an Intercom demo group that shows them under Readings while Show intercom state is on. Document the states and how long ended and missed hold.

- Add the SDK 1 `getVoiceState` read and `voice.state` event, which follow this kiosk's Voice Satellite turns through idle, listening, processing and responding, realtime conversations included. Add a Voice Satellite demo group that shows the state under Readings while Show Voice Satellite state is on. Document the states, the read and event pairing and the fallback for older Kiosk Satellite versions.

- Add SDK 1 gesture triggers: declare `triggers` in the manifest and call `host.fireTrigger(id)` so a gesture mapped to the trigger runs its action. The Hardware keys demo declares a Hardware key trigger and fires it for each press. Document the manifest field, the four fires per second limit, the errors and the fallback for older Kiosk Satellite versions.

- Document the SDK 1 `setVolume` control and the `channel` argument of `getVolume`, covering the master, media, assistant and intercom volumes, and the `device.volume` event now firing for all four.

- Add a Hardware keys demo group that subscribes to `device.key` while Show hardware keys is on and shows the last key, its scan code and a press count under Readings. Document the SDK 1 `device.key` event, its payload, filtering of typed text and its queue limit.

- Add a Status tile demo group that publishes a Hello World demo tile on the Remote Admin Overview Status panel, with a toggle, a level selection and a text setting. Document the SDK 1 status tile API, its limits, lifecycle and the `getPluginStatusTiles` remote command.

- Keep each demo together using manifest display groups. Chart demo settings are followed by the chart and Chart readings. Home Assistant and Shizuku each have dedicated reading groups below their settings.

- Add a Home Assistant demo with the shared entity picker and live state and detail readings. Document SDK 1 entity reads, subscriptions, availability, reconnection and lifecycle rules.

- Document the expanded SDK 1 device snapshot with RAM, internal storage, IP addresses, battery and display readings, including units, null values and polling guidance.

- Document the SDK 1 device codename, board, manufacturer and ordered ABI fields available through `getDeviceInfo` without root or Shizuku.

- Add a Logo size slider to the DVD screensaver demo, from 50% to 200% with the original size at 100%. Size changes save automatically and keep the logo inside its available viewport.

- Bundle screensaver HTML, CSS, JavaScript, images and fonts under assets and load them on demand through the SDK. Move the DVD demo to separate files, retain its color settings and document asset validation and package limits. Raise the inline HTML limit to 512 KiB.

- Add SDK 1 screensaver rendering and a bouncing DVD Logo demo with Logo color and Background color settings. Document stock settings, lifecycle, document limits and fleet behavior.

- Add an opt-in Shizuku demo group to Hello World with process identity, Android version and kernel version diagnostics. Publish results as live readings, handle unavailable access and ignore stale callbacks without affecting the greeting or chart.

- Add optional SDK 1 Shizuku access, documentation and a separate read-only identity example.

- Demonstrate live readings in KS with numeric values, confirmed states, a history sample count and multiline text. Document automatic display of existing SDK 1 entities without an ESPHome connection.

- Document SDK 1 dashboard URL reads and change notifications, with component redaction and an updated read-only example.

- Add SDK 1 writable switches with boolean commands and confirmed states. Hello World exposes its chart toggle and the entity guide documents the protocol limits and migration steps.

### Changed

- Update the plugin description to explain that Hello World showcases SDK capabilities and serves as a starting point for other plugins.

## 1.2.0

### Fixed

- Take release package versions from the GitHub tag automatically. Generate matching manifests and package filenames without requiring a source manifest version bump. Local builds can use `--version` too.

- Handle dotted Android platform directories such as `android-37.0` without crashing. Release builds explicitly select Android 35 and tests cover mixed platform installations.

### Changed

- Lead the README with the plugin SDK, documentation and getting started steps before introducing the Hello World template.

- Document automatic setting saves and the on-device text edit dialog.

- Document the entry row update check and info modal, automatic stop and resume during updates and rollback after failed activation.

- Rename the settings feature to Plugin Manager and hide its follow-up controls while the master switch is off.

### Added

- Support SDK 1 grouped bar charts in regular and mini layouts and add a Chart type selector to Hello World.

- Add SDK 1 numeric, text and binary sensors plus writable selects, with validated metadata, unknown readings and confirmed select callbacks.
- Demonstrate all four entity types in Hello World and document repository update steps.

- Add SDK 1 regular and mini chart publication and removal with bounded history, sample inspection and an existing-repository migration guide.
- Expand Hello World to showcase every settings control, groups and a live simulated chart with a sampler that stops with its session.

- Consolidate all features into the first public SDK 1. Document KS reads, passive events and transient controls and add a buildable screen and screensaver observer.
- Add a release-triggered GitHub Actions build and document required Actions asset publication and developer-only ZIP testing.

- Document reusable plugin actions for gestures, optional drawer shortcuts and optional Home Assistant buttons.

- Document SDK 1 native files, rich settings, runtime status and RGB entities and vendor host interfaces.

- Document the persistent Enable Plugins master switch, paused plugin behavior and state commands.
- Document local ZIP testing through the Developer Tools group on the kiosk and remote admin.

## 1.0.1

### Changed

- Use `kiosk-satellite-plugin.json` as the single manifest in the repository, release assets and plugin ZIP.
- Discover plugins through the latest stable GitHub release with a separate checksum and README from the release tag.
- Consolidated plugin installation and SDK documentation in the Hello World repository.
- Included the SDK consistency check alongside the template's build and test tools.

## 1.0.0

- Floating Hello World window with a configurable greeting and button counter.
- Settings and commands for the Kiosk Satellite plugin subpage.
- Standalone source repository with compile-time SDK, tests and release descriptor generation.
- GitHub repository installation with a manifest and README preview.
