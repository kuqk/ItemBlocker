package pl.variant.listeners;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import pl.variant.itemBlocker;
import pl.variant.model.BlockAction;

public class CraftListener implements Listener {

    private final itemBlocker plugin;

    public CraftListener(itemBlocker plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onCraft(CraftItemEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        ItemStack result = event.getCurrentItem();
        if (isEmpty(result)) {
            Inventory inventory = event.getInventory();
            result = inventory == null ? null : inventory.getItem(event.getRawSlot());
        }
        if (isEmpty(result)) {
            result = event.getInventory().getResult();
        }
        if (isEmpty(result) && event.getRecipe() != null) {
            result = event.getRecipe().getResult();
        }
        if (isEmpty(result)) {
            return;
        }

        plugin.getBlockService().blockIfNeeded(player, result, BlockAction.CRAFTING, event);
    }

    private boolean isEmpty(ItemStack item) {
        return item == null || item.getType().isAir();
    }
}
