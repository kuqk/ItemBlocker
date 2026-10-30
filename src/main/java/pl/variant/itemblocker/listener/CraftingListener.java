package pl.variant.itemblocker.listener;

import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.PrepareSmithingEvent;
import org.bukkit.event.inventory.SmithItemEvent;
import org.bukkit.inventory.ItemStack;
import pl.variant.itemblocker.restriction.Enforcement;
import pl.variant.itemblocker.rule.Action;

/**
 * Crafting and smithing results.
 */
public final class CraftingListener implements Listener {

    private final Enforcement enforcement;

    public CraftingListener(Enforcement enforcement) {
        this.enforcement = enforcement;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onCraft(CraftItemEvent event) {
        if (event.getWhoClicked() instanceof Player player) {
            enforcement.denyItem(player, event.getInventory().getResult(), Action.CRAFTING, event);
        }
    }

    /**
     * Hides a blocked smithing result so it never shows up in the result slot.
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onPrepareSmithing(PrepareSmithingEvent event) {
        ItemStack result = event.getResult();
        for (HumanEntity viewer : event.getViewers()) {
            if (viewer instanceof Player player
                    && enforcement.restrictions().checkItem(player, result, Action.SMITHING).isPresent()) {
                event.setResult(null);
                return;
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onSmith(SmithItemEvent event) {
        if (event.getWhoClicked() instanceof Player player) {
            enforcement.denyItem(player, event.getInventory().getResult(), Action.SMITHING, event);
        }
    }
}
