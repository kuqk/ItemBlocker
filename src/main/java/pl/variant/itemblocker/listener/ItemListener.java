package pl.variant.itemblocker.listener;

import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.inventory.ItemStack;
import pl.variant.itemblocker.restriction.Enforcement;
import pl.variant.itemblocker.restriction.WearableSlots;

import static pl.variant.itemblocker.rule.Action.ARMOR;
import static pl.variant.itemblocker.rule.Action.DROP;
import static pl.variant.itemblocker.rule.Action.PICKUP;
import static pl.variant.itemblocker.rule.Action.USE;

/**
 * Picking up, dropping, using and consuming items, and putting on armor with right click.
 */
public final class ItemListener implements Listener {

    private final Enforcement enforcement;
    private final WearableSlots wearableSlots;

    public ItemListener(Enforcement enforcement, WearableSlots wearableSlots) {
        this.enforcement = enforcement;
        this.wearableSlots = wearableSlots;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPickup(EntityPickupItemEvent event) {
        if (event.getEntity() instanceof Player player) {
            enforcement.denyItem(player, event.getItem().getItemStack(), PICKUP, event);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onDrop(PlayerDropItemEvent event) {
        enforcement.denyItem(event.getPlayer(), event.getItemDrop().getItemStack(), DROP, event);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onConsume(PlayerItemConsumeEvent event) {
        enforcement.denyItem(event.getPlayer(), event.getItem(), USE, event);
    }

    // Right clicks on air arrive already cancelled, so cancelled events are not ignored here. Only
    // the use of the item is denied, which still lets the player open doors or chests.
    @EventHandler(priority = EventPriority.HIGH)
    public void onInteract(PlayerInteractEvent event) {
        boolean clickedBlock = event.getAction() == Action.RIGHT_CLICK_BLOCK;
        if (event.useItemInHand() == Event.Result.DENY || (!clickedBlock && event.getAction() != Action.RIGHT_CLICK_AIR)) {
            return;
        }

        ItemStack item = event.getItem();
        if (item == null || item.getType().isAir()) {
            return;
        }

        // Block items clicked on a block are placed, which the placement listener handles.
        boolean placesBlock = clickedBlock && item.getType().isBlock();
        boolean wearable = wearableSlots.armorSlotOf(item).isPresent();
        if (placesBlock || (!wearable && item.getType().isBlock())) {
            return;
        }

        var action = wearable ? ARMOR : USE;
        enforcement.restrictions().checkItem(event.getPlayer(), item, action).ifPresent(denial -> {
            event.setUseItemInHand(Event.Result.DENY);
            enforcement.notify(event.getPlayer(), denial);
        });
    }
}
