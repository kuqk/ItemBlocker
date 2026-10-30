package pl.variant.itemblocker.listener;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.event.inventory.InventoryPickupItemEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import pl.variant.itemblocker.restriction.Enforcement;
import pl.variant.itemblocker.rule.Action;

import java.util.ArrayList;
import java.util.List;

/**
 * Moving items into and out of containers such as chests, and hoppers moving items on their own.
 */
public final class ContainerListener implements Listener {

    private final Enforcement enforcement;

    public ContainerListener(Enforcement enforcement) {
        this.enforcement = enforcement;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent event) {
        Inventory top = event.getView().getTopInventory();
        if (!(event.getWhoClicked() instanceof Player player) || !isContainer(top)) {
            return;
        }

        for (ItemStack item : movedItems(event, player, top)) {
            if (enforcement.denyItem(player, item, Action.INVENTORY, event)) {
                player.updateInventory();
                return;
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onInventoryDrag(InventoryDragEvent event) {
        Inventory top = event.getView().getTopInventory();
        if (!(event.getWhoClicked() instanceof Player player) || !isContainer(top)) {
            return;
        }

        boolean intoContainer = event.getRawSlots().stream().anyMatch(rawSlot -> rawSlot < top.getSize());
        if (intoContainer) {
            enforcement.denyItem(player, event.getOldCursor(), Action.INVENTORY, event);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onHopperMove(InventoryMoveItemEvent event) {
        Location location = event.getDestination().getLocation();
        if (location == null) {
            location = event.getSource().getLocation();
        }

        String world = location == null || location.getWorld() == null ? null : location.getWorld().getName();
        if (enforcement.restrictions().checkItem(event.getItem(), Action.HOPPER, world).isPresent()) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onHopperPickup(InventoryPickupItemEvent event) {
        String world = event.getItem().getWorld().getName();
        if (enforcement.restrictions().checkItem(event.getItem().getItemStack(), Action.HOPPER, world).isPresent()) {
            event.setCancelled(true);
        }
    }

    /**
     * Returns the items the click would move into or out of the container.
     */
    private static List<ItemStack> movedItems(InventoryClickEvent event, Player player, Inventory top) {
        PlayerInventory inventory = player.getInventory();
        List<ItemStack> items = new ArrayList<>();
        if (event.getClickedInventory() == top) {
            items.add(event.getCurrentItem());
            items.add(event.getCursor());
            if (event.getClick() == ClickType.NUMBER_KEY && event.getHotbarButton() >= 0) {
                items.add(inventory.getItem(event.getHotbarButton()));
            } else if (event.getClick() == ClickType.SWAP_OFFHAND) {
                items.add(inventory.getItemInOffHand());
            }
        } else if (event.isShiftClick()) {
            items.add(event.getCurrentItem());
        } else if (event.getClick() == ClickType.DOUBLE_CLICK) {
            items.add(event.getCursor());
        }
        return items;
    }

    private static boolean isContainer(Inventory top) {
        InventoryType type = top.getType();
        return type != InventoryType.CRAFTING && type != InventoryType.CREATIVE && type != InventoryType.PLAYER;
    }
}
