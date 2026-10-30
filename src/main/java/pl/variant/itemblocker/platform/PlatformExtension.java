package pl.variant.itemblocker.platform;

import org.bukkit.plugin.Plugin;
import pl.variant.itemblocker.restriction.ArmorEnforcer;

import java.util.Optional;
import java.util.ServiceLoader;

/**
 * Extra behavior that needs platform specific API. Implementations live in their own source set and
 * are only loaded when the server provides that API.
 */
public interface PlatformExtension {

    /**
     * Class that only exists on Paper and its forks (Folia, Purpur).
     */
    String PAPER_MARKER = "io.papermc.paper.threadedregions.scheduler.EntityScheduler";

    void enable(Plugin plugin, ArmorEnforcer armorEnforcer);

    /**
     * Returns the extension for the running server, if there is one.
     */
    static Optional<PlatformExtension> forServer(ClassLoader classLoader) {
        try {
            Class.forName(PAPER_MARKER, false, classLoader);
        } catch (ClassNotFoundException exception) {
            return Optional.empty();
        }
        return ServiceLoader.load(PlatformExtension.class, classLoader).findFirst();
    }
}
