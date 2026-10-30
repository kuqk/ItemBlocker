package pl.variant.itemblocker.command;

import org.bukkit.command.CommandSender;
import pl.variant.itemblocker.message.Message;
import pl.variant.itemblocker.message.Placeholder;
import pl.variant.itemblocker.rule.ItemTypes;
import pl.variant.itemblocker.rule.LevelLimits;
import pl.variant.itemblocker.rule.Preset;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

/**
 * {@code /ib list} for an overview of every preset, or {@code /ib list items|enchantments|potions [in:]}
 * for the rules of one kind.
 */
final class ListCommand implements Subcommand {

    private static final String ITEMS = "items";
    private static final String ENCHANTMENTS = "enchantments";
    private static final String POTIONS = "potions";
    private static final String USAGE = "list [items|enchantments|potions] [in:preset]";

    @Override
    public String name() {
        return "list";
    }

    @Override
    public boolean isAvailableTo(CommandSender sender) {
        return sender.hasPermission(Permissions.LIST);
    }

    @Override
    public List<Message> help() {
        return List.of(Message.HELP_LIST);
    }

    @Override
    public void execute(CommandContext context) {
        context.requirePermission(Permissions.LIST);
        String kind = context.arg(0);
        if (kind == null) {
            listPresets(context);
            return;
        }

        Options options = Options.parse(context.argsFrom(1), Set.of(Options.PRESET));
        List<Preset> presets = options.get(Options.PRESET)
                .map(name -> List.of(Arguments.preset(context, name)))
                .orElseGet(() -> context.rules().presets());
        switch (kind.toLowerCase(Locale.ROOT)) {
            case ITEMS -> presets.forEach(preset -> listItems(context, preset));
            case ENCHANTMENTS -> presets.forEach(preset -> listLevels(context, preset, Message.LIST_ENCHANTMENTS_HEADER, Preset::enchantments));
            case POTIONS -> presets.forEach(preset -> listLevels(context, preset, Message.LIST_POTIONS_HEADER, Preset::effects));
            default -> throw context.usage(USAGE);
        }
    }

    private static void listPresets(CommandContext context) {
        context.send(Message.LIST_HEADER);
        for (Preset preset : context.rules().presets()) {
            context.send(Message.LIST_PRESET_ENTRY,
                    Placeholder.of("preset", preset.name()),
                    Placeholder.of("items", preset.items().size()),
                    Placeholder.of("enchantments", preset.enchantments().size()),
                    Placeholder.of("potions", preset.effects().size()),
                    Placeholder.of("worlds", Formats.worlds(context, preset.scope())));
        }
    }

    private static void listItems(CommandContext context, Preset preset) {
        context.send(Message.LIST_ITEMS_HEADER, Placeholder.of("preset", preset.name()));
        if (preset.items().isEmpty()) {
            context.send(Message.LIST_EMPTY);
            return;
        }

        preset.items().entrySet().stream()
                .sorted(Map.Entry.comparingByKey(Comparator.comparing(ItemTypes::key)))
                .forEach(entry -> context.send(Message.LIST_ITEM_ENTRY,
                        Placeholder.of("item", ItemTypes.key(entry.getKey())),
                        Placeholder.of("actions", Formats.actions(context, entry.getValue().actions())),
                        Placeholder.of("worlds", Formats.worlds(context, entry.getValue().scope()))));
    }

    private static void listLevels(CommandContext context, Preset preset, Message header, Function<Preset, LevelLimits> limits) {
        context.send(header, Placeholder.of("preset", preset.name()));
        LevelLimits levels = limits.apply(preset);
        if (levels.isEmpty()) {
            context.send(Message.LIST_EMPTY);
            return;
        }

        levels.minimumLevels().forEach((key, level) -> context.send(Message.LIST_LEVEL_ENTRY,
                Placeholder.of("name", key),
                Placeholder.of("level", level)));
    }

    @Override
    public List<String> complete(CommandContext context) {
        if (context.args().size() == 1) {
            return Completions.matching(context.args().getFirst(), List.of(ITEMS, ENCHANTMENTS, POTIONS));
        }
        return Completions.options(context, List.of(Options.PRESET));
    }
}
