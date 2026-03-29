# 🛡️ ItemBlocker

![Paper](https://cdn.jsdelivr.net/npm/@intergrav/devins-badges@3/assets/compact/supported/paper_vector.svg)
![Purpur](https://cdn.jsdelivr.net/npm/@intergrav/devins-badges@3/assets/compact/supported/purpur_vector.svg)
[![Spigot](https://cdn.jsdelivr.net/npm/@intergrav/devins-badges@3/assets/compact/supported/spigot_vector.svg)](https://www.spigotmc.org/resources/itemblocker.131418/)
[![GitHub](https://cdn.jsdelivr.net/npm/@intergrav/devins-badges@3/assets/compact/available/github_vector.svg)](https://github.com/kuqk/ItemBlocker)
[![Modrinth](https://cdn.jsdelivr.net/npm/@intergrav/devins-badges@3/assets/compact/available/modrinth_vector.svg)](https://modrinth.com/plugin/itemblocker)
[![Hangar](https://cdn.jsdelivr.net/npm/@intergrav/devins-badges@3/assets/compact/available/hangar_vector.svg)](https://hangar.papermc.io/kuqk/ItemBlocker)

**Clean item restrictions for modern Minecraft servers**

ItemBlocker helps you block problem items without turning your config into a mess.

You get one permanent `default` section for quick global bans, `presets` for grouped rules, and per-item `actions` plus `worlds` when you need more control. One item can also have multiple scoped rules, so you can keep `world`, `world_nether` and different action sets under the same material without ugly workarounds.

## ✨ Features

### 📦 Flexible Item Control

Block items by:

- crafting
- pickup
- drop
- use
- place
- armor
- inventory
- hopper

That means you can cover normal interaction, inventory movement and hopper automation in one plugin.

### 🧩 Default + Presets

Use `default` for fast server-wide bans and `presets` for grouped rules like survival, events, admin tools or temporary restrictions.

### 🌍 Per-Item World Rules

Each item can have its own `worlds` section with `all`, `disabled` or a simple world list like `world` and `world_nether`, so one item can behave differently from the rest without awkward workarounds.

### 🧱 Multiple Scopes Per Item

Regular `/ib add` merges actions and worlds into one clean rule for the same item. If you really need separate scoped rules, use `/ib item <item> create ...`. Item info shows scope numbers, and `item edit` / `item delete` can target one scope with `scope:<number>`.

### 💬 Clear Player Messages

Players get a proper message when something is blocked. Reasons are editable, built-in language files are included for English, Polish, German, Spanish, French and Italian, and there is a built-in cooldown to avoid spam.

### 🛠️ Easy Management

- add items with `/ib add <item> actions:... worlds:... preset:...`
- add the held item with `/ib addhand actions:... worlds:... preset:...`
- edit one item scope with `/ib item <item> edit scope:... ...`
- inspect the final result with `/ib info <item>`
- reload everything with `/ib reload`

### 🔑 Flexible Permissions

Use one global bypass or separate bypass permissions for each action, depending on how much control you want to give staff or ranked players.

## 🚀 Quick Start

1. Drop the jar into `plugins/`
2. Start the server once
3. Edit `config.yml`, `blocked-items.yml` and `presets.yml`
4. Use `/ib reload`
5. Check any item with `/ib info <item>`

## 💻 Commands

All important commands support tab completion:

```text
/ib add <item> [actions:...] [worlds:...] [preset:...]
/ib addhand [actions:...] [worlds:...] [preset:...]
/ib item <item> create [actions:...] [worlds:...] [preset:...]
/ib item <item> edit [scope:...] [actions:...] [worlds:...] [preset:...]
/ib item <item> delete [scope:...] [preset:...]
/ib preset set <preset>
/ib preset list
/ib preset create <name>
/ib preset <name> <info|edit|delete>
/ib info <item|preset>
/ib list
/ib help
/ib reload
```

**Aliases:** `/ib`, `/blocker`

## ⚙️ Example Config

### `blocked-items.yml`

```yml
default:
  reason: "&cThis item is blocked by the server team."

  items:
    TNT:
      actions: all
      worlds: all

    ENDER_PEARL:
      actions:
        - use
        - drop
      worlds:
        - world
        - world_nether

    IRON_HELMET:
      - actions:
          - crafting
          - drop
          - armor
        worlds:
          - world
      - actions:
          - inventory
        worlds:
          - world_nether
```

### `presets.yml`

```yml
presets:
  survival:
    description: "Main survival restrictions."
    reason: "&cThis item is disabled on survival worlds."

    items:
      COMMAND_BLOCK:
        actions: all
        worlds: all

      ELYTRA:
        actions:
          - use
        worlds:
          - world
```

## 🛡️ Permissions

```text
itemblocker.use
itemblocker.add
itemblocker.remove
itemblocker.edit
itemblocker.list
itemblocker.reload
itemblocker.addhand
itemblocker.info
itemblocker.preset
itemblocker.bypass
itemblocker.bypass.crafting
itemblocker.bypass.pickup
itemblocker.bypass.drop
itemblocker.bypass.use
itemblocker.bypass.place
itemblocker.bypass.armor
itemblocker.bypass.inventory
itemblocker.bypass.hopper
```

## 💡 Good Fits

- survival servers
- event presets
- admin-only items
- technical block restrictions
- hopper and inventory abuse prevention

## 📋 Requirements

- Java 21+
- Folia
- Paper
- Purpur
- Spigot
- Minecraft 1.21.x and 26.1

## 📄 Changelog

Project history lives in [CHANGELOG.md](CHANGELOG.md).

## 📜 License

This project is licensed under GPL-3.0. See [LICENSE](LICENSE).
