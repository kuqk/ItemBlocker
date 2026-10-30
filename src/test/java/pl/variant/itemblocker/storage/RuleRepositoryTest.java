package pl.variant.itemblocker.storage;

import org.bukkit.Material;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockbukkit.mockbukkit.MockBukkit;
import pl.variant.itemblocker.rule.ItemRule;
import pl.variant.itemblocker.rule.Preset;
import pl.variant.itemblocker.testing.RecordingLogger;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RuleRepositoryTest {

    @TempDir
    Path folder;

    private RuleFile file;
    private RuleRepository repository;

    @BeforeEach
    void setUp() {
        MockBukkit.mock();
        file = new RuleFile(folder.resolve(RuleFile.FILE_NAME), new RecordingLogger().logger());
        repository = new RuleRepository(file);
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    void createsMissingFileOnReload() throws IOException {
        repository.reload();

        assertTrue(file.exists());
        assertTrue(repository.rules().defaultPreset().items().isEmpty());
    }

    @Test
    void updatesArePersisted() throws IOException {
        repository.reload();

        repository.update(rules -> rules.with(Preset.empty("pvp").withItem(Material.TNT, ItemRule.blockEverything())));

        assertTrue(repository.rules().preset("pvp").isPresent());
        assertTrue(file.load().preset("pvp").orElseThrow().item(Material.TNT).isPresent());
    }

    @Test
    void refusesChangesWhileFileIsBroken() throws IOException {
        repository.reload();
        repository.update(rules -> rules.with(Preset.empty("pvp")));
        Files.writeString(file.path(), "presets: [unclosed", StandardCharsets.UTF_8);

        assertThrows(IOException.class, repository::reload);
        assertTrue(repository.rules().preset("pvp").isPresent(), "previous rules stay active");
        assertThrows(IOException.class, () -> repository.update(rules -> rules.with(Preset.empty("other"))));
        assertEquals("presets: [unclosed", Files.readString(file.path(), StandardCharsets.UTF_8));
    }
}
