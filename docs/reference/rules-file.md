# rules.yml

`plugins/ItemBlocker/rules.yml` holds every preset and rule. Commands and the menu write it, but you can also edit it by hand and run `/ib reload`.

{% hint style="info" %}
If `rules.yml` has a YAML error, the previous rules stay active and commands refuse to change them until the file is fixed and reloaded. A command can never overwrite your edits.
{% endhint %}

## Example

```yaml
config-version: 2
presets:
  default:
    description: ''
    reason: '&cThis item is disabled on this server'
    worlds: all
    regions: all
    items:
      tnt:
        actions:
        - place
        - crafting
        worlds: all
      elytra:
        actions:
        - armor
        worlds:
        - world_the_end
        bypass-permission: vip.elytra
      mace:
        actions: obtain
        worlds: all
    groups:
      '*_sword':
        actions:
        - use
        worlds: all
      '#logs':
        actions: all
        worlds: all
    allowed:
    - wooden_sword
    limits:
      ender_pearl:
        stack: 4
        inventory: 16
    filters:
      excalibur:
        type: diamond_sword
        name: Excalibur
        data: nexo:id=excalibur
        actions:
        - use
        - drop
        worlds: all
    enchantments:
      sharpness:
        level: 5
        mode: downgrade
      thorns: 1
    potions:
      strength: 2
  pvp:
    description: Arena rules
    reason: ''
    worlds:
    - arena
    regions: all
    items:
      ender_pearl:
        actions: all
        worlds: all
    groups: {}
    allowed: []
    limits: {}
    filters: {}
    enchantments: {}
    potions: {}
```

## Preset

| Key | Description |
|---|---|
| `description` | Shown in preset listings |
| `reason` | Added to messages about restrictions from this preset |
| `worlds` | `all`, or a list of worlds the whole preset applies in |
| `regions` | `all`, or a list of WorldGuard regions the whole preset applies in |
| `items` | [Item rules](../rules/items.md) by item key |
| `groups` | [Item groups](../rules/groups-and-allowed.md) by pattern; quote patterns that start with `*` or `#` |
| `allowed` | [Allowed items](../rules/groups-and-allowed.md) |
| `limits` | [Limits](../rules/limits.md) by item key, with `stack` and `inventory` |
| `filters` | [Filters](../rules/filters.md) by id |
| `enchantments` | [Enchantments](../rules/enchantments.md): a level, or a section with `level` and `mode` |
| `potions` | [Potion effects](../rules/potion-effects.md) by key, with the lowest blocked level |

## Rules

Item rules, groups and filters share these keys:

| Key | Description |
|---|---|
| `actions` | `all`, `obtain`, or a list of [actions](../rules/actions.md) |
| `worlds` | `all`, or a list of worlds the rule applies in |
| `bypass-permission` | Optional; a permission that ignores just this rule |

Filters add `type`, `name`, `lore`, `model`, `data` (a key or `key=value`) and `enchantment`; at least one is needed.

## Mistakes

Entries ItemBlocker cannot read, such as unknown items or actions, are skipped with a warning in the console. The rest of the file still loads.
