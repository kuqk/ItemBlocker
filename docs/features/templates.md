# Preset templates

Templates are ready-made presets. Creating one makes a normal preset that you can change like any other.

```
/ib template                         # list the templates
/ib template <name> [as:<preset>]    # create a preset from one
```

```
/ib template lifesteal
/ib template survival as:main
```

Without `as:`, the preset is named after the template. In the menu, use the templates button at the bottom of the preset list. It needs `itemblocker.preset`.

## survival

Keeps creative-only and operator items out of survival.

* Every action blocked: bedrock, barrier, light, structure void, structure block, jigsaw, command blocks, command block minecart, debug stick, knowledge book.
* Spawners cannot be placed, traded for or found in loot.
* End portal frames and reinforced deepslate cannot be placed.

## lifesteal

Common rules of lifesteal servers.

* End crystals and respawn anchors cannot be placed; TNT minecarts cannot be placed or crafted.
* Maces cannot be obtained.
* At most one totem of undying, one enchanted golden apple and 16 golden apples.
* Sharpness V is lowered to IV and Protection IV to III; Thorns is removed.
* No Strength II.

## pvp

Fair fights without one-shot explosions.

* End crystals, respawn anchors, TNT and TNT minecarts cannot be placed.
* No attacking with maces.
* At most one totem of undying, one enchanted golden apple and 16 ender pearls.
* Sharpness V and Power V are lowered to IV.
* No Strength II and no Resistance III.

Items a server does not know, such as the mace before 1.21, are left out.
