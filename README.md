# 🛡️ ItemBlocker

<p align="center">
  <img alt="Paper" src="https://cdn.jsdelivr.net/npm/@intergrav/devins-badges@3/assets/compact/supported/paper_vector.svg">
  <img alt="Purpur" src="https://cdn.jsdelivr.net/npm/@intergrav/devins-badges@3/assets/compact/supported/purpur_vector.svg">
  <a href="https://www.spigotmc.org/resources/itemblocker.131418/"><img alt="Spigot" src="https://cdn.jsdelivr.net/npm/@intergrav/devins-badges@3/assets/compact/supported/spigot_vector.svg"></a>
  <a href="https://github.com/kuqk/ItemBlocker"><img alt="GitHub" src="https://cdn.jsdelivr.net/npm/@intergrav/devins-badges@3/assets/compact/available/github_vector.svg"></a>
  <a href="https://modrinth.com/plugin/itemblocker"><img alt="Modrinth" src="https://cdn.jsdelivr.net/npm/@intergrav/devins-badges@3/assets/compact/available/modrinth_vector.svg"></a>
  <a href="https://hangar.papermc.io/kuqk/ItemBlocker"><img alt="Hangar" src="https://cdn.jsdelivr.net/npm/@intergrav/devins-badges@3/assets/compact/available/hangar_vector.svg"></a>
</p>

<p align="center">
  <strong>Clean, detailed, and flexible item restrictions for modern Minecraft servers.</strong>
  <br>
  Block items, smithing results, enchantments, potion effects, inventory movement, hopper movement, and more on Minecraft 1.21.x, 26.1.
</p>

## ✨ What Is ItemBlocker?

ItemBlocker is a plugin for server owners who want strong control over items without turning configuration into chaos.

It is built around a simple idea: keep one clear `default` section for global restrictions, then add `presets` for survival, events, minigames, worlds, or special modes when you need more specific rules.

That means you can do things like:

- block TNT everywhere on the server
- block Elytra only in one preset
- block Smithing results like netherite tools
- block specific enchantments from a chosen level
- block potion effects instead of blocking every single potion item manually
- stop restricted items from being moved through inventories or hoppers

## 🧠 How ItemBlocker Works

ItemBlocker is split into a few clean layers:

- 🌍 `default` is your always-available global section
- 🗂️ `presets` let you create separate groups of rules
- 📦 `items` let you block specific materials and actions
- ✨ `enchantments` let you block enchants from a chosen minimum level
- 🧪 `potions` let you block potion effects from a chosen minimum level
- 💬 `reason` lets you define the message players should understand

In practice, that means you are not limited to "item yes or no". You can decide:

- what is blocked
- where it is blocked
- how it is blocked
- why the player sees that restriction

## 🌍 Default Section

The `default` section lives in `blocked-items.yml` and acts as the global base for the whole server.

Use it when a restriction should always exist unless you intentionally manage something differently with presets.

Inside `default`, you can configure:

- 💬 one global `reason`
- ✨ blocked `enchantments`
- 🧪 blocked `potions`
- 📦 blocked `items`

This is the best place for things like:

- admin-only items
- dangerous survival shortcuts
- overpowered enchants
- potion effects you never want players to keep

## 🗂️ Presets

Presets live in `presets.yml` and are the main tool for grouped restrictions.

Each preset can have its own:

- 📝 `description`
- 💬 `reason`
- 🌍 preset `worlds`
- ✨ enchantment rules
- 🧪 potion rules
- 📦 item rules

### 📌 Important preset behavior

- A preset can be active only in selected worlds.
- An item inside that preset can also have its own `worlds` filter.
- If you want the same item to behave differently in different places, use different presets instead of trying to overload one rule.

## 🌐 Worlds And Targeting

ItemBlocker lets you decide where rules apply in two places:

- 🌍 on the preset itself
- 📦 on the individual item rule

### `worlds: all`

This means the rule works in every world.

