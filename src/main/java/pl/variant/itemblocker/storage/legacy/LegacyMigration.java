package pl.variant.itemblocker.storage.legacy;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import pl.variant.itemblocker.config.Settings;
import pl.variant.itemblocker.rule.RuleBook;
import pl.variant.itemblocker.storage.RuleFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.logging.Logger;

/**
 * Converts the data folder of ItemBlocker 1.x to the 2.x layout.
 *
 * <p>Rules from {@code blocked-items.yml} and {@code presets.yml} are written to {@code rules.yml},
 * settings from the old {@code config.yml} are carried over, and every old file is moved to
 * {@value #BACKUP_FOLDER} instead of being deleted.
 */
public final class LegacyMigration {

    public static final String BACKUP_FOLDER = "backup-1.x";

    private static final String BLOCKED_ITEMS_FILE = "blocked-items.yml";
    private static final String PRESETS_FILE = "presets.yml";
    private static final String CONFIG_FILE = "config.yml";
    private static final String LANGUAGES_FOLDER = "languages";

    private final Path dataFolder;
    private final Logger logger;

    public LegacyMigration(Path dataFolder, Logger logger) {
        this.dataFolder = dataFolder;
        this.logger = logger;
    }

    /**
     * Returns whether the data folder holds 1.x rule files and no 2.x rules yet.
     */
    public boolean isNeeded() {
        return !Files.exists(dataFolder.resolve(RuleFile.FILE_NAME))
                && (Files.exists(dataFolder.resolve(BLOCKED_ITEMS_FILE)) || Files.exists(dataFolder.resolve(PRESETS_FILE)));
    }

    /**
     * Runs the migration.
     *
     * @param defaultConfig contents of the bundled 2.x {@code config.yml}, used as the template for
     *                      the migrated settings so its comments are kept
     * @throws IOException if an old file cannot be read, in which case nothing has changed, or the
     *                     new files cannot be written
     */
    public void migrate(String defaultConfig) throws IOException {
        logger.info("Migrating ItemBlocker 1.x files to the 2.x format");

        YamlConfiguration blockedItems = loadIfExists(BLOCKED_ITEMS_FILE);
        YamlConfiguration presets = loadIfExists(PRESETS_FILE);
        YamlConfiguration oldConfig = loadIfExists(CONFIG_FILE);

        RuleBook rules = new LegacyRuleReader(logger).read(blockedItems, presets);
        String newConfig = oldConfig == null || oldConfig.contains("config-version")
                ? null
                : migrateConfig(oldConfig, defaultConfig);

        // rules.yml is written first: once it exists the migration never runs again, so an
        // interrupted migration leaves the old files in place instead of losing the rules.
        new RuleFile(dataFolder.resolve(RuleFile.FILE_NAME), logger).save(rules);

        Path backup = createBackupFolder();
        for (String name : List.of(BLOCKED_ITEMS_FILE, PRESETS_FILE, LANGUAGES_FOLDER)) {
            moveIfExists(name, backup);
        }
        if (newConfig != null) {
            moveIfExists(CONFIG_FILE, backup);
            Files.writeString(dataFolder.resolve(CONFIG_FILE), newConfig, StandardCharsets.UTF_8);
        }

        logger.info("Migrated " + rules.presets().size() + " presets. The old files are in "
                + dataFolder.relativize(backup) + ". Custom message texts were not migrated, edit the files in the lang folder instead.");
    }

    private Path createBackupFolder() throws IOException {
        Path backup = dataFolder.resolve(BACKUP_FOLDER);
        for (int attempt = 2; Files.exists(backup); attempt++) {
            backup = dataFolder.resolve(BACKUP_FOLDER + "-" + attempt);
        }
        return Files.createDirectories(backup);
    }

    private static String migrateConfig(ConfigurationSection oldConfig, String defaultConfig) throws IOException {
        YamlConfiguration config = new YamlConfiguration();
        try {
            config.loadFromString(defaultConfig);
        } catch (InvalidConfigurationException exception) {
            throw new IOException("The bundled config.yml is not valid YAML", exception);
        }

        copy(oldConfig, "language", config, Settings.LANGUAGE);
        copy(oldConfig, "messages.cooldown-ms", config, Settings.MESSAGE_COOLDOWN);
        copy(oldConfig, "bypass.enabled", config, Settings.BYPASS_ENABLED);
        copy(oldConfig, "bypass.per-action.enabled", config, Settings.PER_ACTION_BYPASS);
        return config.saveToString();
    }

    private static void copy(ConfigurationSection from, String oldPath, ConfigurationSection to, String newPath) {
        if (from.contains(oldPath)) {
            to.set(newPath, from.get(oldPath));
        }
    }

    private YamlConfiguration loadIfExists(String name) throws IOException {
        Path file = dataFolder.resolve(name);
        if (!Files.exists(file)) {
            return null;
        }

        YamlConfiguration yaml = new YamlConfiguration();
        try {
            yaml.loadFromString(Files.readString(file, StandardCharsets.UTF_8));
        } catch (InvalidConfigurationException exception) {
            throw new IOException(name + " is not valid YAML, fix it before upgrading: " + exception.getMessage(), exception);
        }
        return yaml;
    }

    private void moveIfExists(String name, Path backup) throws IOException {
        Path source = dataFolder.resolve(name);
        if (Files.exists(source)) {
            Files.move(source, backup.resolve(name));
        }
    }
}
