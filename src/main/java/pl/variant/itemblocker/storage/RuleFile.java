package pl.variant.itemblocker.storage;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import pl.variant.itemblocker.rule.Action;
import pl.variant.itemblocker.rule.ItemRule;
import pl.variant.itemblocker.rule.ItemTypes;
import pl.variant.itemblocker.rule.LevelLimits;
import pl.variant.itemblocker.rule.Preset;
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
            "Every preset can hold item rules, enchantment limits and potion effect limits.",
            "The default preset always exists and applies everywhere unless you limit its worlds.",
            "",
            "presets:",
            "  <name>:",
            "    description: shown in /ib preset list",
            "    reason: added to messages about restrictions from this preset",
            "    worlds: all, or a list of worlds the whole preset applies in",
            "    items:",
            "      <item>:",
            "        actions: all, or a list of crafting, pickup, drop, use, place, armor, inventory, hopper, smithing",
            "        worlds: all, or a list of worlds this item rule applies in",
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
                items,
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

        ConfigurationSection items = section.createSection("items");
        preset.items().entrySet().stream()
                .sorted(Map.Entry.comparingByKey(Comparator.comparing(ItemTypes::key)))
                .forEach(entry -> {
                    ConfigurationSection rule = items.createSection(ItemTypes.key(entry.getKey()));
                    rule.set("actions", actionsValue(entry.getValue()));
                    rule.set("worlds", scopeValue(entry.getValue().scope()));
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
