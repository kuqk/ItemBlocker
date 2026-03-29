package pl.variant.managers;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import pl.variant.itemBlocker;
import pl.variant.model.BlockAction;

public class ConfigManager {

    private final itemBlocker plugin;
    private volatile FileConfiguration config;
    private volatile boolean bypassEnabled;
    private volatile String defaultPermission = "itemblocker.bypass";
    private volatile boolean perActionBypassEnabled = true;
    private volatile String perActionPrefix = "itemblocker.bypass.";
    private volatile String language = "en";
    private volatile long messageCooldownMillis = 1000L;

    public ConfigManager(itemBlocker plugin) {
        this.plugin = plugin;
    }

    public void loadConfig() {
        plugin.saveDefaultConfig();
        plugin.reloadConfig();
        applySnapshot(plugin.getConfig());
    }

    public void reloadConfiguration() {
        plugin.reloadConfig();
        applySnapshot(plugin.getConfig());
    }

    public boolean canBypass(Player player, BlockAction action) {
        if (!bypassEnabled || player == null) {
            return false;
        }

        if (defaultPermission != null && !defaultPermission.isBlank() && player.hasPermission(defaultPermission)) {
            return true;
        }

        if (!perActionBypassEnabled) {
            return false;
        }

        return perActionPrefix != null
                && !perActionPrefix.isBlank()
                && player.hasPermission(perActionPrefix + action.getPermissionSuffix());
    }

    public String getLanguage() {
        return language;
    }

    public long getMessageCooldownMillis() {
        return messageCooldownMillis;
    }

    public FileConfiguration getConfig() {
        return config;
    }

    private void applySnapshot(FileConfiguration newConfig) {
        config = newConfig;
        bypassEnabled = newConfig.getBoolean("bypass.enabled", true);
        defaultPermission = newConfig.getString(
                "bypass.default-permission",
                newConfig.getString("bypass.global-permission", "itemblocker.bypass")
        );
        perActionBypassEnabled = newConfig.getBoolean("bypass.per-action.enabled", true);
        perActionPrefix = newConfig.getString("bypass.per-action.prefix", "itemblocker.bypass.");
        language = newConfig.getString("language", "en");
        messageCooldownMillis = Math.max(0L, newConfig.getLong("messages.cooldown-ms", 1000L));
    }
}
