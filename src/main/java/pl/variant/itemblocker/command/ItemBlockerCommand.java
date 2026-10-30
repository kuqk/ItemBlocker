package pl.variant.itemblocker.command;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import pl.variant.itemblocker.ItemBlockerPlugin;
import pl.variant.itemblocker.message.Message;
import pl.variant.itemblocker.message.Placeholder;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * The {@code /itemblocker} command. Dispatches to subcommands and turns {@link CommandException}s
 * into messages.
 */
public final class ItemBlockerCommand implements TabExecutor {

    private static final String HELP = "help";

    private final ItemBlockerPlugin plugin;
    private final Map<String, Subcommand> subcommands = new LinkedHashMap<>();

    public ItemBlockerCommand(ItemBlockerPlugin plugin) {
        this.plugin = plugin;
        for (Subcommand subcommand : List.of(
                new BlockCommand(),
                new EditCommand(),
                new UnblockCommand(),
                new ShowCommand(),
                new ListCommand(),
                new PresetCommand(),
                new ReloadCommand()
        )) {
            subcommands.put(subcommand.name(), subcommand);
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0 || args[0].equalsIgnoreCase(HELP)) {
            sendHelp(sender, label);
            return true;
        }

        Subcommand subcommand = subcommands.get(args[0].toLowerCase(Locale.ROOT));
        if (subcommand == null) {
            plugin.messages().send(sender, Message.ERROR_UNKNOWN_COMMAND, Placeholder.of("label", label));
            return true;
        }

        try {
            subcommand.execute(context(sender, label, args));
        } catch (CommandException exception) {
            plugin.messages().send(sender, exception.userMessage(), exception.placeholders());
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        if (args.length <= 1) {
            List<String> names = new ArrayList<>();
            names.add(HELP);
            subcommands.values().stream()
                    .filter(subcommand -> subcommand.isAvailableTo(sender))
                    .map(Subcommand::name)
                    .forEach(names::add);
            return Completions.matching(args.length == 0 ? "" : args[0], names);
        }

        Subcommand subcommand = subcommands.get(args[0].toLowerCase(Locale.ROOT));
        if (subcommand == null || !subcommand.isAvailableTo(sender)) {
            return List.of();
        }

        try {
            return subcommand.complete(context(sender, label, args));
        } catch (CommandException exception) {
            return List.of();
        }
    }

    private void sendHelp(CommandSender sender, String label) {
        Placeholder labelPlaceholder = Placeholder.of("label", label);
        plugin.messages().send(sender, Message.HELP_HEADER, Placeholder.of("version", plugin.getDescription().getVersion()));
        subcommands.values().stream()
                .filter(subcommand -> subcommand.isAvailableTo(sender))
                .flatMap(subcommand -> subcommand.help().stream())
                .forEach(line -> plugin.messages().send(sender, line, labelPlaceholder));
        plugin.messages().send(sender, Message.HELP_OPTIONS);
    }

    private CommandContext context(CommandSender sender, String label, String[] args) {
        return new CommandContext(sender, label, Arrays.asList(args).subList(1, args.length), plugin);
    }
}
