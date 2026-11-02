package pl.variant.itemblocker.rule;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;

/**
 * Blocks a set of actions, optionally only in some worlds.
 *
 * @param actions          the blocked actions
 * @param scope            the worlds they are blocked in
 * @param bypassPermission a permission that lets a player ignore just this rule, or {@code null}
 */
public record ItemRule(Set<Action> actions, WorldScope scope, String bypassPermission) {

    public ItemRule {
        EnumSet<Action> copy = EnumSet.noneOf(Action.class);
        copy.addAll(actions);
        actions = Collections.unmodifiableSet(copy);
        Objects.requireNonNull(scope, "scope");
        bypassPermission = bypassPermission == null || bypassPermission.isBlank() ? null : bypassPermission.trim();
    }

    public ItemRule(Set<Action> actions, WorldScope scope) {
        this(actions, scope, null);
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
        return new ItemRule(combined, scope.union(other.scope),
                other.bypassPermission != null ? other.bypassPermission : bypassPermission);
    }

    public ItemRule withActions(Set<Action> newActions) {
        return new ItemRule(newActions, scope, bypassPermission);
    }

    public ItemRule withScope(WorldScope newScope) {
        return new ItemRule(actions, newScope, bypassPermission);
    }

    public ItemRule withBypassPermission(String permission) {
        return new ItemRule(actions, scope, permission);
    }
}
