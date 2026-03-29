package pl.variant.services;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import pl.variant.itemBlocker;
import pl.variant.model.BlockAction;
import pl.variant.model.BlockCheckResult;

public class BlockService {

    private final itemBlocker plugin;

    public BlockService(itemBlocker plugin) {
        this.plugin = plugin;
    }

    public BlockCheckResult check(Player player, Material material, BlockAction action) {
        if (player == null || material == null || material == Material.AIR) {
            return BlockCheckResult.allowed();
        }

        if (plugin.getConfigManager().canBypass(player, action)) {
            return BlockCheckResult.allowed();
        }

        return plugin.getBlockedItemsManager().check(material, action, player.getWorld().getName());
    }

    public boolean blockIfNeeded(Player player, Material material, BlockAction action, Cancellable cancellable) {
        BlockCheckResult result = check(player, material, action);
        if (!result.isBlocked()) {
            return false;
        }

        cancellable.setCancelled(true);
        plugin.getMessageManager().sendBlockedMessage(player, action, material, result);
        return true;
    }
}
