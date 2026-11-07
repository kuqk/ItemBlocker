package pl.variant.itemblocker.listener;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.ShulkerBox;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType.SlotType;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.bukkit.inventory.meta.BundleMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.junit.jupiter.api.Test;
import pl.variant.itemblocker.rule.Action;
import pl.variant.itemblocker.rule.EnchantmentMode;
import pl.variant.itemblocker.rule.ItemLimit;
import pl.variant.itemblocker.rule.ItemRule;
import pl.variant.itemblocker.rule.Preset;
import pl.variant.itemblocker.rule.RegionScope;
import pl.variant.itemblocker.testing.InventoryViews;
import pl.variant.itemblocker.testing.PluginTest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Regression expectations for cases uncovered by the project audit. */
class ListenerAuditTest extends PluginTest {

    @Test
    void blockedEnchantmentCannotBePickedUpInsideAShulkerBox() {
        blockEnchantment("sharpness", 5);
        ItemStack sword = new ItemStack(Material.DIAMOND_SWORD);
        sword.addUnsafeEnchantment(Enchantment.SHARPNESS, 5);
        ItemStack box = shulkerBoxWith(sword);

        assertTrue(plugin.enforcement().restrictions().checkItem(player, box, Action.PICKUP).isPresent(),
                "A box must not conceal an enchantment that blocks picking up its contents");
    }

    @Test
    void blockedEnchantmentCannotBeMovedByAHopperInsideAShulkerBox() {
        blockEnchantment("sharpness", 5);
        ItemStack sword = new ItemStack(Material.DIAMOND_SWORD);
        sword.addUnsafeEnchantment(Enchantment.SHARPNESS, 5);

        assertTrue(plugin.enforcement().restrictions()
                        .checkItem(shulkerBoxWith(sword), Action.HOPPER, player.getLocation()).isPresent(),
                "Hopper restrictions must inspect enchantments packed inside a box");
    }

    @Test
    void enteringARestrictedRegionRemovesAlreadyWornArmor() {
        updateRules(rules -> rules.with(Preset.empty("arena")
                .withRegions(RegionScope.of(List.of("arena")))
                .withItem(Material.DIAMOND_HELMET, ItemRule.blockEverything())));
        player.getInventory().setHelmet(new ItemStack(Material.DIAMOND_HELMET));

        enterArena();

        assertNull(player.getInventory().getHelmet(),
                "Walking into a region must not preserve armor forbidden inside it");
    }

