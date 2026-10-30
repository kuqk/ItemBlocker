package pl.variant.itemblocker.command;

import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import pl.variant.itemblocker.message.Message;
import pl.variant.itemblocker.message.Placeholder;
import pl.variant.itemblocker.rule.ItemRule;
import pl.variant.itemblocker.rule.ItemTypes;
import pl.variant.itemblocker.rule.LevelLimits;
import pl.variant.itemblocker.rule.Preset;
import pl.variant.itemblocker.rule.RegistryKeys;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.function.Function;

/**
 * {@code /ib show <item|hand>} and {@code /ib show enchantment|potion <name>}: which presets block
 * something, and how.
 */
final class ShowCommand implements Subcommand {

    private static final String USAGE = "show <item|hand|enchantment|potion> [name]";

    @Override
    public String name() {
        return "show";
    }

    @Override
    public boolean isAvailableTo(CommandSender sender) {
        return sender.hasPermission(Permissions.INFO);
    }

    @Override
    public List<Message> help() {
        return List.of(Message.HELP_SHOW);
    }

    @Override
    public void execute(CommandContext context) {
        context.requirePermission(Permissions.INFO);
        String subject = Optional.ofNullable(context.arg(0)).orElseThrow(() -> context.usage(USAGE));
        switch (subject.toLowerCase(Locale.ROOT)) {
            case BlockCommand.ENCHANTMENT -> {
                String key = RegistryKeys.of(Arguments.enchantment(requireName(context)));
                context.send(Message.SHOW_ENCHANTMENT_HEADER, Placeholder.of("enchantment", key));
                showLevels(context, key, Preset::enchantments);
            }
            case BlockCommand.POTION -> {
                String key = RegistryKeys.of(Arguments.effect(requireName(context)));
                context.send(Message.SHOW_POTION_HEADER, Placeholder.of("effect", key));
                showLevels(context, key, Preset::effects);
            }
            default -> showItem(context, Arguments.item(context, subject));
        }
    }

    private static void showItem(CommandContext context, Material material) {
        Placeholder item = Placeholder.of("item", ItemTypes.key(material));
        context.send(Message.SHOW_ITEM_HEADER, item);

        boolean any = false;
        for (Preset preset : context.rules().presets()) {
            Optional<ItemRule> rule = preset.item(material);
            if (rule.isPresent()) {
                any = true;
                context.send(Message.SHOW_ITEM_ENTRY,
                        Placeholder.of("preset", preset.name()),
                        Placeholder.of("actions", Formats.actions(context, rule.get().actions())),
                        Placeholder.of("worlds", Formats.worlds(context, rule.get().scope())));
            }
        }
        if (!any) {
            context.send(Message.SHOW_ITEM_EMPTY, item);
        }
    }

    private static void showLevels(CommandContext context, String key, Function<Preset, LevelLimits> limits) {
        boolean any = false;
        for (Preset preset : context.rules().presets()) {
            OptionalInt level = limits.apply(preset).minimumLevel(key);
            if (level.isPresent()) {
                any = true;
                context.send(Message.SHOW_LEVEL_ENTRY,
                        Placeholder.of("preset", preset.name()),
                        Placeholder.of("level", level.getAsInt()),
                        Placeholder.of("worlds", Formats.worlds(context, preset.scope())));
            }
        }
        if (!any) {
            context.send(Message.SHOW_LEVEL_EMPTY);
        }
    }

    private static String requireName(CommandContext context) {
        return Optional.ofNullable(context.arg(1)).orElseThrow(() -> context.usage(USAGE));
    }

    @Override
    public List<String> complete(CommandContext context) {
        List<String> args = context.args();
        if (args.size() == 1) {
            return Completions.subjects(args.getFirst());
        }
        if (args.size() == 2) {
            return switch (args.getFirst().toLowerCase(Locale.ROOT)) {
                case BlockCommand.ENCHANTMENT -> Completions.enchantments(args.get(1));
                case BlockCommand.POTION -> Completions.effects(args.get(1));
                default -> List.of();
            };
        }
        return List.of();
    }
}
