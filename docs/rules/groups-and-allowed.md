# Item groups and allowed items

## Item groups

A group blocks actions for every item type matching a pattern, so one rule covers many items.

| Pattern | Matches |
|---|---|
| `*_sword` | every sword |
| `netherite_*` | every netherite item |
| `*shulker_box` | every shulker box |
| `#swords` | the Minecraft item tag `minecraft:swords` |
| `#minecraft:logs` | every log, through its tag |

`*` stands for any text. Tags come from Minecraft and data packs, so a group follows them when a data pack adds items.

```
/ib group add <pattern> [actions:...] [worlds:...] [bypass:...] [in:preset]
/ib group remove <pattern> [in:preset]
/ib group list [in:preset]
```

```
/ib group add *_sword actions:use in:pvp
/ib group add #logs actions:place
```

A new group blocks every action until you say otherwise. In the menu, open a preset, then **Item groups**, and click the add button to type a pattern.

## Allowed items

Allowed items are never blocked by the preset's item rules and groups. Together with a group, they block a whole category except a few items:

```
/ib group add *_sword actions:use in:pvp
/ib allow add wooden_sword in:pvp
/ib allow add stone_sword in:pvp
```

```
/ib allow add <item|hand> [in:preset]
/ib allow remove <item|hand> [in:preset]
/ib allow list [in:preset]
```

Allowing only affects the preset it is in. [Filters](filters.md) still apply to allowed items, because they target specific items on purpose.

In the menu, open a preset, then **Allowed items**, and click an item in your own inventory or search for one.
