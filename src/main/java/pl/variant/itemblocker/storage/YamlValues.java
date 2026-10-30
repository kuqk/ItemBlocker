package pl.variant.itemblocker.storage;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

/**
 * Helpers for reading loosely typed YAML values.
 */
public final class YamlValues {

    private YamlValues() {
    }

    /**
     * Reads a list of strings from either a YAML list or a comma separated string.
     */
    public static List<String> strings(Object value) {
        List<String> result = new ArrayList<>();
        if (value instanceof String text) {
            addCommaSeparated(result, text);
        } else if (value instanceof Collection<?> collection) {
            for (Object entry : collection) {
                if (entry != null) {
                    addCommaSeparated(result, entry.toString());
                }
            }
        }
        return result;
    }

    private static void addCommaSeparated(List<String> target, String text) {
        Arrays.stream(text.split(","))
                .map(String::trim)
                .filter(part -> !part.isEmpty())
                .forEach(target::add);
    }
}
