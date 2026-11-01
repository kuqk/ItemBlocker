package pl.variant.itemblocker.platform;

import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

/**
 * Runs work for a player on the next tick, on the thread that owns the player.
 */
@FunctionalInterface
public interface PlayerScheduler {

    void runNextTick(Player player, Runnable task);

    /**
     * Uses the Bukkit scheduler, which runs everything on the main thread. Folia has no main thread,
     * so Paper servers use the player's own scheduler instead.
     */
    static PlayerScheduler bukkit(Plugin plugin) {
        return (player, task) -> plugin.getServer().getScheduler().runTask(plugin, task);
    }
}
