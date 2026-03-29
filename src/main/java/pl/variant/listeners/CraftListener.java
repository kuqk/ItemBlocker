package pl.variant.listeners;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.CraftItemEvent;
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

        if (event.getRecipe() == null) {
            return;
        }

        ItemStack result = event.getRecipe().getResult();
        if (result == null) {
            return;
        }

        plugin.getBlockService().blockIfNeeded(player, result.getType(), BlockAction.CRAFTING, event);
    }
}
