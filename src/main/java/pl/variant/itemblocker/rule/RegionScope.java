package pl.variant.itemblocker.rule;

import java.util.Collection;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/**
 * The WorldGuard regions a preset applies in. An empty set of regions means everywhere, also outside
 * of any region.
 *
 * @param regions lowercase region ids
 */
public record RegionScope(Set<String> regions) {

    public static final RegionScope ALL = new RegionScope(Set.of());

    private static final String ALL_KEYWORD = "all";

    public RegionScope {
        Set<String> normalized = new HashSet<>();
        for (String region : regions) {
            if (region != null && !region.isBlank()) {
                normalized.add(region.trim().toLowerCase(Locale.ROOT));
            }
        }
        regions = Set.copyOf(normalized);
    }

    /**
     * Creates a scope from region ids. The keyword {@code all} anywhere in the list means everywhere.
     */
    public static RegionScope of(Collection<String> regions) {
        for (String region : regions) {
            if (region != null && region.trim().equalsIgnoreCase(ALL_KEYWORD)) {
                return ALL;
            }
        }

        RegionScope scope = new RegionScope(new HashSet<>(regions));
        return scope.regions.isEmpty() ? ALL : scope;
    }

    public boolean isAll() {
        return regions.isEmpty();
    }

    /**
     * Returns whether a place inside the given regions is covered.
     */
    public boolean includesAny(Set<String> regionsAtPlace) {
        return isAll() || regionsAtPlace.stream().anyMatch(regions::contains);
    }
}
