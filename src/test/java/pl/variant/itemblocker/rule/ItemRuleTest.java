package pl.variant.itemblocker.rule;

import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ItemRuleTest {

    @Test
    void blocksOnlyListedActionsInScope() {
        ItemRule rule = new ItemRule(EnumSet.of(Action.PLACE, Action.USE), WorldScope.of(List.of("world")));

        assertTrue(rule.blocks(Action.PLACE, "world"));
        assertFalse(rule.blocks(Action.PLACE, "world_nether"));
        assertFalse(rule.blocks(Action.DROP, "world"));
    }

    @Test
    void mergeCombinesActionsAndWorlds() {
        ItemRule first = new ItemRule(EnumSet.of(Action.PLACE), WorldScope.of(List.of("world")));
        ItemRule second = new ItemRule(EnumSet.of(Action.USE), WorldScope.of(List.of("spawn")));

        ItemRule merged = first.merge(second);

        assertEquals(EnumSet.of(Action.PLACE, Action.USE), merged.actions());
        assertEquals(Set.of("world", "spawn"), merged.scope().worlds());
    }

    @Test
    void actionsAreImmutable() {
        ItemRule rule = new ItemRule(Set.of(), WorldScope.ALL);

        assertTrue(rule.actions().isEmpty());
        assertThrows(UnsupportedOperationException.class, () -> rule.actions().add(Action.USE));
    }

    @Test
    void blockEverythingCoversAllActions() {
        assertTrue(ItemRule.blockEverything().blocksEveryAction());
    }
}
