package pl.variant.itemblocker.gui;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import pl.variant.itemblocker.message.Formats;
import pl.variant.itemblocker.message.Message;
import pl.variant.itemblocker.message.Placeholder;
import pl.variant.itemblocker.rule.Preset;
import pl.variant.itemblocker.rule.RegionScope;
import pl.variant.itemblocker.rule.WorldScope;

import java.util.List;
import java.util.Optional;
import java.util.function.BiFunction;

/**
 * The description, reason, worlds and WorldGuard regions of a preset, each changed by typing in the
 * chat.
 */
final class SettingsMenu extends Menu {

    static final int DESCRIPTION = 10;
    static final int REASON = 12;
    static final int WORLDS = 14;
    static final int REGIONS = 16;
    static final int BACK = 18;
    static final int CLOSE = 26;

    private final Gui gui;
    private final String presetName;

    SettingsMenu(Gui gui, String presetName) {
        super(3, gui.text(Message.GUI_SETTINGS_TITLE, Placeholder.of("preset", presetName)));
        this.gui = gui;
        this.presetName = presetName;
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
        for (int slot = 0; slot < inventory.getSize(); slot++) {
            inventory.setItem(slot, Gui.filler());
        }
        String none = gui.text(Message.WORD_NONE);
        inventory.setItem(DESCRIPTION, Gui.icon(Material.BOOK, gui.text(Message.GUI_DESCRIPTION_NAME,
                Placeholder.of("description", preset.description().isBlank() ? none : preset.description())), gui.lines(Message.GUI_CLEAR_LORE)));
        inventory.setItem(REASON, Gui.icon(Material.PAPER, gui.text(Message.GUI_REASON_NAME,
                Placeholder.of("reason", preset.reason().isBlank() ? none : preset.reason())), gui.lines(Message.GUI_CLEAR_LORE)));
        inventory.setItem(WORLDS, Gui.icon(Material.FILLED_MAP, gui.text(Message.GUI_WORLDS_NAME,
                Placeholder.of("worlds", Formats.worlds(gui.messages(), preset.scope()))), gui.lines(Message.GUI_CHANGE_LORE)));
        inventory.setItem(REGIONS, Gui.icon(Material.COMPASS, gui.text(Message.GUI_REGIONS_NAME,
                Placeholder.of("regions", Formats.regions(gui.messages(), preset.regions()))), gui.lines(Message.GUI_CHANGE_LORE)));
        inventory.setItem(BACK, gui.icon(Material.ARROW, Message.GUI_BACK));
        inventory.setItem(CLOSE, gui.icon(Material.BARRIER, Message.GUI_CLOSE));
    }

    @Override
    protected void click(Player player, int slot, ClickType click) {
        switch (slot) {
            case DESCRIPTION -> ask(player, Message.GUI_WHAT_DESCRIPTION, (preset, answer) -> preset.withDescription(textOrEmpty(answer)));
            case REASON -> ask(player, Message.GUI_WHAT_REASON, (preset, answer) -> preset.withReason(textOrEmpty(answer)));
            case WORLDS -> ask(player, Message.GUI_WHAT_WORLDS, (preset, answer) -> preset.withScope(WorldScope.of(split(answer))));
            case REGIONS -> ask(player, Message.GUI_WHAT_REGIONS, (preset, answer) -> preset.withRegions(RegionScope.of(split(answer))));
            case BACK -> gui.open(player, new PresetMenu(gui, presetName));
            case CLOSE -> gui.close(player);
            default -> {
                // Filler does nothing.
            }
        }
    }

    private void ask(Player player, Message what, BiFunction<Preset, String, Preset> change) {
        gui.ask(player, what, new SettingsMenu(gui, presetName), answer -> {
            gui.updatePreset(player, presetName, preset -> change.apply(preset, answer));
            new SettingsMenu(gui, presetName).open(player);
        });
    }

    private static String textOrEmpty(String answer) {
        return answer.equalsIgnoreCase("none") ? "" : answer;
    }

    private static List<String> split(String answer) {
        return List.of(answer.split("[,\\s]+"));
    }
}
