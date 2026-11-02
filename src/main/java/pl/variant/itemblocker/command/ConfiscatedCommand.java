package pl.variant.itemblocker.command;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import pl.variant.itemblocker.message.Message;
import pl.variant.itemblocker.message.Placeholder;

import java.util.List;
import java.util.Map;

/**
 * {@code /ib confiscated} lists the items taken by scans, and {@code /ib confiscated take <owner>}
 * gives the items of one owner to the player running it.
 */
final class ConfiscatedCommand implements Subcommand {

    private static final String TAKE = "take";
    private static final String USAGE = "confiscated [take <owner>]";

    @Override
    public String name() {
        return "confiscated";
    }

    @Override
    public boolean isAvailableTo(CommandSender sender) {
        return sender.hasPermission(Permissions.SCAN);
    }

    @Override
    public List<Message> help() {
        return List.of(Message.HELP_CONFISCATED);
    }

    @Override
    public void execute(CommandContext context) {
        context.requirePermission(Permissions.SCAN);
        String action = context.arg(0);
        if (action == null) {
            list(context);
        } else if (action.equalsIgnoreCase(TAKE) && context.args().size() > 1) {
            take(context, String.join(" ", context.argsFrom(1)));
        } else {
            throw context.usage(USAGE);
        }
    }

    private static void list(CommandContext context) {
        Map<String, Integer> owners = context.plugin().confiscated().owners();
        context.send(Message.CONFISCATED_HEADER);
        if (owners.isEmpty()) {
            context.send(Message.CONFISCATED_EMPTY);
        }
        owners.forEach((owner, count) -> context.send(Message.CONFISCATED_ENTRY,
                Placeholder.of("owner", owner), Placeholder.of("count", count)));
    }

    private static void take(CommandContext context, String owner) {
        Player player = context.requirePlayer();
        List<ItemStack> items = context.plugin().confiscated().take(owner);
        if (items.isEmpty()) {
            throw new CommandException(Message.CONFISCATED_UNKNOWN, Placeholder.of("owner", owner));
        }

        player.getInventory().addItem(items.toArray(ItemStack[]::new)).values()
                .forEach(leftover -> player.getWorld().dropItemNaturally(player.getLocation(), leftover));
        context.send(Message.CONFISCATED_GIVEN, Placeholder.of("owner", owner), Placeholder.of("count", items.size()));
    }

    @Override
    public List<String> complete(CommandContext context) {
        List<String> args = context.args();
        if (args.size() == 1) {
            return Completions.matching(args.get(0), List.of(TAKE));
        }
        if (args.size() == 2 && args.get(0).equalsIgnoreCase(TAKE)) {
            return Completions.matching(args.get(1), List.copyOf(context.plugin().confiscated().owners().keySet()));
        }
        return List.of();
    }
}
