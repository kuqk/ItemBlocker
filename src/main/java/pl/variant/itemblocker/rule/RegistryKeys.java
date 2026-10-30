package pl.variant.itemblocker.rule;

import org.bukkit.Keyed;
import org.bukkit.NamespacedKey;

import java.util.Locale;

/**
 * Normalizes enchantment and potion effect keys so that {@code Sharpness}, {@code sharpness} and
 * {@code minecraft:sharpness} all refer to the same rule. Keys outside the {@code minecraft}
 * namespace, such as data pack enchantments, keep their namespace.
 */
public final class RegistryKeys {

    private static final String MINECRAFT_PREFIX = NamespacedKey.MINECRAFT + ":";

    private RegistryKeys() {
    }

    public static String normalize(String key) {
        String normalized = key.trim()
                .toLowerCase(Locale.ROOT)
                .replace(' ', '_')
                .replace('-', '_');
        return normalized.startsWith(MINECRAFT_PREFIX)
                ? normalized.substring(MINECRAFT_PREFIX.length())
                : normalized;
    }

    /**
     * Returns the normalized key of a registry entry such as an enchantment or a potion effect type.
     */
    public static String of(Keyed entry) {
        return normalize(entry.getKey().toString());
    }
}
