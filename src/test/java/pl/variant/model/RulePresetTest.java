package pl.variant.model;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RulePresetTest {

    @Test
    void parsesCommaSeparatedWorldStringFromConfig() {
        YamlConfiguration config = new YamlConfiguration();
        ConfigurationSection section = config.createSection("survival");
        section.set("worlds", "world,world_nether");

        RulePreset preset = RulePreset.fromSection("survival", section);

        assertTrue(preset.matchesWorld("world"));
        assertTrue(preset.matchesWorld("world_nether"));
        assertFalse(preset.matchesWorld("spawn"));
    }

    @Test
    void parsesLegacyItemListInsidePresetSection() {
        YamlConfiguration config = new YamlConfiguration();
        ConfigurationSection section = config.createSection("survival");
        section.set("items.ender_pearl", java.util.List.of("use", "drop"));

        RulePreset preset = RulePreset.fromSection("survival", section);

        assertTrue(preset.matches(Material.ENDER_PEARL, BlockAction.USE, "world"));
        assertTrue(preset.matches(Material.ENDER_PEARL, BlockAction.DROP, "world"));
        assertFalse(preset.matches(Material.ENDER_PEARL, BlockAction.PLACE, "world"));
    }
}
