package pl.variant.itemblocker.api;

import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.entity.PlayerMock;
import pl.variant.itemblocker.api.event.ItemBlockedEvent;
import pl.variant.itemblocker.rule.Action;
import pl.variant.itemblocker.testing.PluginTest;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ItemBlockerApiTest extends PluginTest {

    private static ItemStack pearl() {
        return new ItemStack(Material.ENDER_PEARL);
    }

    @Test
    void checksSavedRules() {
        block(Material.TNT, Action.PLACE);

        Restriction restriction = ItemBlockerApi.get().check(player, new ItemStack(Material.TNT), Action.PLACE).orElseThrow();

        assertEquals(new Restriction("place", "TNT", "default", ""), restriction);
        assertFalse(ItemBlockerApi.get().isBlocked(player, new ItemStack(Material.TNT), Action.DROP));
    }

    @Test
    void temporaryRulesForEveryoneLastUntilRemoved() {
        TemporaryRule rule = ItemBlockerApi.get().blockTemporarily(Material.ENDER_PEARL, Set.of(Action.USE), "Event running");

        Restriction restriction = ItemBlockerApi.get().check(player, pearl(), Action.USE).orElseThrow();
        assertEquals("Event running", restriction.reason());
        assertTrue(plugin.rules().rules().defaultPreset().items().isEmpty());

        rule.remove();
        assertFalse(rule.isActive());
        assertFalse(ItemBlockerApi.get().isBlocked(player, pearl(), Action.USE));
    }

    @Test
    void temporaryRulesForOnePlayerEndWhenTheyLeave() {
        PlayerMock other = server.addPlayer();
        other.setOp(false);
        TemporaryRule rule = ItemBlockerApi.get().blockTemporarily(player, Material.ENDER_PEARL, Set.of(Action.USE), "");

        assertTrue(ItemBlockerApi.get().isBlocked(player, pearl(), Action.USE));
        assertFalse(ItemBlockerApi.get().isBlocked(other, pearl(), Action.USE));

        server.getPluginManager().callEvent(new PlayerQuitEvent(player, "left"));
        assertFalse(rule.isActive());
    }

    @Test
    void cancellingTheEventLetsTheActionThrough() {
        block(Material.TNT, Action.PLACE);
        server.getPluginManager().registerEvents(new Listener() {
            @EventHandler
            public void onBlocked(ItemBlockedEvent event) {
                event.setCancelled(event.getRestriction().subject().equals("TNT"));
            }
        }, plugin);

        assertTrue(plugin.enforcement().restrictions().checkItem(player, new ItemStack(Material.TNT), Action.PLACE).isEmpty());
    }
}
