package pl.variant.itemblocker.command;

import pl.variant.itemblocker.message.Message;
import pl.variant.itemblocker.rule.Action;
import pl.variant.itemblocker.rule.RegionScope;
import pl.variant.itemblocker.rule.WorldScope;

import java.util.Set;
import java.util.stream.Collectors;

/**
 * Formats rule values for command output.
 */
final class Formats {

    private Formats() {
    }

    static String actions(CommandContext context, Set<Action> actions) {
        if (actions.size() == Action.values().length) {
            return context.render(Message.WORD_ALL_ACTIONS);
        }
        if (actions.isEmpty()) {
            return context.render(Message.WORD_NO_ACTIONS);
        }
        return actions.stream().map(Action::key).collect(Collectors.joining(", "));
    }

    static String worlds(CommandContext context, WorldScope scope) {
        if (scope.isAll()) {
            return context.render(Message.WORD_ALL_WORLDS);
        }
        return scope.worlds().stream().sorted().collect(Collectors.joining(", "));
    }

    static String regions(CommandContext context, RegionScope scope) {
        if (scope.isAll()) {
            return context.render(Message.WORD_ALL_REGIONS);
        }
        return scope.regions().stream().sorted().collect(Collectors.joining(", "));
    }
}
