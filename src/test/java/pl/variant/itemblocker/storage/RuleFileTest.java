package pl.variant.itemblocker.storage;

import org.bukkit.Material;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockbukkit.mockbukkit.MockBukkit;
import pl.variant.itemblocker.rule.Action;
import pl.variant.itemblocker.rule.ItemRule;
import pl.variant.itemblocker.rule.LevelLimits;
import pl.variant.itemblocker.rule.Preset;
import pl.variant.itemblocker.rule.RuleBook;
import pl.variant.itemblocker.rule.WorldScope;
import pl.variant.itemblocker.testing.RecordingLogger;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RuleFileTest {

    @TempDir
    Path folder;

    private final RecordingLogger logger = new RecordingLogger();
    private RuleFile file;

    @BeforeEach
    void setUp() {
        MockBukkit.mock();
        file = new RuleFile(folder.resolve(RuleFile.FILE_NAME), logger.logger());
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    void savedRulesLoadBackUnchanged() throws IOException {
        RuleBook rules = RuleBook.empty()
                .with(Preset.empty(RuleBook.DEFAULT_PRESET)
                        .withReason("&cBlocked by the server")
                        .withItem(Material.TNT, new ItemRule(EnumSet.of(Action.PLACE, Action.CRAFTING), WorldScope.of(List.of("world"))))
                        .withItem(Material.BEDROCK, ItemRule.blockEverything())
                        .withEnchantments(LevelLimits.NONE.with("sharpness", 5)))
                .with(Preset.empty("pvp")
                        .withDescription("Arena rules")
                        .withScope(WorldScope.of(List.of("arena", "arena_nether")))
                        .withItem(Material.ENDER_PEARL, new ItemRule(Set.of(), WorldScope.ALL))
                        .withEffects(LevelLimits.NONE.with("strength", 2)));

        file.save(rules);
        RuleBook loaded = file.load();

        assertEquals(rules.presets(), loaded.presets());
        assertTrue(logger.warnings().isEmpty(), logger.warnings().toString());
    }

    @Test
    void writesReadableYaml() throws IOException {
        file.save(RuleBook.empty().with(Preset.empty(RuleBook.DEFAULT_PRESET)
                .withItem(Material.TNT, new ItemRule(EnumSet.of(Action.PLACE), WorldScope.ALL))));

        String content = Files.readString(file.path(), StandardCharsets.UTF_8);

        assertTrue(content.startsWith("# ItemBlocker rules"), content);
        assertTrue(content.contains("""
                    items:
                      tnt:
                        actions:
                        - place
                        worlds: all
                """), content);
    }

    @Test
    void skipsInvalidEntriesWithWarnings() throws IOException {
        Files.writeString(file.path(), """
                presets:
                  default:
                    items:
                      not_an_item:
                        actions: all
                      tnt:
                        actions: [place, fly]
                      stone: all
                    enchantments:
                      sharpness: 0
                      mending: 1
                  "Bad Name":
                    items: {}
                """, StandardCharsets.UTF_8);

        RuleBook rules = file.load();

        Preset defaults = rules.defaultPreset();
        assertEquals(Map.of(Material.TNT, new ItemRule(EnumSet.of(Action.PLACE), WorldScope.ALL)), defaults.items());
        assertEquals(Map.of("mending", 1), defaults.enchantments().minimumLevels());
        assertEquals(List.of("default"), rules.presets().stream().map(Preset::name).toList());
        assertEquals(5, logger.warnings().size(), logger.warnings().toString());
    }

    @Test
    void missingActionsBlockEverything() throws IOException {
        Files.writeString(file.path(), """
                presets:
                  default:
                    items:
                      tnt:
                        worlds: [world]
                """, StandardCharsets.UTF_8);

        ItemRule rule = file.load().defaultPreset().item(Material.TNT).orElseThrow();

        assertTrue(rule.blocksEveryAction());
        assertFalse(rule.scope().isAll());
    }

    @Test
    void invalidYamlFails() throws IOException {
        Files.writeString(file.path(), "presets: [unclosed", StandardCharsets.UTF_8);

        assertThrows(IOException.class, file::load);
    }
}
