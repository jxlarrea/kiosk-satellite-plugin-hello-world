# Changelog

## Unreleased

### Changed

- Document the entry row update check and info modal, automatic stop and resume during updates and rollback after failed activation.

- Rename the settings feature to Plugin Manager and hide its follow-up controls while the master switch is off.

### Added

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
