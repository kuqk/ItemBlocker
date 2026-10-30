package pl.variant.itemblocker.rule;

import java.util.Collection;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/**
 * The worlds a rule applies in. An empty set of worlds means every world.
 *
 * @param worlds lowercase world names
 */
public record WorldScope(Set<String> worlds) {

    public static final WorldScope ALL = new WorldScope(Set.of());

    private static final String ALL_KEYWORD = "all";

    public WorldScope {
        Set<String> normalized = new HashSet<>();
        for (String world : worlds) {
            if (world != null && !world.isBlank()) {
                normalized.add(world.trim().toLowerCase(Locale.ROOT));
            }
        }
        worlds = Set.copyOf(normalized);
    }

    /**
     * Creates a scope from world names. The keyword {@code all} anywhere in the list means every world.
     */
    public static WorldScope of(Collection<String> worlds) {
        for (String world : worlds) {
            if (world != null && world.trim().equalsIgnoreCase(ALL_KEYWORD)) {
                return ALL;
            }
        }

        WorldScope scope = new WorldScope(new HashSet<>(worlds));
        return scope.worlds.isEmpty() ? ALL : scope;
    }

    public boolean isAll() {
        return worlds.isEmpty();
    }

    public boolean includes(String world) {
        return isAll() || (world != null && worlds.contains(world.toLowerCase(Locale.ROOT)));
    }

    /**
     * Returns a scope covering every world covered by either scope.
     */
    public WorldScope union(WorldScope other) {
        if (isAll() || other.isAll()) {
            return ALL;
        }

        Set<String> combined = new HashSet<>(worlds);
        combined.addAll(other.worlds);
        return new WorldScope(combined);
    }
}