### `worlds: world,world_nether`

This means the rule is only active in those listed worlds.

### `in:default` and `in:<preset>`

This decides which target you are editing or showing with commands.

Examples:

```text
/ib block TNT in:default
/ib block ELYTRA in:survival
/ib show TNT in:default
/ib show potion strength in:survival
```

## 📦 Item Rules

Item rules are the core of the plugin.

Every item rule answers two questions:

1. Which actions should be blocked?
2. In which worlds should that rule work?

Each item can have one rule per target, and that rule can contain:

- ⚙️ `actions`
- 🌍 `worlds`

You can target an item directly by material name or use `hand` to use the item from the player's main hand.

Examples:

```text
/ib block TNT
/ib block hand actions:use,drop in:survival
/ib edit ELYTRA worlds:world
/ib unblock BEDROCK in:default
```

## ⚔️ Item Actions

ItemBlocker supports the following actions:

- 🛠️ `crafting` - blocks getting the result of a crafting recipe
- 📥 `pickup` - blocks picking the item up from the ground
- 📤 `drop` - blocks throwing the item out of inventory
- 🖱️ `use` - blocks normal use of the item
- 🧱 `place` - blocks placing blocks and special placeable items
- 🪖 `armor` - blocks equipping wearable items
- 🎒 `inventory` - blocks moving restricted items through inventories and containers
- 🔄 `hopper` - blocks moving restricted items with hoppers
- 🔨 `smithing` - blocks smithing-table results for that item

### 🛠️ `crafting`

Use this when players should be able to own an item in some other way, but should not be allowed to craft it.

### 📥 `pickup`

Use this when players should not be able to pick an item up from the ground at all.

### 📤 `drop`

Use this when players should keep the item out of circulation and not throw it around.

### 🖱️ `use`

Use this when the item should exist, but should not be usable in normal right-click behavior.

### 🧱 `place`

Use this when an item should not be placed into the world.

This also covers special placement-style items, not only plain blocks. That makes it useful for things like:

- boats
- rafts
- minecarts
- item frames

### 🪖 `armor`

Use this when wearable items should not be equipped.

ItemBlocker also enforces armor restrictions when needed, so blocked armor is not only stopped on click but can also be removed from equipped slots when players join or change worlds.

### 🎒 `inventory`

Use this when restricted items should not be moved through GUI inventories and containers.

This helps with:

- chest movement
- shift-click transfers
- drag operations
- number-key swaps
- offhand swaps into inventory UIs
- double-click collection behavior

### 🔄 `hopper`

Use this when you want to stop automation from moving the item.

That makes hopper-based bypasses much harder.

## 🔨 Smithing Restrictions

`smithing` is a normal item action in ItemBlocker, not a separate weird subsystem.

That means you can block smithing results the same way you block any other action:

```text
/ib block NETHERITE_SWORD actions:smithing
/ib block DIAMOND_CHESTPLATE actions:smithing in:survival
```

### What this is good for

- ⛏️ blocking netherite upgrades
- 🛡️ blocking armor trim results
- 🧰 blocking specific final smithing products without blocking the entire base item

### Why it is useful

You may want players to:

- own a diamond chestplate
- move it normally
- use it normally
- but still not create the smithing-table result

That is exactly what `actions:smithing` is for.

## ✨ Enchantment Restrictions

Enchantment rules let you block enchants by minimum level.

Example:

```text
/ib block enchantment sharpness level:2
/ib block enchantment mending level:1 in:survival
```

### How enchantment rules work

- ✨ `sharpness: 2` means Sharpness II and higher are blocked
- ✨ `mending: 1` means any Mending is blocked
- ✨ rules can live in `default` or inside a preset

### What ItemBlocker checks

Enchantment restrictions are used in two important ways:

- 🪄 they can block enchanting-table offers and enchanting results
- 📦 they can also block items that already contain a blocked enchantment

