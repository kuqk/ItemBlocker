package pl.variant.listeners;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import pl.variant.itemBlocker;
import pl.variant.model.BlockAction;

public class PickupListener implements Listener {

    private final itemBlocker plugin;

    public PickupListener(itemBlocker plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPickup(EntityPickupItemEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }

        boolean blocked = plugin.getBlockService().blockIfNeeded(
                player,
                event.getItem().getItemStack().getType(),
                BlockAction.PICKUP,
                event
        );

        if (blocked) {
            player.updateInventory();
        }
    }
}
