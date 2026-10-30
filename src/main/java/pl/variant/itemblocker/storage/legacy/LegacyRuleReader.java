package pl.variant.itemblocker.storage.legacy;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import pl.variant.itemblocker.rule.Action;
import pl.variant.itemblocker.rule.ItemRule;
import pl.variant.itemblocker.rule.ItemTypes;
import pl.variant.itemblocker.rule.LevelLimits;
import pl.variant.itemblocker.rule.Preset;
import pl.variant.itemblocker.rule.RuleBook;
import pl.variant.itemblocker.rule.WorldScope;
import pl.variant.itemblocker.storage.YamlValues;

import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Logger;

/**
 * Reads the {@code blocked-items.yml} and {@code presets.yml} files of ItemBlocker 1.x, including the
 * older value formats that 1.x still accepted.
 */
final class LegacyRuleReader {

    private static final int MAX_NAME_LENGTH = 32;

    private final Logger logger;

    LegacyRuleReader(Logger logger) {
        this.logger = logger;
    }

    /**
     * @param blockedItems contents of {@code blocked-items.yml}, or {@code null} if it did not exist
     * @param presets      contents of {@code presets.yml}, or {@code null} if it did not exist
     */
    RuleBook read(ConfigurationSection blockedItems, ConfigurationSection presets) {
        List<Preset> result = new ArrayList<>();

        ConfigurationSection defaultSection = blockedItems == null ? null : blockedItems.getConfigurationSection("default");
        if (defaultSection != null) {
            result.add(readPreset(RuleBook.DEFAULT_PRESET, defaultSection));
        }

        ConfigurationSection presetsSection = presets == null ? null : presets.getConfigurationSection("presets");
        if (presetsSection != null) {
            Set<String> usedNames = new HashSet<>(Set.of(RuleBook.DEFAULT_PRESET));
            for (String name : presetsSection.getKeys(false)) {
                ConfigurationSection section = presetsSection.getConfigurationSection(name);
                if (section == null) {
                    continue;
                }

                Optional<String> newName = convertName(name);
                if (newName.isEmpty() || !usedNames.add(newName.get())) {
                    logger.warning("Could not migrate preset '" + name + "': its name cannot be converted to a unique valid name");
                    continue;
                }
                if (!newName.get().equals(name)) {
                    logger.warning("Renamed preset '" + name + "' to '" + newName.get() + "'");
                }
                result.add(readPreset(newName.get(), section));
            }
        }

        return RuleBook.of(result);
    }

    private Preset readPreset(String name, ConfigurationSection section) {
        Map<Material, ItemRule> items = new LinkedHashMap<>();
        ConfigurationSection itemsSection = section.getConfigurationSection("items");
        if (itemsSection != null) {
            for (String key : itemsSection.getKeys(false)) {
                Optional<Material> material = ItemTypes.parse(key);
                if (material.isEmpty()) {
                    logger.warning("Skipping unknown item '" + key + "' in preset " + name);
                    continue;
                }
                items.put(material.get(), readItemRule(itemsSection.get(key)));
            }
        }

        return new Preset(
                name,
                section.getString("description", ""),
                section.getString("reason", ""),
                readScope(section.get("worlds")),
                items,
                readLevels(section.get("enchantments")),
                readLevels(section.get("potions"))
        );
    }

    /**
     * 1.x accepted a section with actions and worlds, a list of such sections, or a flat list mixing
     * action names and world names.
     */
    private ItemRule readItemRule(Object value) {
        if (value instanceof ConfigurationSection section) {
            return new ItemRule(readActions(section.get("actions")), readScope(section.get("worlds")));
        }
        if (value instanceof Map<?, ?> map) {
            return new ItemRule(readActions(map.get("actions")), readScope(map.get("worlds")));
        }
        if (value instanceof Collection<?> collection && collection.stream().anyMatch(this::isRuleSection)) {
            return collection.stream()
                    .filter(this::isRuleSection)
                    .map(this::readItemRule)
                    .reduce(ItemRule::merge)
                    .orElseGet(ItemRule::blockEverything);
        }

        List<String> actionValues = new ArrayList<>();
        List<String> worldValues = new ArrayList<>();
        for (String entry : YamlValues.strings(value)) {
            if (entry.equalsIgnoreCase("all") || Action.fromKey(entry).isPresent()) {
                actionValues.add(entry);
            } else {
                worldValues.add(entry);
            }
        }
        return new ItemRule(readActions(actionValues.isEmpty() ? null : actionValues), WorldScope.of(worldValues));
    }

    private boolean isRuleSection(Object value) {
        return value instanceof ConfigurationSection || value instanceof Map<?, ?>;
    }

    private static EnumSet<Action> readActions(Object value) {
        List<String> keys = YamlValues.strings(value);
        if (keys.isEmpty() || keys.stream().anyMatch("all"::equalsIgnoreCase)) {
            return EnumSet.allOf(Action.class);
        }

        EnumSet<Action> actions = EnumSet.noneOf(Action.class);
        keys.forEach(key -> Action.fromKey(key).ifPresent(actions::add));
        return actions;
    }

    /**
     * 1.x stored worlds as {@code all}, a list, or a section with a {@code mode} and a {@code list}.
     */
    private static WorldScope readScope(Object value) {
        if (value instanceof ConfigurationSection section) {
            return readScopeSection(section.getString("mode", "disabled"), section.get("list"));
        }
        if (value instanceof Map<?, ?> map) {
            Object mode = map.get("mode");
            return readScopeSection(mode == null ? "disabled" : mode.toString(), map.get("list"));
        }

        List<String> worlds = YamlValues.strings(value);
        if (!worlds.isEmpty() && worlds.getFirst().equalsIgnoreCase("disabled")) {
            return WorldScope.ALL;
        }
        return WorldScope.of(worlds);
    }

    private static WorldScope readScopeSection(String mode, Object list) {
        return mode.equalsIgnoreCase("whitelist") ? WorldScope.of(YamlValues.strings(list)) : WorldScope.ALL;
    }

    /**
     * 1.x stored levels as a map of key to minimum level, or a list of keys blocked from level 1.
     */
    private LevelLimits readLevels(Object value) {
        Map<String, Integer> levels = new LinkedHashMap<>();
        if (value instanceof ConfigurationSection section) {
            section.getKeys(false).forEach(key -> levels.put(key, Math.max(1, section.getInt(key, 1))));
        } else {
            YamlValues.strings(value).forEach(key -> levels.put(key, 1));
        }
        return new LevelLimits(levels);
    }

    private static Optional<String> convertName(String name) {
        String converted = name.trim().toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_-]+", "-");
        converted = converted.replaceAll("^[-_]+", "");
        if (converted.length() > MAX_NAME_LENGTH) {
            converted = converted.substring(0, MAX_NAME_LENGTH);
        }
        return Preset.isValidName(converted) && !converted.equals(RuleBook.DEFAULT_PRESET)
                ? Optional.of(converted)
                : Optional.empty();
    }
}
