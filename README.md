# ItemBlocker

<p align="center">
  <img alt="Paper" src="https://cdn.jsdelivr.net/npm/@intergrav/devins-badges@3/assets/compact/supported/paper_vector.svg">
  <img alt="Purpur" src="https://cdn.jsdelivr.net/npm/@intergrav/devins-badges@3/assets/compact/supported/purpur_vector.svg">
  <a href="https://www.spigotmc.org/resources/itemblocker.131418/"><img alt="Spigot" src="https://cdn.jsdelivr.net/npm/@intergrav/devins-badges@3/assets/compact/supported/spigot_vector.svg"></a>
  <a href="https://modrinth.com/plugin/itemblocker"><img alt="Modrinth" src="https://cdn.jsdelivr.net/npm/@intergrav/devins-badges@3/assets/compact/available/modrinth_vector.svg"></a>
  <a href="https://hangar.papermc.io/kuqk/ItemBlocker"><img alt="Hangar" src="https://cdn.jsdelivr.net/npm/@intergrav/devins-badges@3/assets/compact/available/hangar_vector.svg"></a>
</p>

Item, enchantment and potion effect restrictions for Paper, Folia, Purpur and Spigot, Minecraft 1.20 to 26.3.

- Block items per action: crafting, pickup, drop, use, place, armor, inventory, hopper and smithing.
- Block enchantments and potion effects from a minimum level.
- Limit how big an item stacks and how many of it a player may carry.
- Block whole groups of items, such as every sword, and allow just a few of them.
- Log every blocked action to files and, optionally, a Discord channel.
- Find forbidden items players and containers already have, and remove or confiscate them.
- Let a rank ignore one rule, or a whole preset, with a permission.
- Block one specific item, such as a sword from an RPG plugin, by its name, lore, custom model data, plugin data or enchantments.
- Group rules into presets and limit any rule or preset to specific worlds, or a preset to WorldGuard regions.
- Manage everything in a menu (`/ib gui`), with commands and tab completion, or in one YAML file.
- Messages in English, Polish, German, Spanish, French and Italian, with PlaceholderAPI placeholders.

## Installation

