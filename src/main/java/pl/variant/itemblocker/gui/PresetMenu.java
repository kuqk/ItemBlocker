package pl.variant.itemblocker.gui;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import pl.variant.itemblocker.message.Message;
import pl.variant.itemblocker.message.Names;
import pl.variant.itemblocker.message.Placeholder;
import pl.variant.itemblocker.rule.ItemLimit;
import pl.variant.itemblocker.rule.ItemTypes;
import pl.variant.itemblocker.rule.Preset;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * The items of one preset that have a rule or a limit, 45 per page. Clicking an item in the
 * player's own inventory adds it.
 */
final class PresetMenu extends Menu {

    static final int PAGE_SIZE = 45;
    static final int BACK = 45;
    static final int PREVIOUS = 48;
    static final int ADD = 49;
    static final int NEXT = 50;
    static final int CLOSE = 53;

    private final Gui gui;
    private final String presetName;
    private final int page;
    private List<Material> entries = List.of();

    PresetMenu(Gui gui, String presetName, int page) {
        super(6, gui.text(Message.GUI_PRESET_TITLE, Placeholder.of("preset", presetName)));
        this.gui = gui;
        this.presetName = presetName;
        this.page = page;
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
        List<Material> all = itemsOf(preset);
        entries = all.stream().skip((long) page * PAGE_SIZE).limit(PAGE_SIZE).toList();
        for (int slot = 0; slot < entries.size(); slot++) {
            inventory.setItem(slot, icon(preset, entries.get(slot)));
        }

        for (int slot = PAGE_SIZE; slot < inventory.getSize(); slot++) {
            inventory.setItem(slot, Gui.filler());
        }
        inventory.setItem(BACK, gui.icon(Material.ARROW, Message.GUI_BACK));
        if (page > 0) {
            inventory.setItem(PREVIOUS, gui.icon(Material.ARROW, Message.GUI_PREVIOUS));
        }
        inventory.setItem(ADD, Gui.icon(Material.NAME_TAG, gui.text(Message.GUI_ADD_NAME), gui.lines(Message.GUI_ADD_LORE)));
        if (all.size() > (page + 1) * PAGE_SIZE) {
            inventory.setItem(NEXT, gui.icon(Material.ARROW, Message.GUI_NEXT));
        }
        inventory.setItem(CLOSE, gui.icon(Material.BARRIER, Message.GUI_CLOSE));
    }

    /**
     * Every item with a rule or a limit in the preset, sorted by key.
     */
    static List<Material> itemsOf(Preset preset) {
        return Stream.concat(preset.items().keySet().stream(), preset.limits().keySet().stream())
                .distinct()
                .sorted(Comparator.comparing(ItemTypes::key))
                .toList();
    }

    private ItemStack icon(Preset preset, Material material) {
        return Gui.icon(material, ChatColor.WHITE + Names.of(material), gui.summary(Message.GUI_ENTRY_LORE, preset, material));
    }

    @Override
    protected void click(Player player, int slot, ClickType click) {
        if (slot >= 0 && slot < entries.size()) {
            Material material = entries.get(slot);
            if (click == ClickType.SHIFT_RIGHT) {
                if (gui.updatePreset(player, presetName, preset -> preset.withoutItem(material).withLimit(material, ItemLimit.NONE))) {
                    render();
                }
            } else {
                gui.open(player, new ItemMenu(gui, presetName, material, page));
            }
            return;
        }

        switch (slot) {
            case BACK -> gui.open(player, new PresetsMenu(gui));
            case PREVIOUS -> {
                if (page > 0) {
                    gui.open(player, new PresetMenu(gui, presetName, page - 1));
                }
            }
            case NEXT -> {
                if (gui.preset(presetName).map(preset -> itemsOf(preset).size() > (page + 1) * PAGE_SIZE).orElse(false)) {
                    gui.open(player, new PresetMenu(gui, presetName, page + 1));
                }
            }
            case CLOSE -> gui.close(player);
            default -> {
                // Filler and the add hint do nothing.
            }
        }
    }

    @Override
    protected void clickOwnItem(Player player, ItemStack item) {
        if (ItemTypes.isBlockable(item.getType())) {
            gui.open(player, new ItemMenu(gui, presetName, item.getType(), page));
        }
    }
}
