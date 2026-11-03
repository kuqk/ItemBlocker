package pl.variant.itemblocker.command;

import pl.variant.itemblocker.message.Message;
import pl.variant.itemblocker.message.Placeholder;

import java.util.ArrayList;
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
    public static final String MODE = "mode";
    public static final String AS = "as";
    public static final String STACK = "stack";
    public static final String INVENTORY = "inventory";
    public static final String TYPE = "type";
    public static final String NAME = "name";
    public static final String LORE = "lore";
    public static final String MODEL = "model";
    public static final String DATA = "data";
    public static final String ENCHANTMENT = "enchantment";
    public static final String BYPASS = "bypass";
    public static final String RADIUS = "radius";
    public static final String WORLD = "world";

    private static final String QUOTE = "\"";

    private final Map<String, String> values;

    private Options(Map<String, String> values) {
        this.values = values;
    }

    /**
     * Parses options, accepting only the given keys. A value in double quotes may contain spaces,
     * such as {@code name:"Sword of Kings"}.
     *
     * @throws CommandException if a token is not a known {@code key:value} option
     */
    public static Options parse(List<String> tokens, Set<String> allowedKeys) {
        Map<String, String> values = new HashMap<>();
        for (String token : joinQuoted(tokens)) {
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

    /**
     * Joins tokens of a quoted value that the command line split at spaces, and removes the quotes.
     */
    private static List<String> joinQuoted(List<String> tokens) {
        List<String> joined = new ArrayList<>();
        StringBuilder open = null;
        for (String token : tokens) {
            if (open != null) {
                open.append(' ').append(token);
                if (token.endsWith(QUOTE)) {
                    joined.add(unquote(open.toString()));
                    open = null;
                }
                continue;
            }

            int separator = token.indexOf(':');
            String value = separator < 0 ? "" : token.substring(separator + 1);
            boolean opensQuote = value.startsWith(QUOTE) && (value.length() == 1 || !value.endsWith(QUOTE));
            if (opensQuote) {
                open = new StringBuilder(token);
            } else {
                joined.add(unquote(token));
            }
        }
        if (open != null) {
            joined.add(unquote(open.toString()));
        }
        return joined;
    }

    private static String unquote(String token) {
        int separator = token.indexOf(':');
        if (separator < 0) {
            return token;
        }
        String value = token.substring(separator + 1);
        if (value.startsWith(QUOTE)) {
            value = value.substring(1);
        }
        if (value.endsWith(QUOTE)) {
            value = value.substring(0, value.length() - 1);
        }
        return token.substring(0, separator + 1) + value;
    }

    public Optional<String> get(String key) {
        return Optional.ofNullable(values.get(key));
    }
}
