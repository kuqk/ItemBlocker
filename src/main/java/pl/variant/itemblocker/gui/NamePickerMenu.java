package pl.variant.itemblocker.gui;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import pl.variant.itemblocker.message.Message;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Picks worlds or regions by clicking them instead of typing their names. Nothing selected means
 * everywhere. Every click is saved right away.
 */
final class NamePickerMenu extends Menu {

    static final int MAX_NAMES = 45;
    static final int BACK = 45;
    static final int PREVIOUS = 48;
    static final int EVERYWHERE = 49;
    static final int NEXT = 50;
    static final int CLOSE = 53;

    private final Gui gui;
    private final List<String> names;
    private final Function<String, Material> icon;
    private final Supplier<Set<String>> selected;
    private final Consumer<Set<String>> save;
    private final Supplier<Menu> back;
    private int page;

    /**
     * @param names    the names to offer, such as every loaded world
     * @param icon     the icon of each name
     * @param selected the names currently selected, lowercase; empty means everywhere
     * @param save     saves a new selection
     */
    NamePickerMenu(Gui gui, Message title, List<String> names, Function<String, Material> icon,
                   Supplier<Set<String>> selected, Consumer<Set<String>> save, Supplier<Menu> back) {
        super(6, gui.text(title));
        this.gui = gui;
        this.names = List.copyOf(names);
        this.icon = icon;
        this.selected = selected;
        this.save = save;
        this.back = back;
    }

    @Override
    protected void render() {
        Inventory inventory = getInventory();
        inventory.clear();
        Set<String> current = selected.get();
        int first = page * MAX_NAMES;
        for (int slot = 0; slot < MAX_NAMES && first + slot < names.size(); slot++) {
            String name = names.get(first + slot);
            boolean on = current.contains(name.toLowerCase(Locale.ROOT));
            inventory.setItem(slot, Gui.icon(on ? icon.apply(name) : Material.GRAY_DYE,
                    (on ? ChatColor.GREEN : ChatColor.GRAY) + name,
                    gui.lines(on ? Message.GUI_SELECTED_LORE : Message.GUI_UNSELECTED_LORE)));
        }
        for (int slot = MAX_NAMES; slot < inventory.getSize(); slot++) {
            inventory.setItem(slot, Gui.filler());
        }
        inventory.setItem(BACK, gui.icon(Material.ARROW, Message.GUI_BACK));
        inventory.setItem(EVERYWHERE, Gui.icon(Material.COMPASS, gui.text(Message.GUI_EVERYWHERE_NAME), gui.lines(Message.GUI_EVERYWHERE_LORE)));
        inventory.setItem(CLOSE, gui.icon(Material.BARRIER, Message.GUI_CLOSE));
        if (page > 0) {
            inventory.setItem(PREVIOUS, gui.icon(Material.ARROW, Message.GUI_PREVIOUS));
        }
        if (names.size() > (page + 1) * MAX_NAMES) {
            inventory.setItem(NEXT, gui.icon(Material.ARROW, Message.GUI_NEXT));
        }
    }

    @Override
    protected void click(Player player, int slot, ClickType click) {
        int index = page * MAX_NAMES + slot;
        if (slot >= 0 && slot < MAX_NAMES && index < names.size()) {
            String name = names.get(index).toLowerCase(Locale.ROOT);
            Set<String> updated = new HashSet<>(selected.get());
            if (!updated.remove(name)) {
                updated.add(name);
            }
            save.accept(updated);
            render();
            return;
        }

        switch (slot) {
            case EVERYWHERE -> {
                save.accept(Set.of());
                render();
            }
            case BACK -> gui.open(player, back.get());
            case PREVIOUS -> {
                if (page > 0) {
                    page--;
                    render();
                }
            }
            case NEXT -> {
                if (names.size() > (page + 1) * MAX_NAMES) {
                    page++;
                    render();
                }
            }
            case CLOSE -> gui.close(player);
            default -> {
                // Filler does nothing.
            }
        }
    }
}
