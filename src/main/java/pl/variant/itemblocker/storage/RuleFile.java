package pl.variant.itemblocker.storage;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import pl.variant.itemblocker.rule.Action;
import pl.variant.itemblocker.rule.FilterRule;
import pl.variant.itemblocker.rule.ItemFilter;
import pl.variant.itemblocker.rule.ItemLimit;
import pl.variant.itemblocker.rule.ItemRule;
import pl.variant.itemblocker.rule.ItemTypes;
import pl.variant.itemblocker.rule.LevelLimits;
import pl.variant.itemblocker.rule.Preset;
import pl.variant.itemblocker.rule.RegionScope;
import pl.variant.itemblocker.rule.RuleBook;
import pl.variant.itemblocker.rule.WorldScope;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Logger;

/**
 * Reads and writes {@code rules.yml}.
 */
public final class RuleFile {

    public static final String FILE_NAME = "rules.yml";

    private static final int VERSION = 2;
    private static final String ALL = "all";

    private static final List<String> HEADER = List.of(
            "ItemBlocker rules",
            "",
            "Every preset can hold item rules, item limits, enchantment limits and potion effect limits.",
            "The default preset always exists and applies everywhere unless you limit its worlds.",
            "",
            "presets:",
            "  <name>:",
            "    description: shown in /ib preset list",
            "    reason: added to messages about restrictions from this preset",
            "    worlds: all, or a list of worlds the whole preset applies in",
            "    regions: all, or a list of WorldGuard regions the whole preset applies in",
            "    items:",
            "      <item>:",
            "        actions: all, or a list of crafting, pickup, drop, use, place, armor, inventory, hopper, smithing",
            "        worlds: all, or a list of worlds this item rule applies in",
            "    limits:",
            "      <item>:",
            "        stack: largest stack size",
            "        inventory: most of this item a player may carry",
            "    filters:",
            "      <id>:",
            "        type: item type (optional)",
            "        name: text in the custom name (optional)",
            "        lore: text in a lore line (optional)",
            "        model: custom model data (optional)",
            "        data: persistent data key, or key=value, such as nexo:id=ruby_sword (optional)",
            "        enchantment: an enchantment the item has (optional)",
            "        actions: all, or a list of actions",
            "        worlds: all, or a list of worlds",
            "    enchantments:",
            "      <enchantment>: minimum blocked level",
            "    potions:",
            "      <effect>: minimum blocked level",
            "",
            "Use /ib reload after editing this file by hand."
    );

    private final Path path;
    private final Logger logger;

    public RuleFile(Path path, Logger logger) {
        this.path = path;
        this.logger = logger;
    }

    public Path path() {
        return path;
    }

    public boolean exists() {
        return Files.isRegularFile(path);
    }

    /**
     * Loads the rules. Invalid entries are skipped and logged, a file that is not valid YAML fails.
     *
     * @throws IOException if the file cannot be read or is not valid YAML
     */
    public RuleBook load() throws IOException {
        YamlConfiguration yaml = new YamlConfiguration();
        try {
            yaml.loadFromString(Files.readString(path, StandardCharsets.UTF_8));
        } catch (InvalidConfigurationException exception) {
            throw new IOException(path.getFileName() + " is not valid YAML: " + exception.getMessage(), exception);
        }

        List<Preset> presets = new ArrayList<>();
        ConfigurationSection section = yaml.getConfigurationSection("presets");
        if (section != null) {
            for (String name : section.getKeys(false)) {
                ConfigurationSection presetSection = section.getConfigurationSection(name);
                if (!Preset.isValidName(name) || presetSection == null) {
                    warn("Skipping preset '" + name + "': names may only use a-z, 0-9, _ and -");
                    continue;
                }
                presets.add(readPreset(name, presetSection));
            }
        }
        return RuleBook.of(presets);
    }

