package pl.variant.listeners;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import pl.variant.itemBlocker;

public class UseListener implements Listener {
    
    private final itemBlocker plugin;
    
    public UseListener(itemBlocker plugin) {
        this.plugin = plugin;
    }
    
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onUse(PlayerInteractEvent event) {
        if (!plugin.getConfigManager().isBlockUse()) {
            return;
        }
        
        if (event.getItem() == null || event.getItem().getType() == Material.AIR) {
            return;
        }
        
        Player player = event.getPlayer();
        
        // Sprawdzanie bypass
        if (plugin.getConfigManager().isBypassEnabled() && 
                player.hasPermission("itemblocker.bypass")) {
            return;
        }
        
        if (plugin.getBlockedItemsManager().isBlocked(event.getItem().getType())) {
            event.setCancelled(true);
            plugin.getMessageManager().sendMessage(player, "cannot-use", 
                    "{item}", event.getItem().getType().name());
        }
    }
}

