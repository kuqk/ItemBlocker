# ItemBlocker

<p align="center">
  <img alt="Paper" src="https://cdn.jsdelivr.net/npm/@intergrav/devins-badges@3/assets/compact/supported/paper_vector.svg">
  <img alt="Purpur" src="https://cdn.jsdelivr.net/npm/@intergrav/devins-badges@3/assets/compact/supported/purpur_vector.svg">
  <a href="https://www.spigotmc.org/resources/itemblocker.131418/"><img alt="Spigot" src="https://cdn.jsdelivr.net/npm/@intergrav/devins-badges@3/assets/compact/supported/spigot_vector.svg"></a>
  <a href="https://modrinth.com/plugin/itemblocker"><img alt="Modrinth" src="https://cdn.jsdelivr.net/npm/@intergrav/devins-badges@3/assets/compact/available/modrinth_vector.svg"></a>
  <a href="https://hangar.papermc.io/kuqk/ItemBlocker"><img alt="Hangar" src="https://cdn.jsdelivr.net/npm/@intergrav/devins-badges@3/assets/compact/available/hangar_vector.svg"></a>
</p>

Item, enchantment and potion effect restrictions for Paper, Folia, Purpur and Spigot, Minecraft 1.21 to 26.3.

- Block items per action: crafting, pickup, drop, use, place, armor, inventory, hopper and smithing.
- Block enchantments and potion effects from a minimum level.
- Group rules into presets and limit any rule or preset to specific worlds.
- Manage everything in game with tab completion, or edit one YAML file.
- Messages in English, Polish, German, Spanish, French and Italian.

## Installation

1. Download the jar from [Modrinth](https://modrinth.com/plugin/itemblocker), [Hangar](https://hangar.papermc.io/kuqk/ItemBlocker) or the [releases](https://github.com/kuqk/ItemBlocker/releases).
2. Put it in `plugins/` and start the server.
3. Block something: `/ib block tnt actions:place`.

ItemBlocker needs Java 21 or newer. Minecraft 26.x servers need Java 25.

## How it works

Rules live in presets. The `default` preset always exists and applies everywhere. Create more presets for worlds, events or game modes, and give each preset its own reason shown in block messages.

A preset can block:

- **Items**, per action. An item rule can be limited to some worlds.
- **Enchantments** from a minimum level. Items carrying a blocked enchantment are blocked for every action, enchanting tables stop offering it and anvils do not produce it.
- **Potion effects** from a minimum level. The effect is not applied, and players lose it when they join or change worlds.

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

## Commands

The command is `/itemblocker`, or `/ib`. Every command has tab completion.

```
/ib block <item|hand> [actions:...] [worlds:...] [in:preset]
/ib block enchantment <name> [level:n] [in:preset]
/ib block potion <effect> [level:n] [in:preset]
/ib edit <item|hand> [actions:...] [worlds:...] [in:preset]
/ib unblock <item|hand|enchantment|potion> [name] [in:preset]
/ib show <item|hand|enchantment|potion> [name]
/ib list [items|enchantments|potions] [in:preset]
/ib preset list | show <name> | create <name> | delete <name>
/ib preset edit <name> <description|reason|worlds> [value]
/ib reload
```

- `actions:` takes a comma separated list or `all`, for example `actions:place,use`. Without it, every action is blocked.
- `worlds:` takes a comma separated list or `all`, for example `worlds:world,world_nether`.
- `in:` picks the preset. Without it, the `default` preset is used.
- `block` adds to an existing rule, `edit` replaces only the parts you give.
- `hand` means the item in your main hand.

Examples:

```
/ib block tnt actions:place,crafting
/ib block elytra actions:armor worlds:world_the_end
/ib block enchantment sharpness level:5
/ib preset create pvp
/ib preset edit pvp worlds arena
/ib block potion strength level:2 in:pvp
```

## Permissions

| Permission | Allows | Default |
|---|---|---|
| `itemblocker.use` | Using `/ib` at all | op |
| `itemblocker.add` | `/ib block` for items | op |
| `itemblocker.edit` | `/ib edit` | op |
| `itemblocker.remove` | `/ib unblock` for items | op |
| `itemblocker.enchantment` | Blocking and unblocking enchantments | op |
| `itemblocker.potion` | Blocking and unblocking potion effects | op |
| `itemblocker.info` | `/ib show` and `/ib preset show` | op |
| `itemblocker.list` | `/ib list` and `/ib preset list` | op |
| `itemblocker.preset` | Creating, editing and deleting presets | op |
| `itemblocker.reload` | `/ib reload` | op |
| `itemblocker.admin` | All of the above | op |
| `itemblocker.bypass` | Ignoring every restriction | op |
| `itemblocker.bypass.<action>` | Ignoring one action, for example `itemblocker.bypass.place` | no |
| `itemblocker.bypass.enchantment` | Ignoring enchantment restrictions | no |
| `itemblocker.bypass.potion` | Ignoring potion effect restrictions | no |

Operators bypass every restriction by default. Test your rules with a player who is not an operator, or turn bypassing off in `config.yml`.

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
    items:
      tnt:
        actions:
        - place
        - crafting
        worlds: all
    enchantments:
      sharpness: 5
    potions: {}
  pvp:
    description: Arena rules
    reason: ''
    worlds:
    - arena
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

Message files are in `plugins/ItemBlocker/lang/`. They support `&` color codes and hex colors like `&#ff8800`. Entries you delete fall back to the bundled text, so you only need to keep the ones you change. To add a language, copy `en.yml` to for example `nl.yml` and set `language: nl`.

## Building

```bash
./gradlew build
```

The jar is written to `build/libs/`. `./gradlew runServer` starts a Paper test server with the plugin, `-Pmc=26.3` picks the Minecraft version.

The plugin compiles against the Spigot 1.21.11 API. Code that needs Paper-only API lives in the `paper` source set and is loaded only on Paper and its forks. CI checks that the jar links against every supported Spigot and Paper version.

## License

[GPL-3.0](LICENSE)
