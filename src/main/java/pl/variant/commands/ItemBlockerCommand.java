package pl.variant.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import pl.variant.itemBlocker;

import java.util.List;

public class ItemBlockerCommand implements CommandExecutor, TabCompleter {

    private final CommandUiHandler commandUiHandler;

    public ItemBlockerCommand(itemBlocker plugin) {
        this.commandUiHandler = new CommandUiHandler(plugin);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        return commandUiHandler.onCommand(sender, command, label, args);
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        return commandUiHandler.onTabComplete(sender, command, alias, args);
    }
}
