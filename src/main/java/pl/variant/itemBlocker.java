package pl.variant;

import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;
import pl.variant.commands.ItemBlockerCommand;
import pl.variant.listeners.ArmorListener;
import pl.variant.listeners.CraftListener;
import pl.variant.listeners.DropListener;
import pl.variant.listeners.HopperListener;
import pl.variant.listeners.InventoryListener;
import pl.variant.listeners.PickupListener;
import pl.variant.listeners.PlaceListener;
import pl.variant.listeners.UseListener;
import pl.variant.managers.BlockedItemsManager;
import pl.variant.managers.ConfigManager;
import pl.variant.managers.MessageManager;
import pl.variant.managers.PresetManager;
import pl.variant.services.BlockService;

public final class itemBlocker extends JavaPlugin {
    private ConfigManager configManager;
    private MessageManager messageManager;
    private BlockedItemsManager blockedItemsManager;
    private PresetManager presetManager;
    private BlockService blockService;

    @Override
    public void onEnable() {
        configManager = new ConfigManager(this);
        configManager.loadConfig();

        messageManager = new MessageManager(this);
        blockedItemsManager = new BlockedItemsManager(this);
        presetManager = new PresetManager(this);
        blockService = new BlockService(this);

        messageManager.loadMessages();
        blockedItemsManager.loadBlockedItems();
        presetManager.loadPresets();

        registerListeners();
        registerCommands();

        getLogger().info("ItemBlocker enabled");
    }

    @Override
    public void onDisable() {
        getLogger().info("ItemBlocker disabled");
    }

    private void registerListeners() {
        getServer().getPluginManager().registerEvents(new CraftListener(this), this);
        getServer().getPluginManager().registerEvents(new PickupListener(this), this);
        getServer().getPluginManager().registerEvents(new DropListener(this), this);
        getServer().getPluginManager().registerEvents(new UseListener(this), this);
        getServer().getPluginManager().registerEvents(new PlaceListener(this), this);
        getServer().getPluginManager().registerEvents(new ArmorListener(this), this);
        getServer().getPluginManager().registerEvents(new InventoryListener(this), this);
        getServer().getPluginManager().registerEvents(new HopperListener(this), this);
    }

    private void registerCommands() {
        PluginCommand command = getCommand("itemblocker");
        if (command == null) {
            getLogger().severe("Command 'itemblocker' is not defined in plugin.yml");
            return;
        }

        ItemBlockerCommand executor = new ItemBlockerCommand(this);
        command.setExecutor(executor);
        command.setTabCompleter(executor);
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

    public PresetManager getPresetManager() {
        return presetManager;
    }

    public BlockService getBlockService() {
        return blockService;
    }
}
