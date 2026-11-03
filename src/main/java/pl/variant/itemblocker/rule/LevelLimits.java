package pl.variant.itemblocker.rule;

import java.util.Collections;
import java.util.Map;
import java.util.OptionalInt;
import java.util.TreeMap;

/**
 * Minimum levels from which enchantments or potion effects are blocked, keyed by
 * {@link RegistryKeys normalized} registry key. Enchantments can also say what happens to items
 * that carry them; anything without a mode is {@link EnchantmentMode#BLOCK blocked}.
 *
 * @param minimumLevels the lowest blocked level of each key
 * @param modes         the mode of keys that are not simply blocked
 */
public record LevelLimits(Map<String, Integer> minimumLevels, Map<String, EnchantmentMode> modes) {

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

        TreeMap<String, EnchantmentMode> modesCopy = new TreeMap<>();
        modes.forEach((key, mode) -> {
            String normalized = RegistryKeys.normalize(key);
            if (mode != EnchantmentMode.BLOCK && copy.containsKey(normalized)) {
                modesCopy.put(normalized, mode);
            }
        });
        modes = Collections.unmodifiableMap(modesCopy);
    }

    public LevelLimits(Map<String, Integer> minimumLevels) {
        this(minimumLevels, Map.of());
    }

    public OptionalInt minimumLevel(String key) {
        Integer level = minimumLevels.get(RegistryKeys.normalize(key));
        return level == null ? OptionalInt.empty() : OptionalInt.of(level);
    }

    public EnchantmentMode mode(String key) {
        return modes.getOrDefault(RegistryKeys.normalize(key), EnchantmentMode.BLOCK);
    }

    /**
     * Returns whether any key has a mode other than blocking.
     */
    public boolean hasModes() {
        return !modes.isEmpty();
    }

    public boolean blocks(String key, int level) {
        OptionalInt minimum = minimumLevel(key);
        return minimum.isPresent() && level >= minimum.getAsInt();
    }

    public boolean contains(String key) {
        return minimumLevel(key).isPresent();
    }

    /**
     * Returns limits with the minimum level of the key set, keeping its mode.
     */
    public LevelLimits with(String key, int minimumLevel) {
        TreeMap<String, Integer> updated = new TreeMap<>(minimumLevels);
        updated.put(RegistryKeys.normalize(key), minimumLevel);
        return new LevelLimits(updated, modes);
    }

    /**
     * Returns limits with the mode of a key that already has a minimum level.
     */
    public LevelLimits withMode(String key, EnchantmentMode mode) {
        TreeMap<String, EnchantmentMode> updated = new TreeMap<>(modes);
        updated.put(RegistryKeys.normalize(key), mode);
        return new LevelLimits(minimumLevels, updated);
    }

    public LevelLimits without(String key) {
        TreeMap<String, Integer> updated = new TreeMap<>(minimumLevels);
        updated.remove(RegistryKeys.normalize(key));
        return new LevelLimits(updated, modes);
    }

    public boolean isEmpty() {
        return minimumLevels.isEmpty();
    }

    public int size() {
        return minimumLevels.size();
    }
}
