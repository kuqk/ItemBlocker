package pl.variant.itemblocker.command;

import org.bukkit.command.CommandSender;
import pl.variant.itemblocker.message.Message;
import pl.variant.itemblocker.message.Placeholder;

import java.io.IOException;
import java.util.List;

/**
 * {@code /ib reload}: reloads config.yml, the messages and rules.yml.
 */
final class ReloadCommand implements Subcommand {

    @Override
    public String name() {
        return "reload";
    }

    @Override
    public boolean isAvailableTo(CommandSender sender) {
        return sender.hasPermission(Permissions.RELOAD);
    }

    @Override
    public List<Message> help() {
        return List.of(Message.HELP_RELOAD);
    }

    @Override
    public void execute(CommandContext context) {
        context.requirePermission(Permissions.RELOAD);
        try {
            context.plugin().reload();
        } catch (IOException exception) {
            throw new CommandException(Message.ERROR_RELOAD_FAILED, Placeholder.of("error", exception.getMessage()));
        }
        context.send(Message.RELOADED);
    }

    @Override
    public List<String> complete(CommandContext context) {
        return List.of();
    }
}
