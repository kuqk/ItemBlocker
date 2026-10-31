package pl.variant.itemblocker.rule;

import org.bukkit.Material;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.function.Predicate;

/**
 * Immutable set of presets. The {@value #DEFAULT_PRESET} preset always exists and is checked first,
 * the others follow in name order.
 */
public final class RuleBook {

    public static final String DEFAULT_PRESET = "default";

    private final Map<String, Preset> presets;
    private final boolean usesRegions;

    private RuleBook(Map<String, Preset> presets) {
        this.presets = presets;
        this.usesRegions = presets.values().stream().anyMatch(preset -> !preset.regions().isAll());
    }

    public static RuleBook empty() {
        return of(List.of());
    }

    /**
     * Creates a rule book from presets, adding an empty default preset if none is given.
     */
    public static RuleBook of(Collection<Preset> presets) {
        Map<String, Preset> byName = new TreeMap<>();
        presets.forEach(preset -> byName.put(preset.name(), preset));

        Map<String, Preset> ordered = new LinkedHashMap<>();
        ordered.put(DEFAULT_PRESET, byName.getOrDefault(DEFAULT_PRESET, Preset.empty(DEFAULT_PRESET)));
        byName.forEach(ordered::putIfAbsent);
        return new RuleBook(Collections.unmodifiableMap(ordered));
    }

    public Preset defaultPreset() {
        return presets.get(DEFAULT_PRESET);
    }

    public Optional<Preset> preset(String name) {
        return Optional.ofNullable(presets.get(Preset.normalizeName(name)));
    }

    public List<Preset> presets() {
        return List.copyOf(presets.values());
    }

    /**
     * Returns whether any preset is limited to WorldGuard regions, so regions need to be looked up.
     */
    public boolean usesRegions() {
        return usesRegions;
    }

    /**
     * Returns the first preset that blocks the action for the item at the place.
     */
    public Optional<Preset> findItemBlock(Material material, Action action, Place place) {
        return find(preset -> preset.blocksItem(material, action, place));
    }

    public Optional<Preset> findEnchantmentBlock(String key, int level, Place place) {
        return find(preset -> preset.blocksEnchantment(key, level, place));
    }

    public Optional<Preset> findEffectBlock(String key, int level, Place place) {
        return find(preset -> preset.blocksEffect(key, level, place));
    }

    /**
     * Returns a rule book with the preset added, or replacing the preset with the same name.
     */
    public RuleBook with(Preset preset) {
        Map<String, Preset> updated = new LinkedHashMap<>(presets);
        updated.put(preset.name(), preset);
        return of(updated.values());
    }

    /**
     * Returns a rule book without the named preset.
     *
     * @throws IllegalArgumentException if the name is the default preset
     */
    public RuleBook without(String name) {
        String normalized = Preset.normalizeName(name);
        if (normalized.equals(DEFAULT_PRESET)) {
            throw new IllegalArgumentException("The default preset cannot be removed");
        }

        Map<String, Preset> updated = new LinkedHashMap<>(presets);
        updated.remove(normalized);
        return of(updated.values());
    }

    private Optional<Preset> find(Predicate<Preset> blocks) {
        return presets.values().stream().filter(blocks).findFirst();
    }
}
