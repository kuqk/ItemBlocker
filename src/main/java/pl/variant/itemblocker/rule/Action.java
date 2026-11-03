package pl.variant.itemblocker.rule;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

/**
 * Something done with an item that a rule can block: by a player, or by blocks and the world, such
 * as hoppers, dispensers, crafters and loot.
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
    SMITHING,
    ATTACK,
    BREAK,
    CRAFTER,
    DISPENSE,
    BREW,
    TRADE,
    LOOT,
    ENTITY_DROP;

    /**
     * Keyword for every way of getting hold of an item, so that players may keep what they have
     * but not get more: crafting, crafters, smithing, brewing, trading, loot, mob drops and pickup.
     */
    public static final String OBTAIN_KEYWORD = "obtain";

    public static final Set<Action> OBTAIN = Collections.unmodifiableSet(
            EnumSet.of(CRAFTING, CRAFTER, SMITHING, BREW, TRADE, LOOT, ENTITY_DROP, PICKUP));

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
