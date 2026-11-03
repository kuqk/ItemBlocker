# Exceptions and bypassing

There are four ways to let someone ignore restrictions, from broad to narrow.

| Permission | Ignores |
|---|---|
| `itemblocker.bypass` | every restriction |
| `itemblocker.bypass.<action>` | one action, for example `itemblocker.bypass.place`; also `enchantment`, `potion` and `limit` |
| `itemblocker.bypass.preset.<name>` | everything in one preset, for example `itemblocker.bypass.preset.pvp` |
| the rule's own permission, set with `bypass:` | just that one rule |

## Operators

`itemblocker.bypass` is given to operators by default, so **operators ignore every restriction**. Test rules with a player who is not an operator, or set `bypass.enabled: false` in [config.yml](../reference/config.md).

`bypass.per-action: false` turns off the per-action permissions.

## Preset and rule permissions

These are meant for ranks. They only count when they are granted explicitly, for example with a permission plugin, so operators do not get them just for being operators.

```
/ib block elytra actions:armor bypass:vip.elytra
/ib group add *_sword actions:use bypass:rank.knight in:pvp
/ib filter add excalibur hand bypass:quest.excalibur
/ib edit elytra bypass:none                   # remove the exception
```

```
/lp group vip permission set vip.elytra true
/lp group arena-staff permission set itemblocker.bypass.preset.pvp true
```

In the menu, the rule editor has a button for the bypass permission; type it in the chat, or `none` to remove it.

## What bypassing does not cover

Actions without a player, such as hoppers, dispensers, crafters, loot and mob drops, are never bypassed. The [API](../developers/api.md) can let single actions through with `ItemBlockedEvent`.
