package pl.variant.itemblocker.gui;

import org.bukkit.Material;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType.SlotType;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import pl.variant.itemblocker.platform.PlayerScheduler;
import pl.variant.itemblocker.rule.Action;
import pl.variant.itemblocker.rule.ItemLimit;
import pl.variant.itemblocker.rule.ItemRule;
import pl.variant.itemblocker.rule.Preset;
import pl.variant.itemblocker.rule.WorldScope;
import pl.variant.itemblocker.testing.InventoryViews;
import pl.variant.itemblocker.testing.PluginTest;

import java.util.EnumSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MenuTest extends PluginTest {

    private Gui gui;

    @BeforeEach
    void createGui() {
        gui = new Gui(plugin.rules(), plugin.messages(), PlayerScheduler.bukkit(plugin));
    }

    @Test
    void presetsMenuShowsEveryPreset() {
        updateRules(rules -> rules.with(Preset.empty("pvp")));

        gui.openPresets(player);

        PresetsMenu menu = assertInstanceOf(PresetsMenu.class, openMenu());
        assertEquals(Material.ENCHANTED_BOOK, menu.getInventory().getItem(0).getType());
        assertEquals(Material.BOOK, menu.getInventory().getItem(1).getType());
        assertEquals(18, menu.getInventory().getSize());
    }

    @Test
    void clickingAPresetOpensIt() {
        gui.openPresets(player);

        openMenu().click(player, 0, ClickType.LEFT);
        server.getScheduler().performOneTick();

        assertInstanceOf(PresetMenu.class, openMenu());
    }

    @Test
    void presetMenuListsItemsWithRulesOrLimits() {
        updateDefault(preset -> preset
                .withItem(Material.TNT, ItemRule.blockEverything())
                .withLimit(Material.ENDER_PEARL, new ItemLimit(4, 0)));
        PresetMenu menu = new PresetMenu(gui, "default", 0);

        menu.open(player);

        assertEquals(Material.ENDER_PEARL, menu.getInventory().getItem(0).getType());
        assertEquals(Material.TNT, menu.getInventory().getItem(1).getType());
    }

    @Test
    void clickingAnOwnItemOpensItsEditor() {
        PresetMenu menu = new PresetMenu(gui, "default", 0);
        menu.open(player);

        menu.clickOwnItem(player, new ItemStack(Material.TNT));
        server.getScheduler().performOneTick();

        assertInstanceOf(ItemMenu.class, openMenu());
    }

    @Test
    void togglingActionsCreatesAndRemovesTheRule() {
        ItemMenu menu = new ItemMenu(gui, "default", Material.TNT, 0);
        menu.open(player);
        int place = ItemMenu.FIRST_ACTION + Action.PLACE.ordinal();

        menu.click(player, place, ClickType.LEFT);
        assertEquals(Set.of(Action.PLACE), defaults().item(Material.TNT).orElseThrow().actions());
        assertEquals(Material.RED_DYE, menu.getInventory().getItem(place).getType());

        menu.click(player, place, ClickType.LEFT);
        assertTrue(defaults().item(Material.TNT).isEmpty());
    }

    @Test
    void limitsChangeWithClicks() {
        ItemMenu menu = new ItemMenu(gui, "default", Material.ENDER_PEARL, 0);
        menu.open(player);

        menu.click(player, ItemMenu.STACK, ClickType.LEFT);
        menu.click(player, ItemMenu.STACK, ClickType.LEFT);
        menu.click(player, ItemMenu.INVENTORY, ClickType.SHIFT_LEFT);
        assertEquals(new ItemLimit(2, 16), defaults().limit(Material.ENDER_PEARL));

        menu.click(player, ItemMenu.STACK, ClickType.SHIFT_RIGHT);
        assertEquals(new ItemLimit(0, 16), defaults().limit(Material.ENDER_PEARL));
    }

    @Test
    void stackLimitStaysBelowTheItemsOwnStackSize() {
        assertEquals(15, ItemMenu.adjust(15, ClickType.SHIFT_LEFT, 15));
        assertEquals(0, ItemMenu.adjust(3, ClickType.SHIFT_RIGHT, 63));
        assertEquals(0, ItemMenu.adjust(0, ClickType.LEFT, 0));
    }

    @Test
    void removeDeletesTheRuleAndLimits() {
        updateDefault(preset -> preset
                .withItem(Material.TNT, new ItemRule(EnumSet.of(Action.PLACE), WorldScope.ALL))
                .withLimit(Material.TNT, new ItemLimit(0, 8)));
        ItemMenu menu = new ItemMenu(gui, "default", Material.TNT, 0);
        menu.open(player);

        menu.click(player, ItemMenu.REMOVE, ClickType.LEFT);

        assertTrue(defaults().item(Material.TNT).isEmpty());
        assertTrue(defaults().limits().isEmpty());
    }

    @Test
    void clicksInMenusAreCancelledAndRouted() {
        ItemMenu menu = new ItemMenu(gui, "default", Material.TNT, 0);
        menu.open(player);

        InventoryClickEvent event = new InventoryClickEvent(InventoryViews.container(player, menu.getInventory()),
                SlotType.CONTAINER, ItemMenu.FIRST_ACTION + Action.USE.ordinal(), ClickType.LEFT, InventoryAction.PICKUP_ALL);
        server.getPluginManager().callEvent(event);

        assertTrue(event.isCancelled());
        assertEquals(Set.of(Action.USE), defaults().item(Material.TNT).orElseThrow().actions());
    }

    private Menu openMenu() {
        InventoryHolder holder = player.getOpenInventory().getTopInventory().getHolder();
        return assertInstanceOf(Menu.class, holder);
    }

    private Preset defaults() {
        return plugin.rules().rules().defaultPreset();
    }
}
