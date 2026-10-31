package pl.variant.itemblocker.integration;

import org.bukkit.plugin.Plugin;

import java.util.Optional;
import java.util.ServiceConfigurationError;
import java.util.ServiceLoader;
import java.util.logging.Level;

/**
 * Loads hooks into other plugins. Implementations live in their own source set and are compiled
 * against those plugins, so they are only loaded once the plugin they need is enabled.
 */
public final class Integrations {

    private Integrations() {
    }

    /**
     * Returns the implementation of the hook if the plugin it needs is enabled.
     *
     * @param plugin     ItemBlocker
     * @param dependency name of the plugin the hook needs
     * @param type       hook interface
     */
    public static <T> Optional<T> load(Plugin plugin, String dependency, Class<T> type) {
        if (!plugin.getServer().getPluginManager().isPluginEnabled(dependency)) {
            return Optional.empty();
        }

        try {
            Optional<T> hook = ServiceLoader.load(type, plugin.getClass().getClassLoader()).findFirst();
            hook.ifPresent(ignored -> plugin.getLogger().info("Hooked into " + dependency));
            return hook;
        } catch (ServiceConfigurationError | LinkageError exception) {
            plugin.getLogger().log(Level.WARNING, "Could not hook into " + dependency + ", is it up to date?", exception);
            return Optional.empty();
        }
    }
}
