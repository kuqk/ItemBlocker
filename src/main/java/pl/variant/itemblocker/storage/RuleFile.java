package pl.variant.itemblocker.storage;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import pl.variant.itemblocker.platform.Compat;
import pl.variant.itemblocker.rule.Action;
import pl.variant.itemblocker.rule.EnchantmentMode;
import pl.variant.itemblocker.rule.FilterRule;
import pl.variant.itemblocker.rule.ItemFilter;
import pl.variant.itemblocker.rule.ItemLimit;
import pl.variant.itemblocker.rule.ItemPattern;
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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
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
            "        actions: all, obtain, or a list of crafting, pickup, drop, use, place, armor, inventory, hopper,",
            "                 smithing, attack, break, crafter, dispense, brew, trade, loot, entity_drop",
            "        worlds: all, or a list of worlds this item rule applies in",
            "        bypass-permission: a permission that lets players ignore this rule (optional)",
            "    groups:",
            "      <pattern>: such as '*_sword' or '#swords', with actions, worlds and bypass-permission like items",
            "    allowed: item types this preset's items and groups never block",
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
            "      <enchantment>: minimum blocked level, or a section with level and mode:",
            "        mode: block (the item cannot be used), downgrade (lowered to the highest allowed level),",
            "              remove (the enchantment is taken off) or confiscate (the item is taken away)",
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
        return parse(Files.readString(path, StandardCharsets.UTF_8));
    }

    /**
     * Reads rules in the format of the file from text, such as the bundled preset templates.
     *
     * @throws IOException if the text is not valid YAML
     */
    public RuleBook parse(String content) throws IOException {
        YamlConfiguration yaml = literalKeysYaml();
        try {
            yaml.loadFromString(content);
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
        YamlConfiguration yaml = literalKeysYaml();
        Compat.header(yaml.options(), HEADER);
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
                    items.put(material.get(), readRule(ruleSection, name, key));
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
                readGroups(section.getConfigurationSection("groups"), name),
                readAllowed(section.get("allowed"), name),
                readLimits(section.getConfigurationSection("limits"), name),
                readFilters(section.getConfigurationSection("filters"), name),
                readLevels(section.getConfigurationSection("enchantments"), name),
                readLevels(section.getConfigurationSection("potions"), name)
        );
    }

    private ItemRule readRule(ConfigurationSection section, String preset, String key) {
        return new ItemRule(
                readActions(section.get("actions"), preset, key),
                readScope(section.get("worlds")),
                section.getString("bypass-permission"));
    }

    private Map<ItemPattern, ItemRule> readGroups(ConfigurationSection section, String preset) {
        Map<ItemPattern, ItemRule> groups = new LinkedHashMap<>();
        if (section == null) {
            return groups;
        }

        for (String key : section.getKeys(false)) {
            Optional<ItemPattern> pattern = ItemPattern.parse(key);
            ConfigurationSection ruleSection = section.getConfigurationSection(key);
            if (pattern.isEmpty() || ruleSection == null) {
                warn("Skipping group '" + key + "' in preset " + preset + ": expected a pattern such as *_sword or #swords");
                continue;
            }
            groups.put(pattern.get(), readRule(ruleSection, preset, key));
        }
        return groups;
    }

    private Set<Material> readAllowed(Object value, String preset) {
        Set<Material> allowed = new LinkedHashSet<>();
        for (String key : YamlValues.strings(value)) {
            ItemTypes.parse(key).ifPresentOrElse(allowed::add,
                    () -> warn("Skipping unknown allowed item '" + key + "' in preset " + preset));
        }
        return allowed;
    }

    private EnumSet<Action> readActions(Object value, String preset, String item) {
        List<String> keys = YamlValues.strings(value);
        if (value == null || keys.stream().anyMatch(ALL::equalsIgnoreCase)) {
            return EnumSet.allOf(Action.class);
        }

        EnumSet<Action> actions = EnumSet.noneOf(Action.class);
        for (String key : keys) {
            if (key.equalsIgnoreCase(Action.OBTAIN_KEYWORD)) {
                actions.addAll(Action.OBTAIN);
                continue;
            }
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

    private static YamlConfiguration literalKeysYaml() {
        YamlConfiguration yaml = new YamlConfiguration();
        // Sections are traversed one level at a time. Registry keys and tag names may contain
        // dots, so they must never be interpreted as Bukkit configuration paths.
        yaml.options().pathSeparator('\0');
        return yaml;
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
            filters.put(id, new FilterRule(filter, readRule(filterSection, preset, id)));
        }
        return filters;
    }

    private LevelLimits readLevels(ConfigurationSection section, String preset) {
        if (section == null) {
            return LevelLimits.NONE;
        }

        Map<String, Integer> levels = new LinkedHashMap<>();
        Map<String, EnchantmentMode> modes = new LinkedHashMap<>();
        for (String key : section.getKeys(false)) {
            // A level on its own blocks; a section adds what to do with the item instead.
            ConfigurationSection detail = section.getConfigurationSection(key);
            int level = detail == null ? section.getInt(key, 0) : detail.getInt("level", 0);
            if (level < 1) {
                warn("Skipping '" + key + "' in preset " + preset + ": the level must be a whole number from 1");
                continue;
            }
            levels.put(key, level);
            if (detail != null && detail.isString("mode")) {
                String modeKey = detail.getString("mode");
                EnchantmentMode.fromKey(modeKey).ifPresentOrElse(mode -> modes.put(key, mode),
                        () -> warn("Ignoring unknown mode '" + modeKey + "' for '" + key + "' in preset " + preset));
            }
        }
        return new LevelLimits(levels, modes);
    }

    private static void writeLevels(ConfigurationSection section, LevelLimits limits) {
        limits.minimumLevels().forEach((key, level) -> {
            EnchantmentMode mode = limits.mode(key);
            if (mode == EnchantmentMode.BLOCK) {
                section.set(key, level);
            } else {
                ConfigurationSection detail = section.createSection(key);
                detail.set("level", level);
                detail.set("mode", mode.key());
            }
        });
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
                    writeRule(rule, entry.getValue());
                });

        ConfigurationSection groups = section.createSection("groups");
        preset.groups().forEach((pattern, rule) -> writeRule(groups.createSection(pattern.text()), rule));
        section.set("allowed", preset.allowed().stream().map(ItemTypes::key).sorted().toList());

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
            writeRule(filterSection, rule.rule());
        });

        writeLevels(section.createSection("enchantments"), preset.enchantments());
        section.createSection("potions", preset.effects().minimumLevels());
    }

    private static void writeRule(ConfigurationSection section, ItemRule rule) {
        section.set("actions", actionsValue(rule));
        section.set("worlds", scopeValue(rule.scope()));
        section.set("bypass-permission", rule.bypassPermission());
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
