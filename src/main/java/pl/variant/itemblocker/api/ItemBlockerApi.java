package pl.variant.itemblocker.api;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import pl.variant.itemblocker.rule.Action;

import java.util.Optional;
import java.util.Set;

/**
 * The ItemBlocker API for other plugins: check whether something is blocked and add temporary rules.
 *
 * <pre>{@code
 * ItemBlockerApi api = ItemBlockerApi.get();
 * if (api.isBlocked(player, item, Action.USE)) { ... }
 * TemporaryRule rule = api.blockTemporarily(player, Material.ENDER_PEARL, Set.of(Action.USE), "Not during the quest");
 * // later
 * rule.remove();
 * }</pre>
 *
 * <p>Other plugins can also listen to {@link pl.variant.itemblocker.api.event.ItemBlockedEvent} and
 * cancel it to let a blocked action through.
 */
public interface ItemBlockerApi {

    /**
     * Returns the API of the running ItemBlocker.
     *
     * @throws IllegalStateException if ItemBlocker is not enabled
     */
    static ItemBlockerApi get() {
        ItemBlockerApi api = Bukkit.getServicesManager().load(ItemBlockerApi.class);
        if (api == null) {
            throw new IllegalStateException("ItemBlocker is not enabled");
        }
        return api;
    }

    /**
     * Checks whether the player may perform the action with the item, taking their bypass
     * permissions, temporary rules and the item's enchantments into account.
     */
    Optional<Restriction> check(Player player, ItemStack item, Action action);

    /**
     * Checks an item without a player, as for hoppers. Bypass permissions do not apply.
     */
    Optional<Restriction> check(ItemStack item, Action action, Location location);

    default boolean isBlocked(Player player, ItemStack item, Action action) {
        return check(player, item, action).isPresent();
    }

    /**
     * Blocks the actions for an item type for everyone until the rule is removed or the server stops.
     * Temporary rules are never written to {@code rules.yml}.
     *
     * @param reason shown to players next to the block message, may be empty
     */
    TemporaryRule blockTemporarily(Material material, Set<Action> actions, String reason);

    /**
     * Blocks the actions for an item type for one player until the rule is removed, the player
     * leaves or the server stops.
     */
    TemporaryRule blockTemporarily(Player player, Material material, Set<Action> actions, String reason);
}
