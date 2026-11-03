# WorldGuard

With [WorldGuard](https://enginehub.org/worldguard) installed, a preset can be limited to regions. Everything in the preset then only applies inside them.

```
/ib preset create spawn
/ib preset edit spawn regions spawn market
/ib block tnt in:spawn
```

* A preset applies inside any of its regions, and still only in its worlds.
* `regions all` removes the limit.
* Tab completion and the menu offer the regions of every loaded world.
* Regions are only looked up when some preset uses them, so servers without region presets pay nothing.

Without WorldGuard, a preset limited to regions applies nowhere. ItemBlocker warns about it in the console and after the command.

ItemBlocker hooks into WorldGuard 7.0.9 and newer. The console says `Hooked into WorldGuard` on start.
