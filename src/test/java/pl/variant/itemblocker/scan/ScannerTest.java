package pl.variant.itemblocker.scan;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.Test;
import pl.variant.itemblocker.rule.Action;
import pl.variant.itemblocker.testing.PluginTest;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ScannerTest extends PluginTest {

    private static final Set<Action> PICKUP = EnumSet.of(Action.PICKUP);

    @Test
    void reportsWithoutTouchingTheItems() {
        block(Material.TNT, Action.PICKUP);
        player.getInventory().setItem(0, new ItemStack(Material.TNT, 5));
        player.getEnderChest().setItem(3, new ItemStack(Material.TNT, 2));
        player.getInventory().setItem(1, new ItemStack(Material.STONE));

        List<Finding> findings = scan(ScanMode.REPORT);

        assertEquals(2, findings.size());
        assertEquals(player.getName(), findings.get(0).holder());
        assertEquals(5, player.getInventory().getItem(0).getAmount());
    }

    @Test
    void onlyCountsTheChosenActions() {
        block(Material.TNT, Action.PLACE);
        player.getInventory().setItem(0, new ItemStack(Material.TNT));

        assertTrue(scan(ScanMode.REPORT).isEmpty());
    }

    @Test
    void removesForbiddenItems() {
        block(Material.TNT, Action.PICKUP);
        player.getInventory().setItem(0, new ItemStack(Material.TNT, 5));

        scan(ScanMode.REMOVE);

        assertNull(player.getInventory().getItem(0));
        assertTrue(plugin.confiscated().owners().isEmpty());
    }

    @Test
    void confiscatesForbiddenItemsUntilTaken() {
        block(Material.TNT, Action.PICKUP);
        player.getInventory().setItem(0, new ItemStack(Material.TNT, 5));

        scan(ScanMode.CONFISCATE);

        assertNull(player.getInventory().getItem(0));
        assertEquals(Map.of(player.getName(), 1), plugin.confiscated().owners());
        assertEquals(List.of(new ItemStack(Material.TNT, 5)), plugin.confiscated().take(player.getName()));
        assertTrue(plugin.confiscated().owners().isEmpty());
    }

    private List<Finding> scan(ScanMode mode) {
        List<Finding> result = new ArrayList<>();
        plugin.scanner().scanPlayers(List.of(player), PICKUP, mode, result::addAll);
        server.getScheduler().performOneTick();
        return result;
    }
}
