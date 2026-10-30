package pl.variant.itemblocker.listener;

import org.bukkit.enchantments.Enchantment;
import org.bukkit.enchantments.EnchantmentOffer;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.enchantment.EnchantItemEvent;
import org.bukkit.event.enchantment.PrepareItemEnchantEvent;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.inventory.ItemStack;
import pl.variant.itemblocker.restriction.Denial;
import pl.variant.itemblocker.restriction.Enforcement;

import java.util.Iterator;
import java.util.Map;
import java.util.Optional;

/**
 * Keeps blocked enchantments out of enchanting tables and anvils.
 */
public final class EnchantmentListener implements Listener {

    private final Enforcement enforcement;

    public EnchantmentListener(Enforcement enforcement) {
        this.enforcement = enforcement;
    }

    /**
     * Removes offers for blocked enchantments from the enchanting table.
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPrepareEnchant(PrepareItemEnchantEvent event) {
        EnchantmentOffer[] offers = event.getOffers();
        boolean anyOffer = false;
        boolean anyAllowed = false;
        for (int index = 0; index < offers.length; index++) {
            EnchantmentOffer offer = offers[index];
            if (offer == null) {
                continue;
            }

            anyOffer = true;
            if (enforcement.restrictions().checkEnchanting(event.getEnchanter(), offer.getEnchantment(), offer.getEnchantmentLevel()).isPresent()) {
                offers[index] = null;
            } else {
                anyAllowed = true;
            }
        }

        if (anyOffer && !anyAllowed) {
            event.setCancelled(true);
        }
    }

    /**
     * Drops blocked enchantments that were rolled alongside the offered one. If nothing is left,
     * enchanting is cancelled.
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEnchant(EnchantItemEvent event) {
        Player player = event.getEnchanter();
        Optional<Denial> firstDenial = Optional.empty();
        Iterator<Map.Entry<Enchantment, Integer>> enchantments = event.getEnchantsToAdd().entrySet().iterator();
        while (enchantments.hasNext()) {
            Map.Entry<Enchantment, Integer> entry = enchantments.next();
            Optional<Denial> denial = enforcement.restrictions().checkEnchanting(player, entry.getKey(), entry.getValue());
            if (denial.isPresent()) {
                enchantments.remove();
                firstDenial = firstDenial.or(() -> denial);
            }
        }

        if (event.getEnchantsToAdd().isEmpty()) {
            event.setCancelled(true);
        }
        firstDenial.ifPresent(denial -> enforcement.notify(player, denial));
    }

    /**
     * Hides an anvil result that would carry a blocked enchantment.
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onPrepareAnvil(PrepareAnvilEvent event) {
        ItemStack result = event.getResult();
        for (HumanEntity viewer : event.getViewers()) {
            if (viewer instanceof Player player && enforcement.restrictions().checkEnchantedItem(player, result).isPresent()) {
                event.setResult(null);
                return;
            }
        }
    }
}
