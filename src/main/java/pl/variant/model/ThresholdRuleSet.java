package pl.variant.model;

import org.bukkit.configuration.ConfigurationSection;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.OptionalInt;

public final class ThresholdRuleSet {

    private final Map<String, Integer> thresholds;

    public ThresholdRuleSet(Map<String, Integer> thresholds) {
        this.thresholds = normalizeThresholds(thresholds);
    }

    public static ThresholdRuleSet empty() {
        return new ThresholdRuleSet(Map.of());
    }

    public static ThresholdRuleSet fromConfigValue(Object rawValue) {
        if (rawValue instanceof ConfigurationSection section) {
            return fromMap(section.getValues(false));
        }

        if (rawValue instanceof Map<?, ?> valueMap) {
            return fromMap(valueMap);
        }

        if (rawValue instanceof Collection<?> collection) {
            Map<String, Integer> parsed = new LinkedHashMap<>();
            for (Object entry : collection) {
                if (entry instanceof String stringValue) {
                    String normalized = normalizeKey(stringValue);
                    if (normalized != null) {
                        parsed.put(normalized, 1);
                    }
                }
            }
            return new ThresholdRuleSet(parsed);
        }

        if (rawValue instanceof String stringValue) {
            String normalized = normalizeKey(stringValue);
            return normalized == null
                    ? empty()
                    : new ThresholdRuleSet(Map.of(normalized, 1));
        }

        return empty();
    }

    public boolean matches(String key, int level) {
        OptionalInt threshold = getThreshold(key);
        return threshold.isPresent() && Math.max(1, level) >= threshold.getAsInt();
    }

    public OptionalInt getThreshold(String key) {
        String normalized = normalizeKey(key);
        if (normalized == null) {
            return OptionalInt.empty();
        }

        Integer threshold = thresholds.get(normalized);
        return threshold == null ? OptionalInt.empty() : OptionalInt.of(threshold);
    }

    public boolean contains(String key) {
        return getThreshold(key).isPresent();
    }

    public ThresholdRuleSet withRule(String key, int minimumLevel) {
        String normalized = normalizeKey(key);
        if (normalized == null) {
            return this;
        }

        Map<String, Integer> updated = new LinkedHashMap<>(thresholds);
        updated.put(normalized, normalizeLevel(minimumLevel));
        return new ThresholdRuleSet(updated);
    }

    public ThresholdRuleSet withoutRule(String key) {
        String normalized = normalizeKey(key);
        if (normalized == null || !thresholds.containsKey(normalized)) {
            return this;
        }

        Map<String, Integer> updated = new LinkedHashMap<>(thresholds);
        updated.remove(normalized);
        return new ThresholdRuleSet(updated);
    }

    public Map<String, Integer> asMap() {
        return new LinkedHashMap<>(thresholds);
    }

    public boolean isEmpty() {
        return thresholds.isEmpty();
    }

    public int size() {
        return thresholds.size();
    }

    public List<String> keys() {
        return new ArrayList<>(thresholds.keySet());
    }

    public static String normalizeKey(String rawKey) {
        if (rawKey == null || rawKey.isBlank()) {
            return null;
        }

        String normalized = rawKey.trim().toLowerCase(Locale.ROOT)
                .replace(' ', '_')
                .replace('-', '_');
        int namespaceSeparator = normalized.indexOf(':');
        if (namespaceSeparator >= 0 && namespaceSeparator + 1 < normalized.length()) {
            normalized = normalized.substring(namespaceSeparator + 1);
        }

        return normalized.isBlank() ? null : normalized;
    }

    private static ThresholdRuleSet fromMap(Map<?, ?> valueMap) {
        Map<String, Integer> parsed = new LinkedHashMap<>();
        for (Map.Entry<?, ?> entry : valueMap.entrySet()) {
            if (!(entry.getKey() instanceof String rawKey)) {
                continue;
            }

            String normalized = normalizeKey(rawKey);
            if (normalized == null) {
                continue;
            }

            parsed.put(normalized, parseLevel(entry.getValue()));
        }
        return new ThresholdRuleSet(parsed);
    }

    private static Map<String, Integer> normalizeThresholds(Map<String, Integer> source) {
        Map<String, Integer> normalized = new LinkedHashMap<>();
        if (source == null) {
            return normalized;
        }

        for (Map.Entry<String, Integer> entry : source.entrySet()) {
            String normalizedKey = normalizeKey(entry.getKey());
            if (normalizedKey == null) {
                continue;
            }

            normalized.put(normalizedKey, normalizeLevel(entry.getValue()));
        }
        return normalized;
    }

    private static int parseLevel(Object rawLevel) {
        if (rawLevel instanceof Number number) {
            return normalizeLevel(number.intValue());
        }

        if (rawLevel instanceof String stringValue) {
            try {
                return normalizeLevel(Integer.parseInt(stringValue.trim()));
            } catch (NumberFormatException ignored) {
                return 1;
            }
        }

        return 1;
    }

    private static int normalizeLevel(Integer level) {
        return level == null ? 1 : Math.max(1, level);
    }
}