That second part is especially useful, because it means the plugin does not only stop future enchanting. It also reacts when players try to use or move items that already carry blocked enchants.

Stored enchantments are also checked, so enchanted books are covered too.

### Good examples

- block `mending` on a hardcore preset
- allow `sharpness 1` but block `sharpness 2+`
- disable `infinity` only in survival worlds

## 🧪 Potion Effect Restrictions

Potion rules work similarly to enchantment rules: they use a minimum level.

Example:

```text
/ib block potion strength level:2
/ib block potion speed level:2 in:survival
```

### How potion rules work

- 🧪 `strength: 2` means Strength II and higher are blocked
- 🧪 `speed: 2` means Speed II and higher are blocked
- 🧪 rules can be global or preset-based

### What ItemBlocker actually blocks

Potion rules target the effect itself.

That means ItemBlocker can stop:

- potion effects being applied to a player
- blocked effects remaining active when a player joins
- blocked effects remaining active when a player changes worlds

### Important note

Potion-effect restrictions are not the same thing as item restrictions.

If you block `strength: 2`, the plugin blocks the Strength II effect itself. If you also want to block the potion item from being used, picked up, or moved, you should combine that with normal item rules.

## 💬 Reasons, Source Info, And Player Feedback

Each target can define a `reason`, and that reason is used in the block message shown to players.

This makes the plugin feel much cleaner in real use, because players do not only see "blocked". They can also understand why.

Examples:

- `"&cThis item is blocked globally by the server team."`
- `"&cThis item is disabled on survival worlds."`

ItemBlocker can also show the source of the restriction, so players or staff can understand whether the block came from:

- `default`
- a specific preset

There is also a message cooldown in `config.yml`, which helps avoid chat spam when players keep trying the same blocked action.

## 🧰 Command Flow

The modern command flow is built around a small set of clear commands:

- 🟢 `/ib block` - create a rule or merge into an existing one
- 🟡 `/ib edit` - change only the parts you provide
- 🔴 `/ib unblock` - remove a rule
- 🔍 `/ib show` - inspect one item, preset, enchantment, or potion rule
- 📋 `/ib list` - list configured items, presets, enchantments, potions, or a target
- 🗂️ `/ib preset` - manage presets
- 🔄 `/ib reload` - reload plugin files

Aliases:

- `/ib`
- `/blocker`

### 📌 Default command behavior

- If you create a new item rule and do not provide `actions:`, it defaults to `all`.
- If you create a new item rule and do not provide `worlds:`, it defaults to `all`.
- If you do not provide `in:`, the target is `default`.
- `block` merges into an existing rule instead of blindly replacing it.
- `edit` requires the rule to already exist and only replaces the fields you pass.
- Enchantment and potion rules default to level `1` if you do not provide a higher level.

## 🧪 Command Examples

### 📦 Item examples

```text
/ib block TNT
/ib block hand actions:use,drop in:survival
/ib block DIAMOND_CHESTPLATE actions:smithing
/ib edit TNT worlds:world_nether
/ib edit ELYTRA actions:use,inventory in:survival
/ib unblock COMMAND_BLOCK in:event
/ib show TNT
/ib show TNT in:default
/ib list items
/ib list items in:survival
```

### ✨ Enchantment examples

```text
/ib block enchantment sharpness level:2
/ib block enchantment mending level:1 in:survival
/ib unblock enchantment mending in:survival
/ib show enchantment sharpness
/ib show enchantment infinity in:survival
/ib list enchantments
/ib list enchantments in:survival
```

### 🧪 Potion examples

```text
/ib block potion strength level:2
/ib block potion speed level:2 in:survival
/ib unblock potion strength in:default
/ib show potion speed
/ib show potion speed in:survival
/ib list potions
/ib list potions in:survival
```

### 🗂️ Preset examples

```text
/ib preset list
/ib preset create survival
/ib preset show survival
/ib preset edit survival description Main survival restrictions
/ib preset edit survival reason &cThis item is disabled on survival worlds.
/ib preset edit survival worlds world,world_nether
/ib preset delete event
```

