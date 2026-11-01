package pl.variant.itemblocker.gui;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import pl.variant.itemblocker.message.Formats;
import pl.variant.itemblocker.message.Message;
import pl.variant.itemblocker.message.Placeholder;
import pl.variant.itemblocker.rule.Preset;
import pl.variant.itemblocker.rule.RuleBook;

import java.util.List;

/**
 * Every preset, one icon each. Clicking a preset opens it.
 */
final class PresetsMenu extends Menu {

    private static final int MAX_PRESETS = 45;

    private final Gui gui;
    private final List<String> presets;

    PresetsMenu(Gui gui) {
        this(gui, gui.rules().presets().stream().limit(MAX_PRESETS).map(Preset::name).toList());
    }

    private PresetsMenu(Gui gui, List<String> presets) {
        super(rowsFor(presets.size()), gui.text(Message.GUI_PRESETS_TITLE));
        this.gui = gui;
        this.presets = presets;
    }

    /**
     * Rows for the presets plus a bottom row for the close button.
     */
    private static int rowsFor(int presets) {
        return Math.min(6, (presets + 8) / 9 + 1);
    }

    private int closeSlot() {
        return getInventory().getSize() - 5;
    }

    @Override
    protected void render() {
        Inventory inventory = getInventory();
        inventory.clear();
        RuleBook rules = gui.rules();
        for (int slot = 0; slot < presets.size(); slot++) {
            int index = slot;
            rules.preset(presets.get(slot)).ifPresent(preset -> inventory.setItem(index, icon(preset)));
        }
        for (int slot = inventory.getSize() - 9; slot < inventory.getSize(); slot++) {
            inventory.setItem(slot, Gui.filler());
        }
        inventory.setItem(closeSlot(), gui.icon(Material.BARRIER, Message.GUI_CLOSE));
    }

    private ItemStack icon(Preset preset) {
        Material material = preset.name().equals(RuleBook.DEFAULT_PRESET) ? Material.ENCHANTED_BOOK : Material.BOOK;
        String description = preset.description().isBlank() ? gui.text(Message.PRESETS_NO_DESCRIPTION) : preset.description();
        return Gui.icon(material, gui.text(Message.GUI_PRESET_NAME, Placeholder.of("preset", preset.name())), gui.lines(Message.GUI_PRESET_LORE,
                Placeholder.of("description", description),
                Placeholder.of("worlds", Formats.worlds(gui.messages(), preset.scope())),
                Placeholder.of("regions", Formats.regions(gui.messages(), preset.regions())),
                Placeholder.of("items", preset.items().size()),
                Placeholder.of("limits", preset.limits().size()),
                Placeholder.of("enchantments", preset.enchantments().size()),
                Placeholder.of("potions", preset.effects().size())));
    }

    @Override
    protected void click(Player player, int slot, ClickType click) {
        if (slot >= 0 && slot < presets.size()) {
            gui.open(player, new PresetMenu(gui, presets.get(slot), 0));
        } else if (slot == closeSlot()) {
            gui.close(player);
        }
    }
}
