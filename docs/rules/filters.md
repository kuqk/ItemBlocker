# Item filters

A filter matches items by more than their type, so you can block one specific item, such as a sword from an RPG plugin, without blocking every diamond sword.

## Criteria

Every criterion you set must match. Criteria you leave out are not checked.

| Criterion | Matches | Example |
|---|---|---|
| `type:` | the item type | `type:diamond_sword` |
| `name:` | text in the custom name, ignoring case and colors | `name:Excalibur` |
| `lore:` | text in one lore line, ignoring case and colors | `lore:Soulbound` |
| `model:` | custom model data | `model:1001` |
| `data:` | a persistent data entry, with or without a value | `data:nexo:id=ruby_sword` or `data:nexo:id` |
| `enchantment:` | an enchantment the item has | `enchantment:sharpness` |

Values with spaces go in double quotes: `name:"Sword of Kings"`.

## Make a filter from the item you hold

```
/ib filter add excalibur hand actions:use,drop
```

`hand` copies the item's type, custom name, custom model data and first plugin data entry. Options next to `hand` replace what was copied.

In the menu, open a preset, then **Item filters**, and click an item in your own inventory. The filter is named after the item and opens in the editor, where every criterion can be changed.

## Commands

```
/ib filter add <id> [hand] [type:] [name:] [lore:] [model:] [data:] [enchantment:] [actions:] [worlds:] [bypass:] [in:]
/ib filter remove <id> [in:preset]
/ib filter list [in:preset]
/ib show hand
```

Ids follow the rules of preset names: 1 to 32 characters of `a-z`, `0-9`, `_` and `-`. Adding a filter with an existing id replaces it.

## Items from other plugins

Many plugins mark their items with a persistent data entry, for example Nexo with `nexo:id` and Oraxen with `oraxen:id`. The easiest way to find it is to hold the item and run:

```
/ib filter add <id> hand
```

The answer shows what was copied, such as `data=nexo:id=ruby_sword`. If no `data=` part appears, the plugin keeps its ids where the server API cannot read them; match those items by `name:`, `lore:` or `model:` instead.

## Order

Item rules and groups are checked first, then filters. Filters still apply to [allowed items](groups-and-allowed.md).
