package pl.variant;

import org.bukkit.plugin.java.JavaPlugin;
import pl.variant.commands.ItemBlockerCommand;
import pl.variant.listeners.*;
import pl.variant.managers.BlockedItemsManager;
import pl.variant.managers.ConfigManager;
import pl.variant.managers.MessageManager;

public final class itemBlocker extends JavaPlugin {

    private static itemBlocker instance;
    private ConfigManager configManager;
    private MessageManager messageManager;
    private BlockedItemsManager blockedItemsManager;

    @Override
    public void onEnable() {
        instance = this;
        
        configManager = new ConfigManager(this);
        messageManager = new MessageManager(this);
        blockedItemsManager = new BlockedItemsManager(this);
        
        configManager.loadConfig();
        messageManager.loadMessages();
        blockedItemsManager.loadBlockedItems();
        
        registerListeners();
        registerCommands();
        
        getLogger().info("ItemBlocker enabled!");
    }

    @Override
    public void onDisable() {
        getLogger().info("ItemBlocker disabled!");
    }
    
    private void registerListeners() {
        getServer().getPluginManager().registerEvents(new CraftListener(this), this);
        getServer().getPluginManager().registerEvents(new PickupListener(this), this);
        getServer().getPluginManager().registerEvents(new DropListener(this), this);
        getServer().getPluginManager().registerEvents(new UseListener(this), this);
        getServer().getPluginManager().registerEvents(new PlaceListener(this), this);
        getServer().getPluginManager().registerEvents(new ArmorListener(this), this);
    }
    
    private void registerCommands() {
        getCommand("itemblocker").setExecutor(new ItemBlockerCommand(this));
    }
    
    public static itemBlocker getInstance() {
        return instance;
    }
    
    public ConfigManager getConfigManager() {
        return configManager;
    }
    
    public MessageManager getMessageManager() {
        return messageManager;
    }
    
    public BlockedItemsManager getBlockedItemsManager() {
        return blockedItemsManager;
    }
}
