package pl.variant.itemblocker.listener;

import org.bukkit.Material;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.inventory.InventoryType.SlotType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import pl.variant.itemblocker.rule.Action;
import pl.variant.itemblocker.testing.InventoryViews;
import pl.variant.itemblocker.testing.PluginTest;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ContainerListenerTest extends PluginTest {

    // Raw slots with a 27 slot chest open: 0-26 chest, 27-53 storage, 54-62 hotbar.
    private static final int CHEST_SLOT = 0;
    private static final int FIRST_STORAGE_SLOT = 27;

    private Inventory chest;

    @BeforeEach
    void openChest() {
        chest = server.createInventory(null, 27);
    }

    @Test
    void deniesShiftClickingBlockedItemIntoContainer() {
        block(Material.TNT, Action.INVENTORY);
        player.getInventory().setItem(9, new ItemStack(Material.TNT));

        assertTrue(click(SlotType.CONTAINER, FIRST_STORAGE_SLOT, ClickType.SHIFT_LEFT, InventoryAction.MOVE_TO_OTHER_INVENTORY).isCancelled());
    }

    @Test
    void deniesTakingBlockedItemOutOfContainer() {
        block(Material.TNT, Action.INVENTORY);
        chest.setItem(CHEST_SLOT, new ItemStack(Material.TNT));

        assertTrue(click(SlotType.CONTAINER, CHEST_SLOT, ClickType.LEFT, InventoryAction.PICKUP_ALL).isCancelled());
    }

    @Test
    void allowsMovingWithinOwnInventory() {
        block(Material.TNT, Action.INVENTORY);
        player.getInventory().setItem(9, new ItemStack(Material.TNT));

        assertFalse(click(SlotType.CONTAINER, FIRST_STORAGE_SLOT, ClickType.LEFT, InventoryAction.PICKUP_ALL).isCancelled());
    }

    @Test
    void deniesDraggingIntoContainer() {
        block(Material.TNT, Action.INVENTORY);
        ItemStack tnt = new ItemStack(Material.TNT);

        InventoryDragEvent event = new InventoryDragEvent(view(), null, tnt, false, Map.of(CHEST_SLOT, tnt));
        server.getPluginManager().callEvent(event);

        assertTrue(event.isCancelled());
    }

    @Test
    void hoppersCannotMoveBlockedItems() {
        block(Material.TNT, Action.HOPPER);
        Inventory hopper = server.createInventory(null, InventoryType.HOPPER);

        InventoryMoveItemEvent blocked = new InventoryMoveItemEvent(chest, new ItemStack(Material.TNT), hopper, true);
        InventoryMoveItemEvent allowed = new InventoryMoveItemEvent(chest, new ItemStack(Material.STONE), hopper, true);
        server.getPluginManager().callEvent(blocked);
        server.getPluginManager().callEvent(allowed);

        assertTrue(blocked.isCancelled());
        assertFalse(allowed.isCancelled());
    }

    private InventoryClickEvent click(SlotType type, int rawSlot, ClickType click, InventoryAction action) {
        InventoryClickEvent event = new InventoryClickEvent(view(), type, rawSlot, click, action);
        server.getPluginManager().callEvent(event);
        return event;
    }

    private InventoryView view() {
        return InventoryViews.container(player, chest);
    }
}
