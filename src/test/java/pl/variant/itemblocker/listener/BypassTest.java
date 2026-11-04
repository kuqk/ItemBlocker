package pl.variant.itemblocker.listener;

import org.bukkit.Material;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCreativeEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.inventory.InventoryType.SlotType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.Test;
import pl.variant.itemblocker.rule.Action;
import pl.variant.itemblocker.testing.InventoryViews;
import pl.variant.itemblocker.testing.PluginTest;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BypassTest extends PluginTest {

    @Test
    void blockedItemsCannotBeTakenFromTheCreativeInventory() {
        block(Material.BEDROCK, Action.PICKUP);

        assertTrue(creative(new ItemStack(Material.BEDROCK)).isCancelled());
        assertFalse(creative(new ItemStack(Material.STONE)).isCancelled());
    }

    @Test
    void takingAStonecutterResultIsCrafting() {
        block(Material.STONE_BRICK_SLAB, Action.CRAFTING);
        Inventory stonecutter = server.createInventory(null, InventoryType.STONECUTTER);
        stonecutter.setItem(1, new ItemStack(Material.STONE_BRICK_SLAB, 2));

        InventoryClickEvent blocked = new InventoryClickEvent(InventoryViews.container(player, stonecutter),
                SlotType.RESULT, 1, ClickType.SHIFT_LEFT, InventoryAction.MOVE_TO_OTHER_INVENTORY);
        server.getPluginManager().callEvent(blocked);
        stonecutter.setItem(1, new ItemStack(Material.STONE_BRICK_STAIRS));
        InventoryClickEvent allowed = new InventoryClickEvent(InventoryViews.container(player, stonecutter),
                SlotType.RESULT, 1, ClickType.LEFT, InventoryAction.PICKUP_ALL);
        server.getPluginManager().callEvent(allowed);

        assertTrue(blocked.isCancelled());
        assertFalse(allowed.isCancelled());
    }

    private InventoryCreativeEvent creative(ItemStack item) {
        InventoryCreativeEvent event = new InventoryCreativeEvent(InventoryViews.playerInventory(player), SlotType.QUICKBAR, 36, item);
        server.getPluginManager().callEvent(event);
        return event;
    }
}
