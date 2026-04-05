package pl.variant.listeners;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.ItemStack;
import pl.variant.itemBlocker;
import pl.variant.model.BlockAction;
import pl.variant.services.BlockService;

public class InventoryListener implements Listener {

    private final itemBlocker plugin;

    public InventoryListener(itemBlocker plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        InventoryType topInventoryType = event.getView().getTopInventory().getType();
        boolean hasContainerOpen = topInventoryType != InventoryType.CRAFTING
                && topInventoryType != InventoryType.CREATIVE
                && topInventoryType != InventoryType.PLAYER;
        boolean isTopInventory = event.getClickedInventory() != null
                && event.getClickedInventory().getType() != InventoryType.PLAYER;

        ItemStack candidate = null;

        if (isTopInventory) {
            candidate = pickBlockedItem(
                    player,
                    event.getCurrentItem(),
                    event.getCursor(),
                    resolveHotbarSwapItem(player, event),
                    resolveOffhandSwapItem(player, event)
            );
        } else if (event.isShiftClick() && event.getCurrentItem() != null && hasContainerOpen) {
            candidate = pickBlockedItem(player, event.getCurrentItem());
        } else if (hasContainerOpen && event.getClick() == ClickType.DOUBLE_CLICK) {
            candidate = pickBlockedItem(player, event.getCursor());
        }

        if (candidate == null) {
            return;
        }

        BlockService.ItemBlockDecision decision = plugin.getBlockService().inspect(player, candidate, BlockAction.INVENTORY);
        if (!decision.blocked()) {
            return;
        }

        event.setCancelled(true);
        plugin.getBlockService().sendBlockedDecision(player, decision);
        player.updateInventory();
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        ItemStack item = event.getOldCursor();
        if (item == null) {
            return;
        }

        boolean toContainer = false;
        for (int slot : event.getRawSlots()) {
            if (slot < event.getInventory().getSize()) {
                toContainer = true;
                break;
            }
        }

        if (!toContainer) {
            return;
        }

        BlockService.ItemBlockDecision decision = plugin.getBlockService().inspect(player, item, BlockAction.INVENTORY);
        if (!decision.blocked()) {
            return;
        }

        event.setCancelled(true);
        plugin.getBlockService().sendBlockedDecision(player, decision);
    }

    private ItemStack pickBlockedItem(Player player, ItemStack... candidates) {
        for (ItemStack candidate : candidates) {
            if (candidate == null || candidate.getType() == Material.AIR) {
                continue;
            }

            if (plugin.getBlockService().inspect(player, candidate, BlockAction.INVENTORY).blocked()) {
                return candidate;
            }
        }

        return null;
    }

    private ItemStack resolveHotbarSwapItem(Player player, InventoryClickEvent event) {
        if (event.getClick() != ClickType.NUMBER_KEY) {
            return null;
        }

        int hotbarButton = event.getHotbarButton();
        return hotbarButton < 0 ? null : player.getInventory().getItem(hotbarButton);
    }

    private ItemStack resolveOffhandSwapItem(Player player, InventoryClickEvent event) {
        return event.getClick() == ClickType.SWAP_OFFHAND
                ? player.getInventory().getItemInOffHand()
                : null;
    }
}
