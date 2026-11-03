# Stack and inventory limits

Limits keep an item usable but cap how much of it a player has.

| Limit | Means |
|---|---|
| `stack:` | the largest stack of the item |
| `inventory:` | the most of the item a player may carry in total |

```
/ib limit <item|hand> [stack:<amount>|none] [inventory:<amount>|none] [in:preset]
```

```
/ib limit ender_pearl stack:4 inventory:16
/ib limit totem_of_undying inventory:1
/ib limit ender_pearl stack:none            # remove the stack limit, keep the other
```

Options you leave out keep their value. `/ib list limits` lists them and `/ib show <item>` shows them next to the item's rules.

## How limits are kept

* **Picking up** only takes as much as the player may carry and as fits into stacks within the limit. The rest stays on the ground.
* **Taking items out of a container** over the inventory limit is denied.
* **Anything else**, such as crafting or items from other plugins, is fixed right away: stacks above the limit are split into free slots, and what does not fit or exceeds the inventory limit is dropped at the player's feet.
* Items are never deleted.

Limits are also checked when players join, change worlds and respawn.

## Several presets

If several presets that apply where the player is limit the same item, the smallest limit wins.

## Bypassing

`itemblocker.bypass.limit` ignores every limit. See [Exceptions and bypassing](exceptions.md).

## In the menu

Open an item's editor: left click on the stack or inventory button adds 1, right click takes 1 away, with shift 16. At 0 there is no limit.
