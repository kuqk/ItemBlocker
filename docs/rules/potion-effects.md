# Potion effects

A potion effect limit blocks an effect from a minimum level, wherever it comes from: potions, beacons, commands or other plugins.

```
/ib block potion <effect> [level:<n>] [in:preset]
/ib unblock potion <effect> [in:preset]
/ib show potion <effect>
/ib list potions [in:preset]
```

```
/ib block potion strength level:2      # Strength I is fine, Strength II is not
/ib block potion invisibility          # every level
```

Levels count from 1, as shown in game: Strength II is level 2.

## What happens

* The effect is not applied, and the player is told.
* Players lose a blocked effect they already have when they join or change worlds.
* Brewing stands do not brew potions that give a blocked effect. See the `brew` [action](actions.md).

## Bypassing

`itemblocker.bypass.potion` ignores every potion effect limit. A [preset bypass](exceptions.md) ignores the limits of that preset.

## In the menu

Open a preset, then **Potion effects**. Left click raises the level, right click lowers it, shift + right click unblocks it.
