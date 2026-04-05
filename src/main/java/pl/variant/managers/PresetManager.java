package pl.variant.managers;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.potion.PotionEffectType;
import pl.variant.itemBlocker;
import pl.variant.model.BlockAction;
import pl.variant.model.BlockCheckResult;
import pl.variant.model.ItemRule;
import pl.variant.model.RulePreset;
import pl.variant.model.ThresholdRuleSet;
import pl.variant.model.WorldScopeMode;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public class PresetManager {

    private static final Set<String> RESERVED_TARGET_NAMES = Set.of("default");

    private final itemBlocker plugin;
    private volatile Map<String, RulePreset> presets;
    private File presetsFile;

    public PresetManager(itemBlocker plugin) {
        this.plugin = plugin;
        this.presets = Map.of();
    }

    public synchronized void loadPresets() {
        presetsFile = new File(plugin.getDataFolder(), "presets.yml");
        if (!presetsFile.exists()) {
            plugin.saveResource("presets.yml", false);
        }

        YamlConfiguration presetsConfig;
        try (BufferedReader reader = Files.newBufferedReader(presetsFile.toPath(), StandardCharsets.UTF_8)) {
            presetsConfig = YamlConfiguration.loadConfiguration(reader);
        } catch (IOException exception) {
            plugin.getLogger().severe("Failed to load presets.yml");
            exception.printStackTrace();
            presets = Map.of();
            return;
        }

        Map<String, RulePreset> loadedPresets = new LinkedHashMap<>();
        ConfigurationSection presetsSection = presetsConfig.getConfigurationSection("presets");
        if (presetsSection != null) {
            for (String key : presetsSection.getKeys(false)) {
                ConfigurationSection section = presetsSection.getConfigurationSection(key);
                if (section == null) {
                    continue;
                }

                loadedPresets.put(key.toLowerCase(Locale.ROOT), RulePreset.fromSection(key, section));
            }
        }

        presets = Collections.unmodifiableMap(new LinkedHashMap<>(loadedPresets));
    }

    public synchronized void savePresets() {
        StringBuilder content = new StringBuilder();
        content.append("#   _____ _                 ____  _            _\n");
        content.append("#  |_   _| |               |  _ \\| |          | |\n");
        content.append("#    | | | |_ ___ _ __ ___ | |_) | | ___   ___| | _____ _ __\n");
        content.append("#    | | | __/ _ \\ '_ ` _ \\|  _ <| |/ _ \\ / __| |/ / _ \\ '__|\n");
        content.append("#   _| |_| ||  __/ | | | | | |_) | | (_) | (__|   <  __/ |\n");
        content.append("#  |_____|\\__\\___|_| |_| |_|____/|_|\\___/ \\___|_|\\_\\___|_|\n");
        content.append("#\n");
        content.append("#                ItemBlocker Presets\n\n");
        content.append("# Example presets.\n\n");

        if (presets.isEmpty()) {
            content.append("presets: {}\n");
            writeTextFile(presetsFile.toPath(), content.toString());
            return;
        }

        content.append("presets:\n");
        presets.values().stream()
                .sorted(Comparator.comparing(RulePreset::getName))
                .forEach(preset -> appendPreset(content, preset));

        writeTextFile(presetsFile.toPath(), content.toString());
    }

    public BlockCheckResult check(Material material, BlockAction action, String worldName) {
        for (RulePreset preset : presets.values()) {
            if (preset.matches(material, action, worldName)) {
                return BlockCheckResult.blocked(preset.getName(), preset.getReason(), false);
            }
        }

        return BlockCheckResult.allowed();
    }

    public BlockCheckResult checkEnchantment(Enchantment enchantment, int level, String worldName) {
        for (RulePreset preset : presets.values()) {
            if (preset.matchesEnchantment(enchantment, level, worldName)) {
                return BlockCheckResult.blocked(preset.getName(), preset.getReason(), false);
            }
        }

        return BlockCheckResult.allowed();
    }

    public BlockCheckResult checkPotionEffect(PotionEffectType effectType, int level, String worldName) {
        for (RulePreset preset : presets.values()) {
            if (preset.matchesPotionEffect(effectType, level, worldName)) {
                return BlockCheckResult.blocked(preset.getName(), preset.getReason(), false);
            }
        }

        return BlockCheckResult.allowed();
    }

    public boolean isConfigured(Material material) {
        return presets.values().stream().anyMatch(preset -> preset.references(material));
    }

    public List<RulePreset> getMatchingPresets(Material material) {
        List<RulePreset> matches = new ArrayList<>();
        for (RulePreset preset : presets.values()) {
            if (preset.references(material)) {
                matches.add(preset);
            }
        }
        return matches;
    }

    public synchronized boolean createPreset(String name) {
        String normalized = normalizeName(name);
        if (normalized == null || RESERVED_TARGET_NAMES.contains(normalized) || presets.containsKey(normalized)) {
            return false;
        }

        Map<String, RulePreset> updated = new LinkedHashMap<>(presets);
        updated.put(normalized, RulePreset.empty(normalized));
        presets = Collections.unmodifiableMap(updated);
        savePresets();
        return true;
    }

    public synchronized boolean deletePreset(String name) {
        String normalized = normalizeName(name);
        if (normalized == null || RESERVED_TARGET_NAMES.contains(normalized) || !presets.containsKey(normalized)) {
            return false;
        }

        Map<String, RulePreset> updated = new LinkedHashMap<>(presets);
        updated.remove(normalized);
        presets = Collections.unmodifiableMap(updated);
        savePresets();
        return true;
    }

    public synchronized boolean upsertPresetItem(String name, Material material, Set<BlockAction> actions) {
        String normalized = normalizeName(name);
        RulePreset preset = normalized == null ? null : presets.get(normalized);
        if (preset == null) {
            return false;
        }

        Map<String, RulePreset> updated = new LinkedHashMap<>(presets);
        updated.put(normalized, preset.withItem(material, actions));
        presets = Collections.unmodifiableMap(updated);
        savePresets();
        return true;
    }

    public synchronized boolean replacePresetItemRule(String name, Material material, ItemRule itemRule) {
        String normalized = normalizeName(name);
        RulePreset preset = normalized == null ? null : presets.get(normalized);
        if (preset == null || material == null || itemRule == null) {
            return false;
        }

        Map<String, RulePreset> updated = new LinkedHashMap<>(presets);
        updated.put(normalized, preset.withItemRule(material, itemRule));
        presets = Collections.unmodifiableMap(updated);
        savePresets();
        return true;
    }

    public synchronized boolean appendPresetItem(String name, Material material, Set<BlockAction> actions, WorldScopeMode mode, Set<String> worlds) {
        String normalized = normalizeName(name);
        RulePreset preset = normalized == null ? null : presets.get(normalized);
        if (preset == null) {
            return false;
        }

        Set<String> normalizedWorlds = new LinkedHashSet<>();
        for (String world : worlds) {
            if (world != null && !world.isBlank()) {
                normalizedWorlds.add(world.toLowerCase(Locale.ROOT));
            }
        }

        Map<String, RulePreset> updated = new LinkedHashMap<>(presets);
        updated.put(normalized, preset.appendItem(material, actions, mode, normalizedWorlds));
        presets = Collections.unmodifiableMap(updated);
        savePresets();
        return true;
    }

    public synchronized boolean mergePresetItem(String name, Material material, Set<BlockAction> actions, WorldScopeMode mode, Set<String> worlds) {
        String normalized = normalizeName(name);
        RulePreset preset = normalized == null ? null : presets.get(normalized);
        if (preset == null) {
            return false;
        }

        Set<String> normalizedWorlds = new LinkedHashSet<>();
        for (String world : worlds) {
            if (world != null && !world.isBlank()) {
                normalizedWorlds.add(world.toLowerCase(Locale.ROOT));
            }
        }

        Map<String, RulePreset> updated = new LinkedHashMap<>(presets);
        updated.put(normalized, preset.mergeItem(material, actions, mode, normalizedWorlds));
        presets = Collections.unmodifiableMap(updated);
        savePresets();
        return true;
    }

    public synchronized boolean updatePresetItemWorlds(String name, Material material, WorldScopeMode mode, Set<String> worlds) {
        String normalized = normalizeName(name);
        RulePreset preset = normalized == null ? null : presets.get(normalized);
        if (preset == null || !preset.references(material)) {
            return false;
        }

        Set<String> normalizedWorlds = new LinkedHashSet<>();
        for (String world : worlds) {
            if (world != null && !world.isBlank()) {
                normalizedWorlds.add(world.toLowerCase(Locale.ROOT));
            }
        }

        Map<String, RulePreset> updated = new LinkedHashMap<>(presets);
        updated.put(normalized, preset.withItemWorldScope(material, mode, normalizedWorlds));
        presets = Collections.unmodifiableMap(updated);
        savePresets();
        return true;
    }

    public synchronized boolean removePresetItem(String name, Material material) {
        String normalized = normalizeName(name);
        RulePreset preset = normalized == null ? null : presets.get(normalized);
        if (preset == null || !preset.references(material)) {
            return false;
        }

        Map<String, RulePreset> updated = new LinkedHashMap<>(presets);
        updated.put(normalized, preset.withoutItem(material));
        presets = Collections.unmodifiableMap(updated);
        savePresets();
        return true;
    }

    public synchronized boolean upsertPresetEnchantment(String name, String key, int minimumLevel) {
        String normalized = normalizeName(name);
        RulePreset preset = normalized == null ? null : presets.get(normalized);
        if (preset == null) {
            return false;
        }

        Map<String, RulePreset> updated = new LinkedHashMap<>(presets);
        updated.put(normalized, preset.withEnchantmentRule(key, minimumLevel));
        presets = Collections.unmodifiableMap(updated);
        savePresets();
        return true;
    }

    public synchronized boolean removePresetEnchantment(String name, String key) {
        String normalized = normalizeName(name);
        RulePreset preset = normalized == null ? null : presets.get(normalized);
        if (preset == null || !preset.getEnchantmentRules().contains(key)) {
            return false;
        }

        Map<String, RulePreset> updated = new LinkedHashMap<>(presets);
        updated.put(normalized, preset.withoutEnchantmentRule(key));
        presets = Collections.unmodifiableMap(updated);
        savePresets();
        return true;
    }

    public synchronized boolean upsertPresetPotion(String name, String key, int minimumLevel) {
        String normalized = normalizeName(name);
        RulePreset preset = normalized == null ? null : presets.get(normalized);
        if (preset == null) {
            return false;
        }

        Map<String, RulePreset> updated = new LinkedHashMap<>(presets);
        updated.put(normalized, preset.withPotionRule(key, minimumLevel));
        presets = Collections.unmodifiableMap(updated);
        savePresets();
        return true;
    }

    public synchronized boolean removePresetPotion(String name, String key) {
        String normalized = normalizeName(name);
        RulePreset preset = normalized == null ? null : presets.get(normalized);
        if (preset == null || !preset.getPotionRules().contains(key)) {
            return false;
        }

        Map<String, RulePreset> updated = new LinkedHashMap<>(presets);
        updated.put(normalized, preset.withoutPotionRule(key));
        presets = Collections.unmodifiableMap(updated);
        savePresets();
        return true;
    }

    public synchronized boolean updatePresetDescription(String name, String description) {
        String normalized = normalizeName(name);
        RulePreset preset = normalized == null ? null : presets.get(normalized);
        if (preset == null) {
            return false;
        }

        Map<String, RulePreset> updated = new LinkedHashMap<>(presets);
        updated.put(normalized, preset.withDescription(description));
        presets = Collections.unmodifiableMap(updated);
        savePresets();
        return true;
    }

    public synchronized boolean updatePresetReason(String name, String reason) {
        String normalized = normalizeName(name);
        RulePreset preset = normalized == null ? null : presets.get(normalized);
        if (preset == null) {
            return false;
        }

        Map<String, RulePreset> updated = new LinkedHashMap<>(presets);
        updated.put(normalized, preset.withReason(reason));
        presets = Collections.unmodifiableMap(updated);
        savePresets();
        return true;
    }

    public synchronized boolean updatePresetWorlds(String name, WorldScopeMode mode, Set<String> worlds) {
        String normalized = normalizeName(name);
        RulePreset preset = normalized == null ? null : presets.get(normalized);
        if (preset == null) {
            return false;
        }

        Set<String> normalizedWorlds = new LinkedHashSet<>();
        for (String world : worlds) {
            if (world != null && !world.isBlank()) {
                normalizedWorlds.add(world.toLowerCase(Locale.ROOT));
            }
        }

        Map<String, RulePreset> updated = new LinkedHashMap<>(presets);
        updated.put(normalized, preset.withWorldScope(mode, normalizedWorlds));
        presets = Collections.unmodifiableMap(updated);
        savePresets();
        return true;
    }

    public List<RulePreset> getPresets() {
        return new ArrayList<>(presets.values());
    }

    public Optional<RulePreset> getPreset(String name) {
        String normalized = normalizeName(name);
        if (normalized == null) {
            return Optional.empty();
        }

        return Optional.ofNullable(presets.get(normalized));
    }

    public List<String> getPresetNames() {
        return new ArrayList<>(presets.keySet());
    }

    public int getPresetCount() {
        return presets.size();
    }

    private String normalizeName(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        return value.trim().toLowerCase(Locale.ROOT);
    }

    private void appendPreset(StringBuilder content, RulePreset preset) {
        content.append("  ").append(preset.getName()).append(":\n");

        if (!preset.getDescription().isBlank()) {
            content.append("    description: ").append(quoteYaml(preset.getDescription())).append("\n");
        }

        if (!preset.getReason().isBlank()) {
            content.append("    reason: ").append(quoteYaml(preset.getReason())).append("\n");
        }

        if (preset.getWorldScopeMode() != WorldScopeMode.DISABLED || !preset.getWorlds().isEmpty()) {
            appendWorldSection(content, "    ", preset.getWorldScopeMode(), preset.getWorlds());
        }

        appendThresholdSection(content, "    ", "enchantments", preset.getEnchantmentRules());
        appendThresholdSection(content, "    ", "potions", preset.getPotionRules());

        if (preset.getItemRules().isEmpty()) {
            if (!preset.getEnchantmentRules().isEmpty() || !preset.getPotionRules().isEmpty()) {
                content.append("\n");
            }
            content.append("    items: {}\n\n");
            return;
        }

        if (!preset.getEnchantmentRules().isEmpty() || !preset.getPotionRules().isEmpty()) {
            content.append("\n");
        }
        content.append("    # One item = one rule. Use another preset if the same item needs different behavior.\n");
        content.append("    items:\n");
        preset.getItemRules().entrySet().stream()
                .sorted(Map.Entry.comparingByKey(Comparator.comparing(Material::name)))
                .forEach(entry -> appendItemRule(content, "      ", entry.getKey(), entry.getValue()));

        content.append("\n");
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

    private void appendThresholdSection(StringBuilder content, String indent, String sectionName, ThresholdRuleSet ruleSet) {
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
