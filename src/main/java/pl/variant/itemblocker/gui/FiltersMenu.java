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
import pl.variant.itemblocker.rule.ItemTypes;
import pl.variant.itemblocker.rule.Preset;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * The item filters of a preset. Clicking an item in the player's own inventory creates a filter
 * copied from that item, named after it and blocking every action until changed.
 */
final class FiltersMenu extends ListMenu<String> {

    // Leaves room for a number after it within the 32 characters an id may have.
    private static final int MAX_BASE_LENGTH = 28;

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
        Optional<Preset> preset = gui.preset(presetName);
        if (filter == null || preset.isEmpty()) {
            return;
        }
        String id = idFor(filter, preset.get());
        if (gui.updatePreset(player, presetName, current -> current.withFilter(id, new FilterRule(filter, ItemRule.blockEverything())))) {
            gui.open(player, editor(id));
        }
    }

    /**
     * An id made from the item's name, its plugin data or its type, unique in the preset, such as
     * {@code excalibur} or {@code excalibur-2}.
     */
    static String idFor(ItemFilter filter, Preset preset) {
        String source = filter.name() != null ? filter.name()
                : filter.dataValue() != null ? filter.dataValue()
                : ItemTypes.key(filter.type());
        String base = source.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "-").replaceAll("^-+|-+$", "");
        if (base.isEmpty()) {
            base = "filter";
        }
        base = base.substring(0, Math.min(base.length(), MAX_BASE_LENGTH));

        String id = base;
        for (int number = 2; preset.filter(id).isPresent(); number++) {
            id = base + "-" + number;
        }
        return id;
    }

    private Menu editor(String id) {
        return new RuleMenu(gui, presetName, new FilterTarget(id), () -> atPage(page));
    }

    @Override
    protected Menu atPage(int newPage) {
        return new FiltersMenu(gui, presetName, newPage);
    }
}
