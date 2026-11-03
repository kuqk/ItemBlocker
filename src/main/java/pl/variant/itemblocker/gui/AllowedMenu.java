package pl.variant.itemblocker.gui;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;
import pl.variant.itemblocker.message.Message;
import pl.variant.itemblocker.message.Placeholder;
import pl.variant.itemblocker.rule.ItemTypes;
import pl.variant.itemblocker.rule.Preset;

import java.util.Comparator;
import java.util.List;

/**
 * The item types a preset's item rules and groups never block. Clicking an item in the player's own
 * inventory allows it, clicking an entry removes it.
 */
final class AllowedMenu extends ListMenu<Material> {

    AllowedMenu(Gui gui, String presetName, int page) {
        super(gui, presetName, page, gui.text(Message.GUI_ALLOWED_TITLE, Placeholder.of("preset", presetName)));
    }

    @Override
    protected List<Material> entries(Preset preset) {
        return preset.allowed().stream().sorted(Comparator.comparing(ItemTypes::key)).toList();
    }

    @Override
    protected ItemStack icon(Preset preset, Material material) {
        return Gui.icon(material, null, gui.lines(Message.GUI_ALLOWED_LORE));
    }

    @Override
    protected void clickEntry(Player player, Material material, ClickType click) {
        change(player, preset -> preset.withoutAllowed(material));
    }

    @Override
    protected ItemStack addButton() {
        return Gui.icon(Material.SPYGLASS, gui.text(Message.GUI_SEARCH_ITEMS_NAME), gui.lines(Message.GUI_ADD_ALLOWED_LORE));
    }

    @Override
    protected void clickAdd(Player player) {
        gui.open(player, new ItemPickerMenu(gui, "", 0, (picker, material) -> {
            if (gui.updatePreset(picker, presetName, preset -> preset.withAllowed(material))) {
                gui.open(picker, atPage(page));
            }
        }, () -> atPage(page)));
    }

    @Override
    protected void clickOwnItem(Player player, ItemStack item) {
        if (ItemTypes.isBlockable(item.getType())) {
            change(player, preset -> preset.withAllowed(item.getType()));
        }
    }

    @Override
    protected Menu atPage(int newPage) {
        return new AllowedMenu(gui, presetName, newPage);
    }
}
