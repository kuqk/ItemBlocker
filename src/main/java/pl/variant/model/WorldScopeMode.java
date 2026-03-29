package pl.variant.model;

import java.util.Locale;

public enum WorldScopeMode {

    DISABLED,
    WHITELIST;

    public static WorldScopeMode fromValue(String value) {
        if (value == null || value.isBlank()) {
            return DISABLED;
        }

        try {
            return valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            return DISABLED;
        }
    }
}
