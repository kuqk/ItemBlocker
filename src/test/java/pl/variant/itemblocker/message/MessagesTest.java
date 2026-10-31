package pl.variant.itemblocker.message;

import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.FieldSource;
import pl.variant.itemblocker.integration.PlaceholderResolver;
import pl.variant.itemblocker.testing.RecordingLogger;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static pl.variant.itemblocker.testing.Text.plain;

class MessagesTest {

    static final List<String> LANGUAGES = Messages.LANGUAGES;

    private static final Pattern PLACEHOLDER = Pattern.compile("\\{([a-z_]+)}");

    @TempDir
    Path folder;

    private final RecordingLogger logger = new RecordingLogger();
    private Messages messages;

    @BeforeEach
    void setUp() {
        messages = new Messages(folder, MessagesTest::bundled, logger.logger(), PlaceholderResolver.NONE);
    }

    @ParameterizedTest
    @FieldSource("LANGUAGES")
    void everyMessageIsTranslatedWithTheSamePlaceholders(String language) throws Exception {
        YamlConfiguration english = load("en");
        YamlConfiguration translation = load(language);

        for (Message message : Message.values()) {
            String text = translation.getString(message.path());
            assertNotNull(text, language + " is missing " + message.path());
            assertEquals(placeholders(english.getString(message.path())), placeholders(text),
                    language + " uses different placeholders in " + message.path());
        }
    }

    @Test
    void rendersPlaceholdersPrefixAndColors() {
        messages.load("en");

        String text = messages.render(Message.ITEM_UNBLOCKED, Placeholder.of("item", "tnt"), Placeholder.of("preset", "default"));

        assertEquals("ItemBlocker » Unblocked tnt in default.", plain(text));
        assertTrue(text.startsWith("§6ItemBlocker"), text);
    }

    @Test
    void placeholderValuesAreNotExpandedAgain() {
        messages.load("en");

        String text = messages.render(Message.PRESET_CREATED, Placeholder.of("preset", "{prefix}"));

        assertTrue(plain(text).endsWith("Created preset {prefix}."), text);
    }

    @Test
    void translatesHexColors() {
        assertEquals("§x§f§f§8§8§0§0Hi §aok", Messages.colorize("&#ff8800Hi &aok"));
    }

    @Test
    void copiesBundledFilesAndPrefersServerEdits() throws IOException {
        messages.load("pl");
        for (String language : LANGUAGES) {
            assertTrue(Files.exists(folder.resolve(language + ".yml")), language);
        }

        Files.writeString(folder.resolve("pl.yml"), "prefix: \"[IB] \"\nreloaded: \"{prefix}przeładowano\"\n", StandardCharsets.UTF_8);
        messages.load("pl");

        assertEquals("[IB] przeładowano", messages.render(Message.RELOADED));
        assertEquals("[IB] Nie masz do tego uprawnień.", plain(messages.render(Message.ERROR_NO_PERMISSION)));
    }

    @Test
    void unknownLanguageFallsBackToEnglish() {
        messages.load("xx");

        assertEquals("ItemBlocker » Reloaded the configuration, rules and messages.", plain(messages.render(Message.RELOADED)));
        assertEquals(1, logger.warnings().size());
    }

    @Test
    void customLanguageFileIsUsed() throws IOException {
        Files.createDirectories(folder);
        Files.writeString(folder.resolve("nl.yml"), "reloaded: \"Herladen\"\n", StandardCharsets.UTF_8);

        messages.load("nl");

        assertEquals("Herladen", messages.render(Message.RELOADED));
        assertTrue(logger.warnings().isEmpty(), logger.warnings().toString());
    }

    private static Set<String> placeholders(String text) {
        Set<String> names = new TreeSet<>();
        Matcher matcher = PLACEHOLDER.matcher(text);
        while (matcher.find()) {
            names.add(matcher.group(1));
        }
        return names;
    }

    private static YamlConfiguration load(String language) throws IOException, InvalidConfigurationException {
        YamlConfiguration yaml = new YamlConfiguration();
        try (InputStream stream = bundled("lang/" + language + ".yml")) {
            assertNotNull(stream, language);
            yaml.loadFromString(new String(stream.readAllBytes(), StandardCharsets.UTF_8));
        }
        return yaml;
    }

    private static InputStream bundled(String path) {
        return MessagesTest.class.getResourceAsStream("/" + path);
    }
}
