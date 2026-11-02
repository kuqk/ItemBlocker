package pl.variant.itemblocker.rule;

import org.bukkit.Keyed;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.potion.PotionEffectType;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.stream.StreamSupport;

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

    /**
     * Returns the keys of every enchantment, sorted.
     */
    public static List<String> enchantments() {
        // Registry#stream only exists from 1.20.1, so the registry is iterated instead.
        return StreamSupport.stream(Registry.ENCHANTMENT.spliterator(), false).map(RegistryKeys::of).sorted().toList();
    }

    /**
     * Returns the keys of every potion effect type, sorted.
     */
    // Registry.EFFECT only exists from 1.20.3. PotionEffectType.values is deprecated in favor of it,
    // but it is the listing that works on every supported version.
    @SuppressWarnings("deprecation")
    public static List<String> effects() {
        return Arrays.stream(PotionEffectType.values()).map(RegistryKeys::of).sorted().toList();
    }
}
