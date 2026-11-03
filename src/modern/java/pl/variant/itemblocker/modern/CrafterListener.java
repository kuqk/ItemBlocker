package pl.variant.itemblocker.modern;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.CrafterCraftEvent;
import pl.variant.itemblocker.restriction.Enforcement;
import pl.variant.itemblocker.rule.Action;

/**
 * The crafter block, added in 1.21. Loaded by the plugin only on servers that have it.
 */
public final class CrafterListener implements Listener {

    private final Enforcement enforcement;

    public CrafterListener(Enforcement enforcement) {
        this.enforcement = enforcement;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onCraft(CrafterCraftEvent event) {
        if (enforcement.restrictions().checkItem(event.getResult(), Action.CRAFTER, event.getBlock().getLocation()).isPresent()) {
            event.setCancelled(true);
        }
    }
}
