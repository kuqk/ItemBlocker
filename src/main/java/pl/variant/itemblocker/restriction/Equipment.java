package pl.variant.itemblocker.restriction;

import org.bukkit.inventory.EquipmentSlot;

import java.util.List;
import java.util.Optional;

/**
 * Armor slot helpers.
 */
public final class Equipment {

    public static final List<EquipmentSlot> ARMOR_SLOTS = List.of(
            EquipmentSlot.HEAD,
            EquipmentSlot.CHEST,
            EquipmentSlot.LEGS,
            EquipmentSlot.FEET
    );

    private Equipment() {
    }

    /**
     * Returns the armor slot at a raw slot of the player's own inventory view, where raw slots 5 to 8
     * hold the head, chest, legs and feet.
     */
    public static Optional<EquipmentSlot> armorSlotAtRawSlot(int rawSlot) {
        return switch (rawSlot) {
            case 5 -> Optional.of(EquipmentSlot.HEAD);
            case 6 -> Optional.of(EquipmentSlot.CHEST);
            case 7 -> Optional.of(EquipmentSlot.LEGS);
            case 8 -> Optional.of(EquipmentSlot.FEET);
            default -> Optional.empty();
        };
    }

    /**
     * Returns the armor slot shown at a player inventory index (36 = feet up to 39 = head).
     */
    public static Optional<EquipmentSlot> armorSlotAtIndex(int index) {
        return switch (index) {
            case 36 -> Optional.of(EquipmentSlot.FEET);
            case 37 -> Optional.of(EquipmentSlot.LEGS);
            case 38 -> Optional.of(EquipmentSlot.CHEST);
            case 39 -> Optional.of(EquipmentSlot.HEAD);
            default -> Optional.empty();
        };
    }
}
