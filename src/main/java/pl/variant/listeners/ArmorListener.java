package pl.variant.listeners;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.ItemStack;
import pl.variant.itemBlocker;

public class ArmorListener implements Listener {
    
    private final itemBlocker plugin;
    
    public ArmorListener(itemBlocker plugin) {
        this.plugin = plugin;
    }
    
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onArmorEquip(InventoryClickEvent event) {
        if (!plugin.getConfigManager().isBlockArmor()) {
            return;
        }
        
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }
        
        if (event.getSlotType() != InventoryType.SlotType.ARMOR) {
            return;
        }
        
        Player player = (Player) event.getWhoClicked();
        
        // Sprawdzanie bypass
        if (plugin.getConfigManager().isBypassEnabled() && 
                player.hasPermission("itemblocker.bypass")) {
            return;
        }
        
        ItemStack item = event.getCursor();
        if (item != null && plugin.getBlockedItemsManager().isBlocked(item.getType())) {
            event.setCancelled(true);
            plugin.getMessageManager().sendMessage(player, "cannot-equip", 
                    "{item}", item.getType().name());
        }
    }
}

