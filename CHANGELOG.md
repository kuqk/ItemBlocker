# Changelog

All notable changes to this project are documented here.

## [2.0.0] - 2026-10-30

ItemBlocker 2.0 is a rewrite. Your 1.x files are converted automatically on the first start; see Upgrading below.

### Added

- Hex colors (`&#rrggbb`) in messages.
- Custom languages: add `lang/<code>.yml` and set `language: <code>`.
- `/ib show` lists every preset that blocks an item, enchantment or potion effect.
- Placing boats on water, minecarts, armor stands, item frames, paintings and end crystals is covered by the `place` action, using the server's placement events instead of guessing from item names.
- Emptying a bucket is covered by the `place` action.
- Hoppers picking up item entities are covered by the `hopper` action.
- Anvils no longer produce items with blocked enchantments.

### Changed

- All rules live in `rules.yml`. The 1.x `default` section is now the `default` preset, which works like any other preset.
- Messages live in `lang/<language>.yml` and are fully translated in English, Polish, German, Spanish, French and Italian. Missing entries fall back to the bundled text.
- Items are written as lowercase keys such as `diamond_sword`, with or without `minecraft:`.
- `/ib block` adds the given actions and worlds to an existing rule and `/ib edit` replaces only the given parts.
- An enchanting table roll that includes a blocked enchantment alongside an allowed one now only loses the blocked enchantment instead of being cancelled.
- The bypass permission names are fixed; `bypass.default-permission` and `bypass.per-action.prefix` were removed from `config.yml`.
- Built with Gradle. The plugin compiles against the Spigot API, and Paper-only features live in a separate source set that is only loaded on Paper and its forks.

### Fixed

- Using a blocked item while clicking air, such as throwing an ender pearl, was not blocked, because Bukkit reports those clicks as already cancelled.
- Holding a blocked item no longer stops players from opening doors, chests and other blocks.
- Taking off blocked armor moves the item instead of rebuilding it, so it cannot duplicate items.
- A `rules.yml` with a YAML error no longer leaves the server without rules: the previous rules stay active and changes are refused until it is fixed.
- Saving `rules.yml` is atomic, so a crash during a save cannot leave a half written file.

### Upgrading from 1.x

- `blocked-items.yml` and `presets.yml` are converted to `rules.yml`, including the older formats 1.x still read.
- The language, message cooldown and bypass settings move to the new `config.yml`.
- Every old file, including the `languages` folder, is moved to `backup-1.x`. Custom message texts are not converted.
- Permissions are unchanged; `itemblocker.admin` was added to grant every command at once.
- Most commands keep their names. `/ib show <preset>` and `/ib list presets` are now `/ib preset show <preset>` and `/ib preset list`, and enchantment and potion levels are written as `level:5`.

### Compatibility

- Java 21+ (Minecraft 26.x servers require Java 25)
- Folia, Paper, Purpur and Spigot
- Minecraft 1.21 to 26.3

## [1.2.0] - 2026-10-18

### Added

- Support for Minecraft 26.1.1, 26.1.2, 26.2 and 26.3. The same jar runs on every version from 1.21 to 26.3.
- Blocked armor can no longer be put on with the offhand swap key (F) over an armor slot, by dragging it onto an armor slot, or by a dispenser.
- Item names in commands accept the `minecraft:` prefix, for example `/ib block minecraft:tnt`.

### Fixed

- `/ib block enchantment` and `/ib block potion` with `in:<preset>` reported success when the preset did not exist. They now report that the preset was not found.
- Presets named like a command word (for example `list` or `set`) could not be shown, edited or deleted.
- `/ib preset create` accepted names that broke `presets.yml` (for example `[x]` or `#x`) or clashed with command words. Preset names now use only `a-z`, `0-9`, `_` and `-`.
- Tab completion listed hundreds of `LEGACY_*` materials, and commands accepted them along with non-item materials such as `WATER`, which can never be blocked.
- `itemblocker.list` now allows `/ib list presets`, `/ib list enchantments` and `/ib list potions`, and `itemblocker.info` allows `/ib show enchantment` and `/ib show potion`, as their permission descriptions say.
- Blocked smithing, enchantment and potion effect messages are now translated in German, Spanish, French and Italian. Existing language files are not overwritten, so delete `languages/messages_<lang>.yml` to get the new texts.
- Hangar releases now list Paper 26.x versions, and Modrinth releases list every version from 1.21 to 26.3 instead of stopping at 26.1.

### Compatibility

- Requires Java 21+ (Minecraft 26.x servers require Java 25)
- Supports Folia / Paper / Purpur / Spigot
- Minecraft 1.21 to 26.3

## [1.1.3] - 2026-06-16

### Fixed

- Fixed severe duplication glitches when swapping blocked armor pieces from the hotbar.
- Thanks `traxeur` for reporting this bug!

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
