package pl.variant.itemblocker.gui;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import pl.variant.itemblocker.gui.LevelsMenu.Kind;
import pl.variant.itemblocker.message.Message;
import pl.variant.itemblocker.message.Placeholder;
import pl.variant.itemblocker.rule.Preset;
import pl.variant.itemblocker.rule.RuleBook;

import java.util.Optional;

/**
 * One preset: a button for each kind of rule, its settings, and deleting it.
 */
final class PresetMenu extends Menu {

    static final int ITEMS = 10;
    static final int GROUPS = 11;
    static final int FILTERS = 12;
    static final int ALLOWED = 13;
    static final int ENCHANTMENTS = 14;
    static final int EFFECTS = 15;
    static final int SETTINGS = 16;
    static final int BACK = 18;
    static final int CLOSE = 22;
    static final int DELETE = 26;

    private final Gui gui;
    private final String presetName;

    PresetMenu(Gui gui, String presetName) {
        super(3, gui.text(Message.GUI_PRESET_TITLE, Placeholder.of("preset", presetName)));
        this.gui = gui;
        this.presetName = presetName;
    }

    @Override
    protected void render() {
        Inventory inventory = getInventory();
        inventory.clear();
        Optional<Preset> found = gui.preset(presetName);
        if (found.isEmpty()) {
            return;
        }

        Preset preset = found.get();
        for (int slot = 0; slot < inventory.getSize(); slot++) {
            inventory.setItem(slot, Gui.filler());
        }
        inventory.setItem(ITEMS, section(Material.DIAMOND_SWORD, Message.GUI_SECTION_ITEMS, ItemsMenu.itemsOf(preset).size()));
        inventory.setItem(GROUPS, section(Material.CHEST, Message.GUI_SECTION_GROUPS, preset.groups().size()));
        inventory.setItem(FILTERS, section(Material.NAME_TAG, Message.GUI_SECTION_FILTERS, preset.filters().size()));
        inventory.setItem(ALLOWED, section(Material.EMERALD, Message.GUI_SECTION_ALLOWED, preset.allowed().size()));
        inventory.setItem(ENCHANTMENTS, section(Material.ENCHANTED_BOOK, Message.GUI_SECTION_ENCHANTMENTS, preset.enchantments().size()));
        inventory.setItem(EFFECTS, section(Material.POTION, Message.GUI_SECTION_EFFECTS, preset.effects().size()));
        inventory.setItem(SETTINGS, Gui.icon(Material.COMPARATOR, gui.text(Message.GUI_SECTION_SETTINGS), gui.lines(Message.GUI_CHANGE_LORE)));
        inventory.setItem(BACK, gui.icon(Material.ARROW, Message.GUI_BACK));
        inventory.setItem(CLOSE, gui.icon(Material.BARRIER, Message.GUI_CLOSE));
        if (!preset.name().equals(RuleBook.DEFAULT_PRESET)) {
            inventory.setItem(DELETE, Gui.icon(Material.LAVA_BUCKET, gui.text(Message.GUI_DELETE_NAME), gui.lines(Message.GUI_DELETE_LORE)));
        }
    }

    private ItemStack section(Material icon, Message name, int count) {
        return Gui.icon(icon, gui.text(name), gui.lines(Message.GUI_SECTION_LORE, Placeholder.of("count", count)));
    }

    @Override
    protected void click(Player player, int slot, ClickType click) {
        if (gui.preset(presetName).isEmpty()) {
            gui.open(player, new PresetsMenu(gui));
            return;
        }

        switch (slot) {
            case ITEMS -> gui.open(player, new ItemsMenu(gui, presetName, 0));
            case GROUPS -> gui.open(player, new GroupsMenu(gui, presetName, 0));
            case FILTERS -> gui.open(player, new FiltersMenu(gui, presetName, 0));
            case ALLOWED -> gui.open(player, new AllowedMenu(gui, presetName, 0));
            case ENCHANTMENTS -> gui.open(player, new LevelsMenu(gui, presetName, Kind.ENCHANTMENTS, 0));
            case EFFECTS -> gui.open(player, new LevelsMenu(gui, presetName, Kind.EFFECTS, 0));
            case SETTINGS -> gui.open(player, new SettingsMenu(gui, presetName));
            case BACK -> gui.open(player, new PresetsMenu(gui));
            case CLOSE -> gui.close(player);
            case DELETE -> {
                if (click == ClickType.SHIFT_RIGHT && !presetName.equals(RuleBook.DEFAULT_PRESET)
                        && gui.updateRules(player, rules -> rules.without(presetName))) {
                    gui.open(player, new PresetsMenu(gui));
                }
            }
            default -> {
                // Filler does nothing.
            }
        }
    }
}
