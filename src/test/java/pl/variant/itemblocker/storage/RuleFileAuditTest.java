package pl.variant.itemblocker.storage;

import org.bukkit.NamespacedKey;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockbukkit.mockbukkit.MockBukkit;
import pl.variant.itemblocker.rule.ItemPattern;
import pl.variant.itemblocker.rule.ItemRule;
import pl.variant.itemblocker.rule.LevelLimits;
import pl.variant.itemblocker.rule.Preset;
import pl.variant.itemblocker.rule.RuleBook;
import pl.variant.itemblocker.testing.RecordingLogger;

import java.io.IOException;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Opt-in regression cases discovered during the project audit. */
class RuleFileAuditTest {

    @TempDir
    Path folder;

    private RuleFile file;

    @BeforeEach
    void setUp() {
        MockBukkit.mock();
        file = new RuleFile(folder.resolve(RuleFile.FILE_NAME), new RecordingLogger().logger());
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    void acceptedMixedCaseFilterIdsCanBeLookedUpAndRemoved() throws IOException {
        Preset preset = file.parse("""
                presets:
                  default:
                    filters:
                      Excalibur:
                        type: diamond_sword
                        actions: [use]
                """).defaultPreset();

        assertTrue(preset.filter("Excalibur").isPresent(),
                "An accepted filter must remain accessible by its own id");
        assertTrue(preset.withoutFilter("Excalibur").filters().isEmpty());
    }

    @Test
    void validDottedEnchantmentKeysSurviveSaving() throws IOException {
        String key = "example:power.up";
        assertNotNull(NamespacedKey.fromString(key), "The server accepts this registry key");
        RuleBook original = RuleBook.empty().with(Preset.empty("default")
                .withEnchantments(LevelLimits.NONE.with(key, 3)));

        file.save(original);

        assertEquals(original.defaultPreset().enchantments(), file.load().defaultPreset().enchantments());
    }

    @Test
    void validDottedTagKeysSurviveSaving() throws IOException {
        ItemPattern pattern = ItemPattern.parse("#example:items.swords").orElseThrow();
        RuleBook original = RuleBook.empty().with(Preset.empty("default")
                .withGroup(pattern, ItemRule.blockEverything()));

        file.save(original);

        assertEquals(original.defaultPreset().groups(), file.load().defaultPreset().groups());
    }
}
