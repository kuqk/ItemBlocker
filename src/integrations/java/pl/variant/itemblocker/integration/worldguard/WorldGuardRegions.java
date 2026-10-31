package pl.variant.itemblocker.integration.worldguard;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.sk89q.worldguard.protection.regions.RegionContainer;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import pl.variant.itemblocker.integration.Regions;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/**
 * Regions from WorldGuard. A location is inside every region that contains it, including parent
 * regions, but not the global region.
 */
public final class WorldGuardRegions implements Regions {

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public Set<String> at(Location location) {
        Set<String> ids = new HashSet<>();
        for (ProtectedRegion region : container().createQuery().getApplicableRegions(BukkitAdapter.adapt(location))) {
            ids.add(region.getId());
        }
        return ids;
    }

    @Override
    public List<String> names() {
        Set<String> ids = new TreeSet<>();
        for (World world : Bukkit.getWorlds()) {
            RegionManager manager = container().get(BukkitAdapter.adapt(world));
            if (manager != null) {
                manager.getRegions().keySet().stream()
                        .filter(id -> !id.equals(ProtectedRegion.GLOBAL_REGION))
                        .forEach(ids::add);
            }
        }
        return List.copyOf(ids);
    }

    private static RegionContainer container() {
        return WorldGuard.getInstance().getPlatform().getRegionContainer();
    }
}
