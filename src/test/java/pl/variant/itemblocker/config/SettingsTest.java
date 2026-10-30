package pl.variant.itemblocker.config;

import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class SettingsTest {

    @Test
    void bundledConfigMatchesDefaults() throws IOException, InvalidConfigurationException {
        YamlConfiguration config = new YamlConfiguration();
        try (InputStream stream = getClass().getResourceAsStream("/config.yml")) {
            assertNotNull(stream);
            config.loadFromString(new String(stream.readAllBytes(), StandardCharsets.UTF_8));
        }

        assertEquals(Settings.DEFAULTS, Settings.from(config));
    }

    @Test
    void readsValuesAndClampsNegativeCooldown() {
        YamlConfiguration config = new YamlConfiguration();
        config.set(Settings.LANGUAGE, " PL ");
        config.set(Settings.MESSAGE_COOLDOWN, -5);
        config.set(Settings.BYPASS_ENABLED, false);
        config.set(Settings.PER_ACTION_BYPASS, false);

        assertEquals(new Settings("pl", Duration.ZERO, false, false), Settings.from(config));
    }

    @Test
    void missingValuesUseDefaults() {
        assertEquals(Settings.DEFAULTS, Settings.from(new YamlConfiguration()));
    }
}
