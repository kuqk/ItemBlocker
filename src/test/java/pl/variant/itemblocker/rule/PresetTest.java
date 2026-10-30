package pl.variant.itemblocker.rule;

import org.bukkit.Material;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.EnumSet;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PresetTest {

    @Test
    void presetScopeLimitsItemRules() {
        Preset preset = Preset.empty("Survival")
                .withScope(WorldScope.of(List.of("world")))
                .withItem(Material.TNT, ItemRule.blockEverything());

        assertEquals("survival", preset.name());
        assertTrue(preset.blocksItem(Material.TNT, Action.PLACE, "world"));
        assertFalse(preset.blocksItem(Material.TNT, Action.PLACE, "spawn"));
        assertFalse(preset.blocksItem(Material.STONE, Action.PLACE, "world"));
    }

    @Test
    void itemRuleScopeAppliesInsidePresetScope() {
        Preset preset = Preset.empty("pvp")
                .withItem(Material.ELYTRA, new ItemRule(EnumSet.of(Action.ARMOR), WorldScope.of(List.of("arena"))));

        assertTrue(preset.blocksItem(Material.ELYTRA, Action.ARMOR, "arena"));
        assertFalse(preset.blocksItem(Material.ELYTRA, Action.ARMOR, "world"));
    }

    @Test
    void presetScopeLimitsLevelRules() {
        Preset preset = Preset.empty("pvp")
                .withScope(WorldScope.of(List.of("arena")))
                .withEnchantments(LevelLimits.NONE.with("sharpness", 5))
                .withEffects(LevelLimits.NONE.with("strength", 2));

        assertTrue(preset.blocksEnchantment("sharpness", 5, "arena"));
        assertFalse(preset.blocksEnchantment("sharpness", 5, "world"));
        assertTrue(preset.blocksEffect("strength", 2, "arena"));
        assertFalse(preset.blocksEffect("strength", 1, "arena"));
    }

    @Test
    void withoutItemRemovesRule() {
        Preset preset = Preset.empty("pvp").withItem(Material.TNT, ItemRule.blockEverything()).withoutItem(Material.TNT);

        assertTrue(preset.items().isEmpty());
    }

    @ParameterizedTest
    @ValueSource(strings = {"pvp", "event-1", "survival_2026", "A"})
    void acceptsSafeNames(String name) {
        assertTrue(Preset.isValidName(name));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "-pvp", "[x]", "#x", "a:b", "two words", "a.b", "abcdefghijklmnopqrstuvwxyz0123456"})
    void rejectsUnsafeNames(String name) {
        assertFalse(Preset.isValidName(name));
    }
}
