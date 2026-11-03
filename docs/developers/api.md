# API

Other plugins can check ItemBlocker's rules, add temporary rules and let blocked actions through. This is useful for plugins with their own ranks, quests and game modes.

## Dependency

ItemBlocker is published through [JitPack](https://jitpack.io/#kuqk/ItemBlocker). Use the tag of a release, such as `v2.6.0`.

{% tabs %}
{% tab title="Gradle (Kotlin)" %}
```kotlin
repositories {
    maven("https://jitpack.io")
}

dependencies {
    compileOnly("com.github.kuqk:ItemBlocker:v2.6.0")
}
```
{% endtab %}

{% tab title="Maven" %}
```xml
<repositories>
    <repository>
        <id>jitpack.io</id>
        <url>https://jitpack.io</url>
    </repository>
</repositories>

<dependency>
    <groupId>com.github.kuqk</groupId>
    <artifactId>ItemBlocker</artifactId>
    <version>v2.6.0</version>
    <scope>provided</scope>
</dependency>
```
{% endtab %}
{% endtabs %}

In your `plugin.yml`:

```yaml
depend: [ItemBlocker]      # or softdepend, if your plugin also works without it
```

## Getting the API

```java
ItemBlockerApi api = ItemBlockerApi.get();
```

`get()` throws `IllegalStateException` when ItemBlocker is not enabled. The API is also registered with the services manager:

```java
ItemBlockerApi api = Bukkit.getServicesManager().load(ItemBlockerApi.class);
```

## Checking rules

```java
Optional<Restriction> restriction = api.check(player, item, Action.USE);
boolean blocked = api.isBlocked(player, item, Action.PLACE);
Optional<Restriction> forHopper = api.check(item, Action.HOPPER, location);
```

`check` uses the same rules, bypass permissions, exceptions and enchantment limits as the plugin. The version with a location has no player, so bypass permissions do not apply.

`Restriction` tells why something is blocked:

| Method | Returns |
|---|---|
| `action()` | What is blocked, such as `place`, `enchanted-item` or `enchanting` |
| `subject()` | The item, or the enchantment or effect with its level, as shown to players |
| `preset()` | The name of the preset whose rule blocks it |
| `reason()` | The preset's reason, possibly empty |

`Action` is `pl.variant.itemblocker.rule.Action`, with one constant per [action](../rules/actions.md), such as `Action.USE` or `Action.ENTITY_DROP`.

## Temporary rules

Temporary rules live in memory only: they are never written to `rules.yml` and end when the server stops.

```java
// Everyone
TemporaryRule event = api.blockTemporarily(Material.ELYTRA, Set.of(Action.ARMOR), "Elytras are off during the race");

// One player, until the rule is removed or the player leaves
TemporaryRule quest = api.blockTemporarily(player, Material.ENDER_PEARL, Set.of(Action.USE), "Not during the quest");

quest.remove();
quest.isActive();   // false
```

The reason is shown to players like a preset's reason and may be empty.

## Letting actions through

`ItemBlockedEvent` is called before anything is blocked for a player. Cancel it to let the action through:

```java
@EventHandler
public void onBlocked(ItemBlockedEvent event) {
    if (minigame.isPlaying(event.getPlayer())) {
        event.setCancelled(true);
    }
}
```

| Method | Returns |
|---|---|
| `getPlayer()` | The player |
| `getItem()` | A copy of the item, or `null` for enchanting and potion effects |
| `getRestriction()` | The `Restriction`, as above |

The event is called for every check involving a player: items, enchanted items, enchanting, potion effects and scans of a player's items. It is not called for actions without a player, such as hoppers or mob drops. On Folia it is called on the player's region thread.
