package pl.variant.itemblocker.command;

import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import pl.variant.itemblocker.message.Message;
import pl.variant.itemblocker.message.Names;
import pl.variant.itemblocker.message.Placeholder;
import pl.variant.itemblocker.rule.ItemRule;
import pl.variant.itemblocker.rule.Preset;

import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * {@code /ib edit <item|hand> [actions:] [worlds:] [in:]}, replacing the given parts of an existing
 * item rule.
 */
final class EditCommand implements Subcommand {

    private static final String USAGE = "edit <item|hand> [actions:...] [worlds:...] [in:preset]";

    @Override
    public String name() {
        return "edit";
    }

    @Override
    public boolean isAvailableTo(CommandSender sender) {
        return sender.hasPermission(Permissions.EDIT);
    }

    @Override
    public List<Message> help() {
        return List.of(Message.HELP_EDIT);
    }

    @Override
    public void execute(CommandContext context) {
        context.requirePermission(Permissions.EDIT);
        String input = Optional.ofNullable(context.arg(0)).orElseThrow(() -> context.usage(USAGE));
        Material material = Arguments.item(context, input);
        Options options = Options.parse(context.argsFrom(1), Set.of(Options.ACTIONS, Options.WORLDS, Options.PRESET));
        if (options.get(Options.ACTIONS).isEmpty() && options.get(Options.WORLDS).isEmpty()) {
            throw context.usage(USAGE);
        }

        Preset current = Arguments.preset(context, options);
        if (current.item(material).isEmpty()) {
            throw new CommandException(Message.ITEM_NOT_BLOCKED,
                    Placeholder.of("item", Names.of(material)),
                    Placeholder.of("preset", current.name()));
        }

        Preset preset = context.updatePreset(current.name(), latest -> {
            ItemRule rule = latest.item(material).orElseThrow();
            rule = options.get(Options.ACTIONS).map(Arguments::actions).map(rule::withActions).orElse(rule);
            rule = options.get(Options.WORLDS).map(Arguments::worlds).map(rule::withScope).orElse(rule);
            return latest.withItem(material, rule);
        });

        ItemRule rule = preset.item(material).orElseThrow();
        context.send(Message.ITEM_UPDATED,
                Placeholder.of("item", Names.of(material)),
                Placeholder.of("preset", preset.name()),
                Placeholder.of("actions", Formats.actions(context, rule.actions())),
                Placeholder.of("worlds", Formats.worlds(context, rule.scope())));
    }

    @Override
    public List<String> complete(CommandContext context) {
        if (context.args().size() == 1) {
            return Completions.items(context.arg(0));
        }
        return Completions.options(context, List.of(Options.ACTIONS, Options.WORLDS, Options.PRESET));
    }
}
