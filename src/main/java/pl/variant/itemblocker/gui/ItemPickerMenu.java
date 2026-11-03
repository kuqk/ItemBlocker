package pl.variant.itemblocker.gui;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import pl.variant.itemblocker.message.Message;
import pl.variant.itemblocker.message.Names;
import pl.variant.itemblocker.message.Placeholder;
import pl.variant.itemblocker.rule.ItemTypes;

import java.util.List;
import java.util.Locale;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

/**
 * Every item, 45 per page, narrowed down by a search typed in the chat. The search matches both the
 * readable name and the key, so {@code chestplate}, {@code Diamond} and {@code ender_p} all work.
 */
final class ItemPickerMenu extends Menu {

    static final int PAGE_SIZE = 45;
    static final int BACK = 45;
    static final int PREVIOUS = 48;
    static final int SEARCH = 49;
    static final int NEXT = 50;
    static final int CLOSE = 53;

    private final Gui gui;
    private final String query;
    private final int page;
    private final BiConsumer<Player, Material> pick;
    private final Supplier<Menu> back;
    private final List<Material> matches;

    /**
     * @param query what the item names must contain, empty for every item
     * @param pick  what clicking an item does
     */
    ItemPickerMenu(Gui gui, String query, int page, BiConsumer<Player, Material> pick, Supplier<Menu> back) {
        super(6, gui.text(Message.GUI_PICKER_TITLE));
        this.gui = gui;
        this.query = query.trim().toLowerCase(Locale.ROOT);
        this.page = page;
        this.pick = pick;
        this.back = back;
        this.matches = ItemTypes.keys().stream()
                .map(key -> ItemTypes.parse(key).orElseThrow())
                .filter(this::matches)
                .toList();
    }

    private boolean matches(Material material) {
        if (query.isEmpty()) {
            return true;
        }
        String spaced = query.replace('_', ' ');
        return ItemTypes.key(material).contains(query.replace(' ', '_'))
                || Names.of(material).toLowerCase(Locale.ROOT).contains(spaced);
    }

    List<Material> matches() {
        return matches;
    }

    @Override
    protected void render() {
        Inventory inventory = getInventory();
        inventory.clear();
        List<Material> shown = shown();
        for (int slot = 0; slot < shown.size(); slot++) {
            Material material = shown.get(slot);
            inventory.setItem(slot, Gui.icon(material, ChatColor.WHITE + Names.of(material), gui.lines(Message.GUI_PICK_LORE)));
        }

        for (int slot = PAGE_SIZE; slot < inventory.getSize(); slot++) {
            inventory.setItem(slot, Gui.filler());
        }
        inventory.setItem(BACK, gui.icon(Material.ARROW, Message.GUI_BACK));
        if (page > 0) {
            inventory.setItem(PREVIOUS, gui.icon(Material.ARROW, Message.GUI_PREVIOUS));
        }
        inventory.setItem(SEARCH, Gui.icon(Material.SPYGLASS,
                gui.text(Message.GUI_SEARCH_NAME, Placeholder.of("query", query.isEmpty() ? "-" : query)),
                gui.lines(Message.GUI_SEARCH_LORE)));
        if (matches.size() > (page + 1) * PAGE_SIZE) {
            inventory.setItem(NEXT, gui.icon(Material.ARROW, Message.GUI_NEXT));
        }
        inventory.setItem(CLOSE, gui.icon(Material.BARRIER, Message.GUI_CLOSE));
    }

    private List<Material> shown() {
        return matches.stream().skip((long) page * PAGE_SIZE).limit(PAGE_SIZE).toList();
    }

    @Override
    protected void click(Player player, int slot, ClickType click) {
        List<Material> shown = shown();
        if (slot >= 0 && slot < shown.size()) {
            pick.accept(player, shown.get(slot));
            return;
        }

        switch (slot) {
            case BACK -> gui.open(player, back.get());
            case PREVIOUS -> {
                if (page > 0) {
                    gui.open(player, new ItemPickerMenu(gui, query, page - 1, pick, back));
                }
            }
            case NEXT -> {
                if (matches.size() > (page + 1) * PAGE_SIZE) {
                    gui.open(player, new ItemPickerMenu(gui, query, page + 1, pick, back));
                }
            }
            case SEARCH -> {
                if (click.isRightClick()) {
                    gui.open(player, new ItemPickerMenu(gui, "", 0, pick, back));
                } else {
                    gui.ask(player, Message.GUI_WHAT_SEARCH, this,
                            answer -> new ItemPickerMenu(gui, answer, 0, pick, back).open(player));
                }
            }
            case CLOSE -> gui.close(player);
            default -> {
                // Filler does nothing.
            }
        }
    }
}
