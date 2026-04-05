package pl.variant.utils;

import org.bukkit.Material;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

public final class EquipmentUtils {

    private EquipmentUtils() {
    }

    public static boolean isWearable(ItemStack item) {
        return item != null && getWearableSlot(item.getType()) != null;
    }

    public static EquipmentSlot getWearableSlot(ItemStack item) {
        return item == null ? null : getWearableSlot(item.getType());
    }

    public static EquipmentSlot getWearableSlot(Material material) {
        if (material == null || material.isAir() || !material.isItem()) {
            return null;
        }

        String name = material.name();
        if (isHeadWearable(name)) {
            return EquipmentSlot.HEAD;
        }
        if (isChestWearable(name)) {
            return EquipmentSlot.CHEST;
        }
        if (isLegWearable(name)) {
            return EquipmentSlot.LEGS;
        }
        if (isFootWearable(name)) {
            return EquipmentSlot.FEET;
        }

        return null;
    }

    public static boolean isArmorSlot(EquipmentSlot slot) {
        return slot == EquipmentSlot.HEAD
                || slot == EquipmentSlot.CHEST
                || slot == EquipmentSlot.LEGS
                || slot == EquipmentSlot.FEET;
    }

    public static EquipmentSlot getArmorSlotByInventoryIndex(int slot) {
        return switch (slot) {
            case 39 -> EquipmentSlot.HEAD;
            case 38 -> EquipmentSlot.CHEST;
            case 37 -> EquipmentSlot.LEGS;
            case 36 -> EquipmentSlot.FEET;
            default -> null;
        };
    }

    public static ItemStack getEquippedItem(PlayerInventory inventory, EquipmentSlot slot) {
        if (inventory == null || slot == null) {
            return null;
        }

        return switch (slot) {
            case HEAD -> inventory.getHelmet();
            case CHEST -> inventory.getChestplate();
            case LEGS -> inventory.getLeggings();
            case FEET -> inventory.getBoots();
            default -> null;
        };
    }

    public static void setEquippedItem(PlayerInventory inventory, EquipmentSlot slot, ItemStack item) {
        if (inventory == null || slot == null) {
            return;
        }

        switch (slot) {
            case HEAD -> inventory.setHelmet(item);
            case CHEST -> inventory.setChestplate(item);
            case LEGS -> inventory.setLeggings(item);
            case FEET -> inventory.setBoots(item);
            default -> {
            }
        }
    }

    public static boolean isArmorSlotEmpty(PlayerInventory inventory, EquipmentSlot slot) {
        ItemStack item = getEquippedItem(inventory, slot);
        return item == null || item.getType().isAir();
    }

    private static boolean isHeadWearable(String materialName) {
        return materialName.endsWith("_HELMET")
                || materialName.equals("TURTLE_HELMET")
                || materialName.equals("CARVED_PUMPKIN")
                || materialName.equals("CREEPER_HEAD")
                || materialName.equals("DRAGON_HEAD")
                || materialName.equals("PIGLIN_HEAD")
                || materialName.equals("PLAYER_HEAD")
                || materialName.equals("SKELETON_SKULL")
                || materialName.equals("WITHER_SKELETON_SKULL")
                || materialName.equals("ZOMBIE_HEAD");
    }

    private static boolean isChestWearable(String materialName) {
        return materialName.endsWith("_CHESTPLATE") || materialName.equals("ELYTRA");
    }

    private static boolean isLegWearable(String materialName) {
        return materialName.endsWith("_LEGGINGS");
    }

    private static boolean isFootWearable(String materialName) {
        return materialName.endsWith("_BOOTS");
    }
}
