package pl.variant.utils;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

public final class PlacementUtils {

    private PlacementUtils() {
    }

    public static boolean isPlaceActionItem(ItemStack item) {
        return item != null && isPlaceActionMaterial(item.getType());
    }

    public static boolean isPlaceActionMaterial(Material material) {
        if (material == null || material.isAir() || !material.isItem()) {
            return false;
        }

        return material.isBlock() || isSpecialPlacementMaterial(material);
    }

    public static boolean isSpecialPlacementItem(ItemStack item) {
        return item != null && isSpecialPlacementMaterial(item.getType());
    }

    public static boolean isSpecialPlacementMaterial(Material material) {
        if (material == null || material.isAir() || !material.isItem() || material.isBlock()) {
            return false;
        }

        String name = material.name();
        return name.endsWith("_BOAT")
                || name.endsWith("_CHEST_BOAT")
                || name.endsWith("_RAFT")
                || name.endsWith("_CHEST_RAFT")
                || name.endsWith("MINECART")
                || material == Material.ARMOR_STAND
                || material == Material.ITEM_FRAME
                || material == Material.GLOW_ITEM_FRAME
                || material == Material.PAINTING
                || material == Material.END_CRYSTAL;
    }
}
