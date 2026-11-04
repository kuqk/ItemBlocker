# Changelog

All notable changes to this project are documented here.

## [2.6.2] - 2026-11-04

### Fixed

- Blocked items packed in a shulker box or bundle could be picked up, dropped, put into containers and moved by hoppers. The items inside now count for `pickup`, `drop`, `inventory`, `hopper`, `dispense`, `trade`, `loot` and `entity_drop`.
- Stonecutters could make items blocked for `crafting`.
- Creative players could take blocked items from the creative inventory. This now counts as `pickup`.

### Compatibility

- Java 16+
- Folia, Paper, Purpur and Spigot
- Minecraft 1.17 to 26.3
- Optional: WorldGuard 7.0.9+, PlaceholderAPI 2.11.6+

## [2.6.1] - 2026-11-03

### Added

- The chat prompt of the menu has a clickable `cancel` button; typing `cancel` still works. Language files from earlier versions get the button too.
- The cancel button is in the configured language (`anuluj`, `abbrechen`, ...), and typing that word cancels too.
- Item, enchantment and potion effect names in messages are translated by the game into each player's own language. Item icons in the menu show translated names too. The console and logs keep English names.

### Compatibility

- Java 16+
- Folia, Paper, Purpur and Spigot
- Minecraft 1.17 to 26.3
- Optional: WorldGuard 7.0.9+, PlaceholderAPI 2.11.6+

## [2.6.0] - 2026-11-03

### Added

- Minecraft 1.17, 1.18 and 1.19 support. The same jar now runs on 1.17 to 26.3.

### Changed

- ItemBlocker now needs Java 16 instead of 17, the Java that 1.17 servers run.

### Fixed

- Mob equipment that some older servers drop outside the death event is now also covered by `entity_drop` rules.

### Compatibility

- Java 16+
- Folia, Paper, Purpur and Spigot
- Minecraft 1.17 to 26.3
- Optional: WorldGuard 7.0.9+, PlaceholderAPI 2.11.6+

## [2.5.0] - 2026-11-03

### Added

- Eight new actions: `attack` (hitting with an item), `break` (breaking a block, or any block with a tool), `crafter` (1.21 crafters), `dispense` (dispensers and droppers), `brew` (brewing stands, also by blocked potion effects), `trade` (villagers and wandering traders), `loot` (generated loot) and `entity_drop` (mob drops; players keep theirs). Each has an `itemblocker.bypass.<action>` permission.
- Enchantment modes: a blocked enchantment can now `downgrade` the item to the highest allowed level (Sharpness V becomes IV), `remove` the enchantment or `confiscate` the item, instead of `block`ing it. Set with `mode:` in `/ib block enchantment` or shift + left click in the menu.
- Preset templates: `survival`, `lifesteal` and `pvp`, created with `/ib template <name> [as:<preset>]` or from the menu.
- Menu: an item search over every item, worlds and regions picked from a list instead of typed, and filters named after their item automatically.
- Full documentation at https://opuszek.gitbook.io/itemblocker.
- The `obtain` keyword in `actions:` stands for every way of getting an item (`crafting`, `crafter`, `smithing`, `brew`, `trade`, `loot`, `entity_drop`, `pickup`), so players can keep an item but not get more.

### Changed

- Rules that block `all` actions now also block the new ones.
- The rule editor in `/ib gui` shows the action buttons on two rows.

### Compatibility

- Java 17+
- Folia, Paper, Purpur and Spigot
- Minecraft 1.20 to 26.3
- Optional: WorldGuard 7.0.9+, PlaceholderAPI 2.11.6+

## [2.4.1] - 2026-11-02

### Added

- `/ib gui` covers nearly everything: create, set up and delete presets; item groups, item filters (made from a held item, every criterion editable), allowed items, enchantments and potion effects; worlds and bypass permissions of every rule. Text such as patterns, ids and permissions is typed in the chat, `cancel` goes back.

### Compatibility

- Java 17+
- Folia, Paper, Purpur and Spigot
- Minecraft 1.20 to 26.3
- Optional: WorldGuard 7.0.9+, PlaceholderAPI 2.11.6+

## [2.4.0] - 2026-11-02

### Added

