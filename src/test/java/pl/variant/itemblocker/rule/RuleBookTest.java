package pl.variant.itemblocker.rule;

import org.bukkit.Material;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RuleBookTest {

    @Test
    void alwaysContainsDefaultPresetFirst() {
        RuleBook rules = RuleBook.of(List.of(Preset.empty("zeta"), Preset.empty("alpha")));

        assertEquals(List.of("default", "alpha", "zeta"), rules.presets().stream().map(Preset::name).toList());
        assertEquals("default", rules.defaultPreset().name());
    }

    @Test
    void defaultPresetIsCheckedBeforeOthers() {
        RuleBook rules = RuleBook.empty()
                .with(Preset.empty("alpha").withItem(Material.TNT, ItemRule.blockEverything()))
                .with(Preset.empty(RuleBook.DEFAULT_PRESET).withItem(Material.TNT, ItemRule.blockEverything()));

        assertEquals("default", rules.findItemBlock(Material.TNT, Action.PLACE, "world").orElseThrow().name());
    }

    @Test
    void findsFirstMatchingPreset() {
        RuleBook rules = RuleBook.empty()
                .with(Preset.empty("arena")
                        .withScope(WorldScope.of(List.of("arena")))
                        .withItem(Material.ENDER_PEARL, new ItemRule(EnumSet.of(Action.USE), WorldScope.ALL)))
                .with(Preset.empty("pvp").withEnchantments(LevelLimits.NONE.with("sharpness", 4)));

        assertEquals("arena", rules.findItemBlock(Material.ENDER_PEARL, Action.USE, "arena").orElseThrow().name());
        assertTrue(rules.findItemBlock(Material.ENDER_PEARL, Action.USE, "world").isEmpty());
        assertEquals("pvp", rules.findEnchantmentBlock("minecraft:sharpness", 4, "world").orElseThrow().name());
        assertTrue(rules.findEffectBlock("speed", 1, "world").isEmpty());
    }

    @Test
    void presetLookupIsCaseInsensitive() {
        RuleBook rules = RuleBook.empty().with(Preset.empty("pvp"));

        assertTrue(rules.preset(" PvP ").isPresent());
    }

    @Test
    void defaultPresetCannotBeRemoved() {
        RuleBook rules = RuleBook.empty().with(Preset.empty("pvp"));

        assertTrue(rules.without("pvp").preset("pvp").isEmpty());
        assertThrows(IllegalArgumentException.class, () -> rules.without("default"));
    }
}
