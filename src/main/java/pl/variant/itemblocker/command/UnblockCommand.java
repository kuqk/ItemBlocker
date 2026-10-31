package pl.variant.itemblocker.command;

import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import pl.variant.itemblocker.message.Message;
import pl.variant.itemblocker.message.Names;
import pl.variant.itemblocker.message.Placeholder;
import pl.variant.itemblocker.rule.Preset;
import pl.variant.itemblocker.rule.RegistryKeys;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

/**
 * {@code /ib unblock <item|hand> [in:]} and {@code /ib unblock enchantment|potion <name> [in:]}.
 */
final class UnblockCommand implements Subcommand {

    private static final String USAGE = "unblock <item|hand|enchantment|potion> ... [in:preset]";

    @Override
    public String name() {
        return "unblock";
    }

    @Override
    public boolean isAvailableTo(CommandSender sender) {
        return sender.hasPermission(Permissions.REMOVE)
                || sender.hasPermission(Permissions.ENCHANTMENT)
                || sender.hasPermission(Permissions.POTION);
    }

    @Override
    public List<Message> help() {
        return List.of(Message.HELP_UNBLOCK);
    }

    @Override
    public void execute(CommandContext context) {
        String subject = Optional.ofNullable(context.arg(0)).orElseThrow(() -> context.usage(USAGE));
        switch (subject.toLowerCase(Locale.ROOT)) {
            case BlockCommand.ENCHANTMENT -> unblockEnchantment(context);
            case BlockCommand.POTION -> unblockPotion(context);
            default -> unblockItem(context, subject);
        }
    }

    private static void unblockItem(CommandContext context, String input) {
        context.requirePermission(Permissions.REMOVE);
        Material material = Arguments.item(context, input);
        Preset preset = Arguments.preset(context, Options.parse(context.argsFrom(1), Set.of(Options.PRESET)));
        Placeholder[] placeholders = {Placeholder.of("item", Names.of(material)), Placeholder.of("preset", preset.name())};

        if (preset.item(material).isEmpty()) {
            throw new CommandException(Message.ITEM_NOT_BLOCKED, placeholders);
        }
        context.updatePreset(preset.name(), latest -> latest.withoutItem(material));
        context.send(Message.ITEM_UNBLOCKED, placeholders);
    }

    private static void unblockEnchantment(CommandContext context) {
        context.requirePermission(Permissions.ENCHANTMENT);
        String key = RegistryKeys.of(Arguments.enchantment(requireName(context)));
        Preset preset = Arguments.preset(context, Options.parse(context.argsFrom(2), Set.of(Options.PRESET)));
        Placeholder[] placeholders = {Placeholder.of("enchantment", Names.ofKey(key)), Placeholder.of("preset", preset.name())};

        if (!preset.enchantments().contains(key)) {
            throw new CommandException(Message.ENCHANTMENT_NOT_BLOCKED, placeholders);
        }
        context.updatePreset(preset.name(), latest -> latest.withEnchantments(latest.enchantments().without(key)));
        context.send(Message.ENCHANTMENT_UNBLOCKED, placeholders);
    }

    private static void unblockPotion(CommandContext context) {
        context.requirePermission(Permissions.POTION);
        String key = RegistryKeys.of(Arguments.effect(requireName(context)));
        Preset preset = Arguments.preset(context, Options.parse(context.argsFrom(2), Set.of(Options.PRESET)));
        Placeholder[] placeholders = {Placeholder.of("effect", Names.ofKey(key)), Placeholder.of("preset", preset.name())};

        if (!preset.effects().contains(key)) {
            throw new CommandException(Message.POTION_NOT_BLOCKED, placeholders);
        }
        context.updatePreset(preset.name(), latest -> latest.withEffects(latest.effects().without(key)));
        context.send(Message.POTION_UNBLOCKED, placeholders);
    }

    private static String requireName(CommandContext context) {
        return Optional.ofNullable(context.arg(1)).orElseThrow(() -> context.usage(USAGE));
    }

    @Override
    public List<String> complete(CommandContext context) {
        List<String> args = context.args();
        if (args.size() == 1) {
            return Completions.subjects(args.get(0));
        }

        String subject = args.get(0).toLowerCase(Locale.ROOT);
        if (args.size() == 2 && subject.equals(BlockCommand.ENCHANTMENT)) {
            return Completions.enchantments(args.get(1));
        }
        if (args.size() == 2 && subject.equals(BlockCommand.POTION)) {
            return Completions.effects(args.get(1));
        }
        return Completions.options(context, List.of(Options.PRESET));
    }
}
