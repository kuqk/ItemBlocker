package pl.variant.itemblocker.gui;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import pl.variant.itemblocker.message.Message;
import pl.variant.itemblocker.rule.Preset;

import java.util.List;
import java.util.Optional;
import java.util.function.UnaryOperator;

/**
 * A list of entries in one preset, 45 per page, with a bottom row for going back, paging and an
 * optional button to add entries.
 *
 * @param <T> the entry type
 */
abstract class ListMenu<T> extends Menu {

    static final int PAGE_SIZE = 45;
    static final int BACK = 45;
    static final int PREVIOUS = 48;
    static final int ADD = 49;
    static final int NEXT = 50;
    static final int CLOSE = 53;

    protected final Gui gui;
    protected final String presetName;
    protected final int page;
    private List<T> shown = List.of();

    ListMenu(Gui gui, String presetName, int page, String title) {
        super(6, title);
        this.gui = gui;
        this.presetName = presetName;
        this.page = page;
    }

    /**
     * Every entry of the preset, in display order.
     */
    protected abstract List<T> entries(Preset preset);

    protected abstract ItemStack icon(Preset preset, T entry);

    protected abstract void clickEntry(Player player, T entry, ClickType click);

    /**
     * The button in the middle of the bottom row, or {@code null} for none.
     */
    protected ItemStack addButton() {
        return null;
    }

    protected void clickAdd(Player player) {
    }

    /**
     * The same list on another page.
     */
    protected abstract Menu atPage(int newPage);

    protected Menu back() {
        return new PresetMenu(gui, presetName);
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
        List<T> all = entries(preset);
        shown = all.stream().skip((long) page * PAGE_SIZE).limit(PAGE_SIZE).toList();
        for (int slot = 0; slot < shown.size(); slot++) {
            inventory.setItem(slot, icon(preset, shown.get(slot)));
        }

        for (int slot = PAGE_SIZE; slot < inventory.getSize(); slot++) {
            inventory.setItem(slot, Gui.filler());
        }
        inventory.setItem(BACK, gui.icon(Material.ARROW, Message.GUI_BACK));
        if (page > 0) {
            inventory.setItem(PREVIOUS, gui.icon(Material.ARROW, Message.GUI_PREVIOUS));
        }
        ItemStack add = addButton();
        if (add != null) {
            inventory.setItem(ADD, add);
        }
        if (all.size() > (page + 1) * PAGE_SIZE) {
            inventory.setItem(NEXT, gui.icon(Material.ARROW, Message.GUI_NEXT));
        }
        inventory.setItem(CLOSE, gui.icon(Material.BARRIER, Message.GUI_CLOSE));
    }

    @Override
    protected void click(Player player, int slot, ClickType click) {
        if (gui.preset(presetName).isEmpty()) {
            gui.open(player, new PresetsMenu(gui));
            return;
        }
        if (slot >= 0 && slot < shown.size()) {
            clickEntry(player, shown.get(slot), click);
            return;
        }

        switch (slot) {
            case BACK -> gui.open(player, back());
            case PREVIOUS -> {
                if (page > 0) {
                    gui.open(player, atPage(page - 1));
                }
            }
            case NEXT -> {
                if (gui.preset(presetName).map(preset -> entries(preset).size() > (page + 1) * PAGE_SIZE).orElse(false)) {
                    gui.open(player, atPage(page + 1));
                }
            }
            case ADD -> clickAdd(player);
            case CLOSE -> gui.close(player);
            default -> {
                // Filler does nothing.
            }
        }
    }

    /**
     * Saves a change to the preset and shows the list again.
     */
    protected void change(Player player, UnaryOperator<Preset> change) {
        if (gui.updatePreset(player, presetName, change)) {
            render();
        }
    }
}
