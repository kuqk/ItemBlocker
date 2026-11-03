package pl.variant.itemblocker.message;

import pl.variant.itemblocker.rule.Action;
import pl.variant.itemblocker.rule.EnchantmentMode;
import pl.variant.itemblocker.rule.RegionScope;
import pl.variant.itemblocker.rule.WorldScope;

import java.util.Set;
import java.util.stream.Collectors;

/**
 * Formats rule values for messages and menus.
 */
public final class Formats {

    private Formats() {
    }

    public static String actions(Messages messages, Set<Action> actions) {
        if (actions.size() == Action.values().length) {
            return messages.render(Message.WORD_ALL_ACTIONS);
        }
        if (actions.isEmpty()) {
            return messages.render(Message.WORD_NO_ACTIONS);
        }
        return actions.stream().map(Action::key).collect(Collectors.joining(", "));
    }

    /**
     * A blocked level, with the mode in brackets unless the item is simply blocked, such as {@code 5 (lowered)}.
     */
    public static String level(Messages messages, int level, EnchantmentMode mode) {
        return mode == EnchantmentMode.BLOCK ? Integer.toString(level) : level + " (" + mode(messages, mode) + ")";
    }

    public static String mode(Messages messages, EnchantmentMode mode) {
        return messages.render(switch (mode) {
            case BLOCK -> Message.WORD_MODE_BLOCK;
            case DOWNGRADE -> Message.WORD_MODE_DOWNGRADE;
            case REMOVE -> Message.WORD_MODE_REMOVE;
            case CONFISCATE -> Message.WORD_MODE_CONFISCATE;
        });
    }

    public static String limit(Messages messages, int amount) {
        return amount > 0 ? Integer.toString(amount) : messages.render(Message.WORD_NO_LIMIT);
    }

    public static String worlds(Messages messages, WorldScope scope) {
        if (scope.isAll()) {
            return messages.render(Message.WORD_ALL_WORLDS);
        }
        return scope.worlds().stream().sorted().collect(Collectors.joining(", "));
    }

    public static String regions(Messages messages, RegionScope scope) {
        if (scope.isAll()) {
            return messages.render(Message.WORD_ALL_REGIONS);
        }
        return scope.regions().stream().sorted().collect(Collectors.joining(", "));
    }
}