    @Test
    void enteringARestrictedRegionRemovesAnAlreadyActivePotionEffect() {
        player.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, 1200, 1));
        updateRules(rules -> rules.with(Preset.empty("arena")
                .withRegions(RegionScope.of(List.of("arena")))
                .withEffects(Preset.empty("arena").effects().with("strength", 2))));

        enterArena();

        assertFalse(player.hasPotionEffect(PotionEffectType.STRENGTH),
                "A pre-existing effect must not bypass the region's potion restriction");
    }

    @Test
    void hotbarSwapMayReduceTheCarriedAmountWithoutBeingDeniedByTheLimit() {
        updateDefault(preset -> preset.withLimit(Material.TNT, new ItemLimit(0, 10)));
        player.getInventory().setItem(0, new ItemStack(Material.TNT, 10));
        Inventory chest = server.createInventory(null, 27);
        chest.setItem(0, new ItemStack(Material.TNT, 5));
        InventoryClickEvent event = new InventoryClickEvent(InventoryViews.container(player, chest),
                SlotType.CONTAINER, 0, ClickType.NUMBER_KEY, InventoryAction.HOTBAR_SWAP, 0);

        server.getPluginManager().callEvent(event);

        assertFalse(event.isCancelled(),
                "Swapping ten carried TNT for five in a chest leaves five carried, below the limit");
    }

    @Test
    void disablingBothBypassSettingsMakesOperatorSubjectToRestrictions() throws Exception {
        block(Material.TNT, Action.PLACE);
        plugin.getConfig().set("bypass.enabled", false);
        plugin.getConfig().set("bypass.per-action", false);
        plugin.saveConfig();
        plugin.reload();
        player.setOp(true);

        assertTrue(plugin.enforcement().restrictions()
                        .checkItem(player, new ItemStack(Material.TNT), Action.PLACE).isPresent(),
                "Disabling global and per-action bypass must make an operator subject to the rule");
    }

    @Test
    void perActionBypassRemainsIndependentOfTheGlobalBypassSetting() throws Exception {
        block(Material.TNT, Action.PLACE, Action.DROP);
        plugin.getConfig().set("bypass.enabled", false);
        plugin.saveConfig();
        plugin.reload();
        player.addAttachment(plugin, "itemblocker.bypass.place", true);

        assertTrue(plugin.enforcement().restrictions()
                .checkItem(player, new ItemStack(Material.TNT), Action.PLACE).isEmpty());
        assertTrue(plugin.enforcement().restrictions()
                .checkItem(player, new ItemStack(Material.TNT), Action.DROP).isPresent());
    }

    @Test
    void packedEnchantmentsRespectTheEnchantmentBypassAndAllowedModes() {
        blockEnchantment("sharpness", 5);
        ItemStack sword = new ItemStack(Material.DIAMOND_SWORD);
        sword.addUnsafeEnchantment(Enchantment.SHARPNESS, 5);
        ItemStack box = shulkerBoxWith(sword);
        player.addAttachment(plugin, "itemblocker.bypass.enchantment", true);

        assertTrue(plugin.enforcement().restrictions().checkItem(player, box, Action.PICKUP).isEmpty());

        updateDefault(preset -> preset.withEnchantments(
                preset.enchantments().withMode("sharpness", EnchantmentMode.DOWNGRADE)));
        assertTrue(plugin.enforcement().restrictions()
                .checkItem(box, Action.HOPPER, player.getLocation()).isEmpty());
    }

    @Test
    void nestedShulkerContentsCannotConcealABlockedEnchantment() {
        blockEnchantment("sharpness", 5);
        ItemStack sword = new ItemStack(Material.DIAMOND_SWORD);
        sword.addUnsafeEnchantment(Enchantment.SHARPNESS, 5);

        assertTrue(plugin.enforcement().restrictions()
                .checkItem(player, shulkerBoxWith(shulkerBoxWith(sword)), Action.PICKUP).isPresent());
    }

    @Test
    void aBundleCannotConcealABlockedEnchantment() {
        blockEnchantment("sharpness", 5);
        ItemStack sword = new ItemStack(Material.DIAMOND_SWORD);
        sword.addUnsafeEnchantment(Enchantment.SHARPNESS, 5);
        ItemStack bundle = new ItemStack(Material.BUNDLE);
        BundleMeta meta = (BundleMeta) bundle.getItemMeta();
        meta.setItems(List.of(sword));
        bundle.setItemMeta(meta);

        assertTrue(plugin.enforcement().restrictions().checkItem(player, bundle, Action.PICKUP).isPresent());
    }

    @Test
    void teleportingIntoARestrictedRegionRemovesArmorAndAnActiveEffect() {
        player.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, 1200, 1));
        updateRules(rules -> rules.with(Preset.empty("arena")
                .withRegions(RegionScope.of(List.of("arena")))
                .withItem(Material.DIAMOND_HELMET, ItemRule.blockEverything())
                .withEffects(Preset.empty("arena").effects().with("strength", 2))));
        player.getInventory().setHelmet(new ItemStack(Material.DIAMOND_HELMET));
        Location from = player.getLocation();
        Location to = from.clone().add(20, 0, 0);
        plugin.fakeRegions().setCurrent("arena");
        player.setLocation(to);

        server.getPluginManager().callEvent(new PlayerTeleportEvent(player, from, to,
                PlayerTeleportEvent.TeleportCause.PLUGIN));
        server.getScheduler().performTicks(3);

        assertNull(player.getInventory().getHelmet());
        assertFalse(player.hasPotionEffect(PotionEffectType.STRENGTH));
    }

    @Test
    void enteringARegionStillHonorsItsExplicitPresetBypass() {
        player.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, 1200, 1));
        updateRules(rules -> rules.with(Preset.empty("arena")
                .withRegions(RegionScope.of(List.of("arena")))
                .withItem(Material.DIAMOND_HELMET, ItemRule.blockEverything())
                .withEffects(Preset.empty("arena").effects().with("strength", 2))));
        player.getInventory().setHelmet(new ItemStack(Material.DIAMOND_HELMET));
        player.addAttachment(plugin, "itemblocker.bypass.preset.arena", true);

        enterArena();

        assertTrue(player.getInventory().getHelmet().getType() == Material.DIAMOND_HELMET);
        assertTrue(player.hasPotionEffect(PotionEffectType.STRENGTH));
    }

    @Test
    void aCancelledMoveDoesNotRemoveArmorAsThoughItEnteredTheRegion() {
        updateRules(rules -> rules.with(Preset.empty("arena")
                .withRegions(RegionScope.of(List.of("arena")))
                .withItem(Material.DIAMOND_HELMET, ItemRule.blockEverything())));
        player.getInventory().setHelmet(new ItemStack(Material.DIAMOND_HELMET));
        plugin.fakeRegions().setCurrent("arena");
        Location from = player.getLocation();
        PlayerMoveEvent event = new PlayerMoveEvent(player, from, from.clone().add(1, 0, 0));
        event.setCancelled(true);

        server.getPluginManager().callEvent(event);
        server.getScheduler().performTicks(3);

        assertTrue(player.getInventory().getHelmet().getType() == Material.DIAMOND_HELMET);
    }

    @Test
    void hotbarSwapsStillDenyANetIncreaseAboveTheInventoryLimit() {
        updateDefault(preset -> preset.withLimit(Material.TNT, new ItemLimit(0, 10)));
        player.getInventory().setItem(0, new ItemStack(Material.TNT, 10));
        Inventory chest = server.createInventory(null, 27);
        chest.setItem(0, new ItemStack(Material.TNT, 11));
        InventoryClickEvent event = new InventoryClickEvent(InventoryViews.container(player, chest),
                SlotType.CONTAINER, 0, ClickType.NUMBER_KEY, InventoryAction.HOTBAR_SWAP, 0);

        server.getPluginManager().callEvent(event);

        assertTrue(event.isCancelled());
    }

    @Test
    void hotbarMoveAndReaddDoesNotSubtractItemsThatStayInTheInventory() {
        updateDefault(preset -> preset.withLimit(Material.TNT, new ItemLimit(0, 10)));
        player.getInventory().setItem(0, new ItemStack(Material.TNT, 10));
        Inventory chest = server.createInventory(null, 27);
        chest.setItem(0, new ItemStack(Material.TNT, 5));
        InventoryClickEvent event = new InventoryClickEvent(InventoryViews.container(player, chest),
                SlotType.CONTAINER, 0, ClickType.NUMBER_KEY, InventoryAction.HOTBAR_MOVE_AND_READD, 0);

        server.getPluginManager().callEvent(event);

        assertTrue(event.isCancelled());
    }

    @Test
    void swappingAnUnrelatedHotbarMaterialDoesNotMakeRoomForLimitedItems() {
        updateDefault(preset -> preset.withLimit(Material.TNT, new ItemLimit(0, 10)));
        player.getInventory().setItem(0, new ItemStack(Material.STONE, 10));
        player.getInventory().setItem(1, new ItemStack(Material.TNT, 10));
        Inventory chest = server.createInventory(null, 27);
        chest.setItem(0, new ItemStack(Material.TNT, 5));
        InventoryClickEvent event = new InventoryClickEvent(InventoryViews.container(player, chest),
                SlotType.CONTAINER, 0, ClickType.NUMBER_KEY, InventoryAction.HOTBAR_SWAP, 0);

        server.getPluginManager().callEvent(event);

        assertTrue(event.isCancelled());
    }

    @Test
    void offhandSwapUsesTheNetChangeInCarriedItems() {
        updateDefault(preset -> preset.withLimit(Material.TNT, new ItemLimit(0, 10)));
        player.getInventory().setItemInOffHand(new ItemStack(Material.TNT, 10));
        Inventory chest = server.createInventory(null, 27);
        chest.setItem(0, new ItemStack(Material.TNT, 5));
        InventoryClickEvent event = new InventoryClickEvent(InventoryViews.container(player, chest),
                SlotType.CONTAINER, 0, ClickType.SWAP_OFFHAND, InventoryAction.HOTBAR_SWAP);

        server.getPluginManager().callEvent(event);

        assertFalse(event.isCancelled());
    }

    private void enterArena() {
        Location from = player.getLocation();
        Location to = from.clone().add(1, 0, 0);
        plugin.fakeRegions().setCurrent("arena");
        player.setLocation(to);
        server.getPluginManager().callEvent(new PlayerMoveEvent(player, from, to));
        server.getScheduler().performTicks(3);
    }

    private static ItemStack shulkerBoxWith(ItemStack inside) {
        ItemStack box = new ItemStack(Material.SHULKER_BOX);
        BlockStateMeta meta = (BlockStateMeta) box.getItemMeta();
        ShulkerBox state = (ShulkerBox) meta.getBlockState();
        state.getInventory().addItem(inside);
        meta.setBlockState(state);
        box.setItemMeta(meta);
        return box;
    }
}
