package pl.variant.managers;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.potion.PotionEffectType;
import pl.variant.itemBlocker;
import pl.variant.model.BlockAction;
import pl.variant.model.BlockCheckResult;
import pl.variant.model.ItemRule;
import pl.variant.model.ThresholdRuleSet;
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

    private final itemBlocker plugin;
    private volatile Map<Material, ItemRule> globalItems;
    private volatile File blockedItemsFile;
    private volatile String globalReason;
    private volatile ThresholdRuleSet globalEnchantments;
    private volatile ThresholdRuleSet globalPotions;

    public BlockedItemsManager(itemBlocker plugin) {
        this.plugin = plugin;
        this.globalItems = Map.of();
        this.globalReason = "";
        this.globalEnchantments = ThresholdRuleSet.empty();
        this.globalPotions = ThresholdRuleSet.empty();
    }

    public synchronized void loadBlockedItems() {
        blockedItemsFile = new File(plugin.getDataFolder(), "blocked-items.yml");
        if (!blockedItemsFile.exists()) {
            plugin.saveResource("blocked-items.yml", false);
        }

        FileConfiguration config = loadYaml(blockedItemsFile);
        Map<Material, ItemRule> loadedItems = new LinkedHashMap<>();

        ConfigurationSection globalSection = config.getConfigurationSection(DEFAULT_SECTION_KEY);

        globalEnchantments = globalSection == null
                ? ThresholdRuleSet.empty()
                : ThresholdRuleSet.fromConfigValue(globalSection.get("enchantments"));
        globalPotions = globalSection == null
                ? ThresholdRuleSet.empty()
                : ThresholdRuleSet.fromConfigValue(globalSection.get("potions"));

        globalReason = globalSection == null
                ? ""
                : globalSection.getString("reason", "");

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
                    if (!isSupportedItemRuleValue(rawItemValue)) {
                        plugin.getLogger().warning("Unsupported item rule format for '" + materialName + "' in default section");
                        continue;
                    }
                    loadedItems.put(material, ItemRule.fromConfigValue(rawItemValue));
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
        appendThresholdSection(content, "  ", "enchantments", globalEnchantments);
        appendThresholdSection(content, "  ", "potions", globalPotions);
        if (globalItems.isEmpty()) {
            content.append("  items: {}\n");
        } else {
            content.append("  # One item = one rule. Use presets if the same item needs different behavior.\n");
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

    public BlockCheckResult checkEnchantment(Enchantment enchantment, int level, String worldName) {
        if (enchantment == null) {
            return BlockCheckResult.allowed();
        }

        if (globalEnchantments.matches(enchantment.getKey().getKey(), level)) {
            return BlockCheckResult.blocked(DEFAULT_SECTION_KEY, globalReason, true);
        }

        PresetManager presetManager = plugin.getPresetManager();
        return presetManager == null
                ? BlockCheckResult.allowed()
                : presetManager.checkEnchantment(enchantment, level, worldName);
    }

    public BlockCheckResult checkPotionEffect(PotionEffectType effectType, int level, String worldName) {
        if (effectType == null) {
            return BlockCheckResult.allowed();
        }

        if (globalPotions.matches(effectType.getKey().getKey(), level)) {
            return BlockCheckResult.blocked(DEFAULT_SECTION_KEY, globalReason, true);
        }

        PresetManager presetManager = plugin.getPresetManager();
        return presetManager == null
                ? BlockCheckResult.allowed()
                : presetManager.checkPotionEffect(effectType, level, worldName);
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

    public synchronized boolean upsertGlobalEnchantment(String key, int minimumLevel) {
        globalEnchantments = globalEnchantments.withRule(key, minimumLevel);
        saveBlockedItems();
        return true;
    }

    public synchronized boolean removeGlobalEnchantment(String key) {
        if (!globalEnchantments.contains(key)) {
            return false;
        }

        globalEnchantments = globalEnchantments.withoutRule(key);
        saveBlockedItems();
        return true;
    }

    public synchronized boolean upsertGlobalPotion(String key, int minimumLevel) {
        globalPotions = globalPotions.withRule(key, minimumLevel);
        saveBlockedItems();
        return true;
    }

    public synchronized boolean removeGlobalPotion(String key) {
        if (!globalPotions.contains(key)) {
            return false;
        }

        globalPotions = globalPotions.withoutRule(key);
        saveBlockedItems();
        return true;
    }

    public Map<Material, ItemRule> getGlobalItems() {
        return copyGlobalItems(globalItems);
    }

    public ThresholdRuleSet getGlobalEnchantments() {
        return new ThresholdRuleSet(globalEnchantments.asMap());
    }

    public ThresholdRuleSet getGlobalPotions() {
        return new ThresholdRuleSet(globalPotions.asMap());
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

    public int getBlockedEnchantmentsCount() {
        return globalEnchantments.size();
    }

    public int getBlockedPotionsCount() {
        return globalPotions.size();
    }

    public String getSimpleListReason() {
        return globalReason;
    }

    public synchronized void setGlobalReason(String reason) {
        globalReason = reason == null ? "" : reason;
        saveBlockedItems();
    }

    private Material parseMaterial(String input) {
        try {
            return Material.valueOf(input.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }

    private boolean isSupportedItemRuleValue(Object rawItemValue) {
        return rawItemValue instanceof ConfigurationSection
                || rawItemValue instanceof Map<?, ?>
                || rawItemValue instanceof Collection<?>
                || rawItemValue instanceof String;
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

    private void appendThresholdSection(
            StringBuilder content,
            String indent,
            String sectionName,
            ThresholdRuleSet ruleSet
    ) {
        if (ruleSet == null || ruleSet.isEmpty()) {
            return;
        }

        content.append(indent).append(sectionName).append(":\n");
        content.append(indent).append("  # Format: name: minimum_level\n");
        ruleSet.asMap().entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(entry -> content.append(indent)
                        .append("  ")
                        .append(entry.getKey())
                        .append(": ")
                        .append(entry.getValue())
                        .append("\n"));
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
            content.append(indent).append("# Use 'all' for every world or list only blocked worlds.\n");
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
        content.append(indent).append("# crafting, pickup, drop, use, place, armor, inventory, hopper, smithing\n");
    }

    private List<String> serializeActions(Set<BlockAction> actions) {
        if (actions == null || actions.size() == BlockAction.values().length) {
            return List.of("all");
        }
        if (actions.isEmpty()) {
            return List.of("none");
        }

        List<String> values = new ArrayList<>();
        for (BlockAction action : BlockAction.values()) {
            if (actions.contains(action)) {
                values.add(action.getKey());
            }
        }
        return values.isEmpty() ? List.of("none") : values;
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
