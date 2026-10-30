package pl.variant.itemblocker.listener;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.ArmorStand;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityPlaceEvent;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.Test;
import pl.variant.itemblocker.rule.Action;
import pl.variant.itemblocker.testing.PluginTest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlacementListenerTest extends PluginTest {

    @Test
    void deniesPlacingBlockedBlock() {
        block(Material.TNT, Action.PLACE);

        BlockPlaceEvent event = placeBlock(new ItemStack(Material.TNT));
        server.getPluginManager().callEvent(event);

        assertTrue(event.isCancelled());
        assertEquals(List.of("ItemBlocker » You cannot place tnt."), messages());
    }

    @Test
    void allowsBlocksWithOtherRules() {
        block(Material.TNT, Action.CRAFTING);

        BlockPlaceEvent event = placeBlock(new ItemStack(Material.TNT));
        server.getPluginManager().callEvent(event);

        assertFalse(event.isCancelled());
    }

    @Test
    void deniesEmptyingBlockedBucket() {
        block(Material.LAVA_BUCKET, Action.PLACE);
        player.getInventory().setItemInMainHand(new ItemStack(Material.LAVA_BUCKET));
        Block target = player.getLocation().getBlock();

        PlayerBucketEmptyEvent event = new PlayerBucketEmptyEvent(player, target, target, BlockFace.UP,
                Material.LAVA_BUCKET, new ItemStack(Material.LAVA_BUCKET), EquipmentSlot.HAND);
        server.getPluginManager().callEvent(event);

        assertTrue(event.isCancelled());
    }

    @Test
    void deniesPlacingBlockedEntityItem() {
        block(Material.ARMOR_STAND, Action.PLACE);
        player.getInventory().setItemInOffHand(new ItemStack(Material.ARMOR_STAND));
        ArmorStand stand = player.getWorld().spawn(player.getLocation(), ArmorStand.class);

        EntityPlaceEvent event = new EntityPlaceEvent(stand, player, player.getLocation().getBlock(), BlockFace.UP, EquipmentSlot.OFF_HAND);
        server.getPluginManager().callEvent(event);

        assertTrue(event.isCancelled());
    }

    private BlockPlaceEvent placeBlock(ItemStack item) {
        Block block = player.getLocation().getBlock();
        return new BlockPlaceEvent(block, block.getState(), block.getRelative(BlockFace.DOWN), item, player, true, EquipmentSlot.HAND);
    }
}
