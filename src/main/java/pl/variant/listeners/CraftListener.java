package pl.variant.listeners;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.CraftItemEvent;
import pl.variant.itemBlocker;

public class CraftListener implements Listener {
    
    private final itemBlocker plugin;
    
    public CraftListener(itemBlocker plugin) {
        this.plugin = plugin;
    }
    
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onCraft(CraftItemEvent event) {
        if (!plugin.getConfigManager().isBlockCrafting()) return;
        if (!(event.getWhoClicked() instanceof Player)) return;
        
        Player player = (Player) event.getWhoClicked();
        
        if (plugin.getConfigManager().isBypassEnabled() && 
                player.hasPermission("itemblocker.bypass")) {
            return;
        }
        
        if (plugin.getBlockedItemsManager().isBlocked(event.getRecipe().getResult().getType())) {
            event.setCancelled(true);
            plugin.getMessageManager().sendMessage(player, "cannot-craft", 
                    "{item}", event.getRecipe().getResult().getType().name());
        }
    }
}

