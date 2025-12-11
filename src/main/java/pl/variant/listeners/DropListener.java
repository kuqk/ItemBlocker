package pl.variant.listeners;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerDropItemEvent;
import pl.variant.itemBlocker;

public class DropListener implements Listener {
    
    private final itemBlocker plugin;
    
    public DropListener(itemBlocker plugin) {
        this.plugin = plugin;
    }
    
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onDrop(PlayerDropItemEvent event) {
        if (!plugin.getConfigManager().isBlockDrop()) {
            return;
        }
        
        Player player = event.getPlayer();
        
        // Sprawdzanie bypass
        if (plugin.getConfigManager().isBypassEnabled() && 
                player.hasPermission("itemblocker.bypass")) {
            return;
        }
        
        if (plugin.getBlockedItemsManager().isBlocked(event.getItemDrop().getItemStack().getType())) {
            event.setCancelled(true);
            plugin.getMessageManager().sendMessage(player, "cannot-drop", 
                    "{item}", event.getItemDrop().getItemStack().getType().name());
        }
    }
}

