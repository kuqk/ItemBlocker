# config.yml

`plugins/ItemBlocker/config.yml` holds the settings. Rules live in [rules.yml](rules-file.md). Run `/ib reload` after editing it.

```yaml
config-version: 2

# Language of plugin messages: en, pl, de, es, fr, it, or your own file in lang/
language: en

messages:
  # Minimum time between two restriction messages to the same player, in milliseconds.
  cooldown-ms: 1000

bypass:
  # Players with itemblocker.bypass ignore every restriction.
  enabled: true
  # Players with itemblocker.bypass.<action> ignore restrictions for that action.
  per-action: true

log:
  # Write every blocked action to logs/<date>.log.
  enabled: true
  discord:
    # Discord webhook that gets an alert for every blocked action. Empty turns it off.
    webhook: ""
    # Minimum time between two alerts about the same player, action and item, in seconds.
    cooldown-seconds: 60
```

| Setting | Default | Description |
|---|---|---|
| `language` | `en` | The [message file](messages.md) to use |
| `messages.cooldown-ms` | `1000` | Keeps holding right click from flooding the chat with messages |
| `bypass.enabled` | `true` | Whether the global `itemblocker.bypass` permission works |
| `bypass.per-action` | `true` | Whether `itemblocker.bypass.<action>` and similar work |
| `log.enabled` | `true` | Whether blocked actions are written to [log files](../features/logs.md) |
| `log.discord.webhook` | empty | A Discord webhook URL for [alerts](../features/logs.md#discord-alerts) |
| `log.discord.cooldown-seconds` | `60` | Minimum time between two alerts about the same player, action and item |

Settings missing from the file use their default.

The two bypass switches are independent. To test rules as an operator, set both `bypass.enabled: false` and `bypass.per-action: false`, then run `/ib reload`. Operators inherit the per-action permissions from `itemblocker.bypass`, so disabling only the global check is not enough.
