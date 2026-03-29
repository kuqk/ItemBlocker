package pl.variant.utils;

import java.util.Collection;
import java.util.Locale;
import java.util.stream.Collectors;

public final class TextUtils {

    private TextUtils() {
    }

    public static String formatEnumName(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }

        String[] parts = value.toLowerCase(Locale.ROOT).split("_");
        StringBuilder builder = new StringBuilder();

        for (String part : parts) {
            if (part.isBlank()) {
                continue;
            }

            if (builder.length() > 0) {
                builder.append(' ');
            }

            builder.append(Character.toUpperCase(part.charAt(0)));
            builder.append(part.substring(1));
        }

        return builder.toString();
    }

    public static String join(Collection<String> values) {
        return values.stream()
                .filter(value -> value != null && !value.isBlank())
                .collect(Collectors.joining(", "));
    }
}
