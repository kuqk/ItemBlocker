package pl.variant.model;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.potion.PotionEffectType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public final class RulePreset {

    private final String name;
    private final String description;
    private final String reason;
    private final WorldScopeMode worldScopeMode;
    private final Set<String> worlds;
    private final ThresholdRuleSet enchantmentRules;
    private final ThresholdRuleSet potionRules;
    private final Map<Material, ItemRule> itemRules;

    public RulePreset(
            String name,
            String description,
            String reason,
            WorldScopeMode worldScopeMode,
            Set<String> worlds,
            ThresholdRuleSet enchantmentRules,
            ThresholdRuleSet potionRules,
            Map<Material, ItemRule> itemRules
    ) {
        this.name = name;
        this.description = description == null ? "" : description;
        this.reason = reason == null ? "" : reason;
        this.worldScopeMode = worldScopeMode == null ? WorldScopeMode.DISABLED : worldScopeMode;
        this.worlds = normalizeWorlds(worlds);
        this.enchantmentRules = enchantmentRules == null ? ThresholdRuleSet.empty() : enchantmentRules;
        this.potionRules = potionRules == null ? ThresholdRuleSet.empty() : potionRules;
        this.itemRules = copyItemRules(itemRules);
    }

    public static RulePreset empty(String name) {
        return new RulePreset(name, "", "", WorldScopeMode.DISABLED, Set.of(), ThresholdRuleSet.empty(), ThresholdRuleSet.empty(), Map.of());
    }

    public static RulePreset fromSection(String name, ConfigurationSection section) {
        if (section == null) {
            return empty(name);
        }

        String description = section.getString("description", "");
        String reason = section.getString("reason", "");

        ParsedWorldScopeValue worldScope = parseWorldScopeValue(section.get("worlds"));
        ThresholdRuleSet enchantmentRules = ThresholdRuleSet.fromConfigValue(section.get("enchantments"));
        ThresholdRuleSet potionRules = ThresholdRuleSet.fromConfigValue(section.get("potions"));

        Map<Material, ItemRule> itemRules = new LinkedHashMap<>();
        ConfigurationSection itemsSection = section.getConfigurationSection("items");
        if (itemsSection != null) {
            for (String materialName : itemsSection.getKeys(false)) {
                Material material = parseMaterial(materialName);
                if (material == null) {
                    continue;
                }

                Object rawItemValue = itemsSection.isConfigurationSection(materialName)
                        ? itemsSection.getConfigurationSection(materialName)
                        : itemsSection.get(materialName);
                if (!isSupportedItemRuleValue(rawItemValue)) {
                    continue;
                }
                itemRules.put(material, ItemRule.fromConfigValue(rawItemValue));
            }
        }

        return new RulePreset(
                name,
                description,
                reason,
                worldScope.mode(),
                worldScope.worlds(),
                enchantmentRules,
                potionRules,
                itemRules
        );
    }

    public boolean matches(Material material, BlockAction action, String worldName) {
        ItemRule itemRule = itemRules.get(material);
        if (itemRule == null || !matchesWorld(worldName)) {
            return false;
        }

        return itemRule.matches(action, worldName);
    }

    public boolean references(Material material) {
        return itemRules.containsKey(material);
    }

    public boolean matchesEnchantment(Enchantment enchantment, int level, String worldName) {
        if (enchantment == null || !matchesWorld(worldName)) {
            return false;
        }

        return enchantmentRules.matches(enchantment.getKey().getKey(), level);
    }

    public boolean matchesPotionEffect(PotionEffectType effectType, int level, String worldName) {
        if (effectType == null || !matchesWorld(worldName)) {
            return false;
        }

        return potionRules.matches(effectType.getKey().getKey(), level);
    }

    public boolean matchesWorld(String worldName) {
        if (worldScopeMode == WorldScopeMode.DISABLED || worlds.isEmpty()) {
            return true;
        }

        String normalizedWorld = worldName == null ? "" : worldName.toLowerCase(Locale.ROOT);
        return worlds.contains(normalizedWorld);
    }

    public RulePreset withItem(Material material, Set<BlockAction> actions) {
        Map<Material, ItemRule> updated = copyItemRules(itemRules);
        ItemRule existing = updated.get(material);
        updated.put(material, (existing == null ? ItemRule.allActions() : existing).withActions(actions));
        return new RulePreset(name, description, reason, worldScopeMode, worlds, enchantmentRules, potionRules, updated);
    }

    public RulePreset withItemRule(Material material, ItemRule itemRule) {
        if (material == null || itemRule == null) {
            return this;
        }

        Map<Material, ItemRule> updated = copyItemRules(itemRules);
        updated.put(material, new ItemRule(itemRule.getScopedRules()));
        return new RulePreset(name, description, reason, worldScopeMode, worlds, enchantmentRules, potionRules, updated);
    }

    public RulePreset appendItem(Material material, Set<BlockAction> actions, WorldScopeMode mode, Set<String> newWorlds) {
        Map<Material, ItemRule> updated = copyItemRules(itemRules);
        ItemRule existing = updated.get(material);
        updated.put(material, existing == null
                ? new ItemRule(actions, mode, newWorlds)
                : existing.append(actions, mode, newWorlds));
        return new RulePreset(name, description, reason, worldScopeMode, worlds, enchantmentRules, potionRules, updated);
    }

    public RulePreset mergeItem(Material material, Set<BlockAction> actions, WorldScopeMode mode, Set<String> newWorlds) {
        Map<Material, ItemRule> updated = copyItemRules(itemRules);
        ItemRule existing = updated.get(material);
        updated.put(material, existing == null
                ? new ItemRule(actions, mode, newWorlds)
                : existing.merge(actions, mode, newWorlds));
        return new RulePreset(name, description, reason, worldScopeMode, worlds, enchantmentRules, potionRules, updated);
    }

    public RulePreset withItemWorldScope(Material material, WorldScopeMode mode, Set<String> newWorlds) {
        Map<Material, ItemRule> updated = copyItemRules(itemRules);
        ItemRule existing = updated.get(material);
        if (existing == null) {
            return this;
        }

        updated.put(material, existing.withWorldScope(mode, newWorlds));
        return new RulePreset(name, description, reason, worldScopeMode, worlds, enchantmentRules, potionRules, updated);
    }

    public RulePreset withoutItem(Material material) {
        Map<Material, ItemRule> updated = copyItemRules(itemRules);
        updated.remove(material);
        return new RulePreset(name, description, reason, worldScopeMode, worlds, enchantmentRules, potionRules, updated);
    }

    public RulePreset withDescription(String newDescription) {
        return new RulePreset(name, newDescription, reason, worldScopeMode, worlds, enchantmentRules, potionRules, itemRules);
    }

    public RulePreset withReason(String newReason) {
        return new RulePreset(name, description, newReason, worldScopeMode, worlds, enchantmentRules, potionRules, itemRules);
    }

    public RulePreset withWorldScope(WorldScopeMode mode, Set<String> newWorlds) {
        return new RulePreset(name, description, reason, mode, newWorlds, enchantmentRules, potionRules, itemRules);
    }

    public RulePreset withEnchantmentRule(String key, int minimumLevel) {
        return new RulePreset(
                name,
                description,
                reason,
                worldScopeMode,
                worlds,
                enchantmentRules.withRule(key, minimumLevel),
                potionRules,
                itemRules
        );
    }

    public RulePreset withoutEnchantmentRule(String key) {
        return new RulePreset(
                name,
                description,
                reason,
                worldScopeMode,
                worlds,
                enchantmentRules.withoutRule(key),
                potionRules,
                itemRules
        );
    }

    public RulePreset withPotionRule(String key, int minimumLevel) {
        return new RulePreset(
                name,
                description,
                reason,
                worldScopeMode,
                worlds,
                enchantmentRules,
                potionRules.withRule(key, minimumLevel),
                itemRules
        );
    }

    public RulePreset withoutPotionRule(String key) {
        return new RulePreset(
                name,
                description,
                reason,
                worldScopeMode,
                worlds,
                enchantmentRules,
                potionRules.withoutRule(key),
                itemRules
        );
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public String getReason() {
        return reason;
    }

    public WorldScopeMode getWorldScopeMode() {
        return worldScopeMode;
    }

    public Set<String> getWorlds() {
        return new LinkedHashSet<>(worlds);
    }

    public ThresholdRuleSet getEnchantmentRules() {
        return new ThresholdRuleSet(enchantmentRules.asMap());
    }

    public ThresholdRuleSet getPotionRules() {
        return new ThresholdRuleSet(potionRules.asMap());
    }

    public Map<Material, ItemRule> getItemRules() {
        return copyItemRules(itemRules);
    }

    public Optional<ItemRule> getItemRule(Material material) {
        ItemRule itemRule = itemRules.get(material);
        return itemRule == null ? Optional.empty() : Optional.of(itemRule);
    }

    public EnumSet<BlockAction> getActions(Material material) {
        return getItemRule(material).map(ItemRule::getActions).orElse(null);
    }

    public int getItemCount() {
        return itemRules.size();
    }

    public int getEnchantmentCount() {
        return enchantmentRules.size();
    }

    public int getPotionCount() {
        return potionRules.size();
    }

    private static Map<Material, ItemRule> copyItemRules(Map<Material, ItemRule> source) {
        Map<Material, ItemRule> copy = new LinkedHashMap<>();
        if (source == null) {
            return copy;
        }

        for (Map.Entry<Material, ItemRule> entry : source.entrySet()) {
            ItemRule rule = entry.getValue();
            copy.put(entry.getKey(), new ItemRule(rule.getScopedRules()));
        }
        return copy;
    }

    private static Set<String> normalizeWorlds(Collection<String> values) {
        Set<String> normalized = new LinkedHashSet<>();
        if (values == null) {
            return normalized;
        }

        for (String value : values) {
            if (value != null && !value.isBlank()) {
                normalized.add(value.toLowerCase(Locale.ROOT));
            }
        }
        return normalized;
    }

    private static Material parseMaterial(String value) {
        try {
            return Material.valueOf(value.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }

    private static boolean isSupportedItemRuleValue(Object rawItemValue) {
        return rawItemValue instanceof ConfigurationSection
                || rawItemValue instanceof Map<?, ?>
                || rawItemValue instanceof Collection<?>
                || rawItemValue instanceof String;
    }

    private static Map<?, ?> getMap(Map<?, ?> map, String key) {
        if (map == null) {
            return Map.of();
        }

        Object value = map.get(key);
        return value instanceof Map<?, ?> valueMap ? valueMap : Map.of();
    }

    private static String getString(Map<?, ?> map, String key, String fallback) {
        if (map == null) {
            return fallback;
        }

        Object value = map.get(key);
        return value instanceof String stringValue ? stringValue : fallback;
    }

    private static List<String> getStringList(Object value) {
        if (value instanceof String stringValue) {
            return splitCommaSeparatedValues(stringValue);
        }

        if (!(value instanceof Collection<?> collection)) {
            return List.of();
        }

        List<String> results = new ArrayList<>();
        for (Object entry : collection) {
            if (entry instanceof String stringValue) {
                results.addAll(splitCommaSeparatedValues(stringValue));
            }
        }
        return results;
    }

    private static List<String> splitCommaSeparatedValues(String value) {
        if (value == null || value.isBlank()) {
            return List.of();
        }

        return Arrays.stream(value.split(","))
                .map(String::trim)
                .filter(part -> !part.isBlank())
                .toList();
    }

    private static ParsedWorldScopeValue parseWorldScopeValue(Object rawValue) {
        if (rawValue instanceof ConfigurationSection section) {
            WorldScopeMode mode = WorldScopeMode.fromValue(section.getString("mode", "disabled"));
            Set<String> parsedWorlds = normalizeWorlds(getStringList(section.get("list")));
            return normalizeWorldScope(mode, parsedWorlds);
        }

        if (rawValue instanceof Map<?, ?> valueMap) {
            Map<?, ?> worldsMap = getMap(valueMap, "list").isEmpty() && valueMap.containsKey("mode")
                    ? valueMap
                    : getMap(valueMap, "worlds");
            WorldScopeMode mode = WorldScopeMode.fromValue(getString(worldsMap, "mode", "disabled"));
            Set<String> parsedWorlds = normalizeWorlds(getStringList(worldsMap.get("list")));
            return normalizeWorldScope(mode, parsedWorlds);
        }

        List<String> values = getStringList(rawValue);
        if (values.isEmpty()) {
            return new ParsedWorldScopeValue(WorldScopeMode.DISABLED, Set.of());
        }

        List<String> normalizedValues = values.stream()
                .map(value -> value.toLowerCase(Locale.ROOT))
                .toList();
        String firstValue = normalizedValues.getFirst();
        if (firstValue.equals("all") || firstValue.equals("disabled")) {
            return new ParsedWorldScopeValue(WorldScopeMode.DISABLED, Set.of());
        }

        return normalizeWorldScope(WorldScopeMode.WHITELIST, normalizeWorlds(normalizedValues));
    }

    private static ParsedWorldScopeValue normalizeWorldScope(WorldScopeMode mode, Set<String> worlds) {
        Set<String> normalizedWorlds = normalizeWorlds(worlds);
        WorldScopeMode resolvedMode = mode == null ? WorldScopeMode.DISABLED : mode;
        if (resolvedMode == WorldScopeMode.DISABLED || normalizedWorlds.isEmpty()) {
            return new ParsedWorldScopeValue(WorldScopeMode.DISABLED, Set.of());
        }

        return new ParsedWorldScopeValue(WorldScopeMode.WHITELIST, normalizedWorlds);
    }

    private record ParsedWorldScopeValue(WorldScopeMode mode, Set<String> worlds) {
    }
}
