# Quick start

{% hint style="warning" %}
Operators bypass every restriction by default. Test your rules with a player who is not an operator, or set both `bypass.enabled: false` and `bypass.per-action: false` in [config.yml](../reference/config.md) and run `/ib reload`.
{% endhint %}

## Block your first item

```
/ib block tnt actions:place
```

TNT can no longer be placed. Players see `[ItemBlocker] You cannot place TNT.` Without `actions:`, every action is blocked.

```
/ib block elytra actions:armor worlds:world_the_end
```

Elytras cannot be worn, but only in the End.

## Check and undo

```
/ib show tnt
/ib list items
/ib unblock tnt
```

## Use the menu

```
/ib gui
```

Open a preset, then **Items**, and click an item in your own inventory to set its rules. See [The menu](../features/menu.md).

## Start from a template

```
/ib template survival
```

Creates a `survival` preset that keeps creative-only items such as bedrock, barriers and command blocks out of survival. See [Preset templates](../features/templates.md).

## Some more examples

```
/ib block mace actions:obtain                         # keep your mace, but get no new ones
/ib limit ender_pearl stack:4 inventory:16            # at most 16 pearls, 4 per stack
/ib block enchantment sharpness level:5 mode:downgrade  # Sharpness V becomes IV
/ib block potion strength level:2                     # no Strength II
/ib group add *_sword actions:use in:pvp              # no swords in the pvp preset...
/ib allow add wooden_sword in:pvp                     # ...except wooden ones
/ib filter add excalibur hand actions:use,drop        # block the exact sword you hold
```

## Where to go next

* [Presets](../rules/presets.md) explains how rules are grouped and where they apply.
* [Actions](../rules/actions.md) lists everything that can be blocked.
* [Commands](../reference/commands.md) has every command and option.
