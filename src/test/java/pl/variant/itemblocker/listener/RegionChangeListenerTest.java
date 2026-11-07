package pl.variant.itemblocker.listener;

import org.bukkit.Location;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import pl.variant.itemblocker.rule.Preset;
import pl.variant.itemblocker.rule.RegionScope;
import pl.variant.itemblocker.testing.PluginTest;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RegionChangeListenerTest extends PluginTest {

    private final List<Runnable> tasks = new ArrayList<>();
    private int checks;

    @BeforeEach
    void trackEnforcement() {
        updateRules(rules -> rules.with(Preset.empty("arena")
                .withRegions(RegionScope.of(List.of("arena")))));
        RegionChangeListener listener = new RegionChangeListener((target, task) -> tasks.add(task),
                plugin.fakeRegions(), plugin.rules()::rules, target -> checks++);
        server.getPluginManager().registerEvents(listener, plugin);
    }

    @Test
    void rotationAndMovementWithinOneBlockDoNotScheduleARegionCheck() {
        Location from = new Location(player.getWorld(), 10, 64, 10);
        Location to = from.clone().add(0.2, 0, 0.2);
        to.setYaw(90);

        server.getPluginManager().callEvent(new PlayerMoveEvent(player, from, to));

        assertEquals(0, tasks.size());
    }

    @Test
    void manyMovesInOneTickScheduleOneCheckAndUnchangedRegionsAreNotEnforcedAgain() {
        move();
        move();
        assertEquals(1, tasks.size());
        runTasks();
        assertEquals(1, checks);

        move();
        runTasks();
        assertEquals(1, checks);

        plugin.fakeRegions().setCurrent("arena");
        move();
        runTasks();
        assertEquals(2, checks);
    }

    @Test
    void changingRulesRechecksTheSameRegionOnTheNextMovement() {
        move();
        runTasks();
        updateRules(rules -> rules.with(rules.preset("arena").orElseThrow().withReason("Updated rules")));

        move();
        runTasks();

        assertEquals(2, checks);
    }

    @Test
    void cancelledTeleportDoesNotScheduleEnforcement() {
        Location from = player.getLocation();
        PlayerTeleportEvent event = new PlayerTeleportEvent(player, from, from.clone().add(20, 0, 0),
                PlayerTeleportEvent.TeleportCause.PLUGIN);
        event.setCancelled(true);

        server.getPluginManager().callEvent(event);

        assertEquals(0, tasks.size());
    }

    private void move() {
        Location from = player.getLocation();
        Location to = from.clone().add(1, 0, 0);
        player.setLocation(to);
        server.getPluginManager().callEvent(new PlayerMoveEvent(player, from, to));
    }

    private void runTasks() {
        List<Runnable> ready = new ArrayList<>(tasks);
        tasks.clear();
        ready.forEach(Runnable::run);
    }
}
