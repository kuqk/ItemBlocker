package pl.variant.itemblocker.rule;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;

/**
 * Blocks a set of actions for one item type, optionally only in some worlds.
 */
public record ItemRule(Set<Action> actions, WorldScope scope) {

    public ItemRule {
        EnumSet<Action> copy = EnumSet.noneOf(Action.class);
        copy.addAll(actions);
        actions = Collections.unmodifiableSet(copy);
        Objects.requireNonNull(scope, "scope");
    }

    /**
     * Creates a rule that blocks every action in every world.
     */
    public static ItemRule blockEverything() {
        return new ItemRule(EnumSet.allOf(Action.class), WorldScope.ALL);
    }

    public boolean blocks(Action action, String world) {
        return actions.contains(action) && scope.includes(world);
    }

    public boolean blocksEveryAction() {
        return actions.size() == Action.values().length;
    }

    /**
     * Returns a rule blocking the actions of both rules in the worlds of both rules.
     */
    public ItemRule merge(ItemRule other) {
        EnumSet<Action> combined = EnumSet.noneOf(Action.class);
        combined.addAll(actions);
        combined.addAll(other.actions);
        return new ItemRule(combined, scope.union(other.scope));
    }

    public ItemRule withActions(Set<Action> newActions) {
        return new ItemRule(newActions, scope);
    }

    public ItemRule withScope(WorldScope newScope) {
        return new ItemRule(actions, newScope);
    }
}
