package pl.variant.utils;

import org.bukkit.Material;

import java.util.Arrays;
import java.util.List;

public final class MaterialUtils {

    private static volatile List<String> itemMaterialNames;

    private MaterialUtils() {
    }

    public static Material parseItemMaterial(String input) {
        if (input == null || input.isBlank()) {
            return null;
        }

        Material material = Material.matchMaterial(input.trim());
        return isBlockableItem(material) ? material : null;
    }

    public static boolean isBlockableItem(Material material) {
        return material != null
                && !material.name().startsWith("LEGACY_")
                && !material.isAir()
                && material.isItem();
    }

    public static List<String> getItemMaterialNames() {
        List<String> names = itemMaterialNames;
        if (names == null) {
            names = Arrays.stream(Material.values())
                    .filter(MaterialUtils::isBlockableItem)
                    .map(Material::name)
                    .sorted()
                    .toList();
            itemMaterialNames = names;
        }
        return names;
    }
}
