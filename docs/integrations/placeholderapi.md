# PlaceholderAPI

With [PlaceholderAPI](https://www.spigotmc.org/resources/placeholderapi.6245/) installed, its placeholders work in every message ItemBlocker sends to a player:

```yaml
restriction:
  place: "{prefix}&c%player_name%, you cannot place &f{item}&c.{reason}"
```

* Placeholders are filled in for the player who gets the message. Messages to the console keep them as they are.
* ItemBlocker's own placeholders in braces, such as `{item}`, are filled in after PlaceholderAPI, so an item name can never be read as a placeholder.

ItemBlocker hooks into PlaceholderAPI 2.11.6 and newer. The console says `Hooked into PlaceholderAPI` on start.
