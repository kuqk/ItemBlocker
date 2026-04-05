# Changelog

All notable changes to this project are documented here.

## [1.1.2] - 2026-03-31

### Added

- New default-first public command flow based on `block`, `edit`, `unblock`, `show`, `list`, and `preset`.
- Item `smithing` action as part of the normal item rule system.
- Simpler in-game help and usage output for the new command model.

### Changed

- One item in one target now uses one rule only.
- `block` now merges into an existing item rule instead of overwriting it.
- `edit` now updates only the fields provided by the command.
- Smithing restrictions now use `actions:smithing` instead of a separate netherite-only flow.
- README, example configs, language help, and plugin usage text were rewritten around the new release model.

### Removed

- Public `scope`-based item editing flow.
- Legacy public command variations such as `addhand` and separate netherite command usage from the supported workflow.
- Legacy target aliases and old item rule formats from the supported configuration model.

## [1.1.1] - 2026-03-29

### Added

- New built-in language files for German, Spanish, French and Italian.

### Changed

- Release notes for GitHub Releases, Modrinth and Hangar now come from `CHANGELOG.md`.
- Hangar publishing now skips already existing versions instead of failing on duplicate uploads.

## [1.1.0] - 2026-03-28

### Added

- New `default + presets` restriction layout.
- Full item action editing directly from chat with `add`, `addhand`, `edit` and `remove`.
- Per-item world scopes inside `default` and presets.
- Preset management commands for create, info, delete, description, reason and worlds.
- Per-action bypass permissions.
- `info` style inspection for configured items.
- Hopper protection through `InventoryMoveItemEvent`.
- GitHub Actions workflows for build, release and Modrinth automation.
- `.editorconfig` for cleaner formatting consistency.
- Richer English and Polish translation files.
- Full project changelog.

### Changed

- Reworked `blocked-items.yml` so it now contains only the always-present `default` section.
- Reworked `presets.yml` into the main place for grouped restrictions.
- Updated command flow so config editing and in-game editing use the same structure.
- Improved saved YAML formatting so command-written files stay clean and readable.
- Rewrote README and marketplace descriptions around the new layout and workflow.
- Refreshed in-game messages and help output for a cleaner presentation.

### Compatibility

- Requires Java 21+
- Supports Folia / Paper / Purpur / Spigot
- Targets the Minecraft 1.21.x API line
- CI metadata also tracks the latest stable release, including Minecraft Java Edition 26.1

## [1.0.1] - 2026-01-02

### Added

- Container Interaction Blocking: Prevented taking, putting, and moving blocked items within chests, dispensers, hoppers, and other containers.