    /**
     * Writes the rules, replacing the file atomically so a crash never leaves a half written file.
     */
    public void save(RuleBook rules) throws IOException {
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.options().setHeader(HEADER);
        yaml.set("config-version", VERSION);

        ConfigurationSection presets = yaml.createSection("presets");
        for (Preset preset : rules.presets()) {
            writePreset(presets.createSection(preset.name()), preset);
        }

        Files.createDirectories(path.getParent());
        Path temporary = path.resolveSibling(path.getFileName() + ".tmp");
        Files.writeString(temporary, yaml.saveToString(), StandardCharsets.UTF_8);
        try {
            Files.move(temporary, path, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException exception) {
            Files.move(temporary, path, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private Preset readPreset(String name, ConfigurationSection section) {
        Map<Material, ItemRule> items = new LinkedHashMap<>();
        ConfigurationSection itemsSection = section.getConfigurationSection("items");
        if (itemsSection != null) {
            for (String key : itemsSection.getKeys(false)) {
                Optional<Material> material = ItemTypes.parse(key);
                ConfigurationSection ruleSection = itemsSection.getConfigurationSection(key);
                if (material.isEmpty()) {
                    warn("Skipping unknown item '" + key + "' in preset " + name);
                } else if (ruleSection == null) {
                    warn("Skipping item '" + key + "' in preset " + name + ": expected actions and worlds");
                } else {
                    items.put(material.get(), new ItemRule(
                            readActions(ruleSection.get("actions"), name, key),
                            readScope(ruleSection.get("worlds"))
                    ));
                }
            }
        }

        return new Preset(
                name,
                section.getString("description", ""),
                section.getString("reason", ""),
                readScope(section.get("worlds")),
                RegionScope.of(YamlValues.strings(section.get("regions"))),
                items,
                readLimits(section.getConfigurationSection("limits"), name),
                readFilters(section.getConfigurationSection("filters"), name),
                readLevels(section.getConfigurationSection("enchantments"), name),
                readLevels(section.getConfigurationSection("potions"), name)
        );
    }

    private EnumSet<Action> readActions(Object value, String preset, String item) {
        List<String> keys = YamlValues.strings(value);
        if (value == null || keys.stream().anyMatch(ALL::equalsIgnoreCase)) {
            return EnumSet.allOf(Action.class);
        }

        EnumSet<Action> actions = EnumSet.noneOf(Action.class);
        for (String key : keys) {
            Action.fromKey(key).ifPresentOrElse(
                    actions::add,
                    () -> warn("Ignoring unknown action '" + key + "' for " + item + " in preset " + preset)
            );
        }
        return actions;
    }

    private static WorldScope readScope(Object value) {
        return WorldScope.of(YamlValues.strings(value));
    }

    private Map<Material, ItemLimit> readLimits(ConfigurationSection section, String preset) {
        Map<Material, ItemLimit> limits = new LinkedHashMap<>();
        if (section == null) {
            return limits;
        }

        for (String key : section.getKeys(false)) {
            Optional<Material> material = ItemTypes.parse(key);
            ConfigurationSection limitSection = section.getConfigurationSection(key);
            if (material.isEmpty()) {
                warn("Skipping limits for unknown item '" + key + "' in preset " + preset);
                continue;
            }
            if (limitSection == null) {
                warn("Skipping limits for '" + key + "' in preset " + preset + ": expected stack and inventory");
                continue;
            }

            int stack = limitSection.getInt("stack", 0);
            int inventory = limitSection.getInt("inventory", 0);
            if (stack < 0 || inventory < 0) {
                warn("Skipping limits for '" + key + "' in preset " + preset + ": limits must be whole numbers from 1");
                continue;
            }
            limits.put(material.get(), new ItemLimit(stack, inventory));
        }
        return limits;
    }

    private Map<String, FilterRule> readFilters(ConfigurationSection section, String preset) {
        Map<String, FilterRule> filters = new LinkedHashMap<>();
        if (section == null) {
            return filters;
        }

        for (String id : section.getKeys(false)) {
            ConfigurationSection filterSection = section.getConfigurationSection(id);
            if (!Preset.isValidName(id) || filterSection == null) {
                warn("Skipping filter '" + id + "' in preset " + preset + ": ids may only use a-z, 0-9, _ and -");
                continue;
            }

            String typeKey = filterSection.getString("type");
            Optional<Material> type = typeKey == null ? Optional.empty() : ItemTypes.parse(typeKey);
            if (typeKey != null && type.isEmpty()) {
                warn("Skipping filter '" + id + "' in preset " + preset + ": unknown item '" + typeKey + "'");
                continue;
            }

            String data = filterSection.getString("data");
            int equals = data == null ? -1 : data.indexOf('=');
            ItemFilter filter = new ItemFilter(
                    type.orElse(null),
                    filterSection.getString("name"),
                    filterSection.getString("lore"),
                    filterSection.isInt("model") ? filterSection.getInt("model") : null,
                    equals < 0 ? data : data.substring(0, equals),
                    equals < 0 ? null : data.substring(equals + 1),
                    filterSection.getString("enchantment"));
            if (filter.isEmpty()) {
                warn("Skipping filter '" + id + "' in preset " + preset + ": it has no criteria");
                continue;
            }
            filters.put(id, new FilterRule(filter, new ItemRule(
                    readActions(filterSection.get("actions"), preset, id),
                    readScope(filterSection.get("worlds")))));
        }
        return filters;
    }

    private LevelLimits readLevels(ConfigurationSection section, String preset) {
        if (section == null) {
            return LevelLimits.NONE;
        }

        Map<String, Integer> levels = new LinkedHashMap<>();
        for (String key : section.getKeys(false)) {
            int level = section.getInt(key, 0);
            if (level < 1) {
                warn("Skipping '" + key + "' in preset " + preset + ": the level must be a whole number from 1");
            } else {
                levels.put(key, level);
            }
        }
        return new LevelLimits(levels);
    }

    private static void writePreset(ConfigurationSection section, Preset preset) {
        section.set("description", preset.description());
        section.set("reason", preset.reason());
        section.set("worlds", scopeValue(preset.scope()));
        section.set("regions", preset.regions().isAll() ? ALL : preset.regions().regions().stream().sorted().toList());

        ConfigurationSection items = section.createSection("items");
        preset.items().entrySet().stream()
                .sorted(Map.Entry.comparingByKey(Comparator.comparing(ItemTypes::key)))
                .forEach(entry -> {
                    ConfigurationSection rule = items.createSection(ItemTypes.key(entry.getKey()));
                    rule.set("actions", actionsValue(entry.getValue()));
                    rule.set("worlds", scopeValue(entry.getValue().scope()));
                });

        ConfigurationSection limits = section.createSection("limits");
        preset.limits().entrySet().stream()
                .sorted(Map.Entry.comparingByKey(Comparator.comparing(ItemTypes::key)))
                .forEach(entry -> {
                    ConfigurationSection limit = limits.createSection(ItemTypes.key(entry.getKey()));
                    if (entry.getValue().limitsStack()) {
                        limit.set("stack", entry.getValue().stack());
                    }
                    if (entry.getValue().limitsInventory()) {
                        limit.set("inventory", entry.getValue().inventory());
                    }
                });

        ConfigurationSection filters = section.createSection("filters");
        preset.filters().forEach((id, rule) -> {
            ConfigurationSection filterSection = filters.createSection(id);
            ItemFilter filter = rule.filter();
            filterSection.set("type", filter.type() == null ? null : ItemTypes.key(filter.type()));
            filterSection.set("name", filter.name());
            filterSection.set("lore", filter.lore());
            filterSection.set("model", filter.model());
            filterSection.set("data", filter.dataKey() == null ? null
                    : filter.dataValue() == null ? filter.dataKey() : filter.dataKey() + "=" + filter.dataValue());
            filterSection.set("enchantment", filter.enchantment());
            filterSection.set("actions", actionsValue(rule.rule()));
            filterSection.set("worlds", scopeValue(rule.rule().scope()));
        });

        section.createSection("enchantments", preset.enchantments().minimumLevels());
        section.createSection("potions", preset.effects().minimumLevels());
    }

    private static Object actionsValue(ItemRule rule) {
        if (rule.blocksEveryAction()) {
            return ALL;
        }
        return rule.actions().stream().map(Action::key).toList();
    }

    private static Object scopeValue(WorldScope scope) {
        return scope.isAll() ? ALL : scope.worlds().stream().sorted().toList();
    }

    private void warn(String message) {
        logger.warning(path.getFileName() + ": " + message);
    }
}
