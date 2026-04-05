package pl.variant.listeners;

import org.bukkit.Material;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import pl.variant.itemBlocker;
import pl.variant.model.BlockAction;
import pl.variant.model.ItemRule;

import java.util.EnumSet;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ActionListenerTest {

    private ServerMock server;
    private itemBlocker plugin;
    private Player player;

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
        plugin = MockBukkit.load(itemBlocker.class);
        player = server.addPlayer();
        player.setOp(false);
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    void armorBlockDoesNotCancelRightClickBlockForWearablePlaceableItem() {
        blockItem(Material.CARVED_PUMPKIN, BlockAction.ARMOR);

        PlayerInteractEvent event = new PlayerInteractEvent(
                player,
                Action.RIGHT_CLICK_BLOCK,
                new ItemStack(Material.CARVED_PUMPKIN),
                player.getLocation().getBlock(),
                BlockFace.UP,
                EquipmentSlot.HAND
        );

        new UseListener(plugin).onUse(event);

        assertFalse(event.isCancelled());
    }

    @Test
    void armorBlockCancelsAirEquipAttempt() {
        blockItem(Material.DIAMOND_HELMET, BlockAction.ARMOR);

        PlayerInteractEvent event = new PlayerInteractEvent(
                player,
                Action.RIGHT_CLICK_AIR,
                new ItemStack(Material.DIAMOND_HELMET),
                null,
                BlockFace.SELF,
                EquipmentSlot.HAND
        );

        new UseListener(plugin).onUse(event);

        assertTrue(event.isCancelled());
    }

    @Test
    void useBlockAppliesToOffhandInteraction() {
        blockItem(Material.ENDER_PEARL, BlockAction.USE);

        PlayerInteractEvent event = new PlayerInteractEvent(
                player,
                Action.RIGHT_CLICK_AIR,
                new ItemStack(Material.ENDER_PEARL),
                null,
                BlockFace.SELF,
                EquipmentSlot.OFF_HAND
        );

        new UseListener(plugin).onUse(event);

        assertTrue(event.isCancelled());
    }

    @Test
    void placeBlockCancelsBoatPlacementInteraction() {
        blockItem(Material.OAK_BOAT, BlockAction.PLACE);

        PlayerInteractEvent event = new PlayerInteractEvent(
                player,
                Action.RIGHT_CLICK_BLOCK,
                new ItemStack(Material.OAK_BOAT),
                player.getLocation().getBlock(),
                BlockFace.UP,
                EquipmentSlot.HAND
        );

        new PlaceInteractListener(plugin).onSpecialPlace(event);

        assertTrue(event.isCancelled());
    }

    private void blockItem(Material material, BlockAction action) {
        plugin.getBlockedItemsManager().replaceGlobalItemRule(
                material,
                new ItemRule(EnumSet.of(action), plugin.getBlockedItemsManager().getGlobalItem(material)
                        .map(ItemRule::getWorldScopeMode)
                        .orElse(pl.variant.model.WorldScopeMode.DISABLED), java.util.Set.of())
        );
    }
}
