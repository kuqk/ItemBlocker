package pl.variant.itemblocker.listener;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import pl.variant.itemblocker.integration.Regions;
import pl.variant.itemblocker.platform.PlayerScheduler;
import pl.variant.itemblocker.rule.Place;
import pl.variant.itemblocker.rule.RuleBook;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Supplier;

/** Rechecks persistent player state when movement changes the applicable region rules. */
public final class RegionChangeListener implements Listener {

    private final PlayerScheduler scheduler;
    private final Regions regions;
    private final Supplier<RuleBook> rules;
    private final Consumer<Player> enforce;
    private final Map<UUID, CheckedPlace> checked = new ConcurrentHashMap<>();
    private final Set<UUID> pending = ConcurrentHashMap.newKeySet();

    private record CheckedPlace(Place place, RuleBook rules) {
    }

    public RegionChangeListener(PlayerScheduler scheduler, Regions regions, Supplier<RuleBook> rules,
                                Consumer<Player> enforce) {
        this.scheduler = scheduler;
        this.regions = regions;
        this.rules = rules;
        this.enforce = enforce;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onMove(PlayerMoveEvent event) {
        if (changedBlock(event.getFrom(), event.getTo())) {
            checkNextTick(event.getPlayer());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onTeleport(PlayerTeleportEvent event) {
        if (changedBlock(event.getFrom(), event.getTo())) {
            checkNextTick(event.getPlayer());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        UUID id = event.getPlayer().getUniqueId();
        checked.remove(id);
        pending.remove(id);
    }

    private void checkNextTick(Player player) {
        if (!rules.get().usesRegions()) {
            checked.remove(player.getUniqueId());
            return;
        }
        UUID id = player.getUniqueId();
        if (!pending.add(id)) {
            return;
        }
        scheduler.runNextTick(player, () -> {
            pending.remove(id);
            if (!player.isOnline()) {
                checked.remove(id);
                return;
            }
            RuleBook book = rules.get();
            if (!book.usesRegions()) {
                checked.remove(id);
                return;
            }
            Location location = player.getLocation();
            Place place = new Place(player.getWorld().getName(), regions.at(location));
            CheckedPlace previous = checked.put(id, new CheckedPlace(place, book));
            if (previous == null || previous.rules() != book || !previous.place().equals(place)) {
                enforce.accept(player);
            }
        });
    }

    private static boolean changedBlock(Location from, Location to) {
        return to != null && (!from.getWorld().equals(to.getWorld())
                || from.getBlockX() != to.getBlockX() || from.getBlockY() != to.getBlockY()
                || from.getBlockZ() != to.getBlockZ());
    }
}
