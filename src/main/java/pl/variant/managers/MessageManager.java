package pl.variant.managers;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import pl.variant.itemBlocker;
import pl.variant.model.BlockAction;
import pl.variant.model.BlockCheckResult;
import pl.variant.utils.MessageCooldown;
import pl.variant.utils.TextUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.LinkedHashMap;
import java.util.Map;

public class MessageManager {

    private final itemBlocker plugin;
    private volatile FileConfiguration messagesConfig;
    private volatile File messagesFile;
    private volatile String currentLanguage;
    private volatile MessageCooldown messageCooldown;

    public MessageManager(itemBlocker plugin) {
        this.plugin = plugin;
    }

    public void loadMessages() {
        ensureLanguageFiles();

        currentLanguage = plugin.getConfigManager().getLanguage();
        String resourcePath = "languages/messages_" + currentLanguage + ".yml";
        messagesFile = new File(plugin.getDataFolder(), resourcePath);

        if (!messagesFile.exists()) {
            plugin.getLogger().warning("Language file " + resourcePath + " not found. Falling back to English.");
            currentLanguage = "en";
            resourcePath = "languages/messages_en.yml";
            messagesFile = new File(plugin.getDataFolder(), resourcePath);
        }

        messagesConfig = loadYaml(messagesFile, resourcePath);
        messageCooldown = new MessageCooldown(plugin.getConfigManager().getMessageCooldownMillis());

        plugin.getLogger().info("Loaded language: " + currentLanguage);
    }

    public void reloadMessages() {
        loadMessages();
    }

    public String getMessage(String path) {
        return getMessage(path, Map.of());
    }

    public String getMessage(String path, String placeholder, String value) {
        return getMessage(path, Map.of(placeholder, value));
    }

    public String getMessage(String path, Map<String, String> placeholders) {
        String rawMessage = messagesConfig.getString(path, "&cMissing message: " + path);

        Map<String, String> mergedPlaceholders = new LinkedHashMap<>();
        mergedPlaceholders.put("{prefix}", getPrefix());
        mergedPlaceholders.putAll(placeholders);

        String formatted = rawMessage;
        for (Map.Entry<String, String> entry : mergedPlaceholders.entrySet()) {
            formatted = formatted.replace(entry.getKey(), entry.getValue() == null ? "" : entry.getValue());
        }

        return ChatColor.translateAlternateColorCodes('&', formatted);
    }

    public void sendMessage(CommandSender sender, String path) {
        sender.sendMessage(getMessage(path));
    }

    public void sendMessage(CommandSender sender, String path, String placeholder, String value) {
        sender.sendMessage(getMessage(path, placeholder, value));
    }

    public void sendMessage(CommandSender sender, String path, Map<String, String> placeholders) {
        sender.sendMessage(getMessage(path, placeholders));
    }

    public void sendBlockedMessage(Player player, BlockAction action, Material material, BlockCheckResult result) {
        if (player == null || material == null) {
            return;
        }

        if (!messageCooldown.canSend(player.getUniqueId())) {
            return;
        }

        String reasonSuffix = "";
        if (result.getReason() != null && !result.getReason().isBlank()) {
            reasonSuffix = getMessage("blocked-reason-suffix", Map.of("{reason}", result.getReason()));
        }

        String sourceSuffix = "";
        if (result.getSourceName() != null && !result.getSourceName().isBlank()) {
            sourceSuffix = getMessage(
                    "blocked-source-suffix",
                    Map.of("{source}", result.isSimpleList() ? getMessage("simple-list-label") : result.getSourceName())
            );
        }

        sendMessage(player, action.getMessageKey(), Map.of(
                "{item}", material.name(),
                "{item_pretty}", TextUtils.formatEnumName(material.name()),
                "{reason_suffix}", reasonSuffix,
                "{source_suffix}", sourceSuffix
        ));
    }

    public String getPrefix() {
        String rawPrefix = messagesConfig.getString("prefix", "&8[&6ItemBlocker&8]&r");
        return ChatColor.translateAlternateColorCodes('&', rawPrefix);
    }

    private void ensureLanguageFiles() {
        File languagesDir = new File(plugin.getDataFolder(), "languages");
        if (!languagesDir.exists()) {
            languagesDir.mkdirs();
        }

        saveResourceIfMissing("languages/messages_en.yml");
        saveResourceIfMissing("languages/messages_pl.yml");
        saveResourceIfMissing("languages/messages_de.yml");
        saveResourceIfMissing("languages/messages_es.yml");
        saveResourceIfMissing("languages/messages_fr.yml");
        saveResourceIfMissing("languages/messages_it.yml");
    }

    private void saveResourceIfMissing(String resourcePath) {
        File file = new File(plugin.getDataFolder(), resourcePath);
        if (!file.exists()) {
            plugin.saveResource(resourcePath, false);
        }
    }

    private FileConfiguration loadYaml(File file, String resourcePath) {
        YamlConfiguration loadedConfig;
        try (BufferedReader reader = Files.newBufferedReader(file.toPath(), StandardCharsets.UTF_8)) {
            loadedConfig = YamlConfiguration.loadConfiguration(reader);
        } catch (IOException exception) {
            plugin.getLogger().severe("Failed to read " + file.getName());
            exception.printStackTrace();
            loadedConfig = new YamlConfiguration();
        }

        try (InputStream resourceStream = plugin.getResource(resourcePath)) {
            if (resourceStream != null) {
                YamlConfiguration defaultConfig = YamlConfiguration.loadConfiguration(
                        new InputStreamReader(resourceStream, StandardCharsets.UTF_8)
                );
                loadedConfig.setDefaults(defaultConfig);
            }
        } catch (IOException exception) {
            plugin.getLogger().warning("Failed to load bundled defaults for " + resourcePath);
        }

        return loadedConfig;
    }
}
