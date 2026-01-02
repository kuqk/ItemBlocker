package pl.variant.listeners;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.ItemStack;
import pl.variant.itemBlocker;

public class InventoryListener implements Listener {
    
    private final itemBlocker plugin;
    
    public InventoryListener(itemBlocker plugin) {
        this.plugin = plugin;
    }
    
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!plugin.getConfigManager().isBlockInventory()) {
            return;
        }
        
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }
        
        Player player = (Player) event.getWhoClicked();
        
        if (plugin.getConfigManager().isBypassEnabled() && 
                player.hasPermission("itemblocker.bypass")) {
            return;
        }

        boolean isTopInventory = event.getClickedInventory() != null && 
                               event.getClickedInventory().getType() != InventoryType.PLAYER;
        
        ItemStack clickedItem = event.getCurrentItem();
        ItemStack cursorItem = event.getCursor();
        
        Material blockedMaterial = null;

        if (isTopInventory && clickedItem != null && plugin.getBlockedItemsManager().isBlocked(clickedItem.getType())) {
            blockedMaterial = clickedItem.getType();
        }
        else if (isTopInventory && cursorItem != null && plugin.getBlockedItemsManager().isBlocked(cursorItem.getType())) {
            blockedMaterial = cursorItem.getType();
        }
        else if (event.isShiftClick() && !isTopInventory && event.getInventory().getType() != InventoryType.PLAYER) {
            if (clickedItem != null && plugin.getBlockedItemsManager().isBlocked(clickedItem.getType())) {
                blockedMaterial = clickedItem.getType();
            }
        }

        if (blockedMaterial != null) {
            event.setCancelled(true);
            plugin.getMessageManager().sendMessage(player, "cannot-inventory", 
                    "{item}", blockedMaterial.name());
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!plugin.getConfigManager().isBlockInventory()) {
            return;
        }
        
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }
        
        Player player = (Player) event.getWhoClicked();
        
        if (plugin.getConfigManager().isBypassEnabled() && 
                player.hasPermission("itemblocker.bypass")) {
            return;
        }

        ItemStack item = event.getOldCursor();
        if (item == null || !plugin.getBlockedItemsManager().isBlocked(item.getType())) {
            return;
        }

        boolean toContainer = false;
        for (int slot : event.getRawSlots()) {
            if (slot < event.getInventory().getSize()) {
                toContainer = true;
                break;
            }
        }

        if (toContainer) {
            event.setCancelled(true);
            plugin.getMessageManager().sendMessage(player, "cannot-inventory", 
                    "{item}", item.getType().name());
        }
    }
}
