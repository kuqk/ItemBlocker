package pl.variant.itemblocker.rule;

import org.bukkit.Material;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * A named group of restrictions. Item rules, enchantment limits and effect limits of a preset only
 * apply in the worlds covered by its scope.
 *
 * @param name         lowercase preset name
 * @param description  free text shown in preset listings
 * @param reason       free text appended to messages for restrictions from this preset
 * @param scope        worlds the whole preset applies in
 * @param items        item rules by item type
 * @param enchantments enchantment levels that are blocked
 * @param effects      potion effect levels that are blocked
 */
public record Preset(
        String name,
        String description,
        String reason,
        WorldScope scope,
        Map<Material, ItemRule> items,
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
        EnumMap<Material, ItemRule> itemsCopy = new EnumMap<>(Material.class);
        itemsCopy.putAll(items);
        items = Collections.unmodifiableMap(itemsCopy);
        Objects.requireNonNull(enchantments, "enchantments");
        Objects.requireNonNull(effects, "effects");
    }

    public static Preset empty(String name) {
        return new Preset(name, "", "", WorldScope.ALL, Map.of(), LevelLimits.NONE, LevelLimits.NONE);
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

    public boolean blocksItem(Material material, Action action, String world) {
        ItemRule rule = items.get(material);
        return rule != null && scope.includes(world) && rule.blocks(action, world);
    }

    public boolean blocksEnchantment(String key, int level, String world) {
        return scope.includes(world) && enchantments.blocks(key, level);
    }

    public boolean blocksEffect(String key, int level, String world) {
        return scope.includes(world) && effects.blocks(key, level);
    }

    public Preset withItem(Material material, ItemRule rule) {
        EnumMap<Material, ItemRule> updated = new EnumMap<>(Material.class);
        updated.putAll(items);
        updated.put(material, rule);
        return new Preset(name, description, reason, scope, updated, enchantments, effects);
    }

    public Preset withoutItem(Material material) {
        EnumMap<Material, ItemRule> updated = new EnumMap<>(Material.class);
        updated.putAll(items);
        updated.remove(material);
        return new Preset(name, description, reason, scope, updated, enchantments, effects);
    }

    public Preset withEnchantments(LevelLimits newEnchantments) {
        return new Preset(name, description, reason, scope, items, newEnchantments, effects);
    }

    public Preset withEffects(LevelLimits newEffects) {
        return new Preset(name, description, reason, scope, items, enchantments, newEffects);
    }

    public Preset withDescription(String newDescription) {
        return new Preset(name, newDescription, reason, scope, items, enchantments, effects);
    }

    public Preset withReason(String newReason) {
        return new Preset(name, description, newReason, scope, items, enchantments, effects);
    }

    public Preset withScope(WorldScope newScope) {
        return new Preset(name, description, reason, newScope, items, enchantments, effects);
    }
}
