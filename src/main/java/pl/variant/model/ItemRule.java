package pl.variant.model;

import org.bukkit.configuration.ConfigurationSection;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public final class ItemRule {

    private final EnumSet<BlockAction> actions;
    private final WorldScopeMode worldScopeMode;
    private final Set<String> worlds;

    public ItemRule(Set<BlockAction> actions, WorldScopeMode worldScopeMode, Set<String> worlds) {
        this.actions = normalizeActions(actions);
        ParsedWorldScopeValue normalizedWorldScope = normalizeWorldScope(worldScopeMode, worlds);
        this.worldScopeMode = normalizedWorldScope.mode();
        this.worlds = normalizedWorldScope.worlds();
    }

    public ItemRule(List<ScopedRule> scopedRules) {
        this(resolveMergedRule(scopedRules));
    }

    private ItemRule(ScopedRule scopedRule) {
        this(scopedRule.actions(), scopedRule.mode(), scopedRule.worlds());
    }

    public static ItemRule allActions() {
        return new ItemRule(EnumSet.allOf(BlockAction.class), WorldScopeMode.DISABLED, Set.of());
    }

    public static ItemRule fromConfigValue(Object rawValue) {
        if (rawValue instanceof ConfigurationSection section) {
            return fromSingleRuleValue(section);
        }

        if (rawValue instanceof Map<?, ?> valueMap) {
            return fromSingleRuleValue(valueMap);
        }

        if (rawValue instanceof Collection<?> collection) {
            List<ScopedRule> parsedRules = new ArrayList<>();
            for (Object entry : collection) {
                if (entry instanceof ConfigurationSection sectionEntry) {
                    parsedRules.add(parseScopedRule(sectionEntry));
                    continue;
                }

                if (entry instanceof Map<?, ?> mapEntry) {
                    parsedRules.add(parseScopedRule(mapEntry));
                }
            }

            if (!parsedRules.isEmpty()) {
                return new ItemRule(parsedRules);
            }
        }

        List<String> values = getStringList(rawValue);
        if (!values.isEmpty()) {
            return fromFlatValues(values);
        }

        return allActions();
    }

    public boolean matches(BlockAction action, String worldName) {
        return actions.contains(action) && matchesWorld(worldName);
    }

    public boolean matchesWorld(String worldName) {
        if (worldScopeMode == WorldScopeMode.DISABLED || worlds.isEmpty()) {
            return true;
        }

        String normalizedWorld = worldName == null ? "" : worldName.toLowerCase(Locale.ROOT);
        return worlds.contains(normalizedWorld);
    }

    public ItemRule withActions(Set<BlockAction> newActions) {
        return new ItemRule(newActions, worldScopeMode, worlds);
    }

    public ItemRule withWorldScope(WorldScopeMode mode, Set<String> newWorlds) {
        return new ItemRule(actions, mode, newWorlds);
    }

    public ItemRule append(Set<BlockAction> extraActions, WorldScopeMode mode, Set<String> newWorlds) {
        return merge(extraActions, mode, newWorlds);
    }

    public ItemRule merge(Set<BlockAction> extraActions, WorldScopeMode mode, Set<String> newWorlds) {
        ScopedRule mergedRule = toScopedRule().mergeWith(new ScopedRule(extraActions, mode, newWorlds));
        return new ItemRule(mergedRule.actions(), mergedRule.mode(), mergedRule.worlds());
    }

    public ItemRule replace(Set<BlockAction> newActions, WorldScopeMode mode, Set<String> newWorlds) {
        return new ItemRule(newActions, mode, newWorlds);
    }

    public EnumSet<BlockAction> getActions() {
        return EnumSet.copyOf(actions);
    }

    public WorldScopeMode getWorldScopeMode() {
        return worldScopeMode;
    }

    public Set<String> getWorlds() {
        return new LinkedHashSet<>(worlds);
    }

    public boolean hasCustomWorldScope() {
        return worldScopeMode != WorldScopeMode.DISABLED && !worlds.isEmpty();
    }

    public boolean hasMultipleScopes() {
        return false;
    }

    public List<ScopedRule> getScopedRules() {
        return List.of(toScopedRule());
    }

    private static ItemRule fromSingleRuleValue(Object rawValue) {
        ScopedRule scopedRule = parseScopedRule(rawValue);
        return new ItemRule(scopedRule.actions(), scopedRule.mode(), scopedRule.worlds());
    }

    private static ItemRule fromFlatValues(List<String> values) {
        List<String> actionValues = new ArrayList<>();
        Set<String> worldValues = new LinkedHashSet<>();
        for (String value : values) {
            String normalized = value.toLowerCase(Locale.ROOT);
            if (normalized.equals("all") || BlockAction.fromKey(normalized).isPresent()) {
                actionValues.add(value);
            } else {
                worldValues.add(value);
            }
        }

        WorldScopeMode mode = worldValues.isEmpty() ? WorldScopeMode.DISABLED : WorldScopeMode.WHITELIST;
        return new ItemRule(parseActions(actionValues), mode, worldValues);
    }

    private ScopedRule toScopedRule() {
        return new ScopedRule(actions, worldScopeMode, worlds);
    }

    private static ScopedRule resolveMergedRule(List<ScopedRule> scopedRules) {
        ScopedRule mergedRule = null;
        if (scopedRules != null) {
            for (ScopedRule scopedRule : scopedRules) {
                if (scopedRule == null) {
                    continue;
                }

                mergedRule = mergedRule == null
                        ? new ScopedRule(scopedRule.actions(), scopedRule.mode(), scopedRule.worlds())
                        : mergedRule.mergeWith(scopedRule);
            }
        }

        return mergedRule == null
                ? new ScopedRule(EnumSet.allOf(BlockAction.class), WorldScopeMode.DISABLED, Set.of())
                : mergedRule;
    }

    private static ScopedRule parseScopedRule(Object rawValue) {
        if (rawValue instanceof ConfigurationSection section) {
            EnumSet<BlockAction> parsedActions = parseActions(section.get("actions"));
            ParsedWorldScopeValue worldScope = parseWorldScopeValue(section.get("worlds"));
            return new ScopedRule(parsedActions, worldScope.mode(), worldScope.worlds());
        }

        if (rawValue instanceof Map<?, ?> valueMap) {
            EnumSet<BlockAction> parsedActions = parseActions(valueMap.get("actions"));
            ParsedWorldScopeValue worldScope = parseWorldScopeValue(valueMap.get("worlds"));
            return new ScopedRule(parsedActions, worldScope.mode(), worldScope.worlds());
        }

        List<String> values = getStringList(rawValue);
        if (values.isEmpty()) {
            return new ScopedRule(EnumSet.allOf(BlockAction.class), WorldScopeMode.DISABLED, Set.of());
        }

        List<String> actionValues = new ArrayList<>();
        Set<String> worldValues = new LinkedHashSet<>();
        for (String value : values) {
            String normalized = value.toLowerCase(Locale.ROOT);
            if (normalized.equals("all") || BlockAction.fromKey(normalized).isPresent()) {
                actionValues.add(value);
            } else {
                worldValues.add(value);
            }
        }

        WorldScopeMode mode = worldValues.isEmpty() ? WorldScopeMode.DISABLED : WorldScopeMode.WHITELIST;
        return new ScopedRule(parseActions(actionValues), mode, worldValues);
    }

    private static EnumSet<BlockAction> parseActions(Object rawValue) {
        List<String> values = getStringList(rawValue);
        if (values.isEmpty()) {
            return EnumSet.allOf(BlockAction.class);
        }

        EnumSet<BlockAction> parsedActions = EnumSet.noneOf(BlockAction.class);
        boolean explicitValuesProvided = false;
        for (String value : values) {
            String normalized = value.toLowerCase(Locale.ROOT);
            if (normalized.isBlank()) {
                continue;
            }

            explicitValuesProvided = true;
            if (normalized.equals("all")) {
                return EnumSet.allOf(BlockAction.class);
            }
            if (normalized.equals("none")) {
                continue;
            }

            BlockAction.fromKey(normalized).ifPresent(parsedActions::add);
        }

        return explicitValuesProvided ? parsedActions : EnumSet.allOf(BlockAction.class);
    }

    private static ParsedWorldScopeValue parseWorldScopeValue(Object rawValue) {
        if (rawValue instanceof ConfigurationSection section) {
            WorldScopeMode mode = WorldScopeMode.fromValue(section.getString("mode", "disabled"));
            Set<String> parsedWorlds = normalizeWorlds(section.getStringList("list"));
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

    private static EnumSet<BlockAction> normalizeActions(Set<BlockAction> source) {
        if (source == null) {
            return EnumSet.allOf(BlockAction.class);
        }

        return source.isEmpty() ? EnumSet.noneOf(BlockAction.class) : EnumSet.copyOf(source);
    }

    private static ParsedWorldScopeValue normalizeWorldScope(WorldScopeMode mode, Set<String> sourceWorlds) {
        Set<String> normalizedWorlds = normalizeWorlds(sourceWorlds);
        WorldScopeMode resolvedMode = mode == null ? WorldScopeMode.DISABLED : mode;
        if (resolvedMode == WorldScopeMode.DISABLED || normalizedWorlds.isEmpty()) {
            return new ParsedWorldScopeValue(WorldScopeMode.DISABLED, Set.of());
        }

        return new ParsedWorldScopeValue(WorldScopeMode.WHITELIST, normalizedWorlds);
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

    private static Map<?, ?> getMap(Map<?, ?> map, String key) {
        Object value = map.get(key);
        return value instanceof Map<?, ?> valueMap ? valueMap : Map.of();
    }

    private static String getString(Map<?, ?> map, String key, String fallback) {
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

    public record ScopedRule(Set<BlockAction> actions, WorldScopeMode mode, Set<String> worlds) {

        public ScopedRule {
            actions = normalizeActions(actions);
            ParsedWorldScopeValue normalizedWorldScope = normalizeWorldScope(mode, worlds);
            mode = normalizedWorldScope.mode();
            worlds = normalizedWorldScope.worlds();
        }

        public boolean matches(BlockAction action, String worldName) {
            return actions.contains(action) && matchesWorld(worldName);
        }

        public boolean matchesWorld(String worldName) {
            if (mode == WorldScopeMode.DISABLED || worlds.isEmpty()) {
                return true;
            }

            String normalizedWorld = worldName == null ? "" : worldName.toLowerCase(Locale.ROOT);
            return worlds.contains(normalizedWorld);
        }

        public boolean hasCustomWorldScope() {
            return mode != WorldScopeMode.DISABLED && !worlds.isEmpty();
        }

        public boolean hasSameWorldScope(ScopedRule other) {
            return other != null && mode == other.mode && worlds.equals(other.worlds);
        }

        public ScopedRule mergeActions(Set<BlockAction> extraActions) {
            EnumSet<BlockAction> mergedActions = EnumSet.copyOf(actions);
            if (extraActions != null && !extraActions.isEmpty()) {
                mergedActions.addAll(extraActions);
            }
            return new ScopedRule(mergedActions, mode, worlds);
        }

        public ScopedRule mergeWith(ScopedRule other) {
            if (other == null) {
                return this;
            }

            EnumSet<BlockAction> mergedActions = EnumSet.copyOf(actions);
            mergedActions.addAll(other.actions());

            if (mode == WorldScopeMode.DISABLED || other.mode() == WorldScopeMode.DISABLED) {
                return new ScopedRule(mergedActions, WorldScopeMode.DISABLED, Set.of());
            }

            Set<String> mergedWorlds = new LinkedHashSet<>(worlds);
            mergedWorlds.addAll(other.worlds());
            return new ScopedRule(mergedActions, WorldScopeMode.WHITELIST, mergedWorlds);
        }

        public ScopedRule withActions(Set<BlockAction> newActions) {
            return new ScopedRule(newActions, mode, worlds);
        }

        public ScopedRule withWorldScope(WorldScopeMode newMode, Set<String> newWorlds) {
            return new ScopedRule(actions, newMode, newWorlds);
        }
    }

    private record ParsedWorldScopeValue(WorldScopeMode mode, Set<String> worlds) {
    }
}
