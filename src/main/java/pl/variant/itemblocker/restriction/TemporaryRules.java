package pl.variant.itemblocker.restriction;

import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import pl.variant.itemblocker.api.TemporaryRule;
import pl.variant.itemblocker.rule.Action;
import pl.variant.itemblocker.rule.ItemRule;
import pl.variant.itemblocker.rule.Preset;
import pl.variant.itemblocker.rule.RuleBook;
import pl.variant.itemblocker.rule.WorldScope;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Rules other plugins add through the API. Each rule is a small preset of its own that lives only in
 * memory: rules for everyone are added to the rule book used for checks, rules for one player are
 * checked separately and dropped when the player leaves.
 */
public final class TemporaryRules implements Listener {

    // Saved preset names cannot start with '~', so temporary presets never clash with them.
    private static final String NAME_PREFIX = "~temporary-";

    private final AtomicInteger ids = new AtomicInteger();
    private final AtomicInteger version = new AtomicInteger();
    private final Map<String, Preset> global = new ConcurrentHashMap<>();
    private final Map<UUID, Map<String, Preset>> players = new ConcurrentHashMap<>();
    private volatile Merged merged = new Merged(null, -1, null);

    private record Merged(RuleBook base, int version, RuleBook result) {
    }

    /**
     * Returns the rule book with the temporary rules for everyone added.
     */
    public RuleBook merge(RuleBook base) {
        Merged current = merged;
        int currentVersion = version.get();
        if (current.base() == base && current.version() == currentVersion) {
            return current.result();
        }

        RuleBook result = base;
        for (Preset preset : global.values()) {
            result = result.with(preset);
        }
        merged = new Merged(base, currentVersion, result);
        return result;
    }

    public Collection<Preset> forPlayer(UUID player) {
        Map<String, Preset> rules = players.get(player);
        return rules == null ? List.of() : List.copyOf(rules.values());
    }

    /**
     * Adds a rule for everyone, or for one player if {@code player} is given.
     */
    public TemporaryRule add(UUID player, Material material, Set<Action> actions, String reason) {
        String name = NAME_PREFIX + ids.incrementAndGet();
        Preset preset = Preset.empty(name)
                .withReason(reason)
                .withItem(material, new ItemRule(actions, WorldScope.ALL));
        Map<String, Preset> target = player == null ? global : players.computeIfAbsent(player, key -> new ConcurrentHashMap<>());
        target.put(name, preset);
        version.incrementAndGet();
        return new TemporaryRule() {
            @Override
            public void remove() {
                if (target.remove(name) != null) {
                    version.incrementAndGet();
                }
            }

            @Override
            public boolean isActive() {
                return target.containsKey(name);
            }
        };
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        Map<String, Preset> removed = players.remove(event.getPlayer().getUniqueId());
        if (removed != null) {
            removed.clear();
        }
    }
}
