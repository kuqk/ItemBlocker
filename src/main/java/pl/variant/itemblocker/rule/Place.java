package pl.variant.itemblocker.rule;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/**
 * Where something happens.
 *
 * @param world   world name, or {@code null} if unknown
 * @param regions lowercase ids of the WorldGuard regions at that spot
 */
public record Place(String world, Set<String> regions) {

    public Place {
        Set<String> normalized = new HashSet<>();
        regions.forEach(region -> normalized.add(region.toLowerCase(Locale.ROOT)));
        regions = Set.copyOf(normalized);
    }

    /**
     * Returns a place in the world, outside of any region.
     */
    public static Place inWorld(String world) {
        return new Place(world, Set.of());
    }
}
