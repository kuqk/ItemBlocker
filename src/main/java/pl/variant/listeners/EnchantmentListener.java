package pl.variant.listeners;

import org.bukkit.enchantments.Enchantment;
import org.bukkit.enchantments.EnchantmentOffer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.enchantment.EnchantItemEvent;
import org.bukkit.event.enchantment.PrepareItemEnchantEvent;
import pl.variant.itemBlocker;
import pl.variant.model.BlockCheckResult;
import pl.variant.utils.TextUtils;

import java.util.Map;

public class EnchantmentListener implements Listener {

    private final itemBlocker plugin;

    public EnchantmentListener(itemBlocker plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPrepareEnchant(PrepareItemEnchantEvent event) {
        Player player = event.getEnchanter();
        if (plugin.getConfigManager().canBypass(player, "enchantment")) {
            return;
        }

        EnchantmentOffer[] offers = event.getOffers();
        boolean hasAllowedOffer = false;
        boolean hasAnyOffer = false;

        for (int index = 0; index < offers.length; index++) {
            EnchantmentOffer offer = offers[index];
            if (offer == null) {
                continue;
            }

            hasAnyOffer = true;
            BlockCheckResult result = plugin.getBlockService().checkEnchantment(
                    player,
                    offer.getEnchantment(),
                    offer.getEnchantmentLevel()
            );
            if (result.isBlocked()) {
                offers[index] = null;
                continue;
            }

            hasAllowedOffer = true;
        }

        if (hasAnyOffer && !hasAllowedOffer) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEnchantItem(EnchantItemEvent event) {
        Player player = event.getEnchanter();

        for (Map.Entry<Enchantment, Integer> entry : event.getEnchantsToAdd().entrySet()) {
            BlockCheckResult result = plugin.getBlockService().checkEnchantment(player, entry.getKey(), entry.getValue());
            if (!result.isBlocked()) {
                continue;
            }

            event.setCancelled(true);
            sendBlockedEnchantmentMessage(player, entry.getKey(), entry.getValue(), result);
            return;
        }
    }

    private void sendBlockedEnchantmentMessage(
            Player player,
            Enchantment enchantment,
            int level,
            BlockCheckResult result
    ) {
        String key = enchantment.getKey().getKey();
        plugin.getMessageManager().sendCustomBlockedMessage(player, "blocked-enchantment", Map.of(
                "{enchantment}", key,
                "{enchantment_pretty}", TextUtils.formatEnumName(key.toUpperCase()),
                "{level}", String.valueOf(level)
        ), result);
    }
}
