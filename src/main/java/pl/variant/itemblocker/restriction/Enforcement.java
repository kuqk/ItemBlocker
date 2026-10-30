package pl.variant.itemblocker.restriction;

import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.inventory.ItemStack;
import pl.variant.itemblocker.rule.Action;

import java.util.Optional;

/**
 * What event listeners use to apply restrictions: checks, cancels and notifies in one call.
 */
public final class Enforcement {

    private final RestrictionService restrictions;
    private final Notifier notifier;

    public Enforcement(RestrictionService restrictions, Notifier notifier) {
        this.restrictions = restrictions;
        this.notifier = notifier;
    }

    public RestrictionService restrictions() {
        return restrictions;
    }

    /**
     * Cancels the event and tells the player if the action with the item is blocked.
     *
     * @return whether the event was cancelled
     */
    public boolean denyItem(Player player, ItemStack item, Action action, Cancellable event) {
        return deny(player, restrictions.checkItem(player, item, action), event);
    }

    /**
     * Cancels the event and tells the player if there is a denial.
     *
     * @return whether the event was cancelled
     */
    public boolean deny(Player player, Optional<Denial> denial, Cancellable event) {
        denial.ifPresent(found -> {
            event.setCancelled(true);
            notifier.notify(player, found);
        });
        return denial.isPresent();
    }

    public void notify(Player player, Denial denial) {
        notifier.notify(player, denial);
    }
}
