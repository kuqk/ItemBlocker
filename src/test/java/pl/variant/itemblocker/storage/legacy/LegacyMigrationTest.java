package pl.variant.itemblocker.storage.legacy;

import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockbukkit.mockbukkit.MockBukkit;
import pl.variant.itemblocker.config.Settings;
import pl.variant.itemblocker.rule.Action;
import pl.variant.itemblocker.rule.ItemRule;
import pl.variant.itemblocker.rule.Preset;
import pl.variant.itemblocker.rule.RuleBook;
import pl.variant.itemblocker.rule.WorldScope;
import pl.variant.itemblocker.storage.RuleFile;
import pl.variant.itemblocker.testing.RecordingLogger;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LegacyMigrationTest {

    // The default blocked-items.yml shipped with 1.1.x and 1.2.0.
    private static final String BLOCKED_ITEMS = """
            default:
              reason: "&cThis item is blocked globally by the server team."
              enchantments:
                sharpness: 2
                mending: 1
              potions:
                strength: 2
              items:
                TNT:
                  actions: all
                  worlds:
                    - world
                    - world_nether
                BEDROCK:
                  actions: all
                  worlds: all
                NETHERITE_SWORD:
                  actions:
                    - smithing
                  worlds: all
            """;

    // Older value formats that 1.x still accepted.
    private static final String PRESETS = """
            presets:
              survival:
                description: "Example survival preset."
                reason: "&cThis item is disabled on survival worlds."
                worlds:
                  mode: whitelist
                  list: [world]
                enchantments:
                  - infinity
                items:
                  ENDER_PEARL:
                    - use
                    - drop
                  ELYTRA: "use, world_the_end"
                  SPAWNER:
                    - actions: [place]
                      worlds: [world]
                    - actions: [pickup]
                      worlds: [spawn]
                  FIRE_CHARGE:
                    actions: none
              "Event Mode":
                items: {}
            """;

    private static final String OLD_CONFIG = """
            language: "pl"
            messages:
              cooldown-ms: 1500
            bypass:
              enabled: true
              default-permission: "itemblocker.bypass"
              per-action:
                enabled: false
                prefix: "itemblocker.bypass."
            """;

    @TempDir
    Path folder;

    private final RecordingLogger logger = new RecordingLogger();
    private LegacyMigration migration;

    @BeforeEach
    void setUp() {
        MockBukkit.mock();
        migration = new LegacyMigration(folder, logger.logger());
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    void notNeededWithoutLegacyFiles() {
        assertFalse(migration.isNeeded());
    }

    @Test
    void notNeededOnceRulesExist() throws IOException {
        write("blocked-items.yml", BLOCKED_ITEMS);
        write(RuleFile.FILE_NAME, "presets: {}");

        assertFalse(migration.isNeeded());
    }

    @Test
    void migratesDefaultSection() throws IOException {
        write("blocked-items.yml", BLOCKED_ITEMS);

        assertTrue(migration.isNeeded());
        Preset defaults = migrate().defaultPreset();

        assertEquals("&cThis item is blocked globally by the server team.", defaults.reason());
        assertEquals(Map.of("mending", 1, "sharpness", 2), defaults.enchantments().minimumLevels());
        assertEquals(Map.of("strength", 2), defaults.effects().minimumLevels());
        assertEquals(new ItemRule(EnumSet.allOf(Action.class), WorldScope.of(List.of("world", "world_nether"))),
                defaults.item(Material.TNT).orElseThrow());
        assertEquals(ItemRule.blockEverything(), defaults.item(Material.BEDROCK).orElseThrow());
        assertEquals(EnumSet.of(Action.SMITHING), defaults.item(Material.NETHERITE_SWORD).orElseThrow().actions());
    }

    @Test
    void migratesOlderPresetFormats() throws IOException {
        write("presets.yml", PRESETS);

        RuleBook rules = migrate();
        Preset survival = rules.preset("survival").orElseThrow();

        assertEquals("Example survival preset.", survival.description());
        assertEquals(WorldScope.of(List.of("world")), survival.scope());
        assertEquals(Map.of("infinity", 1), survival.enchantments().minimumLevels());
        assertEquals(new ItemRule(EnumSet.of(Action.USE, Action.DROP), WorldScope.ALL),
                survival.item(Material.ENDER_PEARL).orElseThrow());
        assertEquals(new ItemRule(EnumSet.of(Action.USE), WorldScope.of(List.of("world_the_end"))),
                survival.item(Material.ELYTRA).orElseThrow());
        assertEquals(new ItemRule(EnumSet.of(Action.PLACE, Action.PICKUP), WorldScope.of(List.of("world", "spawn"))),
                survival.item(Material.SPAWNER).orElseThrow());
        assertTrue(survival.item(Material.FIRE_CHARGE).orElseThrow().actions().isEmpty());

        assertTrue(rules.preset("event-mode").isPresent());
        assertTrue(logger.warnings().contains("Renamed preset 'Event Mode' to 'event-mode'"), logger.warnings().toString());
    }

    @Test
    void movesOldFilesToBackupAndMigratesSettings() throws IOException {
        write("blocked-items.yml", BLOCKED_ITEMS);
        write("presets.yml", PRESETS);
        write("config.yml", OLD_CONFIG);
        write("languages/messages_en.yml", "prefix: test");

        migrate();

        Path backup = folder.resolve(LegacyMigration.BACKUP_FOLDER);
        for (String name : List.of("blocked-items.yml", "presets.yml", "config.yml", "languages/messages_en.yml")) {
            assertTrue(Files.exists(backup.resolve(name)), name);
            assertFalse(Files.exists(folder.resolve(name)) && !name.equals("config.yml"), name);
        }

        YamlConfiguration config = YamlConfiguration.loadConfiguration(folder.resolve("config.yml").toFile());
        assertEquals(2, config.getInt("config-version"));
        assertEquals(new Settings("pl", Duration.ofMillis(1500), true, false), Settings.from(config));
        assertFalse(migration.isNeeded());
    }

    @Test
    void secondBackupDoesNotOverwriteFirst() throws IOException {
        Files.createDirectories(folder.resolve(LegacyMigration.BACKUP_FOLDER));
        write("blocked-items.yml", BLOCKED_ITEMS);

        migrate();

        assertTrue(Files.exists(folder.resolve(LegacyMigration.BACKUP_FOLDER + "-2").resolve("blocked-items.yml")));
    }

    @Test
    void invalidLegacyFileChangesNothing() throws IOException {
        write("blocked-items.yml", "default: [unclosed");

        assertThrows(IOException.class, this::migrate);
        assertTrue(Files.exists(folder.resolve("blocked-items.yml")));
        assertFalse(Files.exists(folder.resolve(RuleFile.FILE_NAME)));
    }

    private RuleBook migrate() throws IOException {
        migration.migrate(bundledConfig());
        return new RuleFile(folder.resolve(RuleFile.FILE_NAME), logger.logger()).load();
    }

    private String bundledConfig() throws IOException {
        try (InputStream stream = getClass().getResourceAsStream("/config.yml")) {
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private void write(String name, String content) throws IOException {
        Path file = folder.resolve(name);
        Files.createDirectories(file.getParent());
        Files.writeString(file, content, StandardCharsets.UTF_8);
    }
}
