package pl.variant.model;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;

import java.util.ArrayList;
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
    private final Map<Material, ItemRule> itemRules;

    public RulePreset(
            String name,
            String description,
            String reason,
            WorldScopeMode worldScopeMode,
            Set<String> worlds,
            Map<Material, ItemRule> itemRules
    ) {
        this.name = name;
        this.description = description == null ? "" : description;
        this.reason = reason == null ? "" : reason;
        this.worldScopeMode = worldScopeMode == null ? WorldScopeMode.DISABLED : worldScopeMode;
        this.worlds = normalizeWorlds(worlds);
        this.itemRules = copyItemRules(itemRules);
    }

    public static RulePreset empty(String name) {
        return new RulePreset(name, "", "", WorldScopeMode.DISABLED, Set.of(), Map.of());
    }

    public static RulePreset fromSection(String name, ConfigurationSection section) {
        if (section == null) {
            return empty(name);
        }

        String description = section.getString("description", "");
        String reason = section.getString("reason", "");

        ParsedWorldScopeValue worldScope = parseWorldScopeValue(section.get("worlds"));
        WorldScopeMode worldScopeMode = worldScope.mode();
        Set<String> worlds = worldScope.worlds();

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
                itemRules.put(material, ItemRule.fromConfigValue(rawItemValue));
            }
        } else {
            for (String materialName : section.getStringList("items")) {
                Material material = parseMaterial(materialName);
                if (material == null) {
                    continue;
                }

                itemRules.put(material, ItemRule.allActions());
            }
        }

        return new RulePreset(name, description, reason, worldScopeMode, worlds, itemRules);
    }

    public static RulePreset fromMap(String name, Map<?, ?> map) {
        String description = getString(map, "description", "");
        String reason = getString(map, "reason", "");

        ParsedWorldScopeValue worldScope = parseWorldScopeValue(map.get("worlds"));
        WorldScopeMode worldScopeMode = worldScope.mode();
        Set<String> worlds = worldScope.worlds();

        Map<Material, ItemRule> itemRules = new LinkedHashMap<>();
        Object rawItems = map.get("items");
        if (rawItems instanceof Map<?, ?> itemMap) {
            for (Map.Entry<?, ?> entry : itemMap.entrySet()) {
                if (!(entry.getKey() instanceof String materialName)) {
                    continue;
                }

                Material material = parseMaterial(materialName);
                if (material == null) {
                    continue;
                }

                itemRules.put(material, ItemRule.fromConfigValue(entry.getValue()));
            }
        } else if (rawItems instanceof Collection<?> itemList) {
            for (Object value : itemList) {
                if (!(value instanceof String materialName)) {
                    continue;
                }

                Material material = parseMaterial(materialName);
                if (material == null) {
                    continue;
                }

                itemRules.put(material, ItemRule.allActions());
            }
        }

        return new RulePreset(name, description, reason, worldScopeMode, worlds, itemRules);
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
        return new RulePreset(name, description, reason, worldScopeMode, worlds, updated);
    }

    public RulePreset withItemRule(Material material, ItemRule itemRule) {
        if (material == null || itemRule == null) {
            return this;
        }

        Map<Material, ItemRule> updated = copyItemRules(itemRules);
        updated.put(material, new ItemRule(itemRule.getScopedRules()));
        return new RulePreset(name, description, reason, worldScopeMode, worlds, updated);
    }

    public RulePreset appendItem(Material material, Set<BlockAction> actions, WorldScopeMode mode, Set<String> newWorlds) {
        Map<Material, ItemRule> updated = copyItemRules(itemRules);
        ItemRule existing = updated.get(material);
        if (existing == null) {
            updated.put(material, new ItemRule(actions, mode, newWorlds));
        } else {
            updated.put(material, existing.append(actions, mode, newWorlds));
        }
        return new RulePreset(name, description, reason, worldScopeMode, worlds, updated);
    }

    public RulePreset mergeItem(Material material, Set<BlockAction> actions, WorldScopeMode mode, Set<String> newWorlds) {
        Map<Material, ItemRule> updated = copyItemRules(itemRules);
        ItemRule existing = updated.get(material);
        if (existing == null) {
            updated.put(material, new ItemRule(actions, mode, newWorlds));
        } else {
            updated.put(material, existing.merge(actions, mode, newWorlds));
        }
        return new RulePreset(name, description, reason, worldScopeMode, worlds, updated);
    }

    public RulePreset withItemWorldScope(Material material, WorldScopeMode mode, Set<String> newWorlds) {
        Map<Material, ItemRule> updated = copyItemRules(itemRules);
        ItemRule existing = updated.get(material);
        if (existing == null) {
            return this;
        }

        updated.put(material, existing.withWorldScope(mode, newWorlds));
        return new RulePreset(name, description, reason, worldScopeMode, worlds, updated);
    }

    public RulePreset withoutItem(Material material) {
        Map<Material, ItemRule> updated = copyItemRules(itemRules);
        updated.remove(material);
        return new RulePreset(name, description, reason, worldScopeMode, worlds, updated);
    }

    public RulePreset withDescription(String newDescription) {
        return new RulePreset(name, newDescription, reason, worldScopeMode, worlds, itemRules);
    }

    public RulePreset withReason(String newReason) {
        return new RulePreset(name, description, newReason, worldScopeMode, worlds, itemRules);
    }

    public RulePreset withWorldScope(WorldScopeMode mode, Set<String> newWorlds) {
        return new RulePreset(name, description, reason, mode, newWorlds, itemRules);
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

    private static Map<Material, ItemRule> copyItemRules(Map<Material, ItemRule> source) {
        Map<Material, ItemRule> copy = new LinkedHashMap<>();
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
        if (!(value instanceof Collection<?> collection)) {
            return List.of();
        }

        List<String> results = new ArrayList<>();
        for (Object entry : collection) {
            if (entry instanceof String stringValue) {
                results.add(stringValue);
            }
        }
        return results;
    }

    private static ParsedWorldScopeValue parseWorldScopeValue(Object rawValue) {
        if (rawValue instanceof ConfigurationSection section) {
            WorldScopeMode mode = WorldScopeMode.fromValue(section.getString("mode", "disabled"));
            Set<String> worlds = normalizeWorlds(section.getStringList("list"));
            return new ParsedWorldScopeValue(mode, worlds);
        }

        if (rawValue instanceof Map<?, ?> valueMap) {
            Map<?, ?> worldsMap = getMap(valueMap, "list").isEmpty() && valueMap.containsKey("mode")
                    ? valueMap
                    : getMap(valueMap, "worlds");
            WorldScopeMode mode = WorldScopeMode.fromValue(getString(worldsMap, "mode", "disabled"));
            Set<String> worlds = normalizeWorlds(getStringList(worldsMap.get("list")));
            return new ParsedWorldScopeValue(mode, worlds);
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

        return new ParsedWorldScopeValue(WorldScopeMode.WHITELIST, normalizeWorlds(normalizedValues));
    }

    private record ParsedWorldScopeValue(WorldScopeMode mode, Set<String> worlds) {
    }
}
