package pl.variant.managers;

import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import pl.variant.itemBlocker;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class BlockedItemsManager {
    
    private final itemBlocker plugin;
    private final Set<Material> blockedItems;
    private File blockedItemsFile;
    private FileConfiguration blockedItemsConfig;
    
    public BlockedItemsManager(itemBlocker plugin) {
        this.plugin = plugin;
        this.blockedItems = new HashSet<>();
    }
    
    public void loadBlockedItems() {
        blockedItemsFile = new File(plugin.getDataFolder(), "blocked-items.yml");
        
        if (!blockedItemsFile.exists()) {
            plugin.saveResource("blocked-items.yml", false);
        }
        
        blockedItemsConfig = YamlConfiguration.loadConfiguration(blockedItemsFile);
        blockedItems.clear();
        
        List<String> items = blockedItemsConfig.getStringList("blocked-items");
        for (String itemName : items) {
            try {
                Material material = Material.valueOf(itemName.toUpperCase());
                blockedItems.add(material);
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("Invalid item: " + itemName);
            }
        }
        
        plugin.getLogger().info("Loaded " + blockedItems.size() + " blocked items");
    }
    
    public void saveBlockedItems() {
        List<String> items = new ArrayList<>();
        for (Material material : blockedItems) {
            items.add(material.name());
        }
        
        blockedItemsConfig.set("blocked-items", items);
        
        try {
            blockedItemsConfig.save(blockedItemsFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to save blocked-items.yml!");
            e.printStackTrace();
        }
    }
    
    public boolean isBlocked(Material material) {
        return blockedItems.contains(material);
    }
    
    public boolean addBlockedItem(Material material) {
        if (blockedItems.add(material)) {
            saveBlockedItems();
            return true;
        }
        return false;
    }
    
    public boolean removeBlockedItem(Material material) {
        if (blockedItems.remove(material)) {
            saveBlockedItems();
            return true;
        }
        return false;
    }
    
    public Set<Material> getBlockedItems() {
        return new HashSet<>(blockedItems);
    }
    
    public int getBlockedItemsCount() {
        return blockedItems.size();
    }
}

