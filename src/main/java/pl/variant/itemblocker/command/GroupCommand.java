package pl.variant.itemblocker.command;

import org.bukkit.command.CommandSender;
import pl.variant.itemblocker.message.Formats;
import pl.variant.itemblocker.message.Message;
import pl.variant.itemblocker.message.Placeholder;
import pl.variant.itemblocker.rule.Action;
import pl.variant.itemblocker.rule.ItemPattern;
import pl.variant.itemblocker.rule.ItemRule;
import pl.variant.itemblocker.rule.Preset;
import pl.variant.itemblocker.rule.WorldScope;

import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

/**
 * {@code /ib group add|remove|list}: rules for every item type matching a pattern, such as
 * {@code *_sword} or the {@code #swords} tag.
 */
final class GroupCommand implements Subcommand {

    private static final String ADD = "add";
    private static final String REMOVE = "remove";
    private static final String LIST = "list";
    private static final String USAGE = "group <add|remove|list> <pattern> [actions:] [worlds:] [bypass:] [in:]";
    private static final List<String> EXAMPLES = List.of("*_sword", "*_axe", "netherite_*", "#swords", "#logs");

    @Override
    public String name() {
        return "group";
    }

    @Override
    public boolean isAvailableTo(CommandSender sender) {
        return sender.hasPermission(Permissions.GROUP);
    }

    @Override
    public List<Message> help() {
        return List.of(Message.HELP_GROUP);
    }

    @Override
    public void execute(CommandContext context) {
        context.requirePermission(Permissions.GROUP);
        String action = Optional.ofNullable(context.arg(0)).orElseThrow(() -> context.usage(USAGE)).toLowerCase(Locale.ROOT);
        switch (action) {
            case ADD -> add(context);
            case REMOVE -> remove(context);
            case LIST -> list(context);
            default -> throw context.usage(USAGE);
        }
    }

    private static void add(CommandContext context) {
        ItemPattern pattern = requirePattern(context);
        Options options = Options.parse(context.argsFrom(2), Set.of(Options.ACTIONS, Options.WORLDS, Options.BYPASS, Options.PRESET));
        ItemRule rule = new ItemRule(
                options.get(Options.ACTIONS).map(Arguments::actions).orElseGet(() -> EnumSet.allOf(Action.class)),
                options.get(Options.WORLDS).map(Arguments::worlds).orElse(WorldScope.ALL),
                options.get(Options.BYPASS).map(Arguments::bypassPermission).orElse(null));
        String presetName = Arguments.preset(context, options).name();

        context.updatePreset(presetName, preset -> preset.withGroup(pattern, rule));
        context.send(Message.GROUP_SAVED,
                Placeholder.of("group", pattern.text()),
                Placeholder.of("preset", presetName),
                Placeholder.of("actions", Formats.actions(context.messages(), rule.actions())),
                Placeholder.of("worlds", Formats.worlds(context.messages(), rule.scope())));
    }

    private static void remove(CommandContext context) {
        ItemPattern pattern = requirePattern(context);
        Preset preset = Arguments.preset(context, Options.parse(context.argsFrom(2), Set.of(Options.PRESET)));
        Placeholder group = Placeholder.of("group", pattern.text());
        if (preset.group(pattern).isEmpty()) {
            throw new CommandException(Message.GROUP_UNKNOWN, group, Placeholder.of("preset", preset.name()));
        }

        context.updatePreset(preset.name(), current -> current.withoutGroup(pattern));
        context.send(Message.GROUP_REMOVED, group, Placeholder.of("preset", preset.name()));
    }

    private static void list(CommandContext context) {
        for (Preset preset : Arguments.presets(context, Options.parse(context.argsFrom(1), Set.of(Options.PRESET)))) {
            context.send(Message.LIST_GROUPS_HEADER, Placeholder.of("preset", preset.name()));
            if (preset.groups().isEmpty()) {
                context.send(Message.LIST_EMPTY);
            }
            preset.groups().forEach((pattern, rule) -> context.send(Message.LIST_GROUP_ENTRY,
                    Placeholder.of("group", pattern.text()),
                    Placeholder.of("actions", Formats.actions(context.messages(), rule.actions())),
                    Placeholder.of("worlds", Formats.worlds(context.messages(), rule.scope()))));
        }
    }

    private static ItemPattern requirePattern(CommandContext context) {
        String input = Optional.ofNullable(context.arg(1)).orElseThrow(() -> context.usage(USAGE));
        return ItemPattern.parse(input)
                .orElseThrow(() -> new CommandException(Message.GROUP_INVALID, Placeholder.of("group", input)));
    }

    @Override
    public List<String> complete(CommandContext context) {
        List<String> args = context.args();
        if (args.size() == 1) {
            return Completions.matching(args.get(0), List.of(ADD, REMOVE, LIST));
        }

        String action = args.get(0).toLowerCase(Locale.ROOT);
        if (args.size() == 2 && action.equals(ADD)) {
            return Completions.matching(args.get(1), EXAMPLES);
        }
        if (args.size() == 2 && action.equals(REMOVE)) {
            return Completions.matching(args.get(1), context.rules().presets().stream()
                    .flatMap(preset -> preset.groups().keySet().stream())
                    .map(ItemPattern::text)
                    .distinct()
                    .toList());
        }
        if (action.equals(ADD)) {
            return Completions.options(context, List.of(Options.ACTIONS, Options.WORLDS, Options.BYPASS, Options.PRESET));
        }
        return Completions.options(context, List.of(Options.PRESET));
    }
}
