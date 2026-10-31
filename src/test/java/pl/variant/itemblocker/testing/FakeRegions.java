package pl.variant.itemblocker.testing;

import org.bukkit.Location;
import pl.variant.itemblocker.integration.Regions;

import java.util.List;
import java.util.Set;

/**
 * Stands in for WorldGuard: every location is inside the regions a test sets.
 */
public final class FakeRegions implements Regions {

    private volatile Set<String> current = Set.of();

    public void setCurrent(String... regions) {
        current = Set.of(regions);
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public Set<String> at(Location location) {
        return current;
    }

    @Override
    public List<String> names() {
        return List.of("arena", "spawn");
    }
}
