package pl.variant.listeners;

import com.destroystokyo.paper.event.player.PlayerArmorChangeEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.inventory.EquipmentSlot;

public class PaperArmorChangeListener implements Listener {

    private final ArmorListener armorListener;

    public PaperArmorChangeListener(ArmorListener armorListener) {
        this.armorListener = armorListener;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPaperArmorChange(PlayerArmorChangeEvent event) {
        armorListener.handleArmorChange(
                event.getPlayer(),
                toEquipmentSlot(event.getSlotType()),
                event.getOldItem(),
                event.getNewItem()
        );
    }

    private EquipmentSlot toEquipmentSlot(PlayerArmorChangeEvent.SlotType slotType) {
        if (slotType == null) {
            return null;
        }

        return switch (slotType) {
            case HEAD -> EquipmentSlot.HEAD;
            case CHEST -> EquipmentSlot.CHEST;
            case LEGS -> EquipmentSlot.LEGS;
            case FEET -> EquipmentSlot.FEET;
        };
    }
}
