# Changelog

All notable changes to this project are documented here.

## [1.1.0] - 2026-03-28

### Added

- New `default + presets` restriction layout.
- Full item action editing directly from chat with `add`, `addhand`, `item edit` and `item delete`.
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
- Targets Minecraft 1.21.x and 26.1

## [1.0.1] - 2026-01-02

### Added

- Container Interaction Blocking: Prevented taking, putting, and moving blocked items within chests, dispensers, hoppers,
and other containers.
