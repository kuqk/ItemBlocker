# Enchantments

An enchantment limit blocks an enchantment from a minimum level. Its mode says what happens to items that carry it.

```
/ib block enchantment <name> [level:<n>] [mode:block|downgrade|remove|confiscate] [in:preset]
/ib unblock enchantment <name> [in:preset]
/ib show enchantment <name>
/ib list enchantments [in:preset]
```

`level:` defaults to 1, so without it every level is blocked. Names are the Minecraft keys, such as `sharpness` or `minecraft:sharpness`; enchantments from data packs work with their namespace.

## Modes

Examples for Sharpness blocked from level 5:

| Mode | A Sharpness V sword |
|---|---|
| `block` (the default) | is blocked for every [action](actions.md). Enchanting tables stop offering Sharpness V and anvils do not produce it |
| `downgrade` | becomes Sharpness IV, the highest allowed level |
| `remove` | loses Sharpness |
| `confiscate` | is taken away and kept with [confiscated items](../features/scanning.md#confiscated-items) |

```
/ib block enchantment sharpness level:5 mode:downgrade
/ib block enchantment thorns mode:remove
/ib block enchantment mending mode:confiscate
```

`/ib show` and `/ib list` name the mode, for example `from level 5 (lowered)`.

## When items are fixed

Items with `downgrade`, `remove` and `confiscate` are fixed as soon as a player has them: after joining, changing worlds, respawning, picking up, clicking in an inventory, closing one and enchanting. Enchanted books are fixed the same way. Enchanting tables offer such enchantments, and the result is fixed right after. The player is told what changed.

Items in containers are not changed until a player takes them. To fix those, see [Scanning existing items](../features/scanning.md).

## Bypassing

`itemblocker.bypass.enchantment` ignores every enchantment limit. A [preset bypass](exceptions.md) ignores the limits of that preset.

## In the menu

Open a preset, then **Enchantments**. Left click raises the level an enchantment is blocked from, right click lowers it, shift + right click unblocks it, and shift + left click changes the mode. Blocked enchantments have a red name.
