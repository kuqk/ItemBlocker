package pl.variant.itemblocker.listener;

import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.event.block.BlockDispenseArmorEvent;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryType.SlotType;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.Test;
import pl.variant.itemblocker.rule.Action;
import pl.variant.itemblocker.testing.InventoryViews;
import pl.variant.itemblocker.testing.PluginTest;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ArmorListenerTest extends PluginTest {

    // Raw slots of the player's own inventory view.
    private static final int HELMET_SLOT = 5;
    private static final int FIRST_STORAGE_SLOT = 9;

    @Test
    void deniesPlacingCursorItemInArmorSlot() {
        block(Material.DIAMOND_HELMET, Action.ARMOR);
        player.setItemOnCursor(helmet());

        assertTrue(click(SlotType.ARMOR, HELMET_SLOT, ClickType.LEFT, InventoryAction.PLACE_ALL).isCancelled());
    }

    @Test
    void deniesHotbarAndOffhandSwapsIntoArmorSlot() {
        block(Material.DIAMOND_HELMET, Action.ARMOR);
        player.getInventory().setItem(3, helmet());
        player.getInventory().setItemInOffHand(helmet());

        InventoryClickEvent hotbar = new InventoryClickEvent(view(), SlotType.ARMOR, HELMET_SLOT, ClickType.NUMBER_KEY, InventoryAction.HOTBAR_SWAP, 3);
        server.getPluginManager().callEvent(hotbar);

        assertTrue(hotbar.isCancelled());
        assertTrue(click(SlotType.ARMOR, HELMET_SLOT, ClickType.SWAP_OFFHAND, InventoryAction.HOTBAR_SWAP).isCancelled());
    }

    @Test
    void deniesShiftClickingIntoFreeArmorSlot() {
        block(Material.DIAMOND_HELMET, Action.ARMOR);
        player.getInventory().setItem(FIRST_STORAGE_SLOT, helmet());

        assertTrue(click(SlotType.CONTAINER, FIRST_STORAGE_SLOT, ClickType.SHIFT_LEFT, InventoryAction.MOVE_TO_OTHER_INVENTORY).isCancelled());
    }

    @Test
    void allowsShiftClickWhenArmorSlotIsTaken() {
        block(Material.DIAMOND_HELMET, Action.ARMOR);
        player.getInventory().setHelmet(new ItemStack(Material.IRON_HELMET));
        player.getInventory().setItem(FIRST_STORAGE_SLOT, helmet());

        assertFalse(click(SlotType.CONTAINER, FIRST_STORAGE_SLOT, ClickType.SHIFT_LEFT, InventoryAction.MOVE_TO_OTHER_INVENTORY).isCancelled());
    }

    @Test
    void deniesDraggingOntoArmorSlotOnly() {
        block(Material.DIAMOND_HELMET, Action.ARMOR);

        InventoryDragEvent ontoArmor = new InventoryDragEvent(view(), null, helmet(), false, Map.of(HELMET_SLOT, helmet()));
        InventoryDragEvent ontoStorage = new InventoryDragEvent(view(), null, helmet(), false, Map.of(FIRST_STORAGE_SLOT, helmet()));
        server.getPluginManager().callEvent(ontoArmor);
        server.getPluginManager().callEvent(ontoStorage);

        assertTrue(ontoArmor.isCancelled());
        assertFalse(ontoStorage.isCancelled());
    }

    @Test
    void deniesDispenserPuttingArmorOnPlayer() {
        block(Material.DIAMOND_HELMET, Action.ARMOR);

        BlockDispenseArmorEvent event = new BlockDispenseArmorEvent(player.getLocation().getBlock(), helmet(), player);
        server.getPluginManager().callEvent(event);

        assertTrue(event.isCancelled());
    }

    @Test
    void takesOffBlockedArmorOnJoinWithoutCopyingIt() {
        player.getInventory().setHelmet(helmet());
        block(Material.DIAMOND_HELMET, Action.ARMOR);

        server.getPluginManager().callEvent(new PlayerJoinEvent(player, Component.text("joined")));

        assertNull(player.getInventory().getHelmet());
        assertEquals(1, countInInventory(Material.DIAMOND_HELMET));
        assertEquals(List.of("ItemBlocker » You cannot wear Diamond Helmet."), messages());
    }

    private static ItemStack helmet() {
        return new ItemStack(Material.DIAMOND_HELMET);
    }

    private InventoryClickEvent click(SlotType type, int rawSlot, ClickType click, InventoryAction action) {
        InventoryClickEvent event = new InventoryClickEvent(view(), type, rawSlot, click, action);
        server.getPluginManager().callEvent(event);
        return event;
    }

    private InventoryView view() {
        return InventoryViews.playerInventory(player);
    }

    private int countInInventory(Material material) {
        return Arrays.stream(player.getInventory().getStorageContents())
                .filter(item -> item != null && item.getType() == material)
                .mapToInt(ItemStack::getAmount)
                .sum();
    }
}
