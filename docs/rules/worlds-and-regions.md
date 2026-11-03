# Worlds and regions

## Presets

A whole preset can be limited to some worlds, and with [WorldGuard](../integrations/worldguard.md) to some regions. Everything in the preset then only applies there.

```
/ib preset edit pvp worlds arena arena_nether
/ib preset edit pvp worlds all
/ib preset edit spawn regions spawn market
/ib preset edit spawn regions all
```

A preset limited to regions applies inside any of them, and still only in its worlds. Without WorldGuard, such a preset applies nowhere, and ItemBlocker warns about it.

## Single rules

Item rules, groups and filters can also be limited to worlds:

```
/ib block elytra actions:armor worlds:world_the_end
```

A rule applies where both its own worlds and its preset's worlds and regions include the player.

## Without a player

For hoppers, crafters, dispensers, brewing stands, loot and mob drops, the place of the block or entity counts.

## In the menu

Worlds and regions are picked from a list: click a world or region to add or remove it. **Everywhere** clears the choice. A preset's worlds and regions are under **Settings**, a rule's worlds are in its editor.
