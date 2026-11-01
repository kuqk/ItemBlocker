package pl.variant.itemblocker.command;

import org.bukkit.command.CommandSender;
import pl.variant.itemblocker.message.Message;

import java.util.List;

/**
 * {@code /ib gui}: opens the menu for managing presets, item rules and limits.
 */
final class GuiCommand implements Subcommand {

    @Override
    public String name() {
        return "gui";
    }

    @Override
    public boolean isAvailableTo(CommandSender sender) {
        return sender.hasPermission(Permissions.GUI);
    }

    @Override
    public List<Message> help() {
        return List.of(Message.HELP_GUI);
    }

    @Override
    public void execute(CommandContext context) {
        context.requirePermission(Permissions.GUI);
        context.plugin().gui().openPresets(context.requirePlayer());
    }

    @Override
    public List<String> complete(CommandContext context) {
        return List.of();
    }
}
