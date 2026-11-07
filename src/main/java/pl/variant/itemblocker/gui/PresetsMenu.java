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
 * Every preset, one icon each. Clicking a preset opens it; a button at the bottom creates a new one.
 */
final class PresetsMenu extends Menu {

    private static final int PAGE_SIZE = 45;
    static final int PREVIOUS = 48;
    static final int NEXT = 50;

    private final Gui gui;
    private final List<String> presets;
    private final int page;
    private final boolean hasNext;

    PresetsMenu(Gui gui) {
        this(gui, 0);
    }

    private PresetsMenu(Gui gui, int page) {
        this(gui, gui.rules().presets().stream().map(Preset::name).toList(), page);
    }

    private PresetsMenu(Gui gui, List<String> all, int page) {
        super(rowsFor(Math.min(PAGE_SIZE, all.size())), gui.text(Message.GUI_PRESETS_TITLE));
        this.gui = gui;
        this.page = Math.max(0, Math.min(page, Math.max(0, (all.size() - 1) / PAGE_SIZE)));
        this.presets = all.stream().skip((long) this.page * PAGE_SIZE).limit(PAGE_SIZE).toList();
        this.hasNext = all.size() > (this.page + 1) * PAGE_SIZE;
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

    private int createSlot() {
        return getInventory().getSize() - 9;
    }

    private int templatesSlot() {
        return getInventory().getSize() - 8;
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
        inventory.setItem(createSlot(), Gui.icon(Material.WRITABLE_BOOK, gui.text(Message.GUI_CREATE_NAME), gui.lines(Message.GUI_CREATE_LORE)));
        inventory.setItem(templatesSlot(), Gui.icon(Material.BOOKSHELF, gui.text(Message.GUI_TEMPLATES_NAME), gui.lines(Message.GUI_TEMPLATES_LORE)));
        inventory.setItem(closeSlot(), gui.icon(Material.BARRIER, Message.GUI_CLOSE));
        if (page > 0) {
            inventory.setItem(PREVIOUS, gui.icon(Material.ARROW, Message.GUI_PREVIOUS));
        }
        if (hasNext) {
            inventory.setItem(NEXT, gui.icon(Material.ARROW, Message.GUI_NEXT));
        }
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
            gui.open(player, new PresetMenu(gui, presets.get(slot)));
        } else if (slot == createSlot()) {
            gui.ask(player, Message.GUI_WHAT_PRESET_NAME, new PresetsMenu(gui, page), answer -> create(player, answer));
        } else if (slot == templatesSlot()) {
            gui.open(player, new TemplatesMenu(gui));
        } else if (slot == closeSlot()) {
            gui.close(player);
        } else if (slot == PREVIOUS && page > 0) {
            gui.open(player, new PresetsMenu(gui, page - 1));
        } else if (slot == NEXT && hasNext) {
            gui.open(player, new PresetsMenu(gui, page + 1));
        }
    }

    private void create(Player player, String name) {
        Placeholder preset = Placeholder.of("preset", name);
        if (!Preset.isValidName(name)) {
            gui.send(player, Message.PRESET_INVALID_NAME, preset);
            new PresetsMenu(gui).open(player);
        } else if (gui.preset(name).isPresent()) {
            gui.send(player, Message.PRESET_EXISTS, preset);
            new PresetsMenu(gui).open(player);
        } else if (gui.updateRules(player, rules -> rules.with(Preset.empty(name)))) {
            new PresetMenu(gui, Preset.normalizeName(name)).open(player);
        }
    }
}
