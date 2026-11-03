package pl.variant.itemblocker.listener;

import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockDispenseEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityDropItemEvent;
import org.bukkit.event.inventory.BrewEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.world.LootGenerateEvent;
import org.bukkit.inventory.ItemStack;
import pl.variant.itemblocker.restriction.Enforcement;
import pl.variant.itemblocker.restriction.RestrictionService;
import pl.variant.itemblocker.rule.Action;

/**
 * The ways items come into the world other than crafting and smithing: trading with villagers,
 * loot in chests and other containers, mob drops, brewing and dispensers.
 */
public final class AcquisitionListener implements Listener {

    private final Enforcement enforcement;

    public AcquisitionListener(Enforcement enforcement) {
        this.enforcement = enforcement;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onTrade(InventoryClickEvent event) {
        if (event.getWhoClicked() instanceof Player player
                && event.getInventory().getType() == InventoryType.MERCHANT
                && event.getSlotType() == InventoryType.SlotType.RESULT) {
            enforcement.denyItem(player, event.getCurrentItem(), Action.TRADE, event);
        }
    }

    /**
     * Blocked items are taken out of the loot; the rest is generated as usual.
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onLoot(LootGenerateEvent event) {
        Location location = event.getLootContext().getLocation();
        event.getLoot().removeIf(item -> isBlocked(item, Action.LOOT, location));
    }

    /**
     * Blocked items are taken out of what a mob drops. Players keep dropping everything, so that a
     * rule can never delete what a player owns.
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onDeath(EntityDeathEvent event) {
        if (!(event.getEntity() instanceof Player)) {
            Location location = event.getEntity().getLocation();
            event.getDrops().removeIf(item -> isBlocked(item, Action.ENTITY_DROP, location));
        }
    }

    /**
     * Some older servers drop mob equipment after the death event, outside its list of drops.
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onDropOnDeath(EntityDropItemEvent event) {
        if (event.getEntity() instanceof LivingEntity entity && !(entity instanceof Player) && entity.isDead()
                && isBlocked(event.getItemDrop().getItemStack(), Action.ENTITY_DROP, entity.getLocation())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onDispense(BlockDispenseEvent event) {
        if (isBlocked(event.getItem(), Action.DISPENSE, event.getBlock().getLocation())) {
            event.setCancelled(true);
        }
    }

    /**
     * Brewing stops when any of the potions it would make is blocked, and the ingredient is kept.
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBrew(BrewEvent event) {
        RestrictionService restrictions = enforcement.restrictions();
        Location location = event.getBlock().getLocation();
        if (event.getResults().stream().anyMatch(result -> restrictions.checkBrew(result, location).isPresent())) {
            event.setCancelled(true);
        }
    }

    private boolean isBlocked(ItemStack item, Action action, Location location) {
        return enforcement.restrictions().checkItem(item, action, location).isPresent();
    }
}
