package pl.variant.listeners;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityPotionEffectEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SuspiciousStewMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import pl.variant.itemBlocker;
import pl.variant.model.BlockAction;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PotionRestrictionListenerTest {

    private ServerMock server;
    private itemBlocker plugin;
    private Player player;
    private PotionRestrictionListener listener;

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
        plugin = MockBukkit.load(itemBlocker.class);
        player = server.addPlayer();
        player.setOp(false);
        listener = new PotionRestrictionListener(plugin);
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    void blockedPotionRuleDoesNotBlockCarrierItemInspection() {
        blockPotionEffect(PotionEffectType.STRENGTH, 1);

        assertFalse(plugin.getBlockService()
                .inspect(player, createStewWithEffect(PotionEffectType.STRENGTH, 1), BlockAction.INVENTORY)
                .blocked());
    }

    @Test
    void blockedPotionRuleDoesNotCancelCarrierConsumptionByItself() {
        blockPotionEffect(PotionEffectType.STRENGTH, 1);

        PlayerItemConsumeEvent event = new PlayerItemConsumeEvent(
                player,
                createStewWithEffect(PotionEffectType.STRENGTH, 1),
                EquipmentSlot.HAND
        );

        listener.onConsume(event);

        assertFalse(event.isCancelled());
    }

    @Test
    void blockedPotionRuleStillCancelsEffectApplication() {
        blockPotionEffect(PotionEffectType.STRENGTH, 1);

        EntityPotionEffectEvent event = new EntityPotionEffectEvent(
                player,
                null,
                new PotionEffect(PotionEffectType.STRENGTH, 200, 0),
                EntityPotionEffectEvent.Cause.FOOD,
                EntityPotionEffectEvent.Action.ADDED,
                true
        );

        listener.onPotionEffectApply(event);

        assertTrue(event.isCancelled());
    }

    private void blockPotionEffect(PotionEffectType effectType, int level) {
        plugin.getBlockedItemsManager().upsertGlobalPotion(effectType.getKey().getKey(), level);
    }

    private ItemStack createStewWithEffect(PotionEffectType effectType, int level) {
        ItemStack stew = new ItemStack(Material.SUSPICIOUS_STEW);
        SuspiciousStewMeta meta = (SuspiciousStewMeta) stew.getItemMeta();
        meta.addCustomEffect(new PotionEffect(effectType, 200, Math.max(0, level - 1)), true);
        stew.setItemMeta(meta);
        return stew;
    }
}
