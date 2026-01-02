package pl.variant.managers;

import org.bukkit.configuration.file.FileConfiguration;
import pl.variant.itemBlocker;

public class ConfigManager {
    
    private final itemBlocker plugin;
    private FileConfiguration config;
    
    public ConfigManager(itemBlocker plugin) {
        this.plugin = plugin;
    }
    
    public void loadConfig() {
        plugin.saveDefaultConfig();
        config = plugin.getConfig();
    }
    
    public void reloadConfiguration() {
        plugin.reloadConfig();
        config = plugin.getConfig();
    }
    
    public boolean isBlockCrafting() {
        return config.getBoolean("block-actions.crafting", true);
    }
    
    public boolean isBlockPickup() {
        return config.getBoolean("block-actions.pickup", true);
    }
    
    public boolean isBlockDrop() {
        return config.getBoolean("block-actions.drop", true);
    }
    
    public boolean isBlockUse() {
        return config.getBoolean("block-actions.use", true);
    }
    
    public boolean isBlockPlace() {
        return config.getBoolean("block-actions.place", true);
    }
    
    public boolean isBlockArmor() {
        return config.getBoolean("block-actions.armor", true);
    }
    
    public boolean isBlockInventory() {
        return config.getBoolean("block-actions.inventory", true);
    }
    
    public boolean isBypassEnabled() {
        return config.getBoolean("bypass-permission.enabled", true);
    }
    
    public boolean isDebugMode() {
        return config.getBoolean("debug-mode", false);
    }
    
    public FileConfiguration getConfig() {
        return config;
    }
}

