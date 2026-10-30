package pl.variant.itemblocker.command;

import pl.variant.itemblocker.message.Message;
import pl.variant.itemblocker.message.Placeholder;

import java.io.Serial;
import java.util.List;

/**
 * Stops a command and tells the sender why. Thrown by argument parsing and validation so that
 * subcommands can be written as straight-line code.
 */
public final class CommandException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;

    private final transient Message userMessage;
    private final transient List<Placeholder> placeholders;

    public CommandException(Message userMessage, Placeholder... placeholders) {
        super(userMessage.path(), null, false, false);
        this.userMessage = userMessage;
        this.placeholders = List.of(placeholders);
    }

    public Message userMessage() {
        return userMessage;
    }

    public Placeholder[] placeholders() {
        return placeholders.toArray(Placeholder[]::new);
    }
}
