package pl.variant.itemblocker.command;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.command.CommandSender;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import pl.variant.itemblocker.message.Formats;
import pl.variant.itemblocker.message.Message;
import pl.variant.itemblocker.message.Placeholder;
import pl.variant.itemblocker.rule.Action;
import pl.variant.itemblocker.rule.FilterRule;
import pl.variant.itemblocker.rule.ItemFilter;
import pl.variant.itemblocker.rule.ItemRule;
import pl.variant.itemblocker.rule.Preset;
import pl.variant.itemblocker.rule.RegistryKeys;
import pl.variant.itemblocker.rule.WorldScope;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

/**
 * {@code /ib filter add|remove|list}: rules for items matched by more than their type.
 *
 * <p>{@code /ib filter add <id> hand} copies the held item's type, custom name, custom model data and
 * first persistent data entry, which is how plugins such as Nexo or ExecutableItems mark their items.
 * Options given next to {@code hand} replace what was copied.
 */
final class FilterCommand implements Subcommand {

    private static final String ADD = "add";
    private static final String REMOVE = "remove";
    private static final String LIST = "list";
    private static final String USAGE = "filter <add|remove|list> <id> [hand] [type:] [name:] [lore:] [model:] [data:] [enchantment:] [actions:] [worlds:] [bypass:] [in:]";
    private static final List<String> CRITERIA = List.of(
            Options.TYPE, Options.NAME, Options.LORE, Options.MODEL, Options.DATA, Options.ENCHANTMENT);
    // Namespaces of data the server itself adds, which never identifies a custom item.
    private static final Set<String> SERVER_NAMESPACES = Set.of("minecraft", "bukkit", "paper");

    @Override
    public String name() {
        return "filter";
    }

    @Override
    public boolean isAvailableTo(CommandSender sender) {
        return sender.hasPermission(Permissions.FILTER);
    }

    @Override
    public List<Message> help() {
        return List.of(Message.HELP_FILTER);
    }

    @Override
    public void execute(CommandContext context) {
        context.requirePermission(Permissions.FILTER);
        String action = Optional.ofNullable(context.arg(0)).orElseThrow(() -> context.usage(USAGE)).toLowerCase(Locale.ROOT);
        switch (action) {
            case ADD -> add(context);
            case REMOVE -> remove(context);
            case LIST -> list(context);
            default -> throw context.usage(USAGE);
        }
    }

    private static void add(CommandContext context) {
        String id = requireId(context);
        List<String> rest = new ArrayList<>(context.argsFrom(2));
        boolean fromHand = rest.removeIf(Arguments.HAND::equalsIgnoreCase);
        Set<String> keys = new HashSet<>(CRITERIA);
        keys.addAll(List.of(Options.ACTIONS, Options.WORLDS, Options.BYPASS, Options.PRESET));
        Options options = Options.parse(rest, keys);

        ItemFilter base = fromHand ? fromItem(context.requirePlayer().getInventory().getItemInMainHand()) : null;
        if (fromHand && base == null) {
            throw new CommandException(Message.ERROR_EMPTY_HAND);
        }
        ItemFilter filter = withOptions(context, base, options);
        if (filter.isEmpty()) {
            throw new CommandException(Message.FILTER_EMPTY);
        }

        ItemRule rule = new ItemRule(
                options.get(Options.ACTIONS).map(Arguments::actions).orElseGet(() -> EnumSet.allOf(Action.class)),
                options.get(Options.WORLDS).map(Arguments::worlds).orElse(WorldScope.ALL),
                options.get(Options.BYPASS).map(Arguments::bypassPermission).orElse(null));
        String presetName = Arguments.preset(context, options).name();
        context.updatePreset(presetName, preset -> preset.withFilter(id, new FilterRule(filter, rule)));
        context.send(Message.FILTER_SAVED,
                Placeholder.of("filter", Preset.normalizeName(id)),
                Placeholder.of("preset", presetName),
                Placeholder.of("match", filter.describe()),
                Placeholder.of("actions", Formats.actions(context.messages(), rule.actions())),
                Placeholder.of("worlds", Formats.worlds(context.messages(), rule.scope())));
    }

    private static void remove(CommandContext context) {
        String id = requireId(context);
        Options options = Options.parse(context.argsFrom(2), Set.of(Options.PRESET));
        Preset preset = Arguments.preset(context, options);
        Placeholder filter = Placeholder.of("filter", Preset.normalizeName(id));
        if (preset.filter(id).isEmpty()) {
            throw new CommandException(Message.FILTER_UNKNOWN, filter, Placeholder.of("preset", preset.name()));
        }

        context.updatePreset(preset.name(), current -> current.withoutFilter(id));
        context.send(Message.FILTER_REMOVED, filter, Placeholder.of("preset", preset.name()));
    }

