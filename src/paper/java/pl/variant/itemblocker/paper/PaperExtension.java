package pl.variant.itemblocker.paper;

import com.destroystokyo.paper.event.player.PlayerArmorChangeEvent;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;
import pl.variant.itemblocker.platform.PlatformExtension;
import pl.variant.itemblocker.restriction.ArmorEnforcer;

/**
 * Paper reports every armor change, including ones no other listener sees, such as equipping by a
 * plugin or a command. Blocked armor is taken off one tick later, on the player's own scheduler so
 * it also works on Folia.
 */
public final class PaperExtension implements PlatformExtension, Listener {

    private Plugin plugin;
    private ArmorEnforcer armorEnforcer;

    @Override
    public void enable(Plugin plugin, ArmorEnforcer armorEnforcer) {
        this.plugin = plugin;
        this.armorEnforcer = armorEnforcer;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onArmorChange(PlayerArmorChangeEvent event) {
        Player player = event.getPlayer();
        if (!event.getNewItem().getType().isAir()) {
            player.getScheduler().run(plugin, task -> armorEnforcer.removeBlockedArmor(player), null);
        }
    }
}
