package pl.variant.itemblocker.message;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import pl.variant.itemblocker.integration.PlaceholderResolver;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Loads the message file for the configured language and renders messages with placeholders and
 * colors. Entries missing from a server's file fall back to the bundled text for that language,
 * then to English. Messages sent to a player also get placeholders of other plugins filled in.
 */
public final class Messages {

    public static final List<String> LANGUAGES = List.of("en", "pl", "de", "es", "fr", "it");
    public static final String FALLBACK_LANGUAGE = "en";

    private static final Pattern HEX_COLOR = Pattern.compile("&#([0-9a-fA-F]{6})");
    private static final Pattern PLACEHOLDER = Pattern.compile("\\{([a-z_]+)}");

    private final Path folder;
    private final Function<String, InputStream> bundledFiles;
    private final Logger logger;
    private final PlaceholderResolver placeholderResolver;
    private volatile YamlConfiguration messages = new YamlConfiguration();

    /**
     * @param folder       folder holding the editable language files
     * @param bundledFiles opens a bundled file by its path inside the jar, or returns {@code null}
     * @param logger       logger for problems with the language files
     * @param placeholderResolver fills in placeholders of other plugins in messages sent to players
     */
    public Messages(Path folder, Function<String, InputStream> bundledFiles, Logger logger,
                    PlaceholderResolver placeholderResolver) {
        this.folder = folder;
        this.bundledFiles = bundledFiles;
        this.logger = logger;
        this.placeholderResolver = placeholderResolver;
    }

    /**
     * Copies missing language files to the folder and loads the given language.
     */
    public void load(String language) {
        for (String bundledLanguage : LANGUAGES) {
            copyIfMissing(bundledLanguage);
        }

        String resolved = language;
        if (!LANGUAGES.contains(language) && !Files.exists(fileOf(language))) {
            logger.warning("No messages for language '" + language + "', using " + FALLBACK_LANGUAGE);
            resolved = FALLBACK_LANGUAGE;
        }

        YamlConfiguration english = bundled(FALLBACK_LANGUAGE).orElseGet(YamlConfiguration::new);
        YamlConfiguration defaults = bundled(resolved).orElse(english);
        if (defaults != english) {
            defaults.setDefaults(english);
        }

        YamlConfiguration loaded = readFile(fileOf(resolved));
        loaded.setDefaults(defaults);
        messages = loaded;
    }

    public String render(Message message, Placeholder... placeholders) {
        return render(null, message, placeholders);
    }

    public void send(CommandSender recipient, Message message, Placeholder... placeholders) {
        recipient.sendMessage(render(recipient instanceof Player player ? player : null, message, placeholders));
    }

    /**
     * @param viewer player whose placeholders of other plugins are filled in, or {@code null}
     */
    private String render(Player viewer, Message message, Placeholder... placeholders) {
        Map<String, String> values = new HashMap<>();
        values.put("prefix", template(viewer, Message.PREFIX));
        for (Placeholder placeholder : placeholders) {
            values.put(placeholder.name(), placeholder.value());
        }

        // A single pass, so placeholder values are never scanned for placeholders themselves.
        Matcher matcher = PLACEHOLDER.matcher(template(viewer, message));
        StringBuilder text = new StringBuilder();
        while (matcher.find()) {
            String value = values.getOrDefault(matcher.group(1), matcher.group());
            matcher.appendReplacement(text, Matcher.quoteReplacement(value));
        }
        matcher.appendTail(text);
        return colorize(text.toString());
    }

    private String template(Player viewer, Message message) {
        // getString(path, fallback) would skip the default files, so the fallback is applied here.
        String template = messages.getString(message.path());
        if (template == null) {
            return message.path();
        }
        // Resolved before our own placeholders, so item names and reasons are never treated as
        // placeholders of other plugins.
        return viewer == null ? template : placeholderResolver.resolve(viewer, template);
    }

    /**
     * Translates {@code &} color codes and {@code &#rrggbb} hex colors.
     */
    static String colorize(String text) {
        Matcher matcher = HEX_COLOR.matcher(text);
        StringBuilder result = new StringBuilder();
        while (matcher.find()) {
            StringBuilder hex = new StringBuilder("&x");
            for (char digit : matcher.group(1).toCharArray()) {
                hex.append('&').append(digit);
            }
            matcher.appendReplacement(result, hex.toString());
        }
        matcher.appendTail(result);
        return ChatColor.translateAlternateColorCodes('&', result.toString());
    }

    private Path fileOf(String language) {
        return folder.resolve(language + ".yml");
    }

    private void copyIfMissing(String language) {
        Path target = fileOf(language);
        if (Files.exists(target)) {
            return;
        }

        try (InputStream stream = bundledFiles.apply(resourcePath(language))) {
            if (stream != null) {
                Files.createDirectories(folder);
                Files.copy(stream, target);
            }
        } catch (IOException exception) {
            logger.warning("Could not copy the " + language + " language file: " + exception.getMessage());
        }
    }

    private Optional<YamlConfiguration> bundled(String language) {
        try (InputStream stream = bundledFiles.apply(resourcePath(language))) {
            if (stream == null) {
                return Optional.empty();
            }
            YamlConfiguration yaml = new YamlConfiguration();
            yaml.loadFromString(new String(stream.readAllBytes(), StandardCharsets.UTF_8));
            return Optional.of(yaml);
        } catch (IOException | InvalidConfigurationException exception) {
            logger.warning("Could not read the bundled " + language + " language file: " + exception.getMessage());
            return Optional.empty();
        }
    }

    private YamlConfiguration readFile(Path file) {
        YamlConfiguration yaml = new YamlConfiguration();
        if (!Files.exists(file)) {
            return yaml;
        }

        try {
            yaml.loadFromString(Files.readString(file, StandardCharsets.UTF_8));
        } catch (IOException | InvalidConfigurationException exception) {
            logger.warning("Could not read " + folder.getFileName() + "/" + file.getFileName()
                    + ", using the bundled messages: " + exception.getMessage());
        }
        return yaml;
    }

    private static String resourcePath(String language) {
        return "lang/" + language + ".yml";
    }
}
