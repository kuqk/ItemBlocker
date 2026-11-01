package pl.variant.itemblocker.command;

import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import pl.variant.itemblocker.message.Message;
import pl.variant.itemblocker.message.Names;
import pl.variant.itemblocker.message.Placeholder;
import pl.variant.itemblocker.rule.ItemLimit;
import pl.variant.itemblocker.rule.Preset;

import java.util.List;
import java.util.OptionalInt;
import java.util.Set;

/**
 * {@code /ib limit <item|hand> [stack:] [inventory:] [in:]}, setting the largest stack and the most a
 * player may carry. Options left out keep their current value, {@code none} removes a limit.
 */
final class LimitCommand implements Subcommand {

    private static final String USAGE = "limit <item|hand> [stack:<amount>|none] [inventory:<amount>|none] [in:preset]";

    @Override
    public String name() {
        return "limit";
    }

    @Override
    public boolean isAvailableTo(CommandSender sender) {
        return sender.hasPermission(Permissions.LIMIT);
    }

    @Override
    public List<Message> help() {
        return List.of(Message.HELP_LIMIT);
    }

    @Override
    public void execute(CommandContext context) {
        context.requirePermission(Permissions.LIMIT);
        String subject = context.arg(0);
        if (subject == null) {
            throw context.usage(USAGE);
        }

        Material material = Arguments.item(context, subject);
        Options options = Options.parse(context.argsFrom(1), Set.of(Options.STACK, Options.INVENTORY, Options.PRESET));
        OptionalInt stack = parse(options, Options.STACK);
        OptionalInt inventory = parse(options, Options.INVENTORY);
        if (stack.isEmpty() && inventory.isEmpty()) {
            throw context.usage(USAGE);
        }

        String presetName = Arguments.preset(context, options).name();
        Preset preset = context.updatePreset(presetName, current -> {
            ItemLimit limit = current.limit(material);
            if (stack.isPresent()) {
                limit = limit.withStack(stack.getAsInt());
            }
            if (inventory.isPresent()) {
                limit = limit.withInventory(inventory.getAsInt());
            }
            return current.withLimit(material, limit);
        });

        ItemLimit limit = preset.limit(material);
        Placeholder item = Placeholder.of("item", Names.of(material));
        Placeholder presetPlaceholder = Placeholder.of("preset", preset.name());
        if (limit.isNone()) {
            context.send(Message.LIMIT_REMOVED, item, presetPlaceholder);
        } else {
            context.send(Message.LIMIT_SET, item, presetPlaceholder,
                    Placeholder.of("stack", Formats.limit(context, limit.stack())),
                    Placeholder.of("inventory", Formats.limit(context, limit.inventory())));
        }
    }

    private static OptionalInt parse(Options options, String key) {
        return options.get(key).map(value -> OptionalInt.of(Arguments.limit(value))).orElseGet(OptionalInt::empty);
    }

    @Override
    public List<String> complete(CommandContext context) {
        if (context.args().size() == 1) {
            return Completions.items(context.arg(0));
        }
        return Completions.options(context, List.of(Options.STACK, Options.INVENTORY, Options.PRESET));
    }
}
