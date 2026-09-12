# Changelog

## Unreleased

### Added

- Add SDK 1 screensaver rendering and a bouncing DVD Logo demo with Logo color and Background color settings. Document stock settings, lifecycle, document limits and fleet behavior.

- Add an opt-in Shizuku demo group to Hello World with process identity, Android version and kernel version diagnostics. Publish results as live readings, handle unavailable access and ignore stale callbacks without affecting the greeting or chart.

- Add optional SDK 1 Shizuku access, documentation and a separate read-only identity example.

- Demonstrate live readings in KS with numeric values, confirmed states, a history sample count and multiline text. Document automatic display of existing SDK 1 entities without an ESPHome connection.

- Document SDK 1 dashboard URL reads and change notifications, with component redaction and an updated read-only example.

- Add SDK 1 writable switches with boolean commands and confirmed states. Hello World exposes its chart toggle and the entity guide documents the protocol limits and migration steps.

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
