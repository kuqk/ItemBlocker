package pl.variant.itemblocker.scan;

import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.block.BlockState;
import org.bukkit.block.Chest;
import org.bukkit.block.Container;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import pl.variant.itemblocker.platform.PlayerScheduler;
import pl.variant.itemblocker.platform.RegionScheduler;
import pl.variant.itemblocker.restriction.Denial;
import pl.variant.itemblocker.restriction.RestrictionService;
import pl.variant.itemblocker.rule.Action;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

/**
 * Finds forbidden items that players or containers already hold, for example after adding a rule
 * on a server that has been running for months. Each inventory is read on the thread that owns it,
 * and the findings are handed over once every inventory has been checked.
 */
public final class Scanner {

    private final RestrictionService restrictions;
    private final PlayerScheduler players;
    private final RegionScheduler regions;
    private final ConfiscatedItems confiscated;

    public Scanner(RestrictionService restrictions, PlayerScheduler players, RegionScheduler regions, ConfiscatedItems confiscated) {
        this.restrictions = restrictions;
        this.players = players;
        this.regions = regions;
        this.confiscated = confiscated;
    }

    /**
     * Checks the inventory and ender chest of every player.
     */
    public void scanPlayers(Collection<? extends Player> targets, Set<Action> actions, ScanMode mode, Consumer<List<Finding>> done) {
        scanPlayers(targets, actions, mode, done, exception -> { throw new UncheckedIOException(exception); });
    }

    public void scanPlayers(Collection<? extends Player> targets, Set<Action> actions, ScanMode mode,
                            Consumer<List<Finding>> done, Consumer<IOException> failed) {
        Collector collector = new Collector(targets.size(), done, failed);
        for (Player player : targets) {
            players.runNextTick(player, () -> {
                List<Finding> found = new ArrayList<>();
                try {
                    if (player.isOnline()) {
                        found.addAll(inspect(player.getName(), player.getInventory(), player.getLocation(), player, actions, mode));
                        found.addAll(inspect(player.getName(), player.getEnderChest(), player.getLocation(), player, actions, mode));
                    }
                } catch (UncheckedIOException exception) {
                    collector.failed(exception.getCause());
                } finally {
                    collector.add(found);
                }
            });
        }
    }

    /**
     * Checks every container in the chunks.
     */
    public void scanChunks(Collection<Chunk> chunks, Set<Action> actions, ScanMode mode, Consumer<List<Finding>> done) {
        scanChunks(chunks, actions, mode, done, exception -> { throw new UncheckedIOException(exception); });
    }

    public void scanChunks(Collection<Chunk> chunks, Set<Action> actions, ScanMode mode,
                           Consumer<List<Finding>> done, Consumer<IOException> failed) {
        Collector collector = new Collector(chunks.size(), done, failed);
        for (Chunk chunk : chunks) {
            regions.run(chunk, () -> {
                List<Finding> found = new ArrayList<>();
                try {
                    for (BlockState state : chunk.getTileEntities()) {
                        if (state instanceof Container container) {
                            // A chest's own inventory, so the two halves of a double chest are not counted twice.
                            Inventory inventory = state instanceof Chest chest ? chest.getBlockInventory() : container.getInventory();
                            Location location = state.getLocation();
                            found.addAll(inspect(describe(location), inventory, location, null, actions, mode));
                        }
                    }
                } catch (UncheckedIOException exception) {
                    collector.failed(exception.getCause());
                } finally {
                    collector.add(found);
                }
            });
        }
    }

    private List<Finding> inspect(String holder, Inventory inventory, Location location, Player owner, Set<Action> actions, ScanMode mode) {
        List<Finding> found = new ArrayList<>();
        List<ItemStack> taken = new ArrayList<>();
        List<Integer> slots = new ArrayList<>();
        for (int slot = 0; slot < inventory.getSize(); slot++) {
            ItemStack item = inventory.getItem(slot);
            Optional<Denial> denial = restrictions.checkHeld(item, actions, location, owner);
            if (denial.isEmpty()) {
                continue;
            }

            found.add(new Finding(holder, item.clone(), denial.get().source().name()));
            if (mode != ScanMode.REPORT) {
                slots.add(slot);
                taken.add(item.clone());
            }
        }
        if (mode == ScanMode.CONFISCATE) {
            confiscated.add(holder, taken);
        }
        // A failed confiscation save must leave the source inventory untouched.
        slots.forEach(slot -> inventory.setItem(slot, null));
        return found;
    }

    private static String describe(Location location) {
        return location.getWorld().getName() + " " + location.getBlockX() + " " + location.getBlockY() + " " + location.getBlockZ();
    }

    /**
     * Gathers findings from many tasks and hands them over when the last one is done.
     */
    private static final class Collector {

        private final AtomicInteger remaining;
        private final List<Finding> findings = Collections.synchronizedList(new ArrayList<>());
        private final Consumer<List<Finding>> done;
        private final Consumer<IOException> failed;
        private final AtomicReference<IOException> failure = new AtomicReference<>();

        private Collector(int tasks, Consumer<List<Finding>> done, Consumer<IOException> failed) {
            this.remaining = new AtomicInteger(tasks);
            this.done = done;
            this.failed = failed;
            if (tasks == 0) {
                done.accept(List.of());
            }
        }

        private void failed(IOException exception) {
            failure.compareAndSet(null, exception);
        }

        private void add(List<Finding> found) {
            findings.addAll(found);
            if (remaining.decrementAndGet() == 0) {
                IOException exception = failure.get();
                if (exception != null) {
                    failed.accept(exception);
                    return;
                }
                synchronized (findings) {
                    done.accept(List.copyOf(findings));
                }
            }
        }
    }
}
