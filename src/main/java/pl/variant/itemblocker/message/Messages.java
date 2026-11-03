package pl.variant.itemblocker.message;

import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.chat.TranslatableComponent;
import net.md_5.bungee.api.chat.hover.content.Text;
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
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

    // Prefixes bundled with earlier versions. Files that still have one unchanged get the current one.
    private static final List<String> OLD_PREFIX_LINES = List.of(
            "prefix: \"&6ItemBlocker &8»&r \"",
            "prefix: \"&8[&6ItemBlocker&8]&r \""
    );
    private static final String PREFIX_LINE = "prefix: \"&8[&5ItemBlocker&8]&r \"";

    private static final String BUTTON = "button";
    private static final Pattern OLD_PROMPT_CANCEL = Pattern.compile("(?m)^(  prompt: .*)&fcancel(?=&e)");
    // Private use characters, which never show up in a rendered message by themselves.
    private static final char FIRST_MARKER = '\uE000';

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
            upgradeOldPrefix(fileOf(bundledLanguage));
            upgradeCancelButton(fileOf(bundledLanguage));
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

    /**
     * Sends a message. Players see item, enchantment and effect names in the language of their own
     * game; the console gets the English names.
     */
    public void send(CommandSender recipient, Message message, Placeholder... placeholders) {
        if (recipient instanceof Player player && Arrays.stream(placeholders).anyMatch(p -> p.translationKey() != null)) {
            sendComponents(player, message, Map.of(), placeholders);
        } else {
            recipient.sendMessage(render(recipient instanceof Player player ? player : null, message, placeholders));
        }
    }

    /**
     * Sends a message in which the placeholder {@code {button}} is a button: clicking it runs
     * {@code command} and hovering shows {@code hover}. Without the placeholder, for example in an
     * edited language file, the message is sent as plain text.
     */
    // TextComponent.fromLegacy, which replaces fromLegacyText, only exists on newer servers.
    @SuppressWarnings("deprecation")
    public void sendWithButton(Player player, Message message, Message label, Message hover, String command,
                               Placeholder... placeholders) {
        TextComponent button = new TextComponent(TextComponent.fromLegacyText(render(player, label)));
        button.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, command));
        button.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text(TextComponent.fromLegacyText(render(player, hover)))));
        sendComponents(player, message, Map.of(BUTTON, button), placeholders);
    }

    /**
     * The word that cancels a chat prompt besides {@code cancel}, which is the label of the cancel
     * button without colors.
     */
    public String cancelWord() {
        String word = ChatColor.stripColor(render(Message.GUI_PROMPT_CANCEL));
        return word == null ? "" : word.trim();
    }

    /**
     * Sends a message as chat components: placeholders with a translation key become names the game
     * translates, and {@code components} replace their placeholders as they are. Each of them is
     * rendered as a private use character first, and the text is cut there.
     */
    @SuppressWarnings("deprecation")
    private void sendComponents(Player player, Message message, Map<String, BaseComponent> components,
                                Placeholder... placeholders) {
        Map<Character, BaseComponent> byMarker = new HashMap<>();
        Set<Character> styled = new HashSet<>();
        List<Placeholder> rendered = new ArrayList<>();
        char marker = FIRST_MARKER;
        for (Placeholder placeholder : placeholders) {
            if (placeholder.translationKey() == null) {
                rendered.add(placeholder);
                continue;
            }
            byMarker.put(marker, new TranslatableComponent(placeholder.translationKey()));
            styled.add(marker);
            rendered.add(Placeholder.of(placeholder.name(), marker++));
        }
        for (Map.Entry<String, BaseComponent> component : components.entrySet()) {
            byMarker.put(marker, component.getValue());
            rendered.add(Placeholder.of(component.getKey(), marker++));
        }

        String text = render(player, message, rendered.toArray(Placeholder[]::new));
        List<BaseComponent> parts = new ArrayList<>();
        int start = 0;
        for (int at = 0; at < text.length(); at++) {
            BaseComponent component = byMarker.get(text.charAt(at));
            if (component == null) {
                continue;
            }
            // Every piece continues in the colors that were active before it.
            String active = ChatColor.getLastColors(text.substring(0, at));
            parts.addAll(List.of(TextComponent.fromLegacyText(ChatColor.getLastColors(text.substring(0, start)) + text.substring(start, at))));
            if (styled.contains(text.charAt(at))) {
                BaseComponent[] style = TextComponent.fromLegacyText(active + " ");
                component.copyFormatting(style[style.length - 1], ComponentBuilder.FormatRetention.FORMATTING, true);
            }
            parts.add(component);
            start = at + 1;
        }
        if (parts.isEmpty()) {
            player.sendMessage(text);
            return;
        }
        parts.addAll(List.of(TextComponent.fromLegacyText(ChatColor.getLastColors(text.substring(0, start)) + text.substring(start))));
        player.spigot().sendMessage(parts.toArray(BaseComponent[]::new));
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

    private void upgradeOldPrefix(Path file) {
        try {
            if (!Files.exists(file)) {
                return;
            }
            String text = Files.readString(file, StandardCharsets.UTF_8);
            for (String oldLine : OLD_PREFIX_LINES) {
                if (text.lines().anyMatch(oldLine::equals)) {
                    Files.writeString(file, text.replace(oldLine, PREFIX_LINE), StandardCharsets.UTF_8);
                    return;
                }
            }
        } catch (IOException exception) {
            logger.warning("Could not update the prefix in " + file.getFileName() + ": " + exception.getMessage());
        }
    }

    /**
     * Files from before the cancel button have the plain word in the chat prompt. Only that word is
     * swapped for the button; the rest of the line stays as the server owner left it.
     */
    private void upgradeCancelButton(Path file) {
        try {
            if (!Files.exists(file)) {
                return;
            }
            String text = Files.readString(file, StandardCharsets.UTF_8);
            String upgraded = OLD_PROMPT_CANCEL.matcher(text).replaceAll("$1{" + BUTTON + "}");
            if (!upgraded.equals(text)) {
                Files.writeString(file, upgraded, StandardCharsets.UTF_8);
            }
        } catch (IOException exception) {
            logger.warning("Could not add the cancel button to " + file.getFileName() + ": " + exception.getMessage());
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
