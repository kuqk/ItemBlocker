package pl.variant.itemblocker.command;

import org.bukkit.Chunk;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import pl.variant.itemblocker.message.Message;
import pl.variant.itemblocker.message.Messages;
import pl.variant.itemblocker.message.Names;
import pl.variant.itemblocker.message.Placeholder;
import pl.variant.itemblocker.rule.Action;
import pl.variant.itemblocker.scan.Finding;
import pl.variant.itemblocker.scan.ScanMode;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * {@code /ib scan <players|containers> [remove|confiscate] [actions:] [radius:] [world:]}: finds items
 * that are already in inventories although a rule forbids them. Without {@code remove} or
 * {@code confiscate} it only reports them. By default an item counts as forbidden if players may not
 * pick it up or it carries a blocked enchantment.
 */
final class ScanCommand implements Subcommand {

    private static final String PLAYERS = "players";
    private static final String CONTAINERS = "containers";
    private static final String REMOVE = "remove";
    private static final String CONFISCATE = "confiscate";
    private static final String USAGE = "scan <players|containers> [remove|confiscate] [actions:] [radius:] [world:]";
    private static final int DEFAULT_RADIUS = 4;
    private static final int MAX_RADIUS = 16;
    private static final int MAX_LINES = 15;

    @Override
    public String name() {
        return "scan";
    }

    @Override
    public boolean isAvailableTo(CommandSender sender) {
        return sender.hasPermission(Permissions.SCAN);
    }

    @Override
    public List<Message> help() {
        return List.of(Message.HELP_SCAN);
    }

    @Override
    public void execute(CommandContext context) {
        context.requirePermission(Permissions.SCAN);
        String target = Optional.ofNullable(context.arg(0)).orElseThrow(() -> context.usage(USAGE)).toLowerCase(Locale.ROOT);
        List<String> rest = new ArrayList<>(context.argsFrom(1));
        ScanMode mode = ScanMode.REPORT;
        if (!rest.isEmpty() && rest.get(0).equalsIgnoreCase(REMOVE)) {
            mode = ScanMode.REMOVE;
            rest.remove(0);
        } else if (!rest.isEmpty() && rest.get(0).equalsIgnoreCase(CONFISCATE)) {
            mode = ScanMode.CONFISCATE;
            rest.remove(0);
        }
        Options options = Options.parse(rest, Set.of(Options.ACTIONS, Options.RADIUS, Options.WORLD));
        Set<Action> actions = options.get(Options.ACTIONS).map(Arguments::actions).orElseGet(() -> EnumSet.of(Action.PICKUP));

        ScanMode chosen = mode;
        switch (target) {
            case PLAYERS -> {
                context.send(Message.SCAN_STARTED, Placeholder.of("target", target));
                context.plugin().scanner().scanPlayers(List.copyOf(context.plugin().getServer().getOnlinePlayers()), actions, chosen,
                        findings -> report(context, target, chosen, findings));
            }
            case CONTAINERS -> {
                List<Chunk> chunks = chunks(context, options);
                context.send(Message.SCAN_STARTED, Placeholder.of("target", target));
                context.plugin().scanner().scanChunks(chunks, actions, chosen, findings -> report(context, target, chosen, findings));
            }
            default -> throw context.usage(USAGE);
        }
    }

    /**
     * The loaded chunks of a whole world with {@code world:}, otherwise those around the player.
     */
    private static List<Chunk> chunks(CommandContext context, Options options) {
        try {
            Optional<String> worldName = options.get(Options.WORLD);
            if (worldName.isPresent()) {
                World world = Optional.ofNullable(context.plugin().getServer().getWorld(worldName.get()))
                        .orElseThrow(() -> new CommandException(Message.ERROR_UNKNOWN_WORLD, Placeholder.of("input", worldName.get())));
                return List.of(world.getLoadedChunks());
            }

            Player player = context.requirePlayer();
            int radius = options.get(Options.RADIUS).map(Arguments::limit).map(value -> Math.min(MAX_RADIUS, value)).orElse(DEFAULT_RADIUS);
            Chunk center = player.getLocation().getChunk();
            List<Chunk> chunks = new ArrayList<>();
            for (int x = center.getX() - radius; x <= center.getX() + radius; x++) {
                for (int z = center.getZ() - radius; z <= center.getZ() + radius; z++) {
                    if (player.getWorld().isChunkLoaded(x, z)) {
                        chunks.add(player.getWorld().getChunkAt(x, z));
                    }
                }
            }
            return chunks;
        } catch (UnsupportedOperationException | IllegalStateException exception) {
            // Folia does not let one thread look at chunks of other regions.
            throw new CommandException(Message.SCAN_UNSUPPORTED);
        }
    }

    private static void report(CommandContext context, String target, ScanMode mode, List<Finding> findings) {
        Placeholder targetPlaceholder = Placeholder.of("target", target);
        if (findings.isEmpty()) {
            context.send(Message.SCAN_NONE, targetPlaceholder);
            return;
        }

        Messages messages = context.messages();
        Map<String, Integer> amounts = new LinkedHashMap<>();
        Map<String, Placeholder[]> lines = new LinkedHashMap<>();
        for (Finding finding : findings) {
            Placeholder item = Placeholder.item("item", finding.item());
            String key = finding.holder() + "\u0000" + item.value() + "\u0000" + finding.preset();
            amounts.merge(key, finding.item().getAmount(), Integer::sum);
            lines.putIfAbsent(key, new Placeholder[]{
                    Placeholder.of("holder", finding.holder()),
                    item,
                    Placeholder.of("preset", finding.preset())});
        }

        context.send(Message.SCAN_HEADER, targetPlaceholder, Placeholder.of("count", findings.size()));
        lines.entrySet().stream().limit(MAX_LINES).forEach(line -> {
            List<Placeholder> placeholders = new ArrayList<>(List.of(line.getValue()));
            placeholders.add(Placeholder.of("amount", amounts.get(line.getKey())));
            messages.send(context.sender(), Message.SCAN_ENTRY, placeholders.toArray(Placeholder[]::new));
        });
        if (lines.size() > MAX_LINES) {
            context.send(Message.SCAN_MORE, Placeholder.of("more", lines.size() - MAX_LINES));
        }

        Placeholder count = Placeholder.of("count", findings.size());
        switch (mode) {
            case REPORT -> context.send(Message.SCAN_HINT);
            case REMOVE -> context.send(Message.SCAN_REMOVED, count, targetPlaceholder);
            case CONFISCATE -> context.send(Message.SCAN_CONFISCATED, count, targetPlaceholder, Placeholder.of("label", context.label()));
        }
    }

    @Override
    public List<String> complete(CommandContext context) {
        List<String> args = context.args();
        if (args.size() == 1) {
            return Completions.matching(args.get(0), List.of(PLAYERS, CONTAINERS));
        }
        List<String> options = new ArrayList<>(Completions.options(context, List.of(Options.ACTIONS, Options.RADIUS, Options.WORLD)));
        if (args.size() == 2) {
            options.addAll(0, Completions.matching(args.get(1), List.of(REMOVE, CONFISCATE)));
        }
        return options;
    }
}
