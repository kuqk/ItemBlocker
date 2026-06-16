package pl.variant.listeners;

import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import pl.variant.itemBlocker;
import pl.variant.model.BlockAction;
import pl.variant.utils.EquipmentUtils;
import pl.variant.utils.PlacementUtils;

public class UseListener implements Listener {

    private final itemBlocker plugin;

    public UseListener(itemBlocker plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onUse(PlayerInteractEvent event) {
        Action action = event.getAction();
        if (action != Action.RIGHT_CLICK_AIR && action != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        if (event.getItem() == null || event.getItem().getType() == Material.AIR) {
            return;
        }

        EquipmentSlot armorSlot = EquipmentUtils.getWearableSlot(event.getItem());
        boolean isEquipAttempt = armorSlot != null && (action == Action.RIGHT_CLICK_AIR || !PlacementUtils.isPlaceActionItem(event.getItem()));
        
        if (isEquipAttempt) {
            boolean blocked = plugin.getBlockService().blockIfNeeded(
                    event.getPlayer(),
                    event.getItem(),
                    BlockAction.ARMOR,
                    event
            );
            if (blocked) {
                event.getPlayer().updateInventory();
            }
            return;
        }

        if (PlacementUtils.isPlaceActionItem(event.getItem()) || armorSlot != null) {
            return;
        }

        if (PlacementUtils.isPlaceActionItem(event.getItem()) || armorSlot != null) {
            return;
        }

        plugin.getBlockService().blockIfNeeded(
                event.getPlayer(),
                event.getItem(),
                BlockAction.USE,
                event
        );
    }
}
