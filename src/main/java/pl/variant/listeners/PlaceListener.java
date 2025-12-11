package pl.variant.listeners;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import pl.variant.itemBlocker;

public class PlaceListener implements Listener {
    
    private final itemBlocker plugin;
    
    public PlaceListener(itemBlocker plugin) {
        this.plugin = plugin;
    }
    
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlace(BlockPlaceEvent event) {
        if (!plugin.getConfigManager().isBlockPlace()) {
            return;
        }
        
        Player player = event.getPlayer();
        
        // Sprawdzanie bypass
        if (plugin.getConfigManager().isBypassEnabled() && 
                player.hasPermission("itemblocker.bypass")) {
            return;
        }
        
        if (plugin.getBlockedItemsManager().isBlocked(event.getBlock().getType())) {
            event.setCancelled(true);
            plugin.getMessageManager().sendMessage(player, "cannot-place", 
                    "{item}", event.getBlock().getType().name());
        }
    }
}

