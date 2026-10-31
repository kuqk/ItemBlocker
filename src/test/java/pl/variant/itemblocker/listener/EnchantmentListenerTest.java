package pl.variant.itemblocker.listener;

import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.enchantments.EnchantmentOffer;
import org.bukkit.event.enchantment.EnchantItemEvent;
import org.bukkit.event.enchantment.PrepareItemEnchantEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.view.EnchantmentView;
import org.junit.jupiter.api.Test;
import pl.variant.itemblocker.testing.InventoryViews;
import pl.variant.itemblocker.testing.PluginTest;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EnchantmentListenerTest extends PluginTest {

    @Test
    void removesBlockedOffers() {
        blockEnchantment("sharpness", 4);
        EnchantmentOffer[] offers = {
                new EnchantmentOffer(Enchantment.SHARPNESS, 4, 30),
                new EnchantmentOffer(Enchantment.UNBREAKING, 1, 5),
                null
        };

        PrepareItemEnchantEvent event = prepare(offers);

        assertNull(offers[0]);
        assertNotNull(offers[1]);
        assertFalse(event.isCancelled());
    }

    @Test
    void cancelsWhenEveryOfferIsBlocked() {
        blockEnchantment("sharpness", 1);

        PrepareItemEnchantEvent event = prepare(new EnchantmentOffer[]{new EnchantmentOffer(Enchantment.SHARPNESS, 2, 10)});

        assertTrue(event.isCancelled());
    }

    @Test
    void dropsBlockedEnchantmentsRolledWithTheOffer() {
        blockEnchantment("sharpness", 5);
        Map<Enchantment, Integer> rolled = new HashMap<>(Map.of(Enchantment.SHARPNESS, 5, Enchantment.UNBREAKING, 3));

        EnchantItemEvent event = enchant(rolled);

        assertEquals(Map.of(Enchantment.UNBREAKING, 3), event.getEnchantsToAdd());
        assertFalse(event.isCancelled());
        assertEquals(List.of("ItemBlocker » You cannot enchant with Sharpness 5."), messages());
    }

    @Test
    void cancelsEnchantingWhenNothingIsLeft() {
        blockEnchantment("sharpness", 5);

        EnchantItemEvent event = enchant(new HashMap<>(Map.of(Enchantment.SHARPNESS, 5)));

        assertTrue(event.isCancelled());
    }

    private PrepareItemEnchantEvent prepare(EnchantmentOffer[] offers) {
        PrepareItemEnchantEvent event = new PrepareItemEnchantEvent(player, view(),
                player.getLocation().getBlock(), new ItemStack(Material.DIAMOND_SWORD), offers, 15);
        server.getPluginManager().callEvent(event);
        return event;
    }

    private EnchantItemEvent enchant(Map<Enchantment, Integer> enchantments) {
        EnchantItemEvent event = new EnchantItemEvent(player, view(), player.getLocation().getBlock(),
                new ItemStack(Material.DIAMOND_SWORD), 30, enchantments, Enchantment.SHARPNESS, 5, 2);
        server.getPluginManager().callEvent(event);
        return event;
    }

    private EnchantmentView view() {
        return InventoryViews.typed(EnchantmentView.class, player);
    }
}
