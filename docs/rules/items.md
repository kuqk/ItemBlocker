# Item rules

An item rule blocks [actions](actions.md) for one item type, optionally only in some worlds.

## Block

```
/ib block <item|hand> [actions:...] [worlds:...] [bypass:...] [in:preset]
```

| Option | Default | Example |
|---|---|---|
| `actions:` | every action | `actions:place,use` or `actions:obtain` |
| `worlds:` | every world | `worlds:world_nether,world_the_end` |
| `bypass:` | none | `bypass:vip.elytra`, see [Exceptions](exceptions.md) |
| `in:` | `default` | `in:pvp` |

`hand` means the item in your main hand. Item names are the Minecraft keys, such as `diamond_sword` or `minecraft:diamond_sword`, and tab completion suggests them.

`block` adds to an existing rule: blocking `place` and then `use` blocks both.

## Change

```
/ib edit <item|hand> [actions:...] [worlds:...] [bypass:...] [in:preset]
```

`edit` replaces only the parts you give:

```
/ib edit tnt actions:crafting       # now only crafting is blocked
/ib edit tnt worlds:all             # in every world again
/ib edit elytra bypass:none         # no exception any more
```

## Remove

```
/ib unblock <item|hand> [in:preset]
```

## See

```
/ib show <item|hand>
/ib list items [in:preset]
```

`/ib show hand` also lists the [filters](filters.md) that match the item you hold.

## In the menu

Open a preset, then **Items**. Click an item in your own inventory, or search every item with the button at the bottom, to open its editor. The editor has a button for every action (red is blocked, green is allowed), the worlds, the bypass permission and the item's [limits](limits.md).

## Items from other plugins

An item rule covers every item of its type, including custom items from other plugins that are built on that type. To block one specific custom item, use a [filter](filters.md).
