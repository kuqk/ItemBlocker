# Messages and languages

Every text players and admins see comes from `plugins/ItemBlocker/lang/<language>.yml`.

## Languages

| Code | Language |
|---|---|
| `en` | English |
| `pl` | Polish |
| `de` | German |
| `es` | Spanish |
| `fr` | French |
| `it` | Italian |

Set it with `language:` in [config.yml](config.md) and run `/ib reload`.

To add a language, copy `en.yml` to for example `nl.yml`, translate it and set `language: nl`.

## Item, enchantment and effect names

Names of items, enchantments and potion effects are translated by the game itself, so every player sees them in the language of their own Minecraft: `Jabłko` for a Polish player, `Apple` for an English one. Items with a custom name keep that name, and the console and logs use English names. In the menu, item icons also show their translated name; enchantment and effect icons are in English.

## Editing

The files are copied to the `lang` folder on the first start and are yours to edit. Entries missing from your file use the bundled text, so you only need to keep the ones you change, and new texts from updates work without touching your file. To get a fresh copy, delete the file and run `/ib reload`.

```yaml
prefix: "&8[&5ItemBlocker&8]&r "

restriction:
  place: "{prefix}&cYou cannot place &f{item}&c.{reason}"
```

* `&` color codes such as `&c`, and hex colors such as `&#ff8800`.
* `{prefix}` is the prefix; `{reason}` is the preset's reason, empty when it has none.
* Other placeholders depend on the message, such as `{item}`, `{preset}`, `{level}` or `{enchantment}`. Keep the ones the bundled text uses.
* With [PlaceholderAPI](../integrations/placeholderapi.md), its placeholders such as `%player_name%` work in every message sent to a player.

## Item names

Messages show readable names: `Diamond Chestplate` instead of `diamond_chestplate`, `Fire Aspect` instead of `fire_aspect`. Items with a custom name, including items from other plugins, are shown by that name without its colors.
