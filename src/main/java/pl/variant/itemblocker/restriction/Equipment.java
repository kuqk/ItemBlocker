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