- Item filters: block one specific item by text in its name or lore, custom model data, plugin data (such as `nexo:id=ruby_sword`) or an enchantment, with `/ib filter add <id> hand` or the criteria as options. `/ib filter list`, `/ib filter remove` and the matching filters in `/ib show hand`. Needs `itemblocker.filter`.
- Option values in double quotes may contain spaces.
- Item groups: `/ib group add *_sword` blocks every item type matching a wildcard or an item tag such as `#swords`. Needs `itemblocker.group`.
- Allowed items: `/ib allow add wooden_sword` keeps an item type out of a preset's item rules and groups. Needs `itemblocker.allow`.
- Exceptions: `bypass:<permission>` on `block`, `edit`, `filter add` and `group add` lets players with that permission ignore just that rule, and `itemblocker.bypass.preset.<name>` ignores a whole preset.
- Scanning: `/ib scan players|containers` reports forbidden items already in inventories, ender chests and containers; add `remove` or `confiscate` to take them away, and get confiscated items back with `/ib confiscated take <owner>`. Needs `itemblocker.scan`.
- Logs: every blocked action is written to `logs/<date>.log`, `/ib log [player]` shows the most recent ones, and `log.discord.webhook` in `config.yml` sends alerts to Discord. Needs `itemblocker.log`.
- API for other plugins: `ItemBlockerApi.get()` checks rules and adds temporary rules for everyone or one player, and `ItemBlockedEvent` can be cancelled to let a blocked action through. Available through JitPack as `com.github.kuqk:ItemBlocker`.

### Changed

- The `ItemBlocker` in the message prefix is purple. Language files with an earlier default prefix are updated automatically.

### Compatibility

- Java 17+
- Folia, Paper, Purpur and Spigot
- Minecraft 1.20 to 26.3
- Optional: WorldGuard 7.0.9+, PlaceholderAPI 2.11.6+

## [2.3.0] - 2026-11-01

### Added

- A menu, `/ib gui`, for managing item rules and limits: open a preset, click an item in your inventory to add it, toggle actions and change limits with clicks. Needs `itemblocker.gui`.
- Item limits: `/ib limit <item> stack:<n> inventory:<n>` sets the largest stack of an item and the most of it a player may carry. Picking up stops at the limit, taking more out of a container is denied, and anything over the limit is split into free slots or dropped at the player's feet.
- `/ib list limits` and limits in `/ib show`.
- `itemblocker.limit` and `itemblocker.bypass.limit` permissions.

### Changed

- Messages start with `[ItemBlocker]` instead of `ItemBlocker »`. Language files that still have the old default prefix are updated automatically, edited prefixes stay as they are.
- `/ib help` is shorter, so every line fits the chat. Options are listed once at the end and tab completion suggests the rest.

### Fixed

- A blocked item no longer stops a hopper. Hoppers between chests, barrels, hoppers, droppers, dispensers and shulker boxes move the next allowed item instead.

### Compatibility

- Java 17+
- Folia, Paper, Purpur and Spigot
- Minecraft 1.20 to 26.3
- Optional: WorldGuard 7.0.9+, PlaceholderAPI 2.11.6+

## [2.2.0] - 2026-10-31

### Added

- WorldGuard support: limit a preset to regions with `/ib preset edit <name> regions <region,...>`.
- PlaceholderAPI support: its placeholders work in every message sent to a player.

### Changed

- Messages show readable names, such as `Diamond Chestplate` instead of `diamond_chestplate` and `Fire Aspect` instead of `fire_aspect`.
- Items with a custom name, including items from other plugins, are shown by that name in restriction messages.

### Compatibility

- Java 17+
- Folia, Paper, Purpur and Spigot
- Minecraft 1.20 to 26.3
- Optional: WorldGuard 7.0.9+, PlaceholderAPI 2.11.6+

## [2.1.0] - 2026-10-30

### Added

- Support for Minecraft 1.20 to 1.20.6. The same jar runs on every version from 1.20 to 26.3.

### Changed

- ItemBlocker now needs Java 17 instead of 21, so it also loads on 1.20 servers that still run Java 17.

### Compatibility

- Java 17+
- Folia, Paper, Purpur and Spigot
- Minecraft 1.20 to 26.3

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
