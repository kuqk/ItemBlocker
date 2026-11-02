package pl.variant.itemblocker;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import pl.variant.itemblocker.api.ItemBlockerApi;
import pl.variant.itemblocker.api.Restriction;
import pl.variant.itemblocker.api.TemporaryRule;
import pl.variant.itemblocker.restriction.RestrictionService;
import pl.variant.itemblocker.restriction.TemporaryRules;
import pl.variant.itemblocker.rule.Action;

import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * The {@link ItemBlockerApi} registered with the server's services manager.
 */
final class ItemBlockerService implements ItemBlockerApi {

    private final RestrictionService restrictions;
    private final TemporaryRules temporaryRules;

    ItemBlockerService(RestrictionService restrictions, TemporaryRules temporaryRules) {
        this.restrictions = restrictions;
        this.temporaryRules = temporaryRules;
    }

    @Override
    public Optional<Restriction> check(Player player, ItemStack item, Action action) {
        return restrictions.checkItem(player, item, action).map(RestrictionService::restrictionOf);
    }

    @Override
    public Optional<Restriction> check(ItemStack item, Action action, Location location) {
        return restrictions.checkItem(item, action, location).map(RestrictionService::restrictionOf);
    }

    @Override
    public TemporaryRule blockTemporarily(Material material, Set<Action> actions, String reason) {
        return temporaryRules.add(null, Objects.requireNonNull(material, "material"), actions, reason);
    }

    @Override
    public TemporaryRule blockTemporarily(Player player, Material material, Set<Action> actions, String reason) {
        return temporaryRules.add(player.getUniqueId(), Objects.requireNonNull(material, "material"), actions, reason);
    }
}
