package pl.variant.itemblocker.gui;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import pl.variant.itemblocker.message.Formats;
import pl.variant.itemblocker.message.Message;
import pl.variant.itemblocker.message.Names;
import pl.variant.itemblocker.message.Placeholder;
import pl.variant.itemblocker.rule.Action;
import pl.variant.itemblocker.rule.ItemLimit;
import pl.variant.itemblocker.rule.ItemRule;
import pl.variant.itemblocker.rule.ItemTypes;
import pl.variant.itemblocker.rule.Preset;
import pl.variant.itemblocker.rule.WorldScope;

import java.util.EnumSet;
import java.util.Optional;
import java.util.Set;
import java.util.function.IntUnaryOperator;
import java.util.function.UnaryOperator;

/**
 * Edits one item in a preset: which actions are blocked and its stack and inventory limits.
 */
final class ItemMenu extends Menu {

    static final int SUMMARY = 4;
    static final int FIRST_ACTION = 18;
    static final int STACK = 28;
    static final int INVENTORY = 30;
    static final int WORLDS = 32;
    static final int REMOVE = 34;
    static final int BACK = 36;
    static final int CLOSE = 44;

    private static final int SMALL_STEP = 1;
    private static final int BIG_STEP = 16;
    // A full inventory of full stacks; a larger limit could never be reached.
    private static final int MAX_INVENTORY_LIMIT = 36 * 64;

    private final Gui gui;
    private final String presetName;
    private final Material material;
    private final int returnPage;

    ItemMenu(Gui gui, String presetName, Material material, int returnPage) {
        super(5, gui.text(Message.GUI_ITEM_TITLE,
                Placeholder.of("item", Names.of(material)),
                Placeholder.of("preset", presetName)));
        this.gui = gui;
        this.presetName = presetName;
        this.material = material;
        this.returnPage = returnPage;
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
        inventory.setItem(SUMMARY, Gui.icon(material, ChatColor.WHITE + Names.of(material),
                gui.summary(Message.GUI_ITEM_LORE, preset, material)));

        Set<Action> blocked = preset.item(material).map(ItemRule::actions).orElseGet(Set::of);
        Action[] actions = Action.values();
        for (int index = 0; index < actions.length; index++) {
            boolean isBlocked = blocked.contains(actions[index]);
            inventory.setItem(FIRST_ACTION + index, Gui.icon(isBlocked ? Material.RED_DYE : Material.LIME_DYE,
                    gui.text(Message.GUI_ACTION_NAME, Placeholder.of("action", actions[index].key())),
                    gui.lines(isBlocked ? Message.GUI_ACTION_BLOCKED : Message.GUI_ACTION_ALLOWED)));
        }

        ItemLimit limit = preset.limit(material);
        inventory.setItem(STACK, Gui.icon(Material.HOPPER,
                gui.text(Message.GUI_STACK_NAME, Placeholder.of("limit", Formats.limit(gui.messages(), limit.stack()))),
                gui.lines(Message.GUI_LIMIT_LORE)));
        inventory.setItem(INVENTORY, Gui.icon(Material.CHEST,
                gui.text(Message.GUI_INVENTORY_NAME, Placeholder.of("limit", Formats.limit(gui.messages(), limit.inventory()))),
                gui.lines(Message.GUI_LIMIT_LORE)));
        WorldScope worlds = preset.item(material).map(ItemRule::scope).orElse(WorldScope.ALL);
        inventory.setItem(WORLDS, Gui.icon(Material.FILLED_MAP,
                gui.text(Message.GUI_WORLDS_NAME, Placeholder.of("worlds", Formats.worlds(gui.messages(), worlds))),
                gui.lines(Message.GUI_WORLDS_LORE, Placeholder.of("item_key", ItemTypes.key(material)))));
        inventory.setItem(REMOVE, Gui.icon(Material.LAVA_BUCKET, gui.text(Message.GUI_REMOVE_NAME), gui.lines(Message.GUI_REMOVE_LORE)));
        inventory.setItem(BACK, gui.icon(Material.ARROW, Message.GUI_BACK));
        inventory.setItem(CLOSE, gui.icon(Material.BARRIER, Message.GUI_CLOSE));
    }

    @Override
    protected void click(Player player, int slot, ClickType click) {
        if (gui.preset(presetName).isEmpty()) {
            gui.open(player, new PresetsMenu(gui));
            return;
        }

        int actionIndex = slot - FIRST_ACTION;
        if (actionIndex >= 0 && actionIndex < Action.values().length) {
            change(player, preset -> toggle(preset, Action.values()[actionIndex]));
            return;
        }

        switch (slot) {
            case STACK -> {
                int max = Math.max(0, material.getMaxStackSize() - 1);
                change(player, preset -> {
                    ItemLimit limit = preset.limit(material);
                    return preset.withLimit(material, limit.withStack(adjust(limit.stack(), click, max)));
                });
            }
            case INVENTORY -> change(player, preset -> {
                ItemLimit limit = preset.limit(material);
                return preset.withLimit(material, limit.withInventory(adjust(limit.inventory(), click, MAX_INVENTORY_LIMIT)));
            });
            case REMOVE -> {
                if (gui.updatePreset(player, presetName, preset -> preset.withoutItem(material).withLimit(material, ItemLimit.NONE))) {
                    gui.open(player, new PresetMenu(gui, presetName, returnPage));
                }
            }
            case BACK -> gui.open(player, new PresetMenu(gui, presetName, returnPage));
            case CLOSE -> gui.close(player);
            default -> {
                // Filler, the summary and the worlds hint do nothing.
            }
        }
    }

    private void change(Player player, UnaryOperator<Preset> change) {
        if (gui.updatePreset(player, presetName, change)) {
            render();
        }
    }

    /**
     * Blocks or allows one action. An item with no blocked actions left has no rule.
     */
    private Preset toggle(Preset preset, Action action) {
        Optional<ItemRule> rule = preset.item(material);
        EnumSet<Action> actions = EnumSet.noneOf(Action.class);
        rule.ifPresent(current -> actions.addAll(current.actions()));
        if (!actions.remove(action)) {
            actions.add(action);
        }

        if (actions.isEmpty()) {
            return preset.withoutItem(material);
        }
        return preset.withItem(material, new ItemRule(actions, rule.map(ItemRule::scope).orElse(WorldScope.ALL)));
    }

    /**
     * Left click adds one, right click takes one away, with shift sixteen. Zero means no limit.
     */
    static int adjust(int current, ClickType click, int max) {
        IntUnaryOperator step = switch (click) {
            case LEFT -> value -> value + SMALL_STEP;
            case RIGHT -> value -> value - SMALL_STEP;
            case SHIFT_LEFT -> value -> value + BIG_STEP;
            case SHIFT_RIGHT -> value -> value - BIG_STEP;
            default -> IntUnaryOperator.identity();
        };
        return Math.max(0, Math.min(max, step.applyAsInt(current)));
    }
}
