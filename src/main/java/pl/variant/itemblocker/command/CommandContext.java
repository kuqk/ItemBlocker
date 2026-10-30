package pl.variant.itemblocker.command;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import pl.variant.itemblocker.ItemBlockerPlugin;
import pl.variant.itemblocker.message.Message;
import pl.variant.itemblocker.message.Placeholder;
import pl.variant.itemblocker.rule.Preset;
import pl.variant.itemblocker.rule.RuleBook;

import java.io.IOException;
import java.util.List;
import java.util.function.UnaryOperator;

/**
 * Everything a subcommand needs: who ran it, the arguments after the subcommand name and the
 * plugin services.
 *
 * @param sender who ran the command
 * @param label  the alias that was used, for usage messages
 * @param args   the arguments after the subcommand name
 * @param plugin the plugin
 */
public record CommandContext(CommandSender sender, String label, List<String> args, ItemBlockerPlugin plugin) {

    public CommandContext {
        args = List.copyOf(args);
    }

    public String arg(int index) {
        return index < args.size() ? args.get(index) : null;
    }

    /**
     * Returns the argument being completed: the last one, or an empty string if there is none.
     */
    public String lastArg() {
        return args.isEmpty() ? "" : args.get(args.size() - 1);
    }

    /**
     * Returns the arguments from the given index on.
     */
    public List<String> argsFrom(int index) {
        return index < args.size() ? args.subList(index, args.size()) : List.of();
    }

    public void requirePermission(String permission) {
        if (!sender.hasPermission(permission)) {
            throw new CommandException(Message.ERROR_NO_PERMISSION);
        }
    }

    public Player requirePlayer() {
        if (sender instanceof Player player) {
            return player;
        }
        throw new CommandException(Message.ERROR_PLAYERS_ONLY);
    }

    public CommandException usage(String usage) {
        return new CommandException(Message.ERROR_USAGE, Placeholder.of("label", label), Placeholder.of("usage", usage));
    }

    public RuleBook rules() {
        return plugin.rules().rules();
    }

    /**
     * Applies and saves a change to the rules.
     */
    public RuleBook updateRules(UnaryOperator<RuleBook> change) {
        try {
            return plugin.rules().update(change);
        } catch (IOException exception) {
            throw new CommandException(Message.ERROR_SAVE_FAILED, Placeholder.of("error", exception.getMessage()));
        }
    }

    /**
     * Applies and saves a change to one preset and returns the changed preset.
     */
    public Preset updatePreset(String name, UnaryOperator<Preset> change) {
        RuleBook updated = updateRules(rules -> rules.with(change.apply(rules.preset(name)
                .orElseThrow(() -> new CommandException(Message.ERROR_UNKNOWN_PRESET, Placeholder.of("preset", name))))));
        return updated.preset(name).orElseThrow();
    }

    public void send(Message message, Placeholder... placeholders) {
        plugin.messages().send(sender, message, placeholders);
    }

    public String render(Message message, Placeholder... placeholders) {
        return plugin.messages().render(message, placeholders);
    }
}
