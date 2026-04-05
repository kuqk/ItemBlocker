package pl.variant.listeners;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPotionEffectEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import pl.variant.itemBlocker;
import pl.variant.model.BlockAction;
import pl.variant.model.BlockCheckResult;
import pl.variant.utils.TextUtils;

import java.util.Map;

public class PotionRestrictionListener implements Listener {

    private final itemBlocker plugin;

    public PotionRestrictionListener(itemBlocker plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPotionEffectApply(EntityPotionEffectEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }

        PotionEffect newEffect = event.getNewEffect();
        if (newEffect == null) {
            return;
        }

        BlockCheckResult result = plugin.getBlockService().checkPotionEffect(
                player,
                newEffect.getType(),
                newEffect.getAmplifier() + 1
        );
        if (!result.isBlocked()) {
            return;
        }

        event.setCancelled(true);
        sendBlockedPotionMessage(player, newEffect.getType(), newEffect.getAmplifier() + 1, result);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onConsume(PlayerItemConsumeEvent event) {
        if (!plugin.getBlockService().blockIfNeeded(event.getPlayer(), event.getItem(), BlockAction.USE, event)) {
            return;
        }

        event.getPlayer().updateInventory();
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onJoin(PlayerJoinEvent event) {
        enforcePotionRestrictions(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onWorldChange(PlayerChangedWorldEvent event) {
        enforcePotionRestrictions(event.getPlayer());
    }

    private void enforcePotionRestrictions(Player player) {
        for (PotionEffect potionEffect : player.getActivePotionEffects()) {
            BlockCheckResult result = plugin.getBlockService().checkPotionEffect(
                    player,
                    potionEffect.getType(),
                    potionEffect.getAmplifier() + 1
            );
            if (!result.isBlocked()) {
                continue;
            }

            player.removePotionEffect(potionEffect.getType());
            sendBlockedPotionMessage(player, potionEffect.getType(), potionEffect.getAmplifier() + 1, result);
        }
    }

    private void sendBlockedPotionMessage(
            Player player,
            PotionEffectType effectType,
            int level,
            BlockCheckResult result
    ) {
        String key = effectType.getKey().getKey();
        plugin.getMessageManager().sendCustomBlockedMessage(player, "blocked-potion-effect", Map.of(
                "{effect}", key,
                "{effect_pretty}", TextUtils.formatEnumName(key.toUpperCase()),
                "{level}", String.valueOf(level)
        ), result);
    }
}
