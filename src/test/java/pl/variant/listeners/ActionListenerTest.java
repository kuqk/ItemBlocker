package pl.variant.listeners;

import org.bukkit.Material;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockDispenseArmorEvent;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import pl.variant.itemBlocker;
import pl.variant.model.BlockAction;
import pl.variant.model.ItemRule;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.util.EnumSet;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ActionListenerTest {

    // Raw slots of the player's own inventory view: 5 = helmet, 9 = first storage slot.
    private static final int HELMET_RAW_SLOT = 5;
    private static final int FIRST_STORAGE_RAW_SLOT = 9;

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

    @Test
    void armorBlockCancelsOffhandSwapIntoArmorSlot() {
        blockItem(Material.DIAMOND_HELMET, BlockAction.ARMOR);
        player.getInventory().setItemInOffHand(new ItemStack(Material.DIAMOND_HELMET));

        InventoryClickEvent event = new InventoryClickEvent(
                playerInventoryView(),
                InventoryType.SlotType.ARMOR,
                HELMET_RAW_SLOT,
                ClickType.SWAP_OFFHAND,
                InventoryAction.HOTBAR_SWAP
        );

        new ArmorListener(plugin).onArmorEquip(event);

        assertTrue(event.isCancelled());
    }

    @Test
    void armorBlockCancelsDragIntoArmorSlot() {
        blockItem(Material.DIAMOND_HELMET, BlockAction.ARMOR);

        ItemStack helmet = new ItemStack(Material.DIAMOND_HELMET);
        InventoryDragEvent event = new InventoryDragEvent(
                playerInventoryView(),
                null,
                helmet,
                false,
                Map.of(HELMET_RAW_SLOT, helmet)
        );

        new ArmorListener(plugin).onArmorDrag(event);

        assertTrue(event.isCancelled());
    }

    @Test
    void armorDragOutsideArmorSlotsIsAllowed() {
        blockItem(Material.DIAMOND_HELMET, BlockAction.ARMOR);

        ItemStack helmet = new ItemStack(Material.DIAMOND_HELMET);
        InventoryDragEvent event = new InventoryDragEvent(
                playerInventoryView(),
                null,
                helmet,
                false,
                Map.of(FIRST_STORAGE_RAW_SLOT, helmet)
        );

        new ArmorListener(plugin).onArmorDrag(event);

        assertFalse(event.isCancelled());
    }

    @Test
    void armorBlockCancelsDispenserEquippingPlayer() {
        blockItem(Material.DIAMOND_HELMET, BlockAction.ARMOR);

        BlockDispenseArmorEvent event = new BlockDispenseArmorEvent(
                player.getLocation().getBlock(),
                new ItemStack(Material.DIAMOND_HELMET),
                player
        );

        new ArmorListener(plugin).onDispenseArmor(event);

        assertTrue(event.isCancelled());
    }

    /**
     * MockBukkit does not implement slot conversion for the player's own inventory view, so this
     * wraps it with the CraftBukkit raw slot layout: 0 result, 1-4 crafting, 5-8 armor, 9-35 storage, 36-44 hotbar.
     */
    private InventoryView playerInventoryView() {
        InventoryView delegate = player.getOpenInventory();
        return (InventoryView) Proxy.newProxyInstance(
                InventoryView.class.getClassLoader(),
                new Class<?>[]{InventoryView.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "convertSlot" -> convertPlayerViewSlot((int) args[0]);
                    case "getSlotType" -> playerViewSlotType((int) args[0]);
                    default -> {
                        try {
                            yield method.invoke(delegate, args);
                        } catch (InvocationTargetException exception) {
                            throw exception.getCause();
                        }
                    }
                }
        );
    }

    private static int convertPlayerViewSlot(int rawSlot) {
        if (rawSlot >= 5 && rawSlot <= 8) {
            return 39 - (rawSlot - 5);
        }
        if (rawSlot >= 36 && rawSlot <= 44) {
            return rawSlot - 36;
        }
        return rawSlot;
    }

    private static InventoryType.SlotType playerViewSlotType(int rawSlot) {
        if (rawSlot == 0) {
            return InventoryType.SlotType.RESULT;
        }
        if (rawSlot <= 4) {
            return InventoryType.SlotType.CRAFTING;
        }
        if (rawSlot <= 8) {
            return InventoryType.SlotType.ARMOR;
        }
        return rawSlot <= 35 ? InventoryType.SlotType.CONTAINER : InventoryType.SlotType.QUICKBAR;
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
