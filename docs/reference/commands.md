# Commands

The command is `/itemblocker`, or `/ib`. Every command has tab completion, and `/ib help` lists the ones you may use.

## Options

Most commands take `key:value` options in any order.

| Option | Takes | Example |
|---|---|---|
| `actions:` | a comma separated list of [actions](../rules/actions.md), `all`, `obtain` or `none` | `actions:place,use` |
| `worlds:` | a comma separated list of worlds, or `all` | `worlds:world,world_nether` |
| `in:` | a preset; `default` when left out | `in:pvp` |
| `bypass:` | a permission that ignores just this rule, or `none` | `bypass:vip.elytra` |
| `level:` | a level from 1; 1 when left out | `level:5` |
| `mode:` | an [enchantment mode](../rules/enchantments.md): `block`, `downgrade`, `remove`, `confiscate` | `mode:downgrade` |
| `stack:`, `inventory:` | an amount, or `none` | `stack:4` |
| `type:`, `name:`, `lore:`, `model:`, `data:`, `enchantment:` | [filter criteria](../rules/filters.md) | `name:"Sword of Kings"` |

Values with spaces go in double quotes. `hand` stands for the item in your main hand wherever an item is expected.

## Items

| Command | Does | Permission |
|---|---|---|
| `/ib block <item\|hand> [actions:] [worlds:] [bypass:] [in:]` | Blocks actions, adding to an existing rule | `itemblocker.add` |
| `/ib edit <item\|hand> [actions:] [worlds:] [bypass:] [in:]` | Replaces the parts of a rule you give | `itemblocker.edit` |
| `/ib unblock <item\|hand> [in:]` | Removes a rule | `itemblocker.remove` |
| `/ib limit <item\|hand> [stack:] [inventory:] [in:]` | Sets [limits](../rules/limits.md) | `itemblocker.limit` |

## Groups, allowed items and filters

| Command | Does | Permission |
|---|---|---|
| `/ib group add <pattern> [actions:] [worlds:] [bypass:] [in:]` | Adds or replaces a [group](../rules/groups-and-allowed.md) | `itemblocker.group` |
| `/ib group remove <pattern> [in:]` | Removes a group | `itemblocker.group` |
| `/ib group list [in:]` | Lists groups | `itemblocker.group` |
| `/ib allow add <item\|hand> [in:]` | Allows an item type | `itemblocker.allow` |
| `/ib allow remove <item\|hand> [in:]` | Removes it from the allowed items | `itemblocker.allow` |
| `/ib allow list [in:]` | Lists allowed items | `itemblocker.allow` |
| `/ib filter add <id> [hand] [criteria] [actions:] [worlds:] [bypass:] [in:]` | Adds or replaces a [filter](../rules/filters.md) | `itemblocker.filter` |
| `/ib filter remove <id> [in:]` | Removes a filter | `itemblocker.filter` |
| `/ib filter list [in:]` | Lists filters | `itemblocker.filter` |

## Enchantments and potion effects

| Command | Does | Permission |
|---|---|---|
| `/ib block enchantment <name> [level:] [mode:] [in:]` | Blocks an [enchantment](../rules/enchantments.md) | `itemblocker.enchantment` |
| `/ib unblock enchantment <name> [in:]` | Removes it | `itemblocker.enchantment` |
| `/ib block potion <effect> [level:] [in:]` | Blocks a [potion effect](../rules/potion-effects.md) | `itemblocker.potion` |
| `/ib unblock potion <effect> [in:]` | Removes it | `itemblocker.potion` |

## Looking things up

| Command | Does | Permission |
|---|---|---|
| `/ib show <item\|hand>` | Rules, limits and, for `hand`, matching filters of an item in every preset | `itemblocker.info` |
| `/ib show enchantment <name>` / `/ib show potion <effect>` | The levels blocked in every preset | `itemblocker.info` |
| `/ib list` | Every preset with its numbers of rules | `itemblocker.list` |
| `/ib list items\|limits\|enchantments\|potions [in:]` | The rules of one kind | `itemblocker.list` |

## Presets and templates

| Command | Does | Permission |
|---|---|---|
| `/ib preset list` | Lists presets | `itemblocker.list` |
| `/ib preset show <name>` | Shows a preset | `itemblocker.info` |
| `/ib preset create <name>` | Creates an empty preset | `itemblocker.preset` |
| `/ib preset delete <name>` | Deletes a preset and its rules | `itemblocker.preset` |
| `/ib preset edit <name> description <text>` | Sets the description; no text clears it | `itemblocker.preset` |
| `/ib preset edit <name> reason <text>` | Sets the reason shown to players; no text clears it | `itemblocker.preset` |
| `/ib preset edit <name> worlds <world ...\|all>` | Limits the preset to worlds | `itemblocker.preset` |
| `/ib preset edit <name> regions <region ...\|all>` | Limits the preset to WorldGuard regions | `itemblocker.preset` |
| `/ib template` | Lists [templates](../features/templates.md) | `itemblocker.preset` |
| `/ib template <name> [as:<preset>]` | Creates a preset from a template | `itemblocker.preset` |

## Other

| Command | Does | Permission |
|---|---|---|
| `/ib gui` | Opens the [menu](../features/menu.md) | `itemblocker.gui` |
| `/ib scan players\|containers [remove\|confiscate] [actions:] [radius:] [world:]` | [Scans](../features/scanning.md) for forbidden items | `itemblocker.scan` |
| `/ib confiscated [take <owner>]` | Lists or takes confiscated items | `itemblocker.scan` |
| `/ib log [player]` | The last blocked actions, see [Logs](../features/logs.md) | `itemblocker.log` |
| `/ib reload` | Reloads `config.yml`, `rules.yml` and the messages | `itemblocker.reload` |
| `/ib help` | Lists the commands you may use | `itemblocker.use` |

Every command also needs `itemblocker.use`.
