package pl.variant.itemblocker.listener;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityPlaceEvent;
import org.bukkit.event.hanging.HangingPlaceEvent;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.inventory.EquipmentSlot;
import pl.variant.itemblocker.restriction.Enforcement;
import pl.variant.itemblocker.rule.Action;

/**
 * Placing blocks, emptying buckets and placing item entities such as boats, minecarts, armor
 * stands, end crystals, item frames and paintings.
 */
public final class PlacementListener implements Listener {

    private final Enforcement enforcement;

    public PlacementListener(Enforcement enforcement) {
        this.enforcement = enforcement;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        enforcement.denyItem(event.getPlayer(), event.getItemInHand(), Action.PLACE, event);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBucketEmpty(PlayerBucketEmptyEvent event) {
        Player player = event.getPlayer();
        enforcement.denyItem(player, player.getInventory().getItem(event.getHand()), Action.PLACE, event);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityPlace(EntityPlaceEvent event) {
        Player player = event.getPlayer();
        if (player != null) {
            EquipmentSlot hand = event.getHand();
            enforcement.denyItem(player, player.getInventory().getItem(hand), Action.PLACE, event);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onHangingPlace(HangingPlaceEvent event) {
        Player player = event.getPlayer();
        if (player != null) {
            enforcement.denyItem(player, event.getItemStack(), Action.PLACE, event);
        }
    }
}
