# LuckPerms and other permission plugins

ItemBlocker uses normal server permissions, so it works with LuckPerms and any other permission plugin without setup. Every permission is listed on [Permissions](../reference/permissions.md).

## Examples with LuckPerms

Let moderators manage rules:

```
/lp group moderator permission set itemblocker.admin true
```

Let VIPs wear elytras although a rule blocks them:

```
/ib block elytra actions:armor bypass:vip.elytra
/lp group vip permission set vip.elytra true
```

Let one group ignore a whole preset:

```
/lp group builder permission set itemblocker.bypass.preset.survival true
```

Only in one world, with a context:

```
/lp group vip permission set itemblocker.bypass.use true world=arena
```

## Operators

Operators get `itemblocker.bypass` by default and ignore every restriction. To have operators follow the rules, deny it:

```
/lp user <name> permission set itemblocker.bypass false
```

or set both `bypass.enabled: false` and `bypass.per-action: false` in [config.yml](../reference/config.md), then run `/ib reload`.
