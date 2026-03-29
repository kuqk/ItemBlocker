package pl.variant.listeners;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import pl.variant.itemBlocker;
import pl.variant.model.BlockAction;

public class PlaceListener implements Listener {

    private final itemBlocker plugin;

    public PlaceListener(itemBlocker plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent event) {
        plugin.getBlockService().blockIfNeeded(
                event.getPlayer(),
                event.getItemInHand().getType(),
                BlockAction.PLACE,
                event
        );
    }
}
