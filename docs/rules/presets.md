# Presets

Every rule lives in a preset. A preset groups rules that belong together, for example everything for an arena or an event, and says where they apply.

## The default preset

The `default` preset always exists and applies everywhere unless you limit it. Commands that take `in:<preset>` use `default` when you leave it out.

## What a preset holds

| Part | Page |
|---|---|
| Item rules: which actions are blocked for an item type | [Item rules](items.md) |
| Item groups: rules for every item matching a pattern | [Item groups and allowed items](groups-and-allowed.md) |
| Allowed items: item types the preset never blocks | [Item groups and allowed items](groups-and-allowed.md) |
| Item filters: rules for specific items | [Item filters](filters.md) |
| Stack and inventory limits | [Stack and inventory limits](limits.md) |
| Blocked enchantment levels | [Enchantments](enchantments.md) |
| Blocked potion effect levels | [Potion effects](potion-effects.md) |
| Worlds and WorldGuard regions the whole preset applies in | [Worlds and regions](worlds-and-regions.md) |
| A description, shown in listings | |
| A reason, added to the messages players get | |

## Managing presets

```
/ib preset create pvp
/ib preset edit pvp description Arena rules
/ib preset edit pvp reason &cNot in the arena
/ib preset edit pvp worlds arena arena_nether
/ib preset list
/ib preset show pvp
/ib preset delete pvp
```

Names are 1 to 32 characters of `a-z`, `0-9`, `_` and `-`. They are lowercase; `PvP` and `pvp` are the same preset.

Rules are added to a preset with `in:`:

```
/ib block ender_pearl actions:use in:pvp
```

In the menu, presets are created with the button at the bottom of the preset list, and a preset's description, reason, worlds and regions are under **Settings**. Presets can also be created from [templates](../features/templates.md).

## When several presets block the same thing

Presets are checked in order: `default` first, then the others alphabetically. The first preset that blocks an action decides, and its reason is shown to the player. For [limits](limits.md), the smallest limit of all presets that apply wins.

## The reason

A preset's reason is added to every message about a restriction from that preset:

```
[ItemBlocker] You cannot place TNT. (Raids are disabled)
```

It supports `&` colors and hex colors such as `&#ff8800`. `/ib preset edit <name> reason` without text removes it.
