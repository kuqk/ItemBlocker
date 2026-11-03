# Upgrading from 1.x

ItemBlocker 2 stores its rules in a new format. When it starts and finds the files of 1.x, it converts them automatically.

## What happens

1. `blocked-items.yml`, `presets.yml` and the old `config.yml` are read. This only happens while there is no `rules.yml` yet.
2. Every preset and rule is written to the new `rules.yml`, and settings to the new `config.yml`.
3. The old files and the old `languages` folder are moved to `plugins/ItemBlocker/backup-1.x/`, so nothing is lost. If that folder exists already, `backup-1.x-2` and so on are used.

The console says what was converted. Rules for items the server does not know are skipped with a warning. Preset names that are not valid any more are changed to valid ones, for example `My Preset` becomes `my-preset`.

## What you should check

* **Custom message texts are not converted.** Messages now live in `lang/<language>.yml`. Copy your texts there by hand. See [Messages and languages](../reference/messages.md).
* **Commands changed.** `/ib block`, `/ib edit`, `/ib unblock` and the others take options such as `actions:` and `in:`. See [Commands](../reference/commands.md).
* **Permissions changed.** See [Permissions](../reference/permissions.md).

## Going back

Stop the server, remove the 2.x jar and `rules.yml`, move the files from `backup-1.x/` back into `plugins/ItemBlocker/` and install the 1.x jar again.
