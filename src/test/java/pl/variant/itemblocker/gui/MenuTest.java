package pl.variant.itemblocker.gui;

import org.bukkit.Material;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType.SlotType;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import pl.variant.itemblocker.gui.LevelsMenu.Kind;
import pl.variant.itemblocker.gui.RuleTarget.FilterTarget;
import pl.variant.itemblocker.gui.RuleTarget.GroupTarget;
import pl.variant.itemblocker.gui.RuleTarget.ItemTarget;
import pl.variant.itemblocker.platform.PlayerScheduler;
import pl.variant.itemblocker.rule.Action;
import pl.variant.itemblocker.rule.ItemLimit;
import pl.variant.itemblocker.rule.ItemPattern;
import pl.variant.itemblocker.rule.ItemRule;
import pl.variant.itemblocker.rule.Preset;
import pl.variant.itemblocker.rule.WorldScope;
import pl.variant.itemblocker.testing.InventoryViews;
import pl.variant.itemblocker.testing.PluginTest;

import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MenuTest extends PluginTest {

    private ChatPrompt prompt;
    private Gui gui;

    @BeforeEach
    void createGui() {
        PlayerScheduler scheduler = PlayerScheduler.bukkit(plugin);
        prompt = new ChatPrompt(scheduler);
        gui = new Gui(plugin.rules(), plugin.messages(), scheduler, prompt, plugin.regions());
    }

    @Test
    void presetsMenuShowsEveryPresetAndCreatesNewOnes() {
        updateRules(rules -> rules.with(Preset.empty("pvp")));
        gui.openPresets(player);
        PresetsMenu menu = assertInstanceOf(PresetsMenu.class, openMenu());
        assertEquals(Material.ENCHANTED_BOOK, menu.getInventory().getItem(0).getType());
        assertEquals(Material.BOOK, menu.getInventory().getItem(1).getType());

        menu.click(player, menu.getInventory().getSize() - 9, ClickType.LEFT);
        answer("Event");

        assertTrue(plugin.rules().rules().preset("event").isPresent());
        assertInstanceOf(PresetMenu.class, openMenu());
    }

    @Test
    void presetMenuOpensEachSection() {
        PresetMenu menu = new PresetMenu(gui, "default");
        menu.open(player);

        Map<Integer, Class<?>> sections = Map.of(
                PresetMenu.ITEMS, ItemsMenu.class,
                PresetMenu.GROUPS, GroupsMenu.class,
                PresetMenu.FILTERS, FiltersMenu.class,
                PresetMenu.ALLOWED, AllowedMenu.class,
                PresetMenu.ENCHANTMENTS, LevelsMenu.class,
                PresetMenu.SETTINGS, SettingsMenu.class);
        sections.forEach((slot, type) -> {
            menu.click(player, slot, ClickType.LEFT);
            server.getScheduler().performOneTick();
            assertInstanceOf(type, openMenu());
        });
    }

    @Test
    void itemsListShowsRulesAndLimitsAndAddsOwnItems() {
        updateDefault(preset -> preset
                .withItem(Material.TNT, ItemRule.blockEverything())
                .withLimit(Material.ENDER_PEARL, new ItemLimit(4, 0)));
        ItemsMenu menu = new ItemsMenu(gui, "default", 0);
        menu.open(player);
        assertEquals(Material.ENDER_PEARL, menu.getInventory().getItem(0).getType());
        assertEquals(Material.TNT, menu.getInventory().getItem(1).getType());

        menu.clickOwnItem(player, new ItemStack(Material.STONE));
        server.getScheduler().performOneTick();
        assertInstanceOf(RuleMenu.class, openMenu());
    }

    @Test
    void itemSearchNarrowsDownAndPicks() {
        ItemsMenu items = new ItemsMenu(gui, "default", 0);
        items.open(player);
        items.click(player, ListMenu.ADD, ClickType.LEFT);
        server.getScheduler().performOneTick();
        ItemPickerMenu picker = assertInstanceOf(ItemPickerMenu.class, openMenu());
        assertTrue(picker.matches().size() > ListMenu.PAGE_SIZE);

        picker.click(player, ItemPickerMenu.SEARCH, ClickType.LEFT);
        answer("diamond chest");
        ItemPickerMenu found = assertInstanceOf(ItemPickerMenu.class, openMenu());
        assertEquals(List.of(Material.DIAMOND_CHESTPLATE), found.matches());

        found.click(player, 0, ClickType.LEFT);
        server.getScheduler().performOneTick();
        assertInstanceOf(RuleMenu.class, openMenu());
    }

    @Test
    void togglingActionsCreatesAndRemovesAnItemRule() {
        RuleMenu menu = itemEditor(Material.TNT);
        int place = RuleMenu.FIRST_ACTION + Action.PLACE.ordinal();

        menu.click(player, place, ClickType.LEFT);
        assertEquals(Set.of(Action.PLACE), defaults().item(Material.TNT).orElseThrow().actions());
        assertEquals(Material.RED_DYE, menu.getInventory().getItem(place).getType());

        menu.click(player, place, ClickType.LEFT);
        assertTrue(defaults().item(Material.TNT).isEmpty());
    }

    @Test
    void limitsChangeWithClicks() {
        RuleMenu menu = itemEditor(Material.ENDER_PEARL);

        menu.click(player, RuleMenu.STACK, ClickType.LEFT);
        menu.click(player, RuleMenu.STACK, ClickType.LEFT);
        menu.click(player, RuleMenu.INVENTORY, ClickType.SHIFT_LEFT);
        assertEquals(new ItemLimit(2, 16), defaults().limit(Material.ENDER_PEARL));

        menu.click(player, RuleMenu.STACK, ClickType.SHIFT_RIGHT);
        assertEquals(new ItemLimit(0, 16), defaults().limit(Material.ENDER_PEARL));
    }

    @Test
    void stackLimitStaysBelowTheItemsOwnStackSize() {
        assertEquals(15, RuleMenu.adjust(15, ClickType.SHIFT_LEFT, 15));
        assertEquals(0, RuleMenu.adjust(3, ClickType.SHIFT_RIGHT, 63));
        assertEquals(0, RuleMenu.adjust(0, ClickType.LEFT, 0));
    }

    @Test
    void worldsArePickedAndTheBypassPermissionTyped() {
        updateDefault(preset -> preset.withItem(Material.ELYTRA, new ItemRule(EnumSet.of(Action.ARMOR), WorldScope.ALL)));
        RuleMenu menu = itemEditor(Material.ELYTRA);

        menu.click(player, RuleMenu.WORLDS, ClickType.LEFT);
        server.getScheduler().performOneTick();
        NamePickerMenu worlds = assertInstanceOf(NamePickerMenu.class, openMenu());
        worlds.click(player, 0, ClickType.LEFT);
        assertEquals(WorldScope.of(List.of(player.getWorld().getName())), defaults().item(Material.ELYTRA).orElseThrow().scope());
        worlds.click(player, NamePickerMenu.EVERYWHERE, ClickType.LEFT);
        assertEquals(WorldScope.ALL, defaults().item(Material.ELYTRA).orElseThrow().scope());

        menu.open(player);
        menu.click(player, RuleMenu.BYPASS, ClickType.LEFT);
        answer("vip.elytra");
        assertEquals("vip.elytra", defaults().item(Material.ELYTRA).orElseThrow().bypassPermission());
        assertInstanceOf(RuleMenu.class, openMenu());
    }

    @Test
    void cancellingAPromptChangesNothingAndReopensTheMenu() {
        updateDefault(preset -> preset.withItem(Material.ELYTRA, ItemRule.blockEverything()));
        RuleMenu menu = itemEditor(Material.ELYTRA);

        menu.click(player, RuleMenu.BYPASS, ClickType.LEFT);
        answer("cancel");

        assertEquals(null, defaults().item(Material.ELYTRA).orElseThrow().bypassPermission());
        assertInstanceOf(RuleMenu.class, openMenu());
    }

    @Test
    void removeDeletesTheRuleAndLimits() {
        updateDefault(preset -> preset
                .withItem(Material.TNT, new ItemRule(EnumSet.of(Action.PLACE), WorldScope.ALL))
                .withLimit(Material.TNT, new ItemLimit(0, 8)));
        RuleMenu menu = itemEditor(Material.TNT);

        menu.click(player, RuleMenu.REMOVE, ClickType.LEFT);

        assertTrue(defaults().item(Material.TNT).isEmpty());
        assertTrue(defaults().limits().isEmpty());
    }

    @Test
    void groupsAreAddedByPatternAndEdited() {
        GroupsMenu menu = new GroupsMenu(gui, "default", 0);
        menu.open(player);

        menu.click(player, ListMenu.ADD, ClickType.LEFT);
        answer("*_sword");

        ItemPattern swords = ItemPattern.parse("*_sword").orElseThrow();
        assertTrue(defaults().group(swords).orElseThrow().blocksEveryAction());
        RuleMenu editor = assertInstanceOf(RuleMenu.class, openMenu());
        editor.click(player, RuleMenu.FIRST_ACTION + Action.USE.ordinal(), ClickType.LEFT);
        assertTrue(!defaults().group(swords).orElseThrow().actions().contains(Action.USE));

        new RuleMenu(gui, "default", new GroupTarget(swords), () -> menu).click(player, RuleMenu.REMOVE, ClickType.LEFT);
        assertTrue(defaults().groups().isEmpty());
    }

    @Test
    void filtersAreMadeFromHeldItemsAndTheirCriteriaEdited() {
        ItemStack excalibur = new ItemStack(Material.DIAMOND_SWORD);
        ItemMeta meta = excalibur.getItemMeta();
        meta.setDisplayName("Excalibur");
        excalibur.setItemMeta(meta);
        FiltersMenu menu = new FiltersMenu(gui, "default", 0);
        menu.open(player);

        menu.clickOwnItem(player, excalibur);
        server.getScheduler().performOneTick();
        assertEquals("Excalibur", defaults().filter("excalibur").orElseThrow().filter().name());
        menu.clickOwnItem(player, excalibur);
        assertTrue(defaults().filter("excalibur-2").isPresent());

        RuleMenu editor = assertInstanceOf(RuleMenu.class, openMenu());
        editor.click(player, RuleMenu.FIRST_CRITERION + RuleMenu.Criterion.LORE.ordinal(), ClickType.LEFT);
        answer("Soulbound");
        assertEquals("Soulbound", defaults().filter("excalibur").orElseThrow().filter().lore());

        new RuleMenu(gui, "default", new FilterTarget("excalibur"), () -> menu)
                .click(player, RuleMenu.FIRST_CRITERION + RuleMenu.Criterion.MODEL.ordinal(), ClickType.LEFT);
        answer("abc");
        assertEquals(null, defaults().filter("excalibur").orElseThrow().filter().model());
    }

    @Test
    void allowedItemsAreAddedAndRemovedByClicking() {
        AllowedMenu menu = new AllowedMenu(gui, "default", 0);
        menu.open(player);

        menu.clickOwnItem(player, new ItemStack(Material.WOODEN_SWORD));
        assertEquals(Set.of(Material.WOODEN_SWORD), defaults().allowed());

        menu.click(player, 0, ClickType.LEFT);
        assertTrue(defaults().allowed().isEmpty());
    }

    @Test
    void enchantmentLevelsChangeWithClicks() {
        LevelsMenu menu = new LevelsMenu(gui, "default", Kind.ENCHANTMENTS, 0);
        menu.open(player);
        int sharpness = menu.entries(defaults()).indexOf("sharpness");

        menu.click(player, sharpness, ClickType.LEFT);
        menu.open(player);
        menu.click(player, menu.entries(defaults()).indexOf("sharpness"), ClickType.LEFT);
        assertEquals(Map.of("sharpness", 2), defaults().enchantments().minimumLevels());

        menu.open(player);
        menu.click(player, menu.entries(defaults()).indexOf("sharpness"), ClickType.SHIFT_RIGHT);
        assertTrue(defaults().enchantments().isEmpty());
    }

    @Test
    void settingsAreTypedOrPicked() {
        SettingsMenu menu = new SettingsMenu(gui, "default");
        menu.open(player);

        menu.click(player, SettingsMenu.DESCRIPTION, ClickType.LEFT);
        answer("Survival rules");
        menu.click(player, SettingsMenu.WORLDS, ClickType.LEFT);
        server.getScheduler().performOneTick();
        assertInstanceOf(NamePickerMenu.class, openMenu()).click(player, 0, ClickType.LEFT);

        assertEquals("Survival rules", defaults().description());
        assertEquals(WorldScope.of(List.of(player.getWorld().getName())), defaults().scope());
    }

    @Test
    void presetsAreDeletedWithShiftRightClickOnly() {
        updateRules(rules -> rules.with(Preset.empty("event")));
        PresetMenu menu = new PresetMenu(gui, "event");
        menu.open(player);

        menu.click(player, PresetMenu.DELETE, ClickType.LEFT);
        assertTrue(plugin.rules().rules().preset("event").isPresent());

        menu.click(player, PresetMenu.DELETE, ClickType.SHIFT_RIGHT);
        assertTrue(plugin.rules().rules().preset("event").isEmpty());
    }

    @Test
    void clicksInMenusAreCancelledAndRouted() {
        RuleMenu menu = itemEditor(Material.TNT);

        InventoryClickEvent event = new InventoryClickEvent(InventoryViews.container(player, menu.getInventory()),
                SlotType.CONTAINER, RuleMenu.FIRST_ACTION + Action.USE.ordinal(), ClickType.LEFT, InventoryAction.PICKUP_ALL);
        server.getPluginManager().callEvent(event);

        assertTrue(event.isCancelled());
        assertEquals(Set.of(Action.USE), defaults().item(Material.TNT).orElseThrow().actions());
    }

    private RuleMenu itemEditor(Material material) {
        RuleMenu menu = new RuleMenu(gui, "default", new ItemTarget(material), () -> new ItemsMenu(gui, "default", 0));
        menu.open(player);
        return menu;
    }

    /**
     * Types an answer to the open question, as the player would in the chat.
     */
    private void answer(String text) {
        server.getScheduler().performOneTick();
        assertTrue(prompt.answer(player, text), "no question was asked");
        server.getScheduler().performOneTick();
        server.getScheduler().performOneTick();
    }

    private Menu openMenu() {
        InventoryHolder holder = player.getOpenInventory().getTopInventory().getHolder();
        return assertInstanceOf(Menu.class, holder);
    }

    private Preset defaults() {
        return plugin.rules().rules().defaultPreset();
    }
}
