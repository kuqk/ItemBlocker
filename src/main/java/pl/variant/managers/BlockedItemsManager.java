package pl.variant.managers;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import pl.variant.itemBlocker;
import pl.variant.model.BlockAction;
import pl.variant.model.BlockCheckResult;
import pl.variant.model.ItemRule;
import pl.variant.model.WorldScopeMode;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public class BlockedItemsManager {

    private static final String DEFAULT_SECTION_KEY = "default";
    private static final String LEGACY_DEFAULT_SECTION_KEY = "global";

    private final itemBlocker plugin;
    private volatile Map<Material, ItemRule> globalItems;
    private volatile File blockedItemsFile;
    private volatile String globalReason;

    public BlockedItemsManager(itemBlocker plugin) {
        this.plugin = plugin;
        this.globalItems = Map.of();
        this.globalReason = "";
    }

    public synchronized void loadBlockedItems() {
        blockedItemsFile = new File(plugin.getDataFolder(), "blocked-items.yml");
        if (!blockedItemsFile.exists()) {
            plugin.saveResource("blocked-items.yml", false);
        }

        FileConfiguration config = loadYaml(blockedItemsFile);
        Map<Material, ItemRule> loadedItems = new LinkedHashMap<>();

        ConfigurationSection globalSection = config.getConfigurationSection(DEFAULT_SECTION_KEY);
        if (globalSection == null) {
            globalSection = config.getConfigurationSection(LEGACY_DEFAULT_SECTION_KEY);
        }

        globalReason = globalSection == null
                ? config.getString("default-blocked-reason", config.getString("global-blocked-reason", ""))
                : globalSection.getString("reason", config.getString("default-blocked-reason", config.getString("global-blocked-reason", "")));

        if (globalSection != null) {
            ConfigurationSection itemsSection = globalSection.getConfigurationSection("items");
            if (itemsSection != null) {
                for (String materialName : itemsSection.getKeys(false)) {
                    Material material = parseMaterial(materialName);
                    if (material == null) {
                        plugin.getLogger().warning("Invalid material '" + materialName + "' in default blocked items list");
                        continue;
                    }

                    Object rawItemValue = itemsSection.isConfigurationSection(materialName)
                            ? itemsSection.getConfigurationSection(materialName)
                            : itemsSection.get(materialName);
                    loadedItems.put(material, ItemRule.fromConfigValue(rawItemValue));
                }
            } else {
                List<String> legacyItems = globalSection.getStringList("items");
                EnumSet<BlockAction> legacyActions = parseLegacyActions(globalSection.get("actions"));
                for (String materialName : legacyItems) {
                    Material material = parseMaterial(materialName);
                    if (material == null) {
                        plugin.getLogger().warning("Invalid material '" + materialName + "' in default blocked items list");
                        continue;
                    }

                    loadedItems.put(material, new ItemRule(legacyActions, WorldScopeMode.DISABLED, Set.of()));
                }
            }
        }

        globalItems = copyGlobalItems(loadedItems);
        plugin.getLogger().info("Loaded " + globalItems.size() + " default blocked items");
    }

    public synchronized void saveBlockedItems() {
        StringBuilder content = new StringBuilder();
        content.append("#   _____ _                 ____  _            _\n");
        content.append("#  |_   _| |               |  _ \\| |          | |\n");
        content.append("#    | | | |_ ___ _ __ ___ | |_) | | ___   ___| | _____ _ __\n");
        content.append("#    | | | __/ _ \\ '_ ` _ \\|  _ <| |/ _ \\ / __| |/ / _ \\ '__|\n");
        content.append("#   _| |_| ||  __/ | | | | | |_) | | (_) | (__|   <  __/ |\n");
        content.append("#  |_____|\\__\\___|_| |_| |_|____/|_|\\___/ \\___|_|\\_\\___|_|\n");
        content.append("#\n");
        content.append("#             ItemBlocker Blocked Items\n\n");
        content.append("# Example default rules.\n");
        content.append(DEFAULT_SECTION_KEY).append(":\n");
        content.append("  reason: ").append(quoteYaml(globalReason)).append("\n\n");
        if (globalItems.isEmpty()) {
            content.append("  items: {}\n");
        } else {
            content.append("  items:\n");
            globalItems.entrySet().stream()
                    .sorted(Map.Entry.comparingByKey(Comparator.comparing(Material::name)))
                    .forEach(entry -> appendItemRule(content, "    ", entry.getKey(), entry.getValue()));
        }

        writeTextFile(blockedItemsFile.toPath(), content.toString());
    }

    public BlockCheckResult check(Material material, BlockAction action, String worldName) {
        if (material == null) {
            return BlockCheckResult.allowed();
        }

        ItemRule globalRule = globalItems.get(material);
        if (globalRule != null && globalRule.matches(action, worldName)) {
            return BlockCheckResult.blocked(DEFAULT_SECTION_KEY, globalReason, true);
        }

        PresetManager presetManager = plugin.getPresetManager();
        return presetManager == null
                ? BlockCheckResult.allowed()
                : presetManager.check(material, action, worldName);
    }

    public boolean isConfigured(Material material) {
        if (globalItems.containsKey(material)) {
            return true;
        }

        PresetManager presetManager = plugin.getPresetManager();
        return presetManager != null && presetManager.isConfigured(material);
    }

    public synchronized boolean upsertGlobalItem(Material material, Set<BlockAction> actions) {
        Map<Material, ItemRule> updated = copyGlobalItems(globalItems);
        ItemRule existing = updated.get(material);
        updated.put(material, (existing == null ? ItemRule.allActions() : existing).withActions(actions));
        globalItems = copyGlobalItems(updated);
        saveBlockedItems();
        return true;
    }

    public synchronized boolean replaceGlobalItemRule(Material material, ItemRule itemRule) {
        if (material == null || itemRule == null) {
            return false;
        }

        Map<Material, ItemRule> updated = copyGlobalItems(globalItems);
        updated.put(material, new ItemRule(itemRule.getScopedRules()));
        globalItems = copyGlobalItems(updated);
        saveBlockedItems();
        return true;
    }

    public synchronized boolean appendGlobalItem(Material material, Set<BlockAction> actions, WorldScopeMode mode, Set<String> worlds) {
        Map<Material, ItemRule> updated = copyGlobalItems(globalItems);
        ItemRule existing = updated.get(material);
        if (existing == null) {
            updated.put(material, new ItemRule(actions, mode, normalizeWorlds(worlds)));
        } else {
            updated.put(material, existing.append(actions, mode, normalizeWorlds(worlds)));
        }

        globalItems = copyGlobalItems(updated);
        saveBlockedItems();
        return true;
    }

    public synchronized boolean mergeGlobalItem(Material material, Set<BlockAction> actions, WorldScopeMode mode, Set<String> worlds) {
        Map<Material, ItemRule> updated = copyGlobalItems(globalItems);
        ItemRule existing = updated.get(material);
        if (existing == null) {
            updated.put(material, new ItemRule(actions, mode, normalizeWorlds(worlds)));
        } else {
            updated.put(material, existing.merge(actions, mode, normalizeWorlds(worlds)));
        }

        globalItems = copyGlobalItems(updated);
        saveBlockedItems();
        return true;
    }

    public synchronized boolean updateGlobalItemWorlds(Material material, WorldScopeMode mode, Set<String> worlds) {
        Map<Material, ItemRule> updated = copyGlobalItems(globalItems);
        ItemRule existing = updated.get(material);
        if (existing == null) {
            return false;
        }

        updated.put(material, existing.withWorldScope(mode, normalizeWorlds(worlds)));
        globalItems = copyGlobalItems(updated);
        saveBlockedItems();
        return true;
    }

    public synchronized boolean removeGlobalItem(Material material) {
        Map<Material, ItemRule> updated = copyGlobalItems(globalItems);
        if (updated.remove(material) == null) {
            return false;
        }

        globalItems = copyGlobalItems(updated);
        saveBlockedItems();
        return true;
    }

    public Map<Material, ItemRule> getGlobalItems() {
        return copyGlobalItems(globalItems);
    }

    public Optional<ItemRule> getGlobalItem(Material material) {
        ItemRule rule = globalItems.get(material);
        if (rule == null) {
            return Optional.empty();
        }

        return Optional.of(new ItemRule(rule.getScopedRules()));
    }

    public EnumSet<BlockAction> getGlobalActions(Material material) {
        return getGlobalItem(material).map(ItemRule::getActions).orElse(null);
    }

    public int getBlockedItemsCount() {
        return globalItems.size();
    }

    public String getSimpleListReason() {
        return globalReason;
    }

    public synchronized void setGlobalReason(String reason) {
        globalReason = reason == null ? "" : reason;
        saveBlockedItems();
    }

    private EnumSet<BlockAction> parseLegacyActions(Object value) {
        List<String> actionKeys = extractStringList(value);
        if (actionKeys.isEmpty()) {
            return EnumSet.allOf(BlockAction.class);
        }

        EnumSet<BlockAction> actions = EnumSet.noneOf(BlockAction.class);
        for (String actionKey : actionKeys) {
            String normalized = actionKey.toLowerCase(Locale.ROOT);
            if (normalized.equals("all")) {
                return EnumSet.allOf(BlockAction.class);
            }

            BlockAction.fromKey(normalized).ifPresentOrElse(
                    actions::add,
                    () -> plugin.getLogger().warning("Unknown action '" + actionKey + "' in default blocked items")
            );
        }

        return actions.isEmpty() ? EnumSet.allOf(BlockAction.class) : actions;
    }

    private List<String> extractStringList(Object value) {
        if (value instanceof ConfigurationSection section) {
            value = section.get("actions");
        }

        if (value instanceof String stringValue) {
            return stringValue.isBlank() ? List.of() : List.of(stringValue);
        }

        if (!(value instanceof List<?> values)) {
            return List.of();
        }

        List<String> strings = new ArrayList<>();
        for (Object entry : values) {
            if (entry instanceof String stringValue) {
                strings.add(stringValue);
            }
        }
        return strings;
    }

    private Material parseMaterial(String input) {
        try {
            return Material.valueOf(input.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }

    private Map<Material, ItemRule> copyGlobalItems(Map<Material, ItemRule> source) {
        Map<Material, ItemRule> copy = new LinkedHashMap<>();
        for (Map.Entry<Material, ItemRule> entry : source.entrySet()) {
            ItemRule rule = entry.getValue();
            copy.put(entry.getKey(), new ItemRule(rule.getScopedRules()));
        }
        return copy;
    }

    private FileConfiguration loadYaml(File file) {
        try (BufferedReader reader = Files.newBufferedReader(file.toPath(), StandardCharsets.UTF_8)) {
            return YamlConfiguration.loadConfiguration(reader);
        } catch (IOException exception) {
            plugin.getLogger().severe("Failed to read " + file.getName());
            exception.printStackTrace();
            return new YamlConfiguration();
        }
    }

    private void appendItemRule(StringBuilder content, String indent, Material material, ItemRule rule) {
        content.append(indent).append(material.name()).append(":\n");
        String ruleIndent = indent + "  ";
        if (rule.hasMultipleScopes()) {
            for (ItemRule.ScopedRule scopedRule : rule.getScopedRules()) {
                appendScopedRule(content, ruleIndent, scopedRule, true);
            }
        } else {
            appendScopedRule(content, ruleIndent, rule.getScopedRules().getFirst(), false);
        }
        content.append("\n");
    }

    private void appendScopedRule(StringBuilder content, String indent, ItemRule.ScopedRule rule, boolean listEntry) {
        List<String> actions = serializeActions(rule.actions());
        String firstPropertyIndent = listEntry ? indent + "- " : indent;
        String propertyIndent = listEntry ? indent + "  " : indent;
        String nestedListIndent = propertyIndent + "  ";
        if (actions.size() == 1 && actions.contains("all")) {
            content.append(firstPropertyIndent).append("actions: all\n");
        } else {
            content.append(firstPropertyIndent).append("actions:\n");
            for (String action : actions) {
                content.append(nestedListIndent).append("- ").append(action).append("\n");
            }
        }
        appendActionHints(content, propertyIndent, actions);
        appendWorldSection(content, propertyIndent, rule.mode(), rule.worlds());
    }

    private void appendWorldSection(StringBuilder content, String indent, WorldScopeMode mode, Set<String> worlds) {
        if (mode == WorldScopeMode.DISABLED || worlds.isEmpty()) {
            content.append(indent).append("worlds: all\n");
            content.append(indent).append("# Use 'all' for every world, 'disabled' to ignore world filtering,\n");
            content.append(indent).append("# or list only the worlds where the block should apply.\n");
            return;
        }

        content.append(indent).append("worlds:\n");
        content.append(indent).append("  # Use 'all' for every world or list only blocked worlds.\n");
        content.append(indent).append("  # Example worlds: world, world_nether, world_the_end, spawn\n");
        worlds.stream()
                .sorted()
                .forEach(world -> content.append(indent).append("  - ").append(world).append("\n"));
    }

    private void appendActionHints(StringBuilder content, String indent, List<String> actions) {
        if (!actions.contains("all")) {
            return;
        }

        content.append(indent).append("# Available actions:\n");
        content.append(indent).append("# crafting, pickup, drop, use, place, armor, inventory, hopper\n");
    }

    private List<String> serializeActions(Set<BlockAction> actions) {
        if (actions == null || actions.isEmpty() || actions.size() == BlockAction.values().length) {
            return List.of("all");
        }

        List<String> values = new ArrayList<>();
        for (BlockAction action : BlockAction.values()) {
            if (actions.contains(action)) {
                values.add(action.getKey());
            }
        }
        return values.isEmpty() ? List.of("all") : values;
    }

    private Set<String> normalizeWorlds(Collection<String> worlds) {
        Set<String> normalized = new LinkedHashSet<>();
        if (worlds == null) {
            return normalized;
        }

        for (String world : worlds) {
            if (world != null && !world.isBlank()) {
                normalized.add(world.toLowerCase(Locale.ROOT));
            }
        }

        return normalized;
    }

    private String quoteYaml(String value) {
        String safeValue = value == null ? "" : value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"");
        return "\"" + safeValue + "\"";
    }

    private void writeTextFile(Path path, String content) {
        try {
            Files.writeString(path, content, StandardCharsets.UTF_8);
        } catch (IOException exception) {
            plugin.getLogger().severe("Failed to save " + path.getFileName());
            exception.printStackTrace();
        }
    }
}
