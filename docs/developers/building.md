# Building from source

You need Java 21 to build. The plugin itself is compiled for Java 17.

```bash
git clone https://github.com/kuqk/ItemBlocker.git
cd ItemBlocker
./gradlew build
```

The jar is written to `build/libs/ItemBlocker-<version>.jar`.

## Useful tasks

| Task | Does |
|---|---|
| `./gradlew test` | Runs the tests, on a mock server |
| `./gradlew runServer` | Starts a Paper 1.21.11 test server with the plugin |
| `./gradlew runServer -Pmc=26.3` | The same with another Minecraft version |

## Layout

| Folder | Contains |
|---|---|
| `src/main` | The plugin, compiled against the Spigot API so it runs on every platform |
| `src/paper` | Extras for Paper and its forks, such as the Folia schedulers |
| `src/integrations` | Hooks into WorldGuard and PlaceholderAPI, loaded only when they are installed |
| `src/modern` | Listeners for events of newer versions, such as the 1.21 crafter |
| `docs` | This documentation |

Continuous integration checks every pull request against the Spigot and Paper APIs of every supported version, from 1.20 to 26.3, so the jar keeps loading everywhere.

## Contributing

Issues and pull requests are welcome on [GitHub](https://github.com/kuqk/ItemBlocker).
