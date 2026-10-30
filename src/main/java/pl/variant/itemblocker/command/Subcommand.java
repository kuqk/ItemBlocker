package pl.variant.itemblocker.command;

import org.bukkit.command.CommandSender;
import pl.variant.itemblocker.message.Message;

import java.util.List;

/**
 * One {@code /itemblocker <name>} subcommand.
 */
public interface Subcommand {

    String name();

    /**
     * Returns whether the sender can use at least part of this subcommand, which decides whether it
     * is suggested and listed in the help.
     */
    boolean isAvailableTo(CommandSender sender);

    /**
     * Returns the help lines describing this subcommand.
     */
    List<Message> help();

    /**
     * Runs the subcommand. Validation problems are reported by throwing {@link CommandException}.
     */
    void execute(CommandContext context);

    /**
     * Returns suggestions for the last argument in the context.
     */
    List<String> complete(CommandContext context);
}
