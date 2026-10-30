package pl.variant.itemblocker.rule;

import java.util.Locale;
import java.util.Optional;

/**
 * Something a player can do with an item that a rule can block.
 */
public enum Action {
    CRAFTING,
    PICKUP,
    DROP,
    USE,
    PLACE,
    ARMOR,
    INVENTORY,
    HOPPER,
    SMITHING;

    private final String key = name().toLowerCase(Locale.ROOT);

    /**
     * Returns the lowercase name used in configuration files, commands and permissions.
     */
    public String key() {
        return key;
    }

    public static Optional<Action> fromKey(String key) {
        if (key == null) {
            return Optional.empty();
        }

        String normalized = key.trim().toLowerCase(Locale.ROOT);
        for (Action action : values()) {
            if (action.key.equals(normalized)) {
                return Optional.of(action);
            }
        }
        return Optional.empty();
    }
}
