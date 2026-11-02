package pl.variant.itemblocker.platform;

import org.bukkit.Chunk;
import org.bukkit.plugin.Plugin;

/**
 * Runs work for a chunk on the thread that owns it.
 */
@FunctionalInterface
public interface RegionScheduler {

    void run(Chunk chunk, Runnable task);

    /**
     * Uses the Bukkit scheduler, which runs everything on the main thread. On Paper the region
     * scheduler is used instead, so the work also runs on the right thread on Folia.
     */
    static RegionScheduler bukkit(Plugin plugin) {
        return (chunk, task) -> plugin.getServer().getScheduler().runTask(plugin, task);
    }
}
