package pl.variant.itemblocker.gui;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import pl.variant.itemblocker.message.Message;
import pl.variant.itemblocker.message.Placeholder;
import pl.variant.itemblocker.rule.Preset;

import java.util.List;
import java.util.Map;

/**
 * The bundled preset templates. Clicking one creates a preset with its name and opens it.
 */
final class TemplatesMenu extends Menu {

    static final int FIRST = 10;
    static final int BACK = 18;
    static final int CLOSE = 26;

    private static final Map<String, Material> ICONS = Map.of(
            "survival", Material.GRASS_BLOCK,
            "lifesteal", Material.TOTEM_OF_UNDYING,
            "pvp", Material.DIAMOND_SWORD);

    private final Gui gui;
    private final List<Preset> templates;

    TemplatesMenu(Gui gui) {
        super(3, gui.text(Message.GUI_TEMPLATES_TITLE));
        this.gui = gui;
        this.templates = gui.templates().stream().limit(7).toList();
    }

    @Override
    protected void render() {
        Inventory inventory = getInventory();
        inventory.clear();
        for (int slot = 0; slot < inventory.getSize(); slot++) {
            inventory.setItem(slot, Gui.filler());
        }
        for (int index = 0; index < templates.size(); index++) {
            Preset template = templates.get(index);
            inventory.setItem(FIRST + index, Gui.icon(ICONS.getOrDefault(template.name(), Material.BOOK),
                    gui.text(Message.GUI_PRESET_NAME, Placeholder.of("preset", template.name())),
                    gui.lines(Message.GUI_TEMPLATE_LORE,
                            Placeholder.of("description", template.description()),
                            Placeholder.of("items", template.items().size() + template.groups().size()),
                            Placeholder.of("limits", template.limits().size()),
                            Placeholder.of("enchantments", template.enchantments().size()),
                            Placeholder.of("potions", template.effects().size()))));
        }
        inventory.setItem(BACK, gui.icon(Material.ARROW, Message.GUI_BACK));
        inventory.setItem(CLOSE, gui.icon(Material.BARRIER, Message.GUI_CLOSE));
    }

    @Override
    protected void click(Player player, int slot, ClickType click) {
        int index = slot - FIRST;
        if (index >= 0 && index < templates.size()) {
            Preset template = templates.get(index);
            if (gui.preset(template.name()).isPresent()) {
                gui.send(player, Message.PRESET_EXISTS, Placeholder.of("preset", template.name()));
            } else if (gui.updateRules(player, rules -> rules.with(template))) {
                gui.send(player, Message.TEMPLATE_CREATED, Placeholder.of("preset", template.name()),
                        Placeholder.of("template", template.name()));
                gui.open(player, new PresetMenu(gui, template.name()));
            }
            return;
        }

        switch (slot) {
            case BACK -> gui.open(player, new PresetsMenu(gui));
            case CLOSE -> gui.close(player);
            default -> {
                // Filler does nothing.
            }
        }
    }
}
