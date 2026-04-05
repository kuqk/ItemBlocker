package pl.variant.listeners;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.PrepareSmithingEvent;
import org.bukkit.event.inventory.SmithItemEvent;
import org.bukkit.inventory.ItemStack;
import pl.variant.itemBlocker;
import pl.variant.model.BlockAction;
import pl.variant.services.BlockService;

public class SmithingListener implements Listener {

    private final itemBlocker plugin;

    public SmithingListener(itemBlocker plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPrepareSmithing(PrepareSmithingEvent event) {
        if (!(event.getView().getPlayer() instanceof Player player)) {
            return;
        }

        ItemStack result = event.getResult();
        if (isEmpty(result)) {
            return;
        }

        BlockService.ItemBlockDecision decision = plugin.getBlockService().inspect(player, result, BlockAction.SMITHING);
        if (decision.blocked()) {
            event.setResult(null);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onSmithItem(SmithItemEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        ItemStack result = event.getCurrentItem();
        if (isEmpty(result)) {
            result = event.getInventory().getResult();
        }
        if (isEmpty(result)) {
            return;
        }

        BlockService.ItemBlockDecision decision = plugin.getBlockService().inspect(player, result, BlockAction.SMITHING);
        if (decision.blocked()) {
            event.setCancelled(true);
            plugin.getBlockService().sendBlockedDecision(player, decision);
            player.updateInventory();
            return;
        }
    }

    private boolean isEmpty(ItemStack item) {
        return item == null || item.getType() == Material.AIR;
    }
}
