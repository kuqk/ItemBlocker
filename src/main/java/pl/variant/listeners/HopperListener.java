package pl.variant.listeners;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import pl.variant.itemBlocker;
import pl.variant.model.BlockAction;

public class HopperListener implements Listener {

    private final itemBlocker plugin;

    public HopperListener(itemBlocker plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onItemMove(InventoryMoveItemEvent event) {
        if (plugin.getBlockService().inspect(
                event.getItem(),
                BlockAction.HOPPER,
                event.getDestination().getLocation() == null ? null : event.getDestination().getLocation().getWorld().getName()
        ).blocked()) {
            event.setCancelled(true);
        }
    }
}
