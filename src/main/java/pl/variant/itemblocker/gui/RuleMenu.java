package pl.variant.itemblocker.gui;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import pl.variant.itemblocker.gui.RuleTarget.FilterTarget;
import pl.variant.itemblocker.gui.RuleTarget.GroupTarget;
import pl.variant.itemblocker.gui.RuleTarget.ItemTarget;
import pl.variant.itemblocker.message.Formats;
import pl.variant.itemblocker.message.Message;
import pl.variant.itemblocker.message.Names;
import pl.variant.itemblocker.message.Placeholder;
import pl.variant.itemblocker.rule.Action;
import pl.variant.itemblocker.rule.ItemFilter;
import pl.variant.itemblocker.rule.ItemLimit;
import pl.variant.itemblocker.rule.ItemRule;
import pl.variant.itemblocker.rule.ItemTypes;
import pl.variant.itemblocker.rule.Preset;
import pl.variant.itemblocker.rule.RegistryKeys;
import pl.variant.itemblocker.rule.WorldScope;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.IntUnaryOperator;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

/**
 * Edits one rule: which actions it blocks, its worlds and bypass permission, the stack and inventory
 * limits of an item type, and the criteria of a filter.
 */
final class RuleMenu extends Menu {

    static final int SUMMARY = 4;
    // One button per action across the second and third row.
    static final int FIRST_ACTION = 9;
    static final int WORLDS = 29;
    static final int BYPASS = 31;
    static final int STACK = 33;
    static final int INVENTORY = 35;
    static final int FIRST_CRITERION = 37;
    static final int BACK = 45;
    static final int REMOVE = 49;
    static final int CLOSE = 53;

    /**
     * Filter criteria in the order of their buttons.
     */
    enum Criterion {
        TYPE("Type", Material.ITEM_FRAME),
        NAME("Name", Material.NAME_TAG),
        LORE("Lore", Material.WRITABLE_BOOK),
        MODEL("Model", Material.PAINTING),
        DATA("Data", Material.KNOWLEDGE_BOOK),
        ENCHANTMENT("Enchantment", Material.ENCHANTED_BOOK);

        private final String label;
        private final Material icon;

        Criterion(String label, Material icon) {
            this.label = label;
            this.icon = icon;
        }
    }

    private static final int SMALL_STEP = 1;
    private static final int BIG_STEP = 16;
    // A full inventory of full stacks; a larger limit could never be reached.
    private static final int MAX_INVENTORY_LIMIT = 36 * 64;
    // Lore lines at the end of list entries that only make sense in the list: a blank line and two hints.
    private static final int LIST_HINT_LINES = 3;

    private final Gui gui;
    private final String presetName;
    private final RuleTarget target;
    private final Supplier<Menu> back;

    RuleMenu(Gui gui, String presetName, RuleTarget target, Supplier<Menu> back) {
        super(6, title(gui, presetName, target));
        this.gui = gui;
        this.presetName = presetName;
        this.target = target;
        this.back = back;
    }

    private static String title(Gui gui, String presetName, RuleTarget target) {
        if (target instanceof ItemTarget item) {
            return gui.text(Message.GUI_ITEM_TITLE, Placeholder.of("item", Names.of(item.material())), Placeholder.of("preset", presetName));
        }
        if (target instanceof GroupTarget group) {
            return gui.text(Message.GUI_GROUP_TITLE, Placeholder.of("group", group.pattern().text()));
        }
        return gui.text(Message.GUI_FILTER_TITLE, Placeholder.of("filter", ((FilterTarget) target).id()));
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
        inventory.setItem(SUMMARY, summary(preset));

        Optional<ItemRule> rule = target.rule(preset);
        Set<Action> blocked = rule.map(ItemRule::actions).orElseGet(Set::of);
        Action[] actions = Action.values();
        for (int index = 0; index < actions.length; index++) {
            boolean isBlocked = blocked.contains(actions[index]);
            inventory.setItem(FIRST_ACTION + index, Gui.icon(isBlocked ? Material.RED_DYE : Material.LIME_DYE,
                    gui.text(Message.GUI_ACTION_NAME, Placeholder.of("action", actions[index].key())),
                    gui.lines(isBlocked ? Message.GUI_ACTION_BLOCKED : Message.GUI_ACTION_ALLOWED)));
        }

        WorldScope worlds = rule.map(ItemRule::scope).orElse(WorldScope.ALL);
        inventory.setItem(WORLDS, Gui.icon(Material.FILLED_MAP,
                gui.text(Message.GUI_WORLDS_NAME, Placeholder.of("worlds", Formats.worlds(gui.messages(), worlds))),
                gui.lines(Message.GUI_CHANGE_LORE)));
        String permission = rule.map(ItemRule::bypassPermission).orElse(null);
        inventory.setItem(BYPASS, Gui.icon(Material.TRIPWIRE_HOOK,
                gui.text(Message.GUI_BYPASS_NAME, Placeholder.of("permission", permission == null ? gui.text(Message.WORD_NONE) : permission)),
                gui.lines(Message.GUI_CLEAR_LORE)));

        if (target instanceof ItemTarget item) {
            ItemLimit limit = preset.limit(item.material());
            inventory.setItem(STACK, Gui.icon(Material.HOPPER,
                    gui.text(Message.GUI_STACK_NAME, Placeholder.of("limit", Formats.limit(gui.messages(), limit.stack()))),
                    gui.lines(Message.GUI_LIMIT_LORE)));
            inventory.setItem(INVENTORY, Gui.icon(Material.CHEST,
                    gui.text(Message.GUI_INVENTORY_NAME, Placeholder.of("limit", Formats.limit(gui.messages(), limit.inventory()))),
                    gui.lines(Message.GUI_LIMIT_LORE)));
        }
        if (target instanceof FilterTarget filter) {
            filter.filter(preset).ifPresent(current -> {
                for (Criterion criterion : Criterion.values()) {
                    String value = valueOf(current, criterion);
                    inventory.setItem(FIRST_CRITERION + criterion.ordinal(), Gui.icon(criterion.icon,
                            gui.text(Message.GUI_CRITERION_NAME, Placeholder.of("criterion", criterion.label),
                                    Placeholder.of("value", value == null ? gui.text(Message.WORD_NONE) : value)),
                            gui.lines(Message.GUI_CLEAR_LORE)));
                }
            });
        }

        inventory.setItem(BACK, gui.icon(Material.ARROW, Message.GUI_BACK));
        inventory.setItem(REMOVE, Gui.icon(Material.LAVA_BUCKET, gui.text(Message.GUI_REMOVE_NAME), gui.lines(Message.GUI_REMOVE_LORE)));
        inventory.setItem(CLOSE, gui.icon(Material.BARRIER, Message.GUI_CLOSE));
    }

