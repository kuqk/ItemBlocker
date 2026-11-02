package pl.variant.itemblocker.gui;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;
import pl.variant.itemblocker.gui.RuleTarget.GroupTarget;
import pl.variant.itemblocker.message.Formats;
import pl.variant.itemblocker.message.Message;
import pl.variant.itemblocker.message.Placeholder;
import pl.variant.itemblocker.rule.ItemPattern;
import pl.variant.itemblocker.rule.ItemRule;
import pl.variant.itemblocker.rule.Preset;

import java.util.List;
import java.util.Optional;

/**
 * The item groups of a preset. A new group asks for its pattern in the chat and blocks every action
 * until changed.
 */
final class GroupsMenu extends ListMenu<ItemPattern> {

    GroupsMenu(Gui gui, String presetName, int page) {
        super(gui, presetName, page, gui.text(Message.GUI_GROUPS_TITLE, Placeholder.of("preset", presetName)));
    }

    @Override
    protected List<ItemPattern> entries(Preset preset) {
        return List.copyOf(preset.groups().keySet());
    }

    @Override
    protected ItemStack icon(Preset preset, ItemPattern pattern) {
        ItemRule rule = preset.groups().get(pattern);
        return Gui.icon(new GroupTarget(pattern).icon(preset), ChatColor.WHITE + pattern.text(), gui.lines(Message.GUI_GROUP_LORE,
                Placeholder.of("actions", Formats.actions(gui.messages(), rule.actions())),
                Placeholder.of("worlds", Formats.worlds(gui.messages(), rule.scope()))));
    }

    @Override
    protected void clickEntry(Player player, ItemPattern pattern, ClickType click) {
        if (click == ClickType.SHIFT_RIGHT) {
            change(player, preset -> preset.withoutGroup(pattern));
        } else {
            gui.open(player, editor(pattern));
        }
    }

    @Override
    protected ItemStack addButton() {
        return Gui.icon(Material.CHEST, gui.text(Message.GUI_ADD_GROUP_NAME), gui.lines(Message.GUI_ADD_GROUP_LORE));
    }

    @Override
    protected void clickAdd(Player player) {
        gui.ask(player, Message.GUI_WHAT_PATTERN, atPage(page), answer -> {
            Optional<ItemPattern> pattern = ItemPattern.parse(answer);
            if (pattern.isEmpty()) {
                gui.send(player, Message.GROUP_INVALID, Placeholder.of("group", answer));
                atPage(page).open(player);
                return;
            }
            gui.updatePreset(player, presetName, preset -> preset.group(pattern.get()).isPresent()
                    ? preset
                    : preset.withGroup(pattern.get(), ItemRule.blockEverything()));
            editor(pattern.get()).open(player);
        });
    }

    private Menu editor(ItemPattern pattern) {
        return new RuleMenu(gui, presetName, new GroupTarget(pattern), () -> atPage(page));
    }

    @Override
    protected Menu atPage(int newPage) {
        return new GroupsMenu(gui, presetName, newPage);
    }
}
