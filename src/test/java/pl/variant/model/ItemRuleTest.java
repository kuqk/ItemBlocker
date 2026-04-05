package pl.variant.model;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ItemRuleTest {

    @Test
    void parsesCommaSeparatedActionStringFromConfig() {
        ItemRule rule = ItemRule.fromConfigValue(Map.of(
                "actions", "use,drop",
                "worlds", "all"
        ));

        assertEquals(2, rule.getActions().size());
        assertTrue(rule.getActions().contains(BlockAction.USE));
        assertTrue(rule.getActions().contains(BlockAction.DROP));
    }

    @Test
    void parsesNoneActionFromConfigAsEmptyActionSet() {
        ItemRule rule = ItemRule.fromConfigValue(Map.of(
                "actions", "none",
                "worlds", "all"
        ));

        assertTrue(rule.getActions().isEmpty());
    }
}
