package pl.variant.itemblocker.listener;

import net.kyori.adventure.text.Component;
import org.bukkit.event.entity.EntityPotionEffectEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.junit.jupiter.api.Test;
import pl.variant.itemblocker.testing.PluginTest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PotionEffectListenerTest extends PluginTest {

    @Test
    void cancelsBlockedEffectFromMinimumLevel() {
        blockEffect("strength", 2);

        EntityPotionEffectEvent levelTwo = applyStrength(1);
        EntityPotionEffectEvent levelOne = applyStrength(0);

        assertTrue(levelTwo.isCancelled());
        assertFalse(levelOne.isCancelled());
        assertEquals(List.of("ItemBlocker » The effect Strength 2 is blocked."), messages());
    }

    @Test
    void removesBlockedEffectsOnJoin() {
        player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 600, 0));
        blockEffect("speed", 1);

        server.getPluginManager().callEvent(new PlayerJoinEvent(player, Component.text("joined")));

        assertFalse(player.hasPotionEffect(PotionEffectType.SPEED));
    }

    private EntityPotionEffectEvent applyStrength(int amplifier) {
        EntityPotionEffectEvent event = new EntityPotionEffectEvent(player, null,
                new PotionEffect(PotionEffectType.STRENGTH, 200, amplifier),
                EntityPotionEffectEvent.Cause.POTION_DRINK, EntityPotionEffectEvent.Action.ADDED, false);
        server.getPluginManager().callEvent(event);
        return event;
    }
}
