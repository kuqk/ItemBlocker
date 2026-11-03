# Permissions

## Commands

| Permission | Allows | Default |
|---|---|---|
| `itemblocker.use` | Using `/ib` at all | op |
| `itemblocker.add` | `/ib block` for items | op |
| `itemblocker.edit` | `/ib edit` | op |
| `itemblocker.remove` | `/ib unblock` for items | op |
| `itemblocker.limit` | `/ib limit` | op |
| `itemblocker.filter` | `/ib filter` | op |
| `itemblocker.group` | `/ib group` | op |
| `itemblocker.allow` | `/ib allow` | op |
| `itemblocker.enchantment` | Blocking and unblocking enchantments | op |
| `itemblocker.potion` | Blocking and unblocking potion effects | op |
| `itemblocker.info` | `/ib show` and `/ib preset show` | op |
| `itemblocker.list` | `/ib list` and `/ib preset list` | op |
| `itemblocker.preset` | Creating, editing and deleting presets, `/ib template` | op |
| `itemblocker.gui` | `/ib gui` | op |
| `itemblocker.scan` | `/ib scan` and `/ib confiscated` | op |
| `itemblocker.log` | `/ib log` | op |
| `itemblocker.reload` | `/ib reload` | op |
| `itemblocker.admin` | All of the above | op |
| `itemblocker.*` | `itemblocker.admin` and `itemblocker.bypass` | op |

## Bypassing

| Permission | Ignores | Default |
|---|---|---|
| `itemblocker.bypass` | Every restriction | op |
| `itemblocker.bypass.<action>` | One [action](../rules/actions.md), for example `itemblocker.bypass.place` | no |
| `itemblocker.bypass.enchantment` | Enchantment limits | no |
| `itemblocker.bypass.potion` | Potion effect limits | no |
| `itemblocker.bypass.limit` | Stack and inventory limits | no |
| `itemblocker.bypass.preset.<name>` | Everything in one preset | no, see below |
| A permission set with `bypass:` | Just that rule | no, see below |

The per-action permissions exist for `crafting`, `pickup`, `drop`, `use`, `place`, `armor`, `inventory`, `hopper`, `smithing`, `attack`, `break`, `crafter`, `dispense`, `brew`, `trade`, `loot` and `entity_drop`. The ones for actions without a player only matter for [scans](../features/scanning.md) of a player's items.

Preset and rule permissions only count when granted explicitly, so operators do not get them just for being operators. See [Exceptions and bypassing](../rules/exceptions.md).

{% hint style="warning" %}
Operators have `itemblocker.bypass` by default and ignore every restriction. Test with a player who is not an operator, or set `bypass.enabled: false`.
{% endhint %}
