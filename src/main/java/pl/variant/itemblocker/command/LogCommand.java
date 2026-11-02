package pl.variant.itemblocker.command;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import pl.variant.itemblocker.log.LogEntry;
import pl.variant.itemblocker.message.Message;
import pl.variant.itemblocker.message.Placeholder;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * {@code /ib log [player]}: the most recent blocked actions, newest first.
 */
final class LogCommand implements Subcommand {

    private static final int SHOWN = 10;
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm:ss").withZone(ZoneId.systemDefault());

    @Override
    public String name() {
        return "log";
    }

    @Override
    public boolean isAvailableTo(CommandSender sender) {
        return sender.hasPermission(Permissions.LOG);
    }

    @Override
    public List<Message> help() {
        return List.of(Message.HELP_LOG);
    }

    @Override
    public void execute(CommandContext context) {
        context.requirePermission(Permissions.LOG);
        List<LogEntry> entries = context.plugin().actionLog().recent(context.arg(0), SHOWN);
        context.send(Message.LOG_HEADER);
        if (entries.isEmpty()) {
            context.send(Message.LOG_EMPTY);
        }
        for (LogEntry entry : entries) {
            context.send(Message.LOG_ENTRY,
                    Placeholder.of("time", TIME.format(entry.time())),
                    Placeholder.of("player", entry.player()),
                    Placeholder.of("action", entry.action()),
                    Placeholder.of("item", entry.subject()),
                    Placeholder.of("world", entry.world()),
                    Placeholder.of("preset", entry.preset()));
        }
    }

    @Override
    public List<String> complete(CommandContext context) {
        if (context.args().size() == 1) {
            return Completions.matching(context.arg(0), context.plugin().getServer().getOnlinePlayers().stream()
                    .map(Player::getName)
                    .toList());
        }
        return List.of();
    }
}
