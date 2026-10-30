package pl.variant.itemblocker.restriction;

import org.bukkit.Material;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import java.util.Optional;

/**
 * Tells which armor slot an item is worn in.
 */
@FunctionalInterface
public interface WearableSlots {

    /**
     * Returns the armor slot the item is worn in, or nothing if a player cannot wear it.
     */
    Optional<EquipmentSlot> armorSlotOf(ItemStack item);

    /**
     * Uses the default equipment slot of each item type as defined by the server, so new wearable
     * items are recognized without changes here.
     */
    static WearableSlots fromItemTypes() {
        return item -> {
            if (item == null) {
                return Optional.empty();
            }

            Material type = item.getType();
            if (type.isAir() || !type.isItem()) {
                return Optional.empty();
            }

            EquipmentSlot slot = type.getEquipmentSlot();
            return Equipment.ARMOR_SLOTS.contains(slot) ? Optional.of(slot) : Optional.empty();
        };
    }
}
