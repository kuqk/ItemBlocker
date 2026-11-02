package pl.variant.itemblocker.rule;

import java.util.Objects;

/**
 * Blocks actions for items matching a filter.
 *
 * @param filter which items the rule applies to
 * @param rule   the blocked actions and the worlds they are blocked in
 */
public record FilterRule(ItemFilter filter, ItemRule rule) {

    public FilterRule {
        Objects.requireNonNull(filter, "filter");
        Objects.requireNonNull(rule, "rule");
    }
}
