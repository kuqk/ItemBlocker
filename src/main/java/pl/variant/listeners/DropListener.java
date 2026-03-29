package pl.variant.listeners;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerDropItemEvent;
import pl.variant.itemBlocker;
import pl.variant.model.BlockAction;

public class DropListener implements Listener {

    private final itemBlocker plugin;

    public DropListener(itemBlocker plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onDrop(PlayerDropItemEvent event) {
        plugin.getBlockService().blockIfNeeded(
                event.getPlayer(),
                event.getItemDrop().getItemStack().getType(),
                BlockAction.DROP,
                event
        );
    }
}
