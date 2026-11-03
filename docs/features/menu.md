# The menu

`/ib gui` sets up nearly everything without commands. It needs `itemblocker.gui`.

## Presets

The first screen lists every preset. The bottom row has buttons to:

* **create a preset**: type its name in the chat,
* **start from a template**: Survival, Lifesteal or PvP, see [Preset templates](templates.md).

Click a preset to open it.

## A preset

| Section | What you can do |
|---|---|
| **Items** | Every item with a rule or limit. Click an item in your own inventory, or use the search, to add one. Click an entry to edit it, shift + right click to remove it |
| **Item groups** | Add a group by typing a pattern such as `*_sword` or `#swords` in the chat |
| **Item filters** | Click an item in your own inventory to make a filter from it; it is named after the item |
| **Allowed items** | Click an item in your own inventory, or search, to allow it. Click an entry to remove it |
| **Enchantments** | Left click +1 level, right click -1, shift + right click unblock, shift + left click change the [mode](../rules/enchantments.md) |
| **Potion effects** | Left click +1 level, right click -1, shift + right click unblock |
| **Settings** | Description and reason (typed in the chat), worlds and WorldGuard regions (picked from a list) |

Presets other than `default` can be deleted with shift + right click on the delete button.

## The rule editor

Items, groups and filters share one editor:

* a button for every [action](../rules/actions.md): red is blocked, green is allowed,
* **Worlds**: pick them from a list,
* **Bypass permission**: type it in the chat, `none` removes it,
* for items, the **stack** and **inventory** [limits](../rules/limits.md): left click +1, right click -1, with shift 16,
* for filters, every [criterion](../rules/filters.md): type, name, lore, model data, plugin data and enchantment,
* **Remove** deletes the rule.

## Item search

The search shows every item, 45 per page. Click the search button and type part of a name, such as `diamond chest` or `ender_p`; right click shows every item again. Clicking an item opens its editor, or allows it in the allowed items.

## Typing in the chat

When a menu needs free text, it closes and asks you to type it in the chat. Your message is not shown to anyone. Click `cancel` in that message, or type it or the word from your language file such as `anuluj`, to go back without changing anything, or type `none` to clear a value.

## Saving

Every change is saved to `rules.yml` right away, exactly as if you had used a command.
