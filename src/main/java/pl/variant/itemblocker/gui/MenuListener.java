package pl.variant.itemblocker.gui;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

/**
 * Routes clicks in an open menu to the menu and keeps every item in place. Runs early and cancels,
 * so restriction listeners never see menu clicks.
 */
public final class MenuListener implements Listener {

    @EventHandler(priority = EventPriority.LOW)
    public void onClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof Menu menu) || !(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        event.setCancelled(true);
        Inventory clicked = event.getClickedInventory();
        if (clicked == event.getInventory()) {
            menu.click(player, event.getSlot(), event.getClick());
        } else if (clicked == player.getInventory() && !isEmpty(event.getCurrentItem())) {
            menu.clickOwnItem(player, event.getCurrentItem());
        }
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onDrag(InventoryDragEvent event) {
        if (event.getInventory().getHolder() instanceof Menu) {
            event.setCancelled(true);
        }
    }

    private static boolean isEmpty(ItemStack item) {
        return item == null || item.getType().isAir();
    }
}
