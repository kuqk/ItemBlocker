package pl.variant.itemblocker.rule;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Consumer;
import java.util.regex.Pattern;

/**
 * A named group of restrictions. Everything in a preset only applies in the worlds covered by its
 * scope and, if it is limited to WorldGuard regions, inside them.
 *
 * <p>Item types on the allowed list are never blocked by the preset's item rules or groups, which
 * makes it possible to block a whole group of items except a few.
 *
 * @param name         lowercase preset name
 * @param description  free text shown in preset listings
 * @param reason       free text appended to messages for restrictions from this preset
 * @param scope        worlds the whole preset applies in
 * @param regions      WorldGuard regions the whole preset applies in
 * @param items        item rules by item type
 * @param groups       item rules for every item type matching a pattern, such as {@code *_sword}
 * @param allowed      item types the preset's item rules and groups never block
 * @param limits       stack and inventory limits by item type
 * @param filters      rules for items matched by more than their type, by filter id
 * @param enchantments enchantment levels that are blocked
 * @param effects      potion effect levels that are blocked
 */
public record Preset(
        String name,
        String description,
        String reason,
        WorldScope scope,
        RegionScope regions,
        Map<Material, ItemRule> items,
        Map<ItemPattern, ItemRule> groups,
        Set<Material> allowed,
        Map<Material, ItemLimit> limits,
        Map<String, FilterRule> filters,
        LevelLimits enchantments,
        LevelLimits effects
) {

    // Names are written as plain YAML keys, so they are limited to characters that never need quoting.
    private static final Pattern NAME_PATTERN = Pattern.compile("[a-z0-9][a-z0-9_-]{0,31}");

    public Preset {
        name = normalizeName(name);
        description = Objects.requireNonNullElse(description, "");
        reason = Objects.requireNonNullElse(reason, "");
        Objects.requireNonNull(scope, "scope");
        Objects.requireNonNull(regions, "regions");
        EnumMap<Material, ItemRule> itemsCopy = new EnumMap<>(Material.class);
        itemsCopy.putAll(items);
        items = Collections.unmodifiableMap(itemsCopy);
        groups = Collections.unmodifiableMap(new LinkedHashMap<>(groups));
        EnumSet<Material> allowedCopy = EnumSet.noneOf(Material.class);
        allowedCopy.addAll(allowed);
        allowed = Collections.unmodifiableSet(allowedCopy);
        EnumMap<Material, ItemLimit> limitsCopy = new EnumMap<>(Material.class);
        limits.forEach((material, limit) -> {
            if (!limit.isNone()) {
                limitsCopy.put(material, limit);
            }
        });
        limits = Collections.unmodifiableMap(limitsCopy);
        filters = Collections.unmodifiableMap(new TreeMap<>(filters));
        Objects.requireNonNull(enchantments, "enchantments");
        Objects.requireNonNull(effects, "effects");
    }

    public static Preset empty(String name) {
        return new Preset(name, "", "", WorldScope.ALL, RegionScope.ALL, Map.of(), Map.of(), Set.of(), Map.of(), Map.of(),
                LevelLimits.NONE, LevelLimits.NONE);
    }

    public static String normalizeName(String name) {
        return name.trim().toLowerCase(Locale.ROOT);
    }

    /**
     * Returns whether the name can be stored as a preset name: 1 to 32 characters of {@code a-z},
     * {@code 0-9}, {@code _} or {@code -}, starting with a letter or digit.
     */
    public static boolean isValidName(String name) {
        return NAME_PATTERN.matcher(normalizeName(name)).matches();
    }

    public Optional<ItemRule> item(Material material) {
        return Optional.ofNullable(items.get(material));
    }

    public Optional<ItemRule> group(ItemPattern pattern) {
        return Optional.ofNullable(groups.get(pattern));
    }

    public Optional<FilterRule> filter(String id) {
        return Optional.ofNullable(filters.get(normalizeName(id)));
    }

    public ItemLimit limit(Material material) {
        return limits.getOrDefault(material, ItemLimit.NONE);
    }

    /**
     * Returns whether the preset applies at the place, by its worlds and regions.
     */
    public boolean appliesAt(Place place) {
        return scope.includes(place.world()) && regions.includesAny(place.regions());
    }

    /**
     * Returns whether the item rule or a group of the preset blocks the action for the item type.
     */
    public boolean blocksItem(Material material, Action action, Place place, Exemption exemption) {
        if (allowed.contains(material) || !appliesAt(place) || exemption.exempts(this, null)) {
            return false;
        }

        ItemRule rule = items.get(material);
        if (blocks(rule, action, place, exemption)) {
            return true;
        }
        return groups.entrySet().stream()
                .anyMatch(group -> group.getKey().matches(material) && blocks(group.getValue(), action, place, exemption));
    }

    /**
     * Returns the id of the first filter, in id order, that blocks the action for the item at the place.
     */
    public Optional<String> blockingFilter(ItemStack item, Action action, Place place, Exemption exemption) {
        if (filters.isEmpty() || !appliesAt(place) || exemption.exempts(this, null)) {
            return Optional.empty();
        }
        return filters.entrySet().stream()
                .filter(entry -> blocks(entry.getValue().rule(), action, place, exemption) && entry.getValue().filter().matches(item))
                .map(Map.Entry::getKey)
                .findFirst();
    }

    public boolean blocksEnchantment(String key, int level, Place place, Exemption exemption) {
        return appliesAt(place) && enchantments.blocks(key, level) && !exemption.exempts(this, null);
    }

    public boolean blocksEffect(String key, int level, Place place, Exemption exemption) {
        return appliesAt(place) && effects.blocks(key, level) && !exemption.exempts(this, null);
    }

    private boolean blocks(ItemRule rule, Action action, Place place, Exemption exemption) {
        return rule != null && rule.blocks(action, place.world())
                && (rule.bypassPermission() == null || !exemption.exempts(this, rule.bypassPermission()));
    }

    public Preset withItem(Material material, ItemRule rule) {
        return copy(builder -> builder.items.put(material, rule));
    }

    public Preset withoutItem(Material material) {
        return copy(builder -> builder.items.remove(material));
    }

    public Preset withGroup(ItemPattern pattern, ItemRule rule) {
        return copy(builder -> builder.groups.put(pattern, rule));
    }

    public Preset withoutGroup(ItemPattern pattern) {
        return copy(builder -> builder.groups.remove(pattern));
    }

    public Preset withAllowed(Material material) {
        return copy(builder -> builder.allowed.add(material));
    }

    public Preset withoutAllowed(Material material) {
        return copy(builder -> builder.allowed.remove(material));
    }

    /**
     * Returns a preset with the item's limits replaced. {@link ItemLimit#NONE} removes them.
     */
    public Preset withLimit(Material material, ItemLimit limit) {
        return copy(builder -> builder.limits.put(material, limit));
    }

    /**
     * Returns a preset with the filter rule added or replaced.
     */
    public Preset withFilter(String id, FilterRule rule) {
        return copy(builder -> builder.filters.put(normalizeName(id), rule));
    }

    public Preset withoutFilter(String id) {
        return copy(builder -> builder.filters.remove(normalizeName(id)));
    }

    public Preset withEnchantments(LevelLimits newEnchantments) {
        return copy(builder -> builder.enchantments = newEnchantments);
    }

    public Preset withEffects(LevelLimits newEffects) {
        return copy(builder -> builder.effects = newEffects);
    }

    public Preset withDescription(String newDescription) {
        return copy(builder -> builder.description = newDescription);
    }

    public Preset withReason(String newReason) {
        return copy(builder -> builder.reason = newReason);
    }

    public Preset withScope(WorldScope newScope) {
        return copy(builder -> builder.scope = newScope);
    }

    public Preset withRegions(RegionScope newRegions) {
        return copy(builder -> builder.regions = newRegions);
    }

    private Preset copy(Consumer<Builder> change) {
        Builder builder = new Builder(this);
        change.accept(builder);
        return builder.build();
    }

    /**
     * Mutable copy of a preset, so that each {@code with} method only names what it changes.
     */
    private static final class Builder {

        private final String name;
        private String description;
        private String reason;
        private WorldScope scope;
        private RegionScope regions;
        private final Map<Material, ItemRule> items;
        private final Map<ItemPattern, ItemRule> groups;
        private final Set<Material> allowed;
        private final Map<Material, ItemLimit> limits;
        private final Map<String, FilterRule> filters;
        private LevelLimits enchantments;
        private LevelLimits effects;

        private Builder(Preset preset) {
            name = preset.name;
            description = preset.description;
            reason = preset.reason;
            scope = preset.scope;
            regions = preset.regions;
            items = new EnumMap<>(Material.class);
            items.putAll(preset.items);
            groups = new LinkedHashMap<>(preset.groups);
            allowed = EnumSet.noneOf(Material.class);
            allowed.addAll(preset.allowed);
            limits = new EnumMap<>(Material.class);
            limits.putAll(preset.limits);
            filters = new TreeMap<>(preset.filters);
            enchantments = preset.enchantments;
            effects = preset.effects;
        }

        private Preset build() {
            return new Preset(name, description, reason, scope, regions, items, groups, allowed, limits, filters,
                    enchantments, effects);
        }
    }
}
