package pl.variant.itemblocker.command;

import org.bukkit.Registry;
import org.bukkit.World;
import pl.variant.itemblocker.rule.Action;
import pl.variant.itemblocker.rule.ItemTypes;
import pl.variant.itemblocker.rule.Preset;
import pl.variant.itemblocker.rule.RegistryKeys;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

/**
 * Tab completion helpers.
 */
final class Completions {

    private static final List<String> LEVELS = List.of("1", "2", "3", "4", "5");

    private Completions() {
    }

    /**
     * Returns the values that start with the prefix, ignoring case.
     */
    static List<String> matching(String prefix, Collection<String> values) {
        String lowerPrefix = prefix.toLowerCase(Locale.ROOT);
        return values.stream()
                .filter(value -> value.toLowerCase(Locale.ROOT).startsWith(lowerPrefix))
                .toList();
    }

    static List<String> items(String prefix) {
        List<String> values = new ArrayList<>();
        values.add(Arguments.HAND);
        values.addAll(ItemTypes.keys());
        return matching(prefix, values);
    }

    /**
     * Completes the first argument of commands that take an item, an enchantment or a potion effect.
     */
    static List<String> subjects(String prefix) {
        List<String> values = new ArrayList<>(matching(prefix, List.of(BlockCommand.ENCHANTMENT, BlockCommand.POTION)));
        values.addAll(items(prefix));
        return values;
    }

    static List<String> enchantments(String prefix) {
        return matching(prefix, Registry.ENCHANTMENT.stream().map(RegistryKeys::of).sorted().toList());
    }

    static List<String> effects(String prefix) {
        return matching(prefix, Registry.EFFECT.stream().map(RegistryKeys::of).sorted().toList());
    }

    static List<String> presets(CommandContext context, String prefix) {
        return matching(prefix, context.rules().presets().stream().map(Preset::name).toList());
    }

    static List<String> worlds(CommandContext context) {
        return Stream.concat(
                Stream.of(Arguments.ALL),
                context.plugin().getServer().getWorlds().stream().map(World::getName)
        ).toList();
    }

    /**
     * Completes {@code key:value} options. Before a colon the unused keys are suggested, after it the
     * values for that key.
     */
    static List<String> options(CommandContext context, List<String> keys) {
        String current = context.args().isEmpty() ? "" : context.args().getLast();
        int separator = current.indexOf(':');
        if (separator < 0) {
            List<String> unused = keys.stream()
                    .filter(key -> context.args().stream().noneMatch(arg -> arg.startsWith(key + ":")))
                    .map(key -> key + ":")
                    .toList();
            return matching(current, unused);
        }

        String key = current.substring(0, separator);
        String value = current.substring(separator + 1);
        List<String> values = switch (key) {
            case Options.ACTIONS -> commaSeparated(value, Stream.concat(
                    Stream.of(Arguments.ALL, Arguments.NONE),
                    Arrays.stream(Action.values()).map(Action::key)).toList());
            case Options.WORLDS -> commaSeparated(value, worlds(context));
            case Options.PRESET -> presets(context, value);
            case Options.LEVEL -> matching(value, LEVELS);
            default -> List.of();
        };
        return values.stream().map(completed -> key + ":" + completed).toList();
    }

    /**
     * Completes the last entry of a comma separated list, keeping the entries before it.
     */
    private static List<String> commaSeparated(String value, List<String> candidates) {
        int lastComma = value.lastIndexOf(',');
        String done = value.substring(0, lastComma + 1);
        String partial = value.substring(lastComma + 1);
        List<String> used = List.of(done.split(","));
        return matching(partial, candidates).stream()
                .filter(candidate -> !used.contains(candidate))
                .map(candidate -> done + candidate)
                .toList();
    }
}
