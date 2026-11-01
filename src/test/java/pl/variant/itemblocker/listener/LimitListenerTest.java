package pl.variant.itemblocker.listener;

import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.Item;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType.SlotType;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.Test;
import pl.variant.itemblocker.rule.Action;
import pl.variant.itemblocker.rule.ItemLimit;
import pl.variant.itemblocker.testing.InventoryViews;
import pl.variant.itemblocker.testing.PluginTest;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LimitListenerTest extends PluginTest {

    @Test
    void picksUpOnlyUpToTheInventoryLimit() {
        limit(Material.ENDER_PEARL, new ItemLimit(0, 5));
        player.getInventory().setItem(0, new ItemStack(Material.ENDER_PEARL, 3));
        Item onGround = player.getWorld().dropItem(player.getLocation(), new ItemStack(Material.ENDER_PEARL, 4));

        EntityPickupItemEvent event = pickup(onGround);

        assertTrue(event.isCancelled());
        assertEquals(5, count(Material.ENDER_PEARL));
        assertEquals(2, onGround.getItemStack().getAmount());
        assertEquals(List.of("[ItemBlocker] You can carry at most 5 Ender Pearl."), messages());
    }

    @Test
    void picksUpNormallyBelowTheLimit() {
        limit(Material.ENDER_PEARL, new ItemLimit(0, 16));
        Item onGround = player.getWorld().dropItem(player.getLocation(), new ItemStack(Material.ENDER_PEARL, 4));

        assertFalse(pickup(onGround).isCancelled());
    }

    @Test
    void picksUpNothingWhenNoStackHasRoom() {
        limit(Material.TNT, new ItemLimit(1, 0));
        for (int slot = 0; slot < 36; slot++) {
            player.getInventory().setItem(slot, new ItemStack(Material.TNT));
        }
        Item onGround = player.getWorld().dropItem(player.getLocation(), new ItemStack(Material.TNT, 2));

        assertTrue(pickup(onGround).isCancelled());
        assertEquals(2, onGround.getItemStack().getAmount());
    }

    @Test
    void splitsStacksAboveTheLimit() {
        limit(Material.TNT, new ItemLimit(16, 0));
        player.getInventory().setItem(0, new ItemStack(Material.TNT, 40));

        join();

        List<Integer> stacks = new ArrayList<>();
        for (ItemStack item : player.getInventory().getStorageContents()) {
            if (item != null && item.getType() == Material.TNT) {
                stacks.add(item.getAmount());
            }
        }
        assertEquals(List.of(16, 16, 8), stacks);
        assertEquals(List.of("[ItemBlocker] TNT stacks up to 16 here."), messages());
    }

    @Test
    void dropsWhatExceedsTheInventoryLimit() {
        limit(Material.TNT, new ItemLimit(0, 10));
        player.getInventory().setItem(0, new ItemStack(Material.TNT, 30));

        join();

        assertEquals(10, count(Material.TNT));
        int dropped = player.getWorld().getEntitiesByClass(Item.class).stream()
                .mapToInt(item -> item.getItemStack().getAmount())
                .sum();
        assertEquals(20, dropped);
    }

    @Test
    void deniesShiftClickingOverTheLimitOutOfAContainer() {
        limit(Material.TNT, new ItemLimit(0, 10));
        player.getInventory().setItem(0, new ItemStack(Material.TNT, 8));
        Inventory chest = server.createInventory(null, 27);
        chest.setItem(0, new ItemStack(Material.TNT, 5));

        InventoryClickEvent event = new InventoryClickEvent(InventoryViews.container(player, chest),
                SlotType.CONTAINER, 0, ClickType.SHIFT_LEFT, InventoryAction.MOVE_TO_OTHER_INVENTORY);
        server.getPluginManager().callEvent(event);

        assertTrue(event.isCancelled());
    }

    @Test
    void bypassIgnoresLimits() {
        limit(Material.TNT, new ItemLimit(0, 10));
        player.addAttachment(plugin, "itemblocker.bypass.limit", true);
        player.getInventory().setItem(0, new ItemStack(Material.TNT, 30));

        join();

        assertEquals(30, count(Material.TNT));
    }

    @Test
    void blockedPickupIsNotHandedOutByTheLimit() {
        block(Material.TNT, Action.PICKUP);
        limit(Material.TNT, new ItemLimit(0, 1));
        Item onGround = player.getWorld().dropItem(player.getLocation(), new ItemStack(Material.TNT, 4));

        pickup(onGround);

        assertEquals(0, count(Material.TNT));
    }

    private void limit(Material material, ItemLimit limit) {
        updateDefault(preset -> preset.withLimit(material, limit));
    }

    private EntityPickupItemEvent pickup(Item item) {
        EntityPickupItemEvent event = new EntityPickupItemEvent(player, item, 0);
        server.getPluginManager().callEvent(event);
        return event;
    }

    private void join() {
        server.getPluginManager().callEvent(new PlayerJoinEvent(player, Component.text("joined")));
        server.getScheduler().performOneTick();
    }

    private int count(Material material) {
        int total = 0;
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && item.getType() == material) {
                total += item.getAmount();
            }
        }
        return total;
    }
}
