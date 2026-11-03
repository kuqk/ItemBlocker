# Actions

An action is something done with an item. Item rules, groups and filters say which actions they block.

## Player actions

| Action | Blocks |
|---|---|
| `crafting` | Taking the item from a crafting result |
| `pickup` | Picking the item up |
| `drop` | Dropping the item |
| `use` | Right clicking with the item, eating or drinking it |
| `place` | Placing it as a block, emptying it as a bucket, or placing it as an entity: boats, minecarts, armor stands, item frames, paintings and end crystals |
| `armor` | Wearing it, however it is put on: right click, inventory, dispenser. Blocked armor is taken off when a player joins or changes worlds |
| `inventory` | Moving it into or out of chests and other containers |
| `smithing` | Getting it from a smithing table |
| `attack` | Hitting a mob or player while holding it |
| `break` | Breaking it as a block, or breaking any block while holding it as a tool |
| `trade` | Getting it from a villager or wandering trader |

## Actions without a player

These happen in the world, so player bypass permissions do not apply.

| Action | Blocks |
|---|---|
| `hopper` | Hoppers moving or picking it up. The hopper moves the next allowed item instead, so a blocked item never stops it |
| `crafter` | Crafters (1.21 and newer) making it |
| `dispense` | Dispensers and droppers using it |
| `brew` | Brewing stands making it. Potions with an effect a preset blocks are not brewed either |
| `loot` | It appearing in chests and other generated loot; the rest of the loot is kept |
| `entity_drop` | Mobs dropping it when they die. Players always drop their own items, so nothing a player owns is ever deleted |

## Keywords

| Keyword | Means |
|---|---|
| `all` | Every action. Rules saved as `all` also cover actions added in later versions |
| `obtain` | Every way of getting an item: `crafting`, `crafter`, `smithing`, `brew`, `trade`, `loot`, `entity_drop` and `pickup` |
| `none` | No action |

`obtain` lets players keep and use an item they have but not get more of it:

```
/ib block mace actions:obtain
```

Actions are written as a comma separated list:

```
/ib block tnt actions:place,crafting,dispense
```

## Enchanted items

An item carrying an enchantment that is [blocked](enchantments.md) in `block` mode is blocked for every action, whatever the item rules say.
