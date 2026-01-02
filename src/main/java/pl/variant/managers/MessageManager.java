package pl.variant.managers;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import pl.variant.itemBlocker;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class MessageManager {
    
    private final itemBlocker plugin;
    private FileConfiguration messagesConfig;
    private File messagesFile;
    private String currentLanguage;
    
    public MessageManager(itemBlocker plugin) {
        this.plugin = plugin;
    }
    
    public void loadMessages() {
        currentLanguage = plugin.getConfig().getString("language", "en");
        
        File languagesDir = new File(plugin.getDataFolder(), "languages");
        if (!languagesDir.exists()) {
            languagesDir.mkdirs();
        }
        
        String fileName = "languages/messages_" + currentLanguage + ".yml";
        messagesFile = new File(plugin.getDataFolder(), fileName);
        
        if (!new File(plugin.getDataFolder(), "languages/messages_en.yml").exists()) {
            plugin.saveResource("languages/messages_en.yml", false);
        }
        if (!new File(plugin.getDataFolder(), "languages/messages_pl.yml").exists()) {
            plugin.saveResource("languages/messages_pl.yml", false);
        }
        
        if (!messagesFile.exists()) {
            plugin.getLogger().warning("Language file " + fileName + " not found, using English");
            currentLanguage = "en";
            messagesFile = new File(plugin.getDataFolder(), "languages/messages_en.yml");
        }
        
        messagesConfig = YamlConfiguration.loadConfiguration(messagesFile);
        
        InputStream defConfigStream = plugin.getResource(fileName);
        if (defConfigStream != null) {
            YamlConfiguration defConfig = YamlConfiguration.loadConfiguration(new InputStreamReader(defConfigStream));
            messagesConfig.setDefaults(defConfig);
        }
        
        plugin.getLogger().info("Language: " + currentLanguage);
    }
    
    public void reloadMessages() {
        loadMessages();
    }
    
    public void saveMessages() {
        try {
            messagesConfig.save(messagesFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to save messages file!");
            e.printStackTrace();
        }
    }
    
    public String getMessage(String path) {
        String message = messagesConfig.getString(path);
        if (message == null) {
            return "§cMissing message: " + path;
        }
        return message.replace('&', '§');
    }
    
    public String getMessage(String path, String placeholder, String value) {
        return getMessage(path).replace(placeholder, value);
    }
    
    public void sendMessage(Player player, String path) {
        player.sendMessage(getMessage(path));
    }
    
    public void sendMessage(Player player, String path, String placeholder, String value) {
        player.sendMessage(getMessage(path, placeholder, value));
    }
    
    public String getPrefix() {
        return getMessage("prefix");
    }
}

