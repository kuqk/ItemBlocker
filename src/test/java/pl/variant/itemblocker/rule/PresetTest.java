package pl.variant.itemblocker.rule;

import org.bukkit.Material;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.EnumSet;
import java.util.List;
import java.util.Map;
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
        assertTrue(preset.blocksItem(Material.TNT, Action.PLACE, Place.inWorld("world"), Exemption.NONE));
        assertFalse(preset.blocksItem(Material.TNT, Action.PLACE, Place.inWorld("spawn"), Exemption.NONE));
        assertFalse(preset.blocksItem(Material.STONE, Action.PLACE, Place.inWorld("world"), Exemption.NONE));
    }

    @Test
    void itemRuleScopeAppliesInsidePresetScope() {
        Preset preset = Preset.empty("pvp")
                .withItem(Material.ELYTRA, new ItemRule(EnumSet.of(Action.ARMOR), WorldScope.of(List.of("arena"))));

        assertTrue(preset.blocksItem(Material.ELYTRA, Action.ARMOR, Place.inWorld("arena"), Exemption.NONE));
        assertFalse(preset.blocksItem(Material.ELYTRA, Action.ARMOR, Place.inWorld("world"), Exemption.NONE));
    }

    @Test
    void presetScopeLimitsLevelRules() {
        Preset preset = Preset.empty("pvp")
                .withScope(WorldScope.of(List.of("arena")))
                .withEnchantments(LevelLimits.NONE.with("sharpness", 5))
                .withEffects(LevelLimits.NONE.with("strength", 2));

        assertTrue(preset.blocksEnchantment("sharpness", 5, Place.inWorld("arena"), Exemption.NONE));
        assertFalse(preset.blocksEnchantment("sharpness", 5, Place.inWorld("world"), Exemption.NONE));
        assertTrue(preset.blocksEffect("strength", 2, Place.inWorld("arena"), Exemption.NONE));
        assertFalse(preset.blocksEffect("strength", 1, Place.inWorld("arena"), Exemption.NONE));
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

        assertTrue(preset.blocksItem(Material.TNT, Action.PLACE, new Place("world", Set.of("spawn", "town")), Exemption.NONE));
        assertFalse(preset.blocksItem(Material.TNT, Action.PLACE, new Place("world", Set.of("town")), Exemption.NONE));
        assertFalse(preset.blocksItem(Material.TNT, Action.PLACE, Place.inWorld("world"), Exemption.NONE));
    }

    @Test
    void regionKeywordAllMeansEverywhere() {
        assertTrue(RegionScope.of(List.of("spawn", "all")).isAll());
        assertTrue(RegionScope.ALL.includesAny(Set.of()));
    }

    @Test
    void groupsBlockEveryMatchingTypeExceptAllowedOnes() {
        Preset preset = Preset.empty("pvp")
                .withGroup(ItemPattern.parse("*_sword").orElseThrow(), new ItemRule(EnumSet.of(Action.USE), WorldScope.ALL))
                .withAllowed(Material.WOODEN_SWORD);
        Place world = Place.inWorld("world");

        assertTrue(preset.blocksItem(Material.NETHERITE_SWORD, Action.USE, world, Exemption.NONE));
        assertFalse(preset.blocksItem(Material.WOODEN_SWORD, Action.USE, world, Exemption.NONE));
        assertFalse(preset.blocksItem(Material.DIAMOND_AXE, Action.USE, world, Exemption.NONE));
    }

    @Test
    void exemptionsSkipOneRuleOrTheWholePreset() {
        Preset preset = Preset.empty("pvp")
                .withItem(Material.ELYTRA, new ItemRule(EnumSet.of(Action.ARMOR), WorldScope.ALL, "vip.elytra"))
                .withItem(Material.TNT, ItemRule.blockEverything());
        Place world = Place.inWorld("world");
        Exemption vip = (p, permission) -> "vip.elytra".equals(permission);
        Exemption wholePreset = (p, permission) -> permission == null;

        assertFalse(preset.blocksItem(Material.ELYTRA, Action.ARMOR, world, vip));
        assertTrue(preset.blocksItem(Material.TNT, Action.PLACE, world, vip));
        assertFalse(preset.blocksItem(Material.TNT, Action.PLACE, world, wholePreset));
    }

    @Test
    void patternsAreWildcardsOrTags() {
        assertTrue(ItemPattern.parse("netherite_*").orElseThrow().matches(Material.NETHERITE_HELMET));
        assertEquals("#minecraft:swords", ItemPattern.parse("#minecraft:swords").orElseThrow().text());
        assertTrue(ItemPattern.parse("diamond_sword").isEmpty());
        assertTrue(ItemPattern.parse("*bad pattern").isEmpty());
    }

    @Test
    void constructorNormalizesFilterIdsForLookupAndRemoval() {
        FilterRule filter = new FilterRule(new ItemFilter(Material.DIAMOND_SWORD, null, null, null,
                null, null, null), ItemRule.blockEverything());
        Preset preset = new Preset("default", "", "", WorldScope.ALL, RegionScope.ALL,
                Map.of(), Map.of(), Set.of(), Map.of(), Map.of("Excalibur", filter), LevelLimits.NONE, LevelLimits.NONE);

        assertEquals(Set.of("excalibur"), preset.filters().keySet());
        assertEquals(filter, preset.filter("EXCALIBUR").orElseThrow());
        assertTrue(preset.withoutFilter("Excalibur").filters().isEmpty());
    }
}
