package pl.variant.itemblocker.gui;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;
import pl.variant.itemblocker.gui.RuleTarget.FilterTarget;
import pl.variant.itemblocker.message.Formats;
import pl.variant.itemblocker.message.Message;
import pl.variant.itemblocker.message.Placeholder;
import pl.variant.itemblocker.rule.FilterRule;
import pl.variant.itemblocker.rule.ItemFilter;
import pl.variant.itemblocker.rule.ItemRule;
import pl.variant.itemblocker.rule.Preset;

import java.util.List;

/**
 * The item filters of a preset. Clicking an item in the player's own inventory asks for an id in the
 * chat and creates a filter copied from that item, blocking every action until changed.
 */
final class FiltersMenu extends ListMenu<String> {

    FiltersMenu(Gui gui, String presetName, int page) {
        super(gui, presetName, page, gui.text(Message.GUI_FILTERS_TITLE, Placeholder.of("preset", presetName)));
    }

    @Override
    protected List<String> entries(Preset preset) {
        return List.copyOf(preset.filters().keySet());
    }

    @Override
    protected ItemStack icon(Preset preset, String id) {
        FilterRule rule = preset.filters().get(id);
        return Gui.icon(new FilterTarget(id).icon(preset), ChatColor.WHITE + id, gui.lines(Message.GUI_FILTER_LORE,
                Placeholder.of("match", rule.filter().describe()),
                Placeholder.of("actions", Formats.actions(gui.messages(), rule.rule().actions())),
                Placeholder.of("worlds", Formats.worlds(gui.messages(), rule.rule().scope()))));
    }

    @Override
    protected void clickEntry(Player player, String id, ClickType click) {
        if (click == ClickType.SHIFT_RIGHT) {
            change(player, preset -> preset.withoutFilter(id));
        } else {
            gui.open(player, editor(id));
        }
    }

    @Override
    protected ItemStack addButton() {
        return Gui.icon(Material.NAME_TAG, gui.text(Message.GUI_ADD_NAME), gui.lines(Message.GUI_ADD_FILTER_LORE));
    }

    @Override
    protected void clickOwnItem(Player player, ItemStack item) {
        ItemFilter filter = ItemFilter.of(item);
        if (filter == null) {
            return;
        }
        gui.ask(player, Message.GUI_WHAT_FILTER_ID, atPage(page), answer -> {
            if (!Preset.isValidName(answer)) {
                gui.send(player, Message.FILTER_INVALID_ID, Placeholder.of("filter", answer));
                atPage(page).open(player);
                return;
            }
            String id = Preset.normalizeName(answer);
            gui.updatePreset(player, presetName, preset -> preset.filter(id).isPresent()
                    ? preset
                    : preset.withFilter(id, new FilterRule(filter, ItemRule.blockEverything())));
            editor(id).open(player);
        });
    }

    private Menu editor(String id) {
        return new RuleMenu(gui, presetName, new FilterTarget(id), () -> atPage(page));
    }

    @Override
    protected Menu atPage(int newPage) {
        return new FiltersMenu(gui, presetName, newPage);
    }
}
