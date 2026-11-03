# Logs and Discord alerts

## Log files

Every blocked action is written to `plugins/ItemBlocker/logs/<date>.log`, one line each:

```
2026-11-02 17:40:01 | Steve | place | TNT | world | default
```

The columns are time, player, action, item, world and preset. Enchantments and effects are written with their level, such as `Sharpness 5`, and fixed enchantments as `enchantment-downgraded` and similar.

Holding a blocked item and clicking again within a second is only logged once. Files are written in the background, so a slow disk never holds up the server.

## /ib log

```
/ib log            # the last ten blocked actions
/ib log <player>   # the last ten of one player
```

It needs `itemblocker.log` and shows the most recent of the last 500 actions since the server started.

## Discord alerts

1. In Discord, open the channel settings, then **Integrations**, **Webhooks**, and create a webhook. Copy its URL.
2. Put it in `config.yml`:

```yaml
log:
  enabled: true
  discord:
    webhook: "https://discord.com/api/webhooks/..."
    cooldown-seconds: 60
```

3. Run `/ib reload`.

Each blocked action is posted to the channel, at most once per `cooldown-seconds` for the same player, action and item. Alerts never ping anyone, even if an item is named `@everyone`. If Discord cannot be reached, a warning is logged at most once a minute.

## Turning logging off

Set `log.enabled: false`. Discord alerts still work when a webhook is set.
