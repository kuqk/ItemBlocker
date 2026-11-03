package pl.variant.itemblocker.listener;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.enchantment.EnchantItemEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import pl.variant.itemblocker.platform.PlayerScheduler;
import pl.variant.itemblocker.restriction.EnchantmentFixer;
import pl.variant.itemblocker.rule.RuleBook;

import java.util.function.Supplier;

/**
 * Lowers, removes or confiscates enchantments a tick after items may have reached a player: on
 * joining, changing worlds, respawning, picking up, clicking in an inventory, closing one and
 * enchanting.
 */
public final class EnchantmentFixListener implements Listener {

    private final EnchantmentFixer fixer;
    private final PlayerScheduler scheduler;
    private final Supplier<RuleBook> rules;

    public EnchantmentFixListener(EnchantmentFixer fixer, PlayerScheduler scheduler, Supplier<RuleBook> rules) {
        this.fixer = fixer;
        this.scheduler = scheduler;
        this.rules = rules;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        fixNextTick(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onWorldChange(PlayerChangedWorldEvent event) {
        fixNextTick(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onRespawn(PlayerRespawnEvent event) {
        fixNextTick(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPickup(EntityPickupItemEvent event) {
        if (event.getEntity() instanceof Player player) {
            fixNextTick(player);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onClick(InventoryClickEvent event) {
        if (event.getWhoClicked() instanceof Player player) {
            fixNextTick(player);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onClose(InventoryCloseEvent event) {
        if (event.getPlayer() instanceof Player player) {
            fixNextTick(player);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEnchant(EnchantItemEvent event) {
        fixNextTick(event.getEnchanter());
    }

    private void fixNextTick(Player player) {
        if (!rules.get().usesEnchantmentModes()) {
            return;
        }
        scheduler.runNextTick(player, () -> {
            if (player.isOnline()) {
                fixer.fixInventory(player);
            }
        });
    }
}
