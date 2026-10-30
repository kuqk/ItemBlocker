package pl.variant.itemblocker.command;

import pl.variant.itemblocker.message.Message;
import pl.variant.itemblocker.message.Placeholder;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Named {@code key:value} options such as {@code actions:place,use} or {@code in:pvp}.
 */
public final class Options {

    public static final String ACTIONS = "actions";
    public static final String WORLDS = "worlds";
    public static final String PRESET = "in";
    public static final String LEVEL = "level";

    private final Map<String, String> values;

    private Options(Map<String, String> values) {
        this.values = values;
    }

    /**
     * Parses options, accepting only the given keys.
     *
     * @throws CommandException if a token is not a known {@code key:value} option
     */
    public static Options parse(List<String> tokens, Set<String> allowedKeys) {
        Map<String, String> values = new HashMap<>();
        for (String token : tokens) {
            int separator = token.indexOf(':');
            String key = separator < 0 ? "" : token.substring(0, separator).toLowerCase(Locale.ROOT);
            String value = separator < 0 ? "" : token.substring(separator + 1).trim();
            if (!allowedKeys.contains(key) || value.isEmpty() || values.containsKey(key)) {
                throw new CommandException(Message.ERROR_UNKNOWN_OPTION, Placeholder.of("input", token));
            }
            values.put(key, value);
        }
        return new Options(values);
    }

    public Optional<String> get(String key) {
        return Optional.ofNullable(values.get(key));
    }
}
