package pl.variant.model;

import org.bukkit.configuration.ConfigurationSection;

import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public final class ItemRule {

    private final List<ScopedRule> scopedRules;

    public ItemRule(Set<BlockAction> actions, WorldScopeMode worldScopeMode, Set<String> worlds) {
        this(List.of(new ScopedRule(actions, worldScopeMode, worlds)));
    }

    public ItemRule(List<ScopedRule> scopedRules) {
        this.scopedRules = normalizeRules(scopedRules);
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
                    continue;
                }
            }

            if (!parsedRules.isEmpty()) {
                return new ItemRule(parsedRules);
            }
        }

        List<String> values = getStringList(rawValue);
        if (values.isEmpty()) {
            return new ItemRule(EnumSet.allOf(BlockAction.class), WorldScopeMode.DISABLED, Set.of());
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
        return new ItemRule(parseActions(actionValues), mode, worldValues);
    }

    public boolean matches(BlockAction action, String worldName) {
        for (ScopedRule scopedRule : scopedRules) {
            if (scopedRule.matches(action, worldName)) {
                return true;
            }
        }
        return false;
    }

    public boolean matchesWorld(String worldName) {
        for (ScopedRule scopedRule : scopedRules) {
            if (scopedRule.matchesWorld(worldName)) {
                return true;
            }
        }
        return false;
    }

    public ItemRule withActions(Set<BlockAction> newActions) {
        if (scopedRules.isEmpty()) {
            return new ItemRule(newActions, WorldScopeMode.DISABLED, Set.of());
        }

        List<ScopedRule> updated = new ArrayList<>();
        for (ScopedRule scopedRule : scopedRules) {
            updated.add(scopedRule.withActions(newActions));
        }
        return new ItemRule(updated);
    }

    public ItemRule withWorldScope(WorldScopeMode mode, Set<String> newWorlds) {
        if (scopedRules.isEmpty()) {
            return new ItemRule(EnumSet.allOf(BlockAction.class), mode, newWorlds);
        }

        List<ScopedRule> updated = new ArrayList<>();
        for (ScopedRule scopedRule : scopedRules) {
            updated.add(scopedRule.withWorldScope(mode, newWorlds));
        }
        return new ItemRule(updated);
    }

    public ItemRule append(Set<BlockAction> actions, WorldScopeMode mode, Set<String> worlds) {
        ScopedRule incoming = new ScopedRule(actions, mode, worlds);
        List<ScopedRule> updated = new ArrayList<>(scopedRules);

        for (int index = 0; index < updated.size(); index++) {
            ScopedRule existing = updated.get(index);
            if (!existing.hasSameWorldScope(incoming)) {
                continue;
            }

            updated.set(index, existing.mergeActions(incoming.actions()));
            return new ItemRule(updated);
        }

        updated.add(incoming);
        return new ItemRule(updated);
    }

    public ItemRule merge(Set<BlockAction> actions, WorldScopeMode mode, Set<String> worlds) {
        ScopedRule incoming = new ScopedRule(actions, mode, worlds);
        if (scopedRules.isEmpty()) {
            return new ItemRule(List.of(incoming));
        }

        if (scopedRules.size() > 1) {
            return append(actions, mode, worlds);
        }

        return new ItemRule(List.of(scopedRules.getFirst().mergeWith(incoming)));
    }

    public ItemRule replace(Set<BlockAction> actions, WorldScopeMode mode, Set<String> worlds) {
        return new ItemRule(actions, mode, worlds);
    }

    public EnumSet<BlockAction> getActions() {
        EnumSet<BlockAction> combined = EnumSet.noneOf(BlockAction.class);
        for (ScopedRule scopedRule : scopedRules) {
            combined.addAll(scopedRule.actions());
        }
        return combined.isEmpty() ? EnumSet.allOf(BlockAction.class) : combined;
    }

    public WorldScopeMode getWorldScopeMode() {
        if (scopedRules.isEmpty()) {
            return WorldScopeMode.DISABLED;
        }

        ScopedRule first = scopedRules.getFirst();
        for (ScopedRule scopedRule : scopedRules) {
            if (scopedRule.mode() != first.mode() || !scopedRule.worlds().equals(first.worlds())) {
                return scopedRule.worlds().isEmpty() ? WorldScopeMode.DISABLED : WorldScopeMode.WHITELIST;
            }
        }

        return first.mode();
    }

    public Set<String> getWorlds() {
        Set<String> combined = new LinkedHashSet<>();
        for (ScopedRule scopedRule : scopedRules) {
            combined.addAll(scopedRule.worlds());
        }
        return combined;
    }

    public boolean hasCustomWorldScope() {
        return scopedRules.stream().anyMatch(ScopedRule::hasCustomWorldScope);
    }

    public boolean hasMultipleScopes() {
        return scopedRules.size() > 1;
    }

    public List<ScopedRule> getScopedRules() {
        return copyRules(scopedRules);
    }

    private static ItemRule fromSingleRuleValue(Object rawValue) {
        return new ItemRule(List.of(parseScopedRule(rawValue)));
    }

    private static ScopedRule parseScopedRule(Object rawValue) {
        if (rawValue instanceof ConfigurationSection section) {
            EnumSet<BlockAction> actions = parseActions(section.get("actions"));
            ParsedWorldScopeValue worldScope = parseWorldScopeValue(section.get("worlds"));
            return new ScopedRule(actions, worldScope.mode(), worldScope.worlds());
        }

        if (rawValue instanceof Map<?, ?> valueMap) {
            EnumSet<BlockAction> actions = parseActions(valueMap.get("actions"));
            ParsedWorldScopeValue worldScope = parseWorldScopeValue(valueMap.get("worlds"));
            return new ScopedRule(actions, worldScope.mode(), worldScope.worlds());
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

    private static List<ScopedRule> normalizeRules(List<ScopedRule> rules) {
        List<ScopedRule> normalized = new ArrayList<>();
        if (rules == null || rules.isEmpty()) {
            normalized.add(new ScopedRule(EnumSet.allOf(BlockAction.class), WorldScopeMode.DISABLED, Set.of()));
            return normalized;
        }

        for (ScopedRule rule : rules) {
            if (rule == null) {
                continue;
            }

            boolean merged = false;
            for (int index = 0; index < normalized.size(); index++) {
                ScopedRule existing = normalized.get(index);
                if (!existing.hasSameWorldScope(rule)) {
                    continue;
                }

                normalized.set(index, existing.mergeActions(rule.actions()));
                merged = true;
                break;
            }

            if (!merged) {
                normalized.add(new ScopedRule(rule.actions(), rule.mode(), rule.worlds()));
            }
        }

        if (normalized.isEmpty()) {
            normalized.add(new ScopedRule(EnumSet.allOf(BlockAction.class), WorldScopeMode.DISABLED, Set.of()));
        }

        return normalized;
    }

    private static List<ScopedRule> copyRules(List<ScopedRule> rules) {
        List<ScopedRule> copy = new ArrayList<>();
        for (ScopedRule rule : rules) {
            copy.add(new ScopedRule(rule.actions(), rule.mode(), rule.worlds()));
        }
        return copy;
    }

    private static EnumSet<BlockAction> parseActions(Object rawValue) {
        List<String> values = getStringList(rawValue);
        if (values.isEmpty()) {
            return EnumSet.allOf(BlockAction.class);
        }

        EnumSet<BlockAction> actions = EnumSet.noneOf(BlockAction.class);
        for (String value : values) {
            String normalized = value.toLowerCase(Locale.ROOT);
            if (normalized.equals("all")) {
                return EnumSet.allOf(BlockAction.class);
            }

            BlockAction.fromKey(normalized).ifPresent(actions::add);
        }

        return actions.isEmpty() ? EnumSet.allOf(BlockAction.class) : actions;
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
            return stringValue.isBlank() ? List.of() : List.of(stringValue);
        }

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

    public record ScopedRule(EnumSet<BlockAction> actions, WorldScopeMode mode, Set<String> worlds) {

        public ScopedRule(Set<BlockAction> actions, WorldScopeMode mode, Set<String> worlds) {
            this(
                    actions == null || actions.isEmpty() ? EnumSet.allOf(BlockAction.class) : EnumSet.copyOf(actions),
                    mode == null ? WorldScopeMode.DISABLED : mode,
                    normalizeWorlds(worlds)
            );
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
            EnumSet<BlockAction> merged = EnumSet.copyOf(actions);
            if (extraActions != null && !extraActions.isEmpty()) {
                merged.addAll(extraActions);
            }
            return new ScopedRule(merged, mode, worlds);
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
