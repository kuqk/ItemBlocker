# Installation

## Requirements

| | |
|---|---|
| Server | Paper, Folia, Purpur or Spigot |
| Minecraft | 1.20 to 26.3 |
| Java | 17 or newer |

The same jar runs on every supported version. Features that need a newer version, such as the crafter from 1.21, switch on by themselves when the server has them.

## Install

1. Download `ItemBlocker-<version>.jar` from [Modrinth](https://modrinth.com/plugin/itemblocker), [Hangar](https://hangar.papermc.io/kuqk/ItemBlocker) or the [GitHub releases](https://github.com/kuqk/ItemBlocker/releases).
2. Put it in your server's `plugins/` folder.
3. Start or restart the server.

ItemBlocker creates `plugins/ItemBlocker/` with:

| File | Contains |
|---|---|
| `config.yml` | Language, message cooldown, bypass and log settings. See [config.yml](../reference/config.md). |
| `rules.yml` | All presets and rules. See [rules.yml](../reference/rules-file.md). |
| `lang/` | One message file per language. See [Messages and languages](../reference/messages.md). |
| `logs/` | One file per day with every blocked action, once something was blocked. |
| `confiscated.yml` | Items taken away by scans or enchantment rules, once there are any. |

## Optional plugins

ItemBlocker works on its own. When these plugins are installed, it hooks into them:

* [WorldGuard](../integrations/worldguard.md): limit presets to regions.
* [PlaceholderAPI](../integrations/placeholderapi.md): use its placeholders in messages.

## Updating

Replace the jar and restart. Your `rules.yml`, `config.yml` and message files are kept. New settings and messages fall back to their defaults until you add them. Read the [changelog](https://github.com/kuqk/ItemBlocker/blob/main/CHANGELOG.md) for changes in behaviour.