1. Download the jar from [Modrinth](https://modrinth.com/plugin/itemblocker), [Hangar](https://hangar.papermc.io/kuqk/ItemBlocker) or the [releases](https://github.com/kuqk/ItemBlocker/releases).
2. Put it in `plugins/` and start the server.
3. Block something: `/ib block tnt actions:place`.

ItemBlocker runs on Java 17 or newer, so it works with whatever Java your server already uses.

## How it works

Rules live in presets. The `default` preset always exists and applies everywhere. Create more presets for worlds, events or game modes, and give each preset its own reason shown in block messages.

A preset can block:

- **Items**, per action. An item rule can be limited to some worlds.
- **Enchantments** from a minimum level. Items carrying a blocked enchantment are blocked for every action, enchanting tables stop offering it and anvils do not produce it.
- **Potion effects** from a minimum level. The effect is not applied, and players lose it when they join or change worlds.
- **Item groups**: every item type matching a pattern, such as `*_sword`, `netherite_*` or the item tag `#swords`, with actions and worlds like an item rule.
- **Allowed items**: item types the preset's item rules and groups never block, for example a group blocking every sword with `wooden_sword` allowed.
- **Item filters**: specific items matched by text in their name or lore, custom model data, a persistent data entry (how plugins such as Nexo, Oraxen or ExecutableItems mark their items) or an enchantment, each with its own actions and worlds. Every criterion you set must match. Item filters are checked after the rules by item type.
- **Item limits**: the largest stack of an item and the most of it a player may carry. If several presets limit the same item, the smallest limit wins.

| Action | Blocks |
|---|---|
| `crafting` | Taking the item from a crafting result |
| `pickup` | Picking the item up |
| `drop` | Dropping the item |
| `use` | Right clicking with the item, eating or drinking it |
| `place` | Placing it as a block, emptying it as a bucket, or placing it as an entity (boats, minecarts, armor stands, item frames, paintings, end crystals) |
| `armor` | Wearing it, however it is put on. Blocked armor is taken off when a player joins or changes worlds |
| `inventory` | Moving it into or out of chests and other containers |
| `hopper` | Hoppers moving or picking it up |
| `smithing` | Getting it from a smithing table |

Limits are kept like this: players only pick up as much as they may carry and as fits into stacks within the limit, and taking more out of a container is denied. Anything that still goes over, for example by crafting, is fixed right away: stacks above the limit are split into free slots and what does not fit or exceeds the inventory limit is dropped at the player's feet. Items are never deleted.

## Commands

The command is `/itemblocker`, or `/ib`. Every command has tab completion.

```
/ib block <item|hand> [actions:...] [worlds:...] [in:preset]
/ib block enchantment <name> [level:n] [in:preset]
/ib block potion <effect> [level:n] [in:preset]
/ib edit <item|hand> [actions:...] [worlds:...] [in:preset]
/ib limit <item|hand> [stack:n|none] [inventory:n|none] [in:preset]
/ib filter add <id> [hand] [type:] [name:] [lore:] [model:] [data:] [enchantment:] [actions:] [worlds:] [in:preset]
/ib filter remove <id> [in:preset]
/ib filter list [in:preset]
/ib group add <pattern> [actions:...] [worlds:...] [bypass:...] [in:preset]
/ib group remove <pattern> [in:preset]
/ib group list [in:preset]
/ib allow add|remove <item|hand> [in:preset]
/ib allow list [in:preset]
/ib scan players|containers [remove|confiscate] [actions:...] [radius:n] [world:name]
/ib confiscated [take <owner>]
/ib log [player]
/ib unblock <item|hand|enchantment|potion> [name] [in:preset]
/ib show <item|hand|enchantment|potion> [name]
/ib list [items|limits|enchantments|potions] [in:preset]
/ib preset list | show <name> | create <name> | delete <name>
/ib preset edit <name> <description|reason|worlds|regions> [value]
/ib gui
/ib reload
```

- `actions:` takes a comma separated list or `all`, for example `actions:place,use`. Without it, every action is blocked.
- `worlds:` takes a comma separated list or `all`, for example `worlds:world,world_nether`.
- `stack:` and `inventory:` set the largest stack and the most a player may carry, `none` removes the limit. Options you leave out keep their value.
- `hand` in `/ib filter add` copies the held item's type, custom name, custom model data and plugin data; options you add replace what was copied. Values with spaces go in double quotes, for example `name:"Sword of Kings"`. `data:` takes a key or `key=value`, such as `data:nexo:id=ruby_sword`.
- `bypass:` names a permission that lets players ignore just that rule, for example `bypass:vip.elytra`; `bypass:none` removes it. It works on `block`, `edit`, `filter add` and `group add`.
- `in:` picks the preset. Without it, the `default` preset is used.
- `block` adds to an existing rule, `edit` replaces only the parts you give.
- `hand` means the item in your main hand.

Examples:

```
/ib block tnt actions:place,crafting
/ib block elytra actions:armor worlds:world_the_end
/ib block enchantment sharpness level:5
/ib limit ender_pearl stack:4 inventory:16
/ib filter add excalibur hand actions:use,drop
/ib filter add soulbound lore:Soulbound actions:drop
/ib group add *_sword actions:use in:pvp
/ib allow add wooden_sword in:pvp
/ib block elytra actions:armor bypass:vip.elytra
/ib preset create pvp
/ib preset edit pvp worlds arena
/ib block potion strength level:2 in:pvp
/ib preset create spawn
/ib preset edit spawn regions spawn,market
```

## Integrations

Both are optional. ItemBlocker hooks into them when they are installed and works the same without them.

- **WorldGuard**: limit a preset to regions with `/ib preset edit <name> regions <region,...>`. The preset then only applies inside those regions, and still only in its worlds. `regions all` removes the limit. Without WorldGuard, a preset limited to regions applies nowhere.
- **PlaceholderAPI**: placeholders such as `%player_name%` work in every message sent to a player.

Permissions work with any permission plugin, such as LuckPerms, including per world and per server contexts.

## Scanning existing items

Rules only stop new actions. Items players got before a rule existed stay where they are, so `/ib scan` finds them:

- `/ib scan players` checks the inventory and ender chest of every online player.
- `/ib scan containers` checks chests, barrels, shulker boxes and other containers in loaded chunks around you (`radius:` chunks, 4 by default), or in a whole world with `world:<name>`.
- Without more arguments it only reports what it found. Add `remove` to delete those items, or `confiscate` to move them to `confiscated.yml`. `/ib confiscated` lists them by owner and `/ib confiscated take <owner>` gives them to you.
- An item counts as forbidden if players may not pick it up or it carries a blocked enchantment. Use `actions:` to count other actions, for example `actions:use,place`. Players' own bypass permissions apply.

## Logs and Discord alerts

Every blocked action is written to `plugins/ItemBlocker/logs/<date>.log`, one line each:

```
2026-11-02 17:40:01 | Steve | place | TNT | world | default
```

`/ib log` shows the last ten, `/ib log <player>` those of one player. Holding a blocked item and clicking again within a second is only logged once.

To get alerts on Discord, create a webhook in the channel settings and put its URL in `config.yml`:

```yaml
log:
  enabled: true
  discord:
    webhook: "https://discord.com/api/webhooks/..."
    cooldown-seconds: 60   # one alert per player, action and item per minute
```

Alerts never ping anyone, even if an item is named `@everyone`.

## Menu

`/ib gui` opens a menu with every preset. Open a preset to see its items, then:

- click an item in your own inventory to add it,
- click an item to edit it: toggle each action, and change the stack and inventory limits with left click (+1) and right click (-1), with shift for steps of 16,
- shift + right click an item to remove its rule and limits.

Every change is saved right away. Descriptions, worlds and regions are still set with commands.

## Permissions

| Permission | Allows | Default |
|---|---|---|
| `itemblocker.use` | Using `/ib` at all | op |
| `itemblocker.add` | `/ib block` for items | op |
| `itemblocker.edit` | `/ib edit` | op |
| `itemblocker.limit` | `/ib limit` | op |
| `itemblocker.filter` | `/ib filter` | op |
| `itemblocker.group` | `/ib group` | op |
| `itemblocker.allow` | `/ib allow` | op |
| `itemblocker.scan` | `/ib scan` and `/ib confiscated` | op |
| `itemblocker.log` | `/ib log` | op |
| `itemblocker.remove` | `/ib unblock` for items | op |
| `itemblocker.enchantment` | Blocking and unblocking enchantments | op |
| `itemblocker.potion` | Blocking and unblocking potion effects | op |
| `itemblocker.info` | `/ib show` and `/ib preset show` | op |
| `itemblocker.list` | `/ib list` and `/ib preset list` | op |
| `itemblocker.preset` | Creating, editing and deleting presets | op |
| `itemblocker.reload` | `/ib reload` | op |
| `itemblocker.gui` | `/ib gui` | op |
| `itemblocker.admin` | All of the above | op |
| `itemblocker.bypass` | Ignoring every restriction | op |
| `itemblocker.bypass.<action>` | Ignoring one action, for example `itemblocker.bypass.place` | no |
| `itemblocker.bypass.enchantment` | Ignoring enchantment restrictions | no |
| `itemblocker.bypass.potion` | Ignoring potion effect restrictions | no |
| `itemblocker.bypass.limit` | Ignoring stack and inventory limits | no |
| `itemblocker.bypass.preset.<name>` | Ignoring one preset, for example `itemblocker.bypass.preset.pvp` | no |
| any permission set with `bypass:` | Ignoring that one rule | no |

The preset and rule permissions only count when they are granted explicitly, for example by a permission plugin, so operators do not get them just for being operators. Operators bypass every restriction by default. Test your rules with a player who is not an operator, or turn bypassing off in `config.yml`.

## Configuration

### config.yml

```yaml
language: en          # en, pl, de, es, fr or it; messages are in the lang folder
messages:
  cooldown-ms: 1000   # minimum time between two block messages to the same player
bypass:
  enabled: true       # itemblocker.bypass skips every restriction
  per-action: true    # itemblocker.bypass.<action> skips one kind of restriction
```

### rules.yml

Commands write this file, but you can also edit it and run `/ib reload`.

```yaml
presets:
  default:
    description: ''
    reason: '&cThis item is disabled on this server'
    worlds: all
    regions: all
    items:
      tnt:
        actions:
        - place
        - crafting
        worlds: all
    limits:
      ender_pearl:
        stack: 4
        inventory: 16
    filters:
      excalibur:
        type: diamond_sword
        name: Excalibur
        data: nexo:id=excalibur
        actions:
        - use
        worlds: all
    enchantments:
      sharpness: 5
    potions: {}
  pvp:
    description: Arena rules
    reason: ''
    worlds:
    - arena
    regions: all
    items:
      ender_pearl:
        actions: all
        worlds: all
    enchantments: {}
    potions:
      strength: 2
```

If `rules.yml` has a YAML error, the previous rules stay active and commands refuse to change them until the file is fixed and reloaded, so a command can never overwrite your edits.

### Messages

Message files are in `plugins/ItemBlocker/lang/`. They support `&` color codes, hex colors like `&#ff8800` and, with PlaceholderAPI installed, its placeholders. Entries you delete fall back to the bundled text, so you only need to keep the ones you change. To add a language, copy `en.yml` to for example `nl.yml` and set `language: nl`.

## Building

```bash
./gradlew build
```

The jar is written to `build/libs/`. `./gradlew runServer` starts a Paper test server with the plugin, `-Pmc=26.3` picks the Minecraft version.

The plugin compiles against the Spigot 1.21.11 API. Code that needs Paper-only API lives in the `paper` source set and is loaded only on Paper and its forks. CI checks that the jar links against every supported Spigot and Paper version.

## License

[GPL-3.0](LICENSE)
