package pl.variant.itemblocker.command;

import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import pl.variant.itemblocker.message.Message;
import pl.variant.itemblocker.message.Names;
import pl.variant.itemblocker.message.Placeholder;
import pl.variant.itemblocker.rule.Action;
import pl.variant.itemblocker.rule.ItemRule;
import pl.variant.itemblocker.rule.Preset;
import pl.variant.itemblocker.rule.RegistryKeys;
import pl.variant.itemblocker.rule.WorldScope;

import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

/**
 * {@code /ib block <item|hand> [actions:] [worlds:] [in:]}, adding the actions and worlds to the
 * item's rule, and {@code /ib block enchantment|potion <name> [level:] [in:]}.
 */
final class BlockCommand implements Subcommand {

    static final String ENCHANTMENT = "enchantment";
    static final String POTION = "potion";

    private static final String USAGE = "block <item|hand|enchantment|potion> ...";

    @Override
    public String name() {
        return "block";
    }

    @Override
    public boolean isAvailableTo(CommandSender sender) {
        return sender.hasPermission(Permissions.ADD)
                || sender.hasPermission(Permissions.ENCHANTMENT)
                || sender.hasPermission(Permissions.POTION);
    }

    @Override
    public List<Message> help() {
        return List.of(Message.HELP_BLOCK, Message.HELP_BLOCK_ENCHANTMENT, Message.HELP_BLOCK_POTION);
    }

    @Override
    public void execute(CommandContext context) {
        String subject = context.arg(0);
        if (subject == null) {
            throw context.usage(USAGE);
        }

        switch (subject.toLowerCase(Locale.ROOT)) {
            case ENCHANTMENT -> blockEnchantment(context);
            case POTION -> blockPotion(context);
            default -> blockItem(context, subject);
        }
    }

    private static void blockItem(CommandContext context, String input) {
        context.requirePermission(Permissions.ADD);
        Material material = Arguments.item(context, input);
        Options options = Options.parse(context.argsFrom(1), Set.of(Options.ACTIONS, Options.WORLDS, Options.PRESET));
        String presetName = Arguments.preset(context, options).name();

        ItemRule added = new ItemRule(
                options.get(Options.ACTIONS).map(Arguments::actions).orElseGet(() -> EnumSet.allOf(Action.class)),
                options.get(Options.WORLDS).map(Arguments::worlds).orElse(WorldScope.ALL)
        );
        boolean existed = context.rules().preset(presetName).flatMap(preset -> preset.item(material)).isPresent();
        Preset preset = context.updatePreset(presetName, current -> current.withItem(material,
                current.item(material).map(rule -> rule.merge(added)).orElse(added)));

        ItemRule rule = preset.item(material).orElseThrow();
        context.send(existed ? Message.ITEM_UPDATED : Message.ITEM_BLOCKED,
                Placeholder.of("item", Names.of(material)),
                Placeholder.of("preset", preset.name()),
                Placeholder.of("actions", Formats.actions(context, rule.actions())),
                Placeholder.of("worlds", Formats.worlds(context, rule.scope())));
    }

    private static void blockEnchantment(CommandContext context) {
        context.requirePermission(Permissions.ENCHANTMENT);
        String key = RegistryKeys.of(Arguments.enchantment(requireName(context)));
        Options options = Options.parse(context.argsFrom(2), Set.of(Options.LEVEL, Options.PRESET));
        int level = Arguments.level(options);
        String presetName = Arguments.preset(context, options).name();

        context.updatePreset(presetName, preset -> preset.withEnchantments(preset.enchantments().with(key, level)));
        context.send(Message.ENCHANTMENT_BLOCKED,
                Placeholder.of("enchantment", Names.ofKey(key)),
                Placeholder.of("level", level),
                Placeholder.of("preset", presetName));
    }

    private static void blockPotion(CommandContext context) {
        context.requirePermission(Permissions.POTION);
        String key = RegistryKeys.of(Arguments.effect(requireName(context)));
        Options options = Options.parse(context.argsFrom(2), Set.of(Options.LEVEL, Options.PRESET));
        int level = Arguments.level(options);
        String presetName = Arguments.preset(context, options).name();

        context.updatePreset(presetName, preset -> preset.withEffects(preset.effects().with(key, level)));
        context.send(Message.POTION_BLOCKED,
                Placeholder.of("effect", Names.ofKey(key)),
                Placeholder.of("level", level),
                Placeholder.of("preset", presetName));
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
        if (subject.equals(ENCHANTMENT) || subject.equals(POTION)) {
            if (args.size() == 2) {
                return subject.equals(ENCHANTMENT) ? Completions.enchantments(args.get(1)) : Completions.effects(args.get(1));
            }
            return Completions.options(context, List.of(Options.LEVEL, Options.PRESET));
        }
        return Completions.options(context, List.of(Options.ACTIONS, Options.WORLDS, Options.PRESET));
    }
}
