package pl.variant.itemblocker.rule;

import java.util.Collections;
import java.util.Map;
import java.util.OptionalInt;
import java.util.TreeMap;

/**
 * Minimum levels from which enchantments or potion effects are blocked, keyed by
 * {@link RegistryKeys normalized} registry key.
 */
public record LevelLimits(Map<String, Integer> minimumLevels) {

    public static final LevelLimits NONE = new LevelLimits(Map.of());

    public LevelLimits {
        TreeMap<String, Integer> copy = new TreeMap<>();
        minimumLevels.forEach((key, level) -> {
            if (level < 1) {
                throw new IllegalArgumentException("Minimum level for " + key + " must be at least 1, was " + level);
            }
            copy.put(RegistryKeys.normalize(key), level);
        });
        minimumLevels = Collections.unmodifiableMap(copy);
    }

    public OptionalInt minimumLevel(String key) {
        Integer level = minimumLevels.get(RegistryKeys.normalize(key));
        return level == null ? OptionalInt.empty() : OptionalInt.of(level);
    }

    public boolean blocks(String key, int level) {
        OptionalInt minimum = minimumLevel(key);
        return minimum.isPresent() && level >= minimum.getAsInt();
    }

    public boolean contains(String key) {
        return minimumLevel(key).isPresent();
    }

    public LevelLimits with(String key, int minimumLevel) {
        TreeMap<String, Integer> updated = new TreeMap<>(minimumLevels);
        updated.put(RegistryKeys.normalize(key), minimumLevel);
        return new LevelLimits(updated);
    }

    public LevelLimits without(String key) {
        TreeMap<String, Integer> updated = new TreeMap<>(minimumLevels);
        updated.remove(RegistryKeys.normalize(key));
        return new LevelLimits(updated);
    }

    public boolean isEmpty() {
        return minimumLevels.isEmpty();
    }

    public int size() {
        return minimumLevels.size();
    }
}
