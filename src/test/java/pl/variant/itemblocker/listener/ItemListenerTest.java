package pl.variant.itemblocker.listener;

import org.bukkit.Material;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Item;
import org.bukkit.event.Event;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.Test;
import pl.variant.itemblocker.testing.PluginTest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static pl.variant.itemblocker.rule.Action.ARMOR;
import static pl.variant.itemblocker.rule.Action.DROP;
import static pl.variant.itemblocker.rule.Action.PICKUP;
import static pl.variant.itemblocker.rule.Action.USE;

class ItemListenerTest extends PluginTest {

    @Test
    void deniesUsingBlockedItemOnAir() {
        block(Material.ENDER_PEARL, USE);

        PlayerInteractEvent event = rightClickAir(new ItemStack(Material.ENDER_PEARL));
        server.getPluginManager().callEvent(event);

        assertEquals(Event.Result.DENY, event.useItemInHand());
        assertEquals(List.of("ItemBlocker » You cannot use ender_pearl."), messages());
    }

    @Test
    void stillLetsPlayerInteractWithClickedBlock() {
        block(Material.FLINT_AND_STEEL, USE);

        PlayerInteractEvent event = rightClickBlock(new ItemStack(Material.FLINT_AND_STEEL));
        server.getPluginManager().callEvent(event);

        assertEquals(Event.Result.DENY, event.useItemInHand());
        assertNotEquals(Event.Result.DENY, event.useInteractedBlock());
    }

    @Test
    void allowsItemsWithoutRules() {
        PlayerInteractEvent event = rightClickAir(new ItemStack(Material.ENDER_PEARL));
        server.getPluginManager().callEvent(event);

        assertNotEquals(Event.Result.DENY, event.useItemInHand());
        assertTrue(messages().isEmpty());
    }

    @Test
    void rightClickingArmorChecksArmorRule() {
        block(Material.DIAMOND_HELMET, ARMOR);

        PlayerInteractEvent event = rightClickAir(new ItemStack(Material.DIAMOND_HELMET));
        server.getPluginManager().callEvent(event);

        assertEquals(Event.Result.DENY, event.useItemInHand());
    }

    @Test
    void placingWearableBlockIsLeftToPlacementRules() {
        block(Material.CARVED_PUMPKIN, ARMOR);

        PlayerInteractEvent event = rightClickBlock(new ItemStack(Material.CARVED_PUMPKIN));
        server.getPluginManager().callEvent(event);

        assertNotEquals(Event.Result.DENY, event.useItemInHand());
    }

    @Test
    void deniesPickupAndDrop() {
        block(Material.TNT, PICKUP, DROP);
        Item item = player.getWorld().dropItem(player.getLocation(), new ItemStack(Material.TNT));

        EntityPickupItemEvent pickup = new EntityPickupItemEvent(player, item, 0);
        server.getPluginManager().callEvent(pickup);
        PlayerDropItemEvent drop = new PlayerDropItemEvent(player, item);
        server.getPluginManager().callEvent(drop);

        assertTrue(pickup.isCancelled());
        assertTrue(drop.isCancelled());
    }

    @Test
    void deniesConsumingBlockedFood() {
        block(Material.GOLDEN_APPLE, USE);

        PlayerItemConsumeEvent event = new PlayerItemConsumeEvent(player, new ItemStack(Material.GOLDEN_APPLE), EquipmentSlot.HAND);
        server.getPluginManager().callEvent(event);

        assertTrue(event.isCancelled());
    }

    @Test
    void bypassPermissionAllowsUse() {
        block(Material.ENDER_PEARL, USE);
        player.addAttachment(plugin, "itemblocker.bypass.use", true);

        PlayerInteractEvent event = rightClickAir(new ItemStack(Material.ENDER_PEARL));
        server.getPluginManager().callEvent(event);

        assertFalse(event.useItemInHand() == Event.Result.DENY);
    }

    private PlayerInteractEvent rightClickAir(ItemStack item) {
        PlayerInteractEvent event = new PlayerInteractEvent(player, Action.RIGHT_CLICK_AIR, item, null, BlockFace.SELF, EquipmentSlot.HAND);
        // A real server fires air clicks with the block interaction already denied, which makes the
        // event count as cancelled.
        event.setUseInteractedBlock(Event.Result.DENY);
        return event;
    }

    private PlayerInteractEvent rightClickBlock(ItemStack item) {
        return new PlayerInteractEvent(player, Action.RIGHT_CLICK_BLOCK, item,
                player.getLocation().getBlock(), BlockFace.UP, EquipmentSlot.HAND);
    }
}
