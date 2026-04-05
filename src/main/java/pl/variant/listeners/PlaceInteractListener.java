package pl.variant.listeners;

import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import pl.variant.itemBlocker;
import pl.variant.model.BlockAction;
import pl.variant.utils.PlacementUtils;

public class PlaceInteractListener implements Listener {

    private final itemBlocker plugin;

    public PlaceInteractListener(itemBlocker plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onSpecialPlace(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        ItemStack item = event.getItem();
        if (isEmpty(item) || !PlacementUtils.isSpecialPlacementItem(item)) {
            return;
        }

        if (plugin.getBlockService().blockIfNeeded(event.getPlayer(), item, BlockAction.PLACE, event)) {
            event.getPlayer().updateInventory();
        }
    }

    private boolean isEmpty(ItemStack item) {
        return item == null || item.getType() == Material.AIR;
    }
}
