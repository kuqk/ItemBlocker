package pl.variant.listeners;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import pl.variant.itemBlocker;
import pl.variant.utils.MessageCooldown;

public class PickupListener implements Listener {
    
    private final itemBlocker plugin;
    private final MessageCooldown messageCooldown;
    
    public PickupListener(itemBlocker plugin) {
        this.plugin = plugin;
        this.messageCooldown = new MessageCooldown(1000);
    }
    
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPickup(EntityPickupItemEvent event) {
        if (!plugin.getConfigManager().isBlockPickup()) return;
        if (!(event.getEntity() instanceof Player)) return;
        
        Player player = (Player) event.getEntity();
        
        if (plugin.getConfigManager().isBypassEnabled() && 
                player.hasPermission("itemblocker.bypass")) {
            return;
        }
        
        if (plugin.getBlockedItemsManager().isBlocked(event.getItem().getItemStack().getType())) {
            event.setCancelled(true);
            
            player.updateInventory();
            
            if (messageCooldown.canSend(player.getUniqueId())) {
                plugin.getMessageManager().sendMessage(player, "cannot-pickup", 
                        "{item}", event.getItem().getItemStack().getType().name());
            }
        }
    }
}