    private static void list(CommandContext context) {
        for (Preset preset : Arguments.presets(context, Options.parse(context.argsFrom(1), Set.of(Options.PRESET)))) {
            context.send(Message.LIST_FILTERS_HEADER, Placeholder.of("preset", preset.name()));
            if (preset.filters().isEmpty()) {
                context.send(Message.LIST_EMPTY);
            }
            preset.filters().forEach((id, rule) -> context.send(Message.LIST_FILTER_ENTRY,
                    Placeholder.of("filter", id),
                    Placeholder.of("match", rule.filter().describe()),
                    Placeholder.of("actions", Formats.actions(context.messages(), rule.rule().actions())),
                    Placeholder.of("worlds", Formats.worlds(context.messages(), rule.rule().scope()))));
        }
    }

    private static String requireId(CommandContext context) {
        String id = Optional.ofNullable(context.arg(1)).orElseThrow(() -> context.usage(USAGE));
        if (!Preset.isValidName(id)) {
            throw new CommandException(Message.FILTER_INVALID_ID, Placeholder.of("filter", id));
        }
        return id;
    }

    /**
     * Copies what identifies the item: its type, custom name, custom model data and the first
     * persistent data entry that a plugin added.
     */
    @SuppressWarnings("deprecation")
    static ItemFilter fromItem(ItemStack item) {
        if (item == null || item.getType().isAir()) {
            return null;
        }
        if (!item.hasItemMeta()) {
            return new ItemFilter(item.getType(), null, null, null, null, null, null);
        }

        ItemMeta meta = item.getItemMeta();
        String name = meta.hasDisplayName() ? ChatColor.stripColor(meta.getDisplayName()) : null;
        Integer model = meta.hasCustomModelData() ? meta.getCustomModelData() : null;
        PersistentDataContainer data = meta.getPersistentDataContainer();
        Optional<NamespacedKey> key = data.getKeys().stream()
                .filter(candidate -> !SERVER_NAMESPACES.contains(candidate.getNamespace()))
                .filter(candidate -> data.has(candidate, PersistentDataType.STRING))
                .findFirst();
        return new ItemFilter(item.getType(), name, null, model,
                key.map(NamespacedKey::toString).orElse(null),
                key.map(found -> data.get(found, PersistentDataType.STRING)).orElse(null),
                null);
    }

    private static ItemFilter withOptions(CommandContext context, ItemFilter base, Options options) {
        Material type = options.get(Options.TYPE)
                .map(input -> Arguments.item(context, input))
                .orElse(base == null ? null : base.type());
        Integer model = options.get(Options.MODEL).map(FilterCommand::model).orElse(base == null ? null : base.model());
        String dataKey = base == null ? null : base.dataKey();
        String dataValue = base == null ? null : base.dataValue();
        Optional<String> data = options.get(Options.DATA);
        if (data.isPresent()) {
            int equals = data.get().indexOf('=');
            dataKey = equals < 0 ? data.get() : data.get().substring(0, equals);
            dataValue = equals < 0 ? null : data.get().substring(equals + 1);
        }
        String enchantment = options.get(Options.ENCHANTMENT)
                .map(input -> RegistryKeys.of(Arguments.enchantment(input)))
                .orElse(base == null ? null : base.enchantment());
        return new ItemFilter(type,
                options.get(Options.NAME).orElse(base == null ? null : base.name()),
                options.get(Options.LORE).orElse(base == null ? null : base.lore()),
                model, dataKey, dataValue, enchantment);
    }

    private static Integer model(String input) {
        try {
            return Integer.parseInt(input);
        } catch (NumberFormatException exception) {
            throw new CommandException(Message.ERROR_INVALID_MODEL, Placeholder.of("input", input));
        }
    }

    @Override
    public List<String> complete(CommandContext context) {
        List<String> args = context.args();
        if (args.size() == 1) {
            return Completions.matching(args.get(0), List.of(ADD, REMOVE, LIST));
        }

        String action = args.get(0).toLowerCase(Locale.ROOT);
        if (action.equals(REMOVE) && args.size() == 2) {
            return Completions.matching(args.get(1), context.rules().presets().stream()
                    .flatMap(preset -> preset.filters().keySet().stream())
                    .distinct()
                    .toList());
        }
        if (action.equals(ADD) && args.size() >= 3) {
            List<String> keys = Stream.concat(CRITERIA.stream(), Stream.of(Options.ACTIONS, Options.WORLDS, Options.BYPASS, Options.PRESET)).toList();
            List<String> options = new ArrayList<>(Completions.options(context, keys));
            if (Arguments.HAND.startsWith(context.lastArg().toLowerCase(Locale.ROOT))
                    && args.stream().noneMatch(Arguments.HAND::equalsIgnoreCase)) {
                options.add(0, Arguments.HAND);
            }
            return options;
        }
        if (action.equals(REMOVE) || action.equals(LIST)) {
            return Completions.options(context, List.of(Options.PRESET));
        }
        return List.of();
    }
}
