package pl.variant.itemblocker.rule;

import org.bukkit.Material;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Converts between item types and the lowercase keys used in rules and commands, such as
 * {@code diamond_sword}. Only real, non-legacy items can have rules.
 */
public final class ItemTypes {

    private static volatile List<String> keys;

    private ItemTypes() {
    }

    /**
     * Parses an item key such as {@code tnt}, {@code TNT} or {@code minecraft:tnt}.
     */
    public static Optional<Material> parse(String key) {
        if (key == null || key.isBlank()) {
            return Optional.empty();
        }

        Material material = Material.matchMaterial(key.trim());
        return isBlockable(material) ? Optional.of(material) : Optional.empty();
    }

    public static boolean isBlockable(Material material) {
        return material != null && !isLegacy(material) && !material.isAir() && material.isItem();
    }

    // Deprecated only to discourage using legacy materials; it is still the way to recognize them.
    @SuppressWarnings("deprecation")
    private static boolean isLegacy(Material material) {
        return material.isLegacy();
    }

    public static String key(Material material) {
        return material.name().toLowerCase(Locale.ROOT);
    }

    /**
     * Returns the keys of every item that can have rules, sorted alphabetically.
     */
    public static List<String> keys() {
        List<String> result = keys;
        if (result == null) {
            result = Arrays.stream(Material.values())
                    .filter(ItemTypes::isBlockable)
                    .map(ItemTypes::key)
                    .sorted()
                    .toList();
            keys = result;
        }
        return result;
    }
}