## ⚙️ Configuration Files

ItemBlocker uses three main files:

- `config.yml`
- `blocked-items.yml`
- `presets.yml`

### ⚙️ `config.yml`

This file controls general plugin behavior.

What you can set here:

- 🌐 plugin language
- 💬 message cooldown
- 🔐 whether bypass permissions are enabled
- 🔑 the global bypass permission node
- 🧩 whether per-action bypass permissions are enabled
- 🏷️ the permission prefix for per-action bypasses

Supported built-in languages:

- `en`
- `pl`
- `de`
- `es`
- `fr`
- `it`

### 🌍 `blocked-items.yml`

This file contains the global `default` section.

What you can set here:

- 💬 default reason
- ✨ global enchantment thresholds
- 🧪 global potion thresholds
- 📦 global item rules

Example:

```yml
default:
  reason: "&cThis item is blocked globally by the server team."

  enchantments:
    sharpness: 2
    mending: 1

  potions:
    strength: 2

  items:
    TNT:
      actions: all
      worlds: all

    BEDROCK:
      actions: all
      worlds: all

    NETHERITE_SWORD:
      actions:
        - smithing
      worlds: all
```

### 🗂️ `presets.yml`

This file contains grouped restrictions.

What you can set here for each preset:

- 📝 description
- 💬 reason
- 🌍 preset worlds
- ✨ enchantment thresholds
- 🧪 potion thresholds
- 📦 item rules

Example:

```yml
presets:
  survival:
    description: "Main survival restrictions."
    reason: "&cThis item is disabled on survival worlds."
    worlds:
      - world
      - world_nether

    enchantments:
      infinity: 1

    potions:
      speed: 2

    items:
      COMMAND_BLOCK:
        actions: all
        worlds: all

      ELYTRA:
        actions:
          - use
          - inventory
        worlds: all

      NETHERITE_PICKAXE:
        actions:
          - smithing
        worlds: all
```

## 🔐 Permissions

ItemBlocker includes both management permissions and bypass permissions.

### 🧰 Management permissions

```text
itemblocker.*
itemblocker.use
itemblocker.add
itemblocker.remove
itemblocker.edit
itemblocker.list
itemblocker.reload
itemblocker.info
itemblocker.preset
itemblocker.enchantment
itemblocker.potion
```

### 🚪 Global bypass

```text
itemblocker.bypass
```

This bypasses every ItemBlocker restriction.

### 🧩 Per-action bypass permissions

```text
itemblocker.bypass.crafting
itemblocker.bypass.pickup
itemblocker.bypass.drop
itemblocker.bypass.use
itemblocker.bypass.place
itemblocker.bypass.armor
itemblocker.bypass.inventory
itemblocker.bypass.hopper
itemblocker.bypass.smithing
itemblocker.bypass.enchantment
itemblocker.bypass.potion
```

This is useful when you want staff or special ranks to ignore only selected checks instead of the whole plugin.

## 🚀 Quick Start

1. Put the jar into `plugins/`.
2. Start the server once.
3. Open `config.yml` and choose your language and bypass setup.
4. Add global restrictions in `blocked-items.yml`.
5. Create grouped restrictions in `presets.yml` if needed.
6. Run `/ib reload`.
7. Test your blocked items, enchantments, potion effects, and presets in game.

## 📥 Download

- Modrinth: https://modrinth.com/plugin/itemblocker
- Hangar: https://hangar.papermc.io/kuqk/ItemBlocker
- Spigot: https://www.spigotmc.org/resources/itemblocker.131418/
- GitHub: https://github.com/kuqk/ItemBlocker

## ✅ Compatibility

- ☕ Java 21+
- 🧩 Folia, Paper, Purpur, and Spigot
- 🎮 Minecraft `1.21.x` `26.1`

## 📜 License

GPL-3.0
