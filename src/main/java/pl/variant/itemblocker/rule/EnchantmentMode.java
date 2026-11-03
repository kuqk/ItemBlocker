package pl.variant.itemblocker.rule;

import java.util.Locale;
import java.util.Optional;

/**
 * What happens to an item that carries an enchantment above the allowed level.
 */
public enum EnchantmentMode {

    /** The item cannot be used for anything. */
    BLOCK,

    /** The enchantment is lowered to the highest allowed level, so Sharpness V becomes Sharpness IV. */
    DOWNGRADE,

    /** The enchantment is taken off the item. */
    REMOVE,

    /** The item is taken away and kept with the confiscated items. */
    CONFISCATE;

    private final String key = name().toLowerCase(Locale.ROOT);

    public String key() {
        return key;
    }

    public static Optional<EnchantmentMode> fromKey(String key) {
        if (key == null) {
            return Optional.empty();
        }
        String normalized = key.trim().toLowerCase(Locale.ROOT);
        for (EnchantmentMode mode : values()) {
            if (mode.key.equals(normalized)) {
                return Optional.of(mode);
            }
        }
        return Optional.empty();
    }

    /**
     * The next mode, for cycling through them in the menu.
     */
    public EnchantmentMode next() {
        return values()[(ordinal() + 1) % values().length];
    }
}
