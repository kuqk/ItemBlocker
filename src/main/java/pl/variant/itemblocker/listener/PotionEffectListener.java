package pl.variant.itemblocker.listener;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPotionEffectEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.potion.PotionEffect;
import pl.variant.itemblocker.restriction.Enforcement;

/**
 * Stops blocked potion effects from being applied and removes them when a player joins or changes
 * worlds.
 */
public final class PotionEffectListener implements Listener {

    private final Enforcement enforcement;

    public PotionEffectListener(Enforcement enforcement) {
        this.enforcement = enforcement;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEffect(EntityPotionEffectEvent event) {
        PotionEffect effect = event.getNewEffect();
        if (effect != null && event.getEntity() instanceof Player player) {
            enforcement.deny(player, enforcement.restrictions().checkEffect(player, effect.getType(), levelOf(effect)), event);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        removeBlockedEffects(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onWorldChange(PlayerChangedWorldEvent event) {
        removeBlockedEffects(event.getPlayer());
    }

    private void removeBlockedEffects(Player player) {
        for (PotionEffect effect : player.getActivePotionEffects()) {
            enforcement.restrictions().checkEffect(player, effect.getType(), levelOf(effect)).ifPresent(denial -> {
                player.removePotionEffect(effect.getType());
                enforcement.notify(player, denial);
            });
        }
    }

    private static int levelOf(PotionEffect effect) {
        return effect.getAmplifier() + 1;
    }
}
