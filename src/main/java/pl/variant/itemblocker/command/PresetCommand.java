package pl.variant.itemblocker.command;

import org.bukkit.command.CommandSender;
import pl.variant.itemblocker.message.Message;
import pl.variant.itemblocker.message.Placeholder;
import pl.variant.itemblocker.rule.Preset;
import pl.variant.itemblocker.rule.RegionScope;
import pl.variant.itemblocker.rule.RuleBook;
import pl.variant.itemblocker.rule.WorldScope;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * {@code /ib preset list|show|create|delete|edit}.
 */
final class PresetCommand implements Subcommand {

    private static final String LIST = "list";
    private static final String SHOW = "show";
    private static final String CREATE = "create";
    private static final String DELETE = "delete";
    private static final String EDIT = "edit";
    private static final String DESCRIPTION = "description";
    private static final String REASON = "reason";
    private static final String WORLDS = "worlds";
    private static final String REGIONS = "regions";

    private static final String USAGE = "preset <list|show|create|delete|edit> [name]";
    private static final String EDIT_USAGE = "preset edit <name> <description|reason|worlds|regions> [value]";

    @Override
    public String name() {
        return "preset";
    }

    @Override
    public boolean isAvailableTo(CommandSender sender) {
        return sender.hasPermission(Permissions.PRESET)
                || sender.hasPermission(Permissions.LIST)
                || sender.hasPermission(Permissions.INFO);
    }

    @Override
    public List<Message> help() {
        return List.of(Message.HELP_PRESET);
    }

    @Override
    public void execute(CommandContext context) {
        String action = Optional.ofNullable(context.arg(0)).orElse(LIST).toLowerCase(Locale.ROOT);
        switch (action) {
            case LIST -> list(context);
            case SHOW -> show(context);
            case CREATE -> create(context);
            case DELETE -> delete(context);
            case EDIT -> edit(context);
            default -> throw context.usage(USAGE);
        }
    }

    private static void list(CommandContext context) {
        context.requirePermission(Permissions.LIST);
        context.send(Message.PRESETS_HEADER);
        for (Preset preset : context.rules().presets()) {
            context.send(Message.PRESETS_ENTRY,
                    Placeholder.of("preset", preset.name()),
                    Placeholder.of("description", describe(context, preset)));
        }
    }

    private static void show(CommandContext context) {
        context.requirePermission(Permissions.INFO);
        Preset preset = Arguments.preset(context, requireName(context, USAGE));
        context.send(Message.PRESETS_DETAILS_HEADER, Placeholder.of("preset", preset.name()));
        context.send(Message.PRESETS_DETAILS_DESCRIPTION, Placeholder.of("description", describe(context, preset)));
        context.send(Message.PRESETS_DETAILS_REASON, Placeholder.of("reason",
                preset.reason().isBlank() ? context.render(Message.WORD_NONE) : preset.reason()));
        context.send(Message.PRESETS_DETAILS_WORLDS, Placeholder.of("worlds", Formats.worlds(context, preset.scope())));
        context.send(Message.PRESETS_DETAILS_REGIONS, Placeholder.of("regions", Formats.regions(context, preset.regions())));
        context.send(Message.PRESETS_DETAILS_COUNTS,
                Placeholder.of("items", preset.items().size()),
                Placeholder.of("enchantments", preset.enchantments().size()),
                Placeholder.of("potions", preset.effects().size()));
    }

    private static void create(CommandContext context) {
        context.requirePermission(Permissions.PRESET);
        String name = requireName(context, USAGE);
        Placeholder preset = Placeholder.of("preset", name);
        if (!Preset.isValidName(name)) {
            throw new CommandException(Message.PRESET_INVALID_NAME, preset);
        }
        if (context.rules().preset(name).isPresent()) {
            throw new CommandException(Message.PRESET_EXISTS, preset);
        }

        context.updateRules(rules -> rules.with(Preset.empty(name)));
        context.send(Message.PRESET_CREATED, Placeholder.of("preset", Preset.normalizeName(name)));
    }

    private static void delete(CommandContext context) {
        context.requirePermission(Permissions.PRESET);
        Preset preset = Arguments.preset(context, requireName(context, USAGE));
        if (preset.name().equals(RuleBook.DEFAULT_PRESET)) {
            throw new CommandException(Message.PRESET_DEFAULT_PROTECTED);
        }

        context.updateRules(rules -> rules.without(preset.name()));
        context.send(Message.PRESET_DELETED, Placeholder.of("preset", preset.name()));
    }

    private static void edit(CommandContext context) {
        context.requirePermission(Permissions.PRESET);
        String name = Arguments.preset(context, requireName(context, EDIT_USAGE)).name();
        String field = Optional.ofNullable(context.arg(2)).orElseThrow(() -> context.usage(EDIT_USAGE)).toLowerCase(Locale.ROOT);
        String value = String.join(" ", context.argsFrom(3)).trim();
        Placeholder preset = Placeholder.of("preset", name);

        switch (field) {
            case DESCRIPTION -> {
                context.updatePreset(name, current -> current.withDescription(value));
                context.send(Message.PRESET_DESCRIPTION_SET, preset);
            }
            case REASON -> {
                context.updatePreset(name, current -> current.withReason(value));
                context.send(Message.PRESET_REASON_SET, preset);
            }
            case WORLDS -> {
                WorldScope scope = Arguments.worlds(value.isEmpty() ? Arguments.ALL : value.replace(' ', ','));
                context.updatePreset(name, current -> current.withScope(scope));
                context.send(Message.PRESET_WORLDS_SET, preset, Placeholder.of("worlds", Formats.worlds(context, scope)));
            }
            case REGIONS -> {
                RegionScope regions = Arguments.regions(value.isEmpty() ? Arguments.ALL : value.replace(' ', ','));
                context.updatePreset(name, current -> current.withRegions(regions));
                context.send(Message.PRESET_REGIONS_SET, preset, Placeholder.of("regions", Formats.regions(context, regions)));
                if (!regions.isAll() && !context.plugin().regions().isAvailable()) {
                    context.send(Message.PRESET_REGIONS_UNAVAILABLE);
                }
            }
            default -> throw context.usage(EDIT_USAGE);
        }
    }

    private static String describe(CommandContext context, Preset preset) {
        return preset.description().isBlank() ? context.render(Message.PRESETS_NO_DESCRIPTION) : preset.description();
    }

    private static String requireName(CommandContext context, String usage) {
        return Optional.ofNullable(context.arg(1)).orElseThrow(() -> context.usage(usage));
    }

    @Override
    public List<String> complete(CommandContext context) {
        List<String> args = context.args();
        if (args.size() == 1) {
            return Completions.matching(args.get(0), List.of(LIST, SHOW, CREATE, DELETE, EDIT));
        }

        String action = args.get(0).toLowerCase(Locale.ROOT);
        boolean takesPreset = action.equals(SHOW) || action.equals(DELETE) || action.equals(EDIT);
        if (args.size() == 2 && takesPreset) {
            return Completions.presets(context, args.get(1));
        }
        if (args.size() == 3 && action.equals(EDIT)) {
            return Completions.matching(args.get(2), List.of(DESCRIPTION, REASON, WORLDS, REGIONS));
        }
        if (args.size() >= 4 && action.equals(EDIT) && args.get(2).equalsIgnoreCase(WORLDS)) {
            return Completions.matching(context.lastArg(), Completions.worlds(context));
        }
        if (args.size() >= 4 && action.equals(EDIT) && args.get(2).equalsIgnoreCase(REGIONS)) {
            return Completions.matching(context.lastArg(), Completions.regions(context));
        }
        return List.of();
    }
}
