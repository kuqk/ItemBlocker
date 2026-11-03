# FAQ and troubleshooting

## My rule does nothing

Most often you are testing as an operator. Operators have `itemblocker.bypass` and ignore every restriction. Test with a player who is not an operator, or set `bypass.enabled: false` in [config.yml](reference/config.md) and run `/ib reload`.

If that is not it:

* `/ib show <item>` lists every rule for the item and the preset it is in.
* Check the preset's worlds and regions with `/ib preset show <name>`.
* Check the console for warnings about `rules.yml`.

## A player still has a blocked item

Rules stop new actions; items players already had stay. Use [`/ib scan`](features/scanning.md) to find and remove or confiscate them, or [limits](rules/limits.md) and [enchantment modes](rules/enchantments.md), which fix items as soon as players have them.

## How do I let players keep an item but not get new ones?

```
/ib block mace actions:obtain
```

See [Actions](rules/actions.md#keywords).

## How do I block one custom item from another plugin?

Use a [filter](rules/filters.md): hold the item and run `/ib filter add <id> hand actions:...`.

## How do I block every sword except one?

```
/ib group add *_sword actions:use
/ib allow add wooden_sword
```

See [Item groups and allowed items](rules/groups-and-allowed.md).

## Does it work on Folia?

Yes. Everything runs on the thread that owns the player, block or chunk.

## Does it work on Spigot?

Yes. Some extras use Paper when it is there: for example, on Paper armor put on by other plugins or commands is also checked.

## I edited rules.yml and now nothing changes

`/ib reload` loads the file again. If the file has a YAML error, the console says where, the previous rules stay active, and commands refuse to change rules until the file is fixed and reloaded.

## Messages show in the wrong language

Set `language:` in `config.yml` and run `/ib reload`. Your own edits in `lang/` win over the bundled texts; delete a file to get a fresh copy.

## Where do I report a bug?

On [GitHub issues](https://github.com/kuqk/ItemBlocker/issues), with your server version, ItemBlocker version and the relevant part of `rules.yml`.
