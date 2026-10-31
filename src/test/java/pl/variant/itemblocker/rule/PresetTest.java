package pl.variant.itemblocker.rule;

import org.bukkit.Material;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

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
        assertTrue(preset.blocksItem(Material.TNT, Action.PLACE, Place.inWorld("world")));
        assertFalse(preset.blocksItem(Material.TNT, Action.PLACE, Place.inWorld("spawn")));
        assertFalse(preset.blocksItem(Material.STONE, Action.PLACE, Place.inWorld("world")));
    }

    @Test
    void itemRuleScopeAppliesInsidePresetScope() {
        Preset preset = Preset.empty("pvp")
                .withItem(Material.ELYTRA, new ItemRule(EnumSet.of(Action.ARMOR), WorldScope.of(List.of("arena"))));

        assertTrue(preset.blocksItem(Material.ELYTRA, Action.ARMOR, Place.inWorld("arena")));
        assertFalse(preset.blocksItem(Material.ELYTRA, Action.ARMOR, Place.inWorld("world")));
    }

    @Test
    void presetScopeLimitsLevelRules() {
        Preset preset = Preset.empty("pvp")
                .withScope(WorldScope.of(List.of("arena")))
                .withEnchantments(LevelLimits.NONE.with("sharpness", 5))
                .withEffects(LevelLimits.NONE.with("strength", 2));

        assertTrue(preset.blocksEnchantment("sharpness", 5, Place.inWorld("arena")));
        assertFalse(preset.blocksEnchantment("sharpness", 5, Place.inWorld("world")));
        assertTrue(preset.blocksEffect("strength", 2, Place.inWorld("arena")));
        assertFalse(preset.blocksEffect("strength", 1, Place.inWorld("arena")));
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

    @Test
    void regionsLimitWherePresetApplies() {
        Preset preset = Preset.empty("spawn")
                .withRegions(RegionScope.of(List.of("Spawn")))
                .withItem(Material.TNT, ItemRule.blockEverything());

        assertTrue(preset.blocksItem(Material.TNT, Action.PLACE, new Place("world", Set.of("spawn", "town"))));
        assertFalse(preset.blocksItem(Material.TNT, Action.PLACE, new Place("world", Set.of("town"))));
        assertFalse(preset.blocksItem(Material.TNT, Action.PLACE, Place.inWorld("world")));
    }

    @Test
    void regionKeywordAllMeansEverywhere() {
        assertTrue(RegionScope.of(List.of("spawn", "all")).isAll());
        assertTrue(RegionScope.ALL.includesAny(Set.of()));
    }
}
