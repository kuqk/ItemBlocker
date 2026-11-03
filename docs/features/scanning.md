# Scanning existing items

Rules stop new actions. Items players got before a rule existed stay where they are. A scan finds them, which is useful when you add rules to a server that has been running for months.

```
/ib scan players [remove|confiscate] [actions:...]
/ib scan containers [remove|confiscate] [actions:...] [radius:<chunks>] [world:<name>]
```

It needs `itemblocker.scan`.

## What is scanned

| Target | Checks |
|---|---|
| `players` | the inventory, armor, off hand and ender chest of every online player |
| `containers` | chests, barrels, shulker boxes, hoppers and other containers in loaded chunks around you (`radius:`, 4 chunks by default, at most 16), or in a whole world with `world:<name>` |

From the console, `containers` needs `world:`.

## What counts as forbidden

By default, an item that players may not pick up, or that carries a [blocked enchantment](../rules/enchantments.md). Use `actions:` to count other actions:

```
/ib scan players actions:use,place
/ib scan containers world:world actions:all
```

Players' own bypass permissions and exceptions apply.

## Report first

Without `remove` or `confiscate`, the scan only reports what it found, grouped by player or container, item and preset:

```
Forbidden items in players: 3
 • Steve: 8x TNT (default)
 • Alex: 1x Netherite Sword (pvp)
Add remove or confiscate to the command to take them away.
```

Then run it again with:

* `remove` to delete the items,
* `confiscate` to move them to `confiscated.yml`.

## Confiscated items

```
/ib confiscated                     # who had items taken, and how many stacks
/ib confiscated take <owner>        # give them to you
```

The owner is a player name, or the location of a container such as `world 120 64 -30`. Items that do not fit into your inventory are dropped at your feet. [Enchantments in `confiscate` mode](../rules/enchantments.md) put their items here too.

## Folia

Scans run on the thread that owns each player and chunk, so they also work on Folia.