    private ItemStack summary(Preset preset) {
        Material icon = target.icon(preset);
        if (target instanceof ItemTarget item) {
            return Gui.icon(icon, ChatColor.WHITE + Names.of(item.material()), gui.summary(Message.GUI_ITEM_LORE, preset, item.material()));
        }
        if (target instanceof GroupTarget group) {
            return Gui.icon(icon, ChatColor.WHITE + group.pattern().text(), withoutHints(gui.lines(Message.GUI_GROUP_LORE,
                    Placeholder.of("actions", actionsText(preset)), Placeholder.of("worlds", worldsText(preset)))));
        }
        FilterTarget filter = (FilterTarget) target;
        return Gui.icon(icon, ChatColor.WHITE + filter.id(), withoutHints(gui.lines(Message.GUI_FILTER_LORE,
                Placeholder.of("match", filter.filter(preset).map(ItemFilter::describe).orElse("")),
                Placeholder.of("actions", actionsText(preset)), Placeholder.of("worlds", worldsText(preset)))));
    }

    private String actionsText(Preset preset) {
        return Formats.actions(gui.messages(), target.rule(preset).map(ItemRule::actions).orElseGet(Set::of));
    }

    private String worldsText(Preset preset) {
        return Formats.worlds(gui.messages(), target.rule(preset).map(ItemRule::scope).orElse(WorldScope.ALL));
    }

    private static List<String> withoutHints(List<String> lore) {
        return new ArrayList<>(lore.subList(0, Math.max(0, lore.size() - LIST_HINT_LINES)));
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
        int criterionIndex = slot - FIRST_CRITERION;
        if (target instanceof FilterTarget filter && criterionIndex >= 0 && criterionIndex < Criterion.values().length) {
            askCriterion(player, filter, Criterion.values()[criterionIndex]);
            return;
        }

        switch (slot) {
            case WORLDS -> askForRule(player, Message.GUI_WHAT_WORLDS,
                    (rule, answer) -> rule.withScope(WorldScope.of(List.of(answer.split("[,\\s]+")))));
            case BYPASS -> askForRule(player, Message.GUI_WHAT_PERMISSION,
                    (rule, answer) -> rule.withBypassPermission(answer.equalsIgnoreCase("none") ? null : answer));
            case STACK, INVENTORY -> {
                if (target instanceof ItemTarget item) {
                    changeLimit(player, item.material(), slot == STACK, click);
                }
            }
            case REMOVE -> {
                if (gui.updatePreset(player, presetName, target::without)) {
                    gui.open(player, back.get());
                }
            }
            case BACK -> gui.open(player, back.get());
            case CLOSE -> gui.close(player);
            default -> {
                // Filler and the summary do nothing.
            }
        }
    }

    private void change(Player player, UnaryOperator<Preset> change) {
        if (gui.updatePreset(player, presetName, change)) {
            render();
        }
    }

