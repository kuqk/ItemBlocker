package pl.variant.listeners;

import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import pl.variant.itemBlocker;
import pl.variant.model.BlockAction;
import pl.variant.utils.EquipmentUtils;

public class UseListener implements Listener {

    private final itemBlocker plugin;

    public UseListener(itemBlocker plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onUse(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }

        if (event.getItem() == null || event.getItem().getType() == Material.AIR) {
            return;
        }

        boolean blocked = plugin.getBlockService().blockIfNeeded(
                event.getPlayer(),
                event.getItem().getType(),
                BlockAction.USE,
                event
        );
        if (blocked) {
            return;
        }

        EquipmentSlot armorSlot = EquipmentUtils.getWearableSlot(event.getItem());
        if (armorSlot == null || !EquipmentUtils.isArmorSlotEmpty(event.getPlayer().getInventory(), armorSlot)) {
            return;
        }

        plugin.getBlockService().blockIfNeeded(
                event.getPlayer(),
                event.getItem().getType(),
                BlockAction.ARMOR,
                event
        );
    }
}
