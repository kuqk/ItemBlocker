package pl.variant.itemblocker.integration;

import org.bukkit.Location;

import java.util.List;
import java.util.Set;

/**
 * Looks up protected regions, which WorldGuard provides when it is installed.
 */
public interface Regions {

    /**
     * Used when no region plugin is installed: there are no regions anywhere.
     */
    Regions NONE = new Regions() {
        @Override
        public boolean isAvailable() {
            return false;
        }

        @Override
        public Set<String> at(Location location) {
            return Set.of();
        }

        @Override
        public List<String> names() {
            return List.of();
        }
    };

    boolean isAvailable();

    /**
     * Returns the ids of the regions at the location.
     */
    Set<String> at(Location location);

    /**
     * Returns the ids of every region in every loaded world, for tab completion.
     */
    List<String> names();
}
