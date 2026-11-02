package pl.variant.itemblocker.command;

import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import pl.variant.itemblocker.message.Message;
import pl.variant.itemblocker.message.Names;
import pl.variant.itemblocker.message.Placeholder;
import pl.variant.itemblocker.rule.ItemTypes;
import pl.variant.itemblocker.rule.Preset;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

/**
 * {@code /ib allow add|remove|list}: item types a preset's item rules and groups never block.
 */
final class AllowCommand implements Subcommand {

    private static final String ADD = "add";
    private static final String REMOVE = "remove";
    private static final String LIST = "list";
    private static final String USAGE = "allow <add|remove|list> <item|hand> [in:]";

    @Override
    public String name() {
        return "allow";
    }

    @Override
    public boolean isAvailableTo(CommandSender sender) {
        return sender.hasPermission(Permissions.ALLOW);
    }

    @Override
    public List<Message> help() {
        return List.of(Message.HELP_ALLOW);
    }

    @Override
    public void execute(CommandContext context) {
        context.requirePermission(Permissions.ALLOW);
        String action = Optional.ofNullable(context.arg(0)).orElseThrow(() -> context.usage(USAGE)).toLowerCase(Locale.ROOT);
        switch (action) {
            case ADD, REMOVE -> change(context, action.equals(ADD));
            case LIST -> list(context);
            default -> throw context.usage(USAGE);
        }
    }

    private static void change(CommandContext context, boolean add) {
        Material material = Arguments.item(context, Optional.ofNullable(context.arg(1)).orElseThrow(() -> context.usage(USAGE)));
        Preset preset = Arguments.preset(context, Options.parse(context.argsFrom(2), Set.of(Options.PRESET)));
        Placeholder item = Placeholder.of("item", Names.of(material));
        Placeholder presetName = Placeholder.of("preset", preset.name());
        if (!add && !preset.allowed().contains(material)) {
            throw new CommandException(Message.ALLOW_UNKNOWN, item, presetName);
        }

        context.updatePreset(preset.name(), current -> add ? current.withAllowed(material) : current.withoutAllowed(material));
        context.send(add ? Message.ALLOW_ADDED : Message.ALLOW_REMOVED, item, presetName);
    }

    private static void list(CommandContext context) {
        for (Preset preset : Arguments.presets(context, Options.parse(context.argsFrom(1), Set.of(Options.PRESET)))) {
            context.send(Message.LIST_ALLOWED_HEADER, Placeholder.of("preset", preset.name()));
            if (preset.allowed().isEmpty()) {
                context.send(Message.LIST_EMPTY);
            }
            preset.allowed().stream()
                    .sorted(Comparator.comparing(ItemTypes::key))
                    .forEach(material -> context.send(Message.LIST_ALLOWED_ENTRY, Placeholder.of("item", Names.of(material))));
        }
    }

    @Override
    public List<String> complete(CommandContext context) {
        List<String> args = context.args();
        if (args.size() == 1) {
            return Completions.matching(args.get(0), List.of(ADD, REMOVE, LIST));
        }

        String action = args.get(0).toLowerCase(Locale.ROOT);
        if (args.size() == 2 && action.equals(ADD)) {
            return Completions.items(args.get(1));
        }
        if (args.size() == 2 && action.equals(REMOVE)) {
            return Completions.matching(args.get(1), context.rules().presets().stream()
                    .flatMap(preset -> preset.allowed().stream())
                    .map(ItemTypes::key)
                    .distinct()
                    .toList());
        }
        return Completions.options(context, List.of(Options.PRESET));
    }
}