    /**
     * Blocks or allows one action. An item type with no blocked actions left has no rule.
     */
    private Preset toggle(Preset preset, Action action) {
        Optional<ItemRule> rule = target.rule(preset);
        EnumSet<Action> actions = EnumSet.noneOf(Action.class);
        rule.ifPresent(current -> actions.addAll(current.actions()));
        if (!actions.remove(action)) {
            actions.add(action);
        }

        if (actions.isEmpty() && target instanceof ItemTarget item) {
            return preset.withoutItem(item.material());
        }
        ItemRule updated = rule.map(current -> current.withActions(actions)).orElseGet(() -> new ItemRule(actions, WorldScope.ALL));
        return target.withRule(preset, updated);
    }

    private interface RuleChange {
        ItemRule apply(ItemRule rule, String answer);
    }

    /**
     * Asks for text that changes the rule. Items without a rule have nothing to change yet.
     */
    private void askForRule(Player player, Message what, RuleChange change) {
        if (gui.preset(presetName).flatMap(target::rule).isEmpty()) {
            return;
        }
        gui.ask(player, what, reopened(), answer -> {
            gui.updatePreset(player, presetName, preset -> target.rule(preset)
                    .map(rule -> target.withRule(preset, change.apply(rule, answer)))
                    .orElse(preset));
            reopened().open(player);
        });
    }

    private void askCriterion(Player player, FilterTarget filter, Criterion criterion) {
        gui.ask(player, Message.GUI_WHAT_VALUE, reopened(), answer -> {
            gui.preset(presetName).flatMap(filter::filter).ifPresent(current -> {
                String value = answer.equalsIgnoreCase("none") ? null : answer;
                Optional<ItemFilter> updated = withCriterion(player, current, criterion, value);
                if (updated.isEmpty()) {
                    return;
                }
                if (updated.get().isEmpty()) {
                    gui.send(player, Message.FILTER_EMPTY);
                } else {
                    gui.updatePreset(player, presetName, preset -> filter.withFilter(preset, updated.get()));
                }
            });
            reopened().open(player);
        });
    }

    /**
     * Returns the filter with one criterion replaced, or empty if the value is not valid, after
     * telling the player why.
     */
    private Optional<ItemFilter> withCriterion(Player player, ItemFilter filter, Criterion criterion, String value) {
        Material type = filter.type();
        String name = filter.name();
        String lore = filter.lore();
        Integer model = filter.model();
        String dataKey = filter.dataKey();
        String dataValue = filter.dataValue();
        String enchantment = filter.enchantment();
        switch (criterion) {
            case TYPE -> {
                type = value == null ? null : ItemTypes.parse(value).orElse(null);
                if (value != null && type == null) {
                    gui.send(player, Message.ERROR_UNKNOWN_ITEM, Placeholder.of("input", value));
                    return Optional.empty();
                }
            }
            case NAME -> name = value;
            case LORE -> lore = value;
            case MODEL -> {
                try {
                    model = value == null ? null : Integer.parseInt(value);
                } catch (NumberFormatException exception) {
                    gui.send(player, Message.ERROR_INVALID_MODEL, Placeholder.of("input", value));
                    return Optional.empty();
                }
            }
            case DATA -> {
                int equals = value == null ? -1 : value.indexOf('=');
                dataKey = equals < 0 ? value : value.substring(0, equals);
                dataValue = equals < 0 ? null : value.substring(equals + 1);
            }
            case ENCHANTMENT -> {
                enchantment = value == null ? null : RegistryKeys.normalize(value);
                if (enchantment != null && !RegistryKeys.enchantments().contains(enchantment)) {
                    gui.send(player, Message.ERROR_UNKNOWN_ENCHANTMENT, Placeholder.of("input", value));
                    return Optional.empty();
                }
            }
        }
        return Optional.of(new ItemFilter(type, name, lore, model, dataKey, dataValue, enchantment));
    }

    private static String valueOf(ItemFilter filter, Criterion criterion) {
        return switch (criterion) {
            case TYPE -> filter.type() == null ? null : ItemTypes.key(filter.type());
            case NAME -> filter.name();
            case LORE -> filter.lore();
            case MODEL -> filter.model() == null ? null : filter.model().toString();
            case DATA -> filter.dataKey() == null ? null
                    : filter.dataValue() == null ? filter.dataKey() : filter.dataKey() + "=" + filter.dataValue();
            case ENCHANTMENT -> filter.enchantment();
        };
    }

    private void changeLimit(Player player, Material material, boolean stack, ClickType click) {
        int max = stack ? Math.max(0, material.getMaxStackSize() - 1) : MAX_INVENTORY_LIMIT;
        change(player, preset -> {
            ItemLimit limit = preset.limit(material);
            return preset.withLimit(material, stack
                    ? limit.withStack(adjust(limit.stack(), click, max))
                    : limit.withInventory(adjust(limit.inventory(), click, max)));
        });
    }

    private Menu reopened() {
        return new RuleMenu(gui, presetName, target, back);
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
