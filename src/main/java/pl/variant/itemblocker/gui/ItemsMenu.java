package pl.variant.itemblocker.gui;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;
import pl.variant.itemblocker.gui.RuleTarget.ItemTarget;
import pl.variant.itemblocker.message.Message;
import pl.variant.itemblocker.message.Names;
import pl.variant.itemblocker.message.Placeholder;
import pl.variant.itemblocker.rule.ItemTypes;
import pl.variant.itemblocker.rule.Preset;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

/**
 * The item types of a preset that have a rule or a limit. Clicking an item in the player's own
 * inventory adds it, and the search button finds any other item.
 */
final class ItemsMenu extends ListMenu<Material> {

    ItemsMenu(Gui gui, String presetName, int page) {
        super(gui, presetName, page, gui.text(Message.GUI_ITEMS_TITLE, Placeholder.of("preset", presetName)));
    }

    /**
     * Every item type with a rule or a limit in the preset, sorted by key.
     */
    static List<Material> itemsOf(Preset preset) {
        return Stream.concat(preset.items().keySet().stream(), preset.limits().keySet().stream())
                .distinct()
                .sorted(Comparator.comparing(ItemTypes::key))
                .toList();
    }

    @Override
    protected List<Material> entries(Preset preset) {
        return itemsOf(preset);
    }

    @Override
    protected ItemStack icon(Preset preset, Material material) {
        return Gui.icon(material, ChatColor.WHITE + Names.of(material), gui.summary(Message.GUI_ENTRY_LORE, preset, material));
    }

    @Override
    protected void clickEntry(Player player, Material material, ClickType click) {
        if (click == ClickType.SHIFT_RIGHT) {
            change(player, new ItemTarget(material)::without);
        } else {
            gui.open(player, editor(material));
        }
    }

    @Override
    protected ItemStack addButton() {
        return Gui.icon(Material.SPYGLASS, gui.text(Message.GUI_SEARCH_ITEMS_NAME), gui.lines(Message.GUI_SEARCH_ITEMS_LORE));
    }

    @Override
    protected void clickAdd(Player player) {
        gui.open(player, new ItemPickerMenu(gui, "", 0, (picker, material) -> gui.open(picker, editor(material)), () -> atPage(page)));
    }

    @Override
    protected void clickOwnItem(Player player, ItemStack item) {
        if (ItemTypes.isBlockable(item.getType())) {
            gui.open(player, editor(item.getType()));
        }
    }

    private Menu editor(Material material) {
        return new RuleMenu(gui, presetName, new ItemTarget(material), () -> atPage(page));
    }

    @Override
    protected Menu atPage(int newPage) {
        return new ItemsMenu(gui, presetName, newPage);
    }
}
