package pl.variant.itemblocker.listener;

import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import pl.variant.itemblocker.platform.PlayerScheduler;
import pl.variant.itemblocker.restriction.LimitEnforcer;

/**
 * Stack and inventory limits. Runs after the other listeners, so items blocked outright are never
 * handed out here.
 */
public final class LimitListener implements Listener {

    private final LimitEnforcer limits;
    private final PlayerScheduler scheduler;

    public LimitListener(LimitEnforcer limits, PlayerScheduler scheduler) {
        this.limits = limits;
        this.scheduler = scheduler;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPickup(EntityPickupItemEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }

        Item entity = event.getItem();
        ItemStack stack = entity.getItemStack();
        LimitEnforcer.Allowance allowance = limits.allowedPickup(player, stack);
        if (allowance.amount() < stack.getAmount()) {
            // Only part of the stack may be taken: hand it over here and leave the rest on the ground.
            event.setCancelled(true);
            if (allowance.amount() > 0) {
                ItemStack taken = stack.clone();
                taken.setAmount(allowance.amount());
                int notAdded = player.getInventory().addItem(taken).values().stream().mapToInt(ItemStack::getAmount).sum();
                ItemStack left = stack.clone();
                left.setAmount(stack.getAmount() - allowance.amount() + notAdded);
                entity.setItemStack(left);
            }
            allowance.denial().ifPresent(denial -> limits.notify(player, denial));
        }
        enforceNextTick(player);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        Inventory top = event.getInventory();
        PlayerInventory own = player.getInventory();
        boolean fromContainer = isContainer(top) && event.getClickedInventory() == top;
        ItemStack incoming = null;
        int amount = 0;
        if (fromContainer && (event.isShiftClick() || event.getClick() == ClickType.NUMBER_KEY)) {
            incoming = event.getCurrentItem();
            amount = incoming == null ? 0 : incoming.getAmount();
        } else if (isContainer(top) && event.getClickedInventory() == own && !isEmpty(event.getCursor())) {
            incoming = event.getCursor();
            amount = event.getClick() == ClickType.RIGHT ? 1 : incoming.getAmount();
        }

        if (incoming != null && !limits.mayReceive(player, incoming, amount)) {
            event.setCancelled(true);
            return;
        }
        enforceNextTick(player);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        Inventory top = event.getInventory();
        boolean intoOwnInventory = event.getRawSlots().stream().anyMatch(rawSlot -> rawSlot >= top.getSize());
        if (isContainer(top) && intoOwnInventory) {
            ItemStack dragged = event.getOldCursor();
            int remaining = event.getCursor() == null ? 0 : event.getCursor().getAmount();
            if (!limits.mayReceive(player, dragged, dragged.getAmount() - remaining)) {
                event.setCancelled(true);
                return;
            }
        }
        enforceNextTick(player);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        enforceNextTick(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onWorldChange(PlayerChangedWorldEvent event) {
        enforceNextTick(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onRespawn(PlayerRespawnEvent event) {
        enforceNextTick(event.getPlayer());
    }

    private void enforceNextTick(Player player) {
        if (!limits.appliesTo(player)) {
            return;
        }
        scheduler.runNextTick(player, () -> {
            if (player.isOnline()) {
                limits.enforce(player);
            }
        });
    }

    private static boolean isContainer(Inventory top) {
        InventoryType type = top.getType();
        return type != InventoryType.CRAFTING && type != InventoryType.CREATIVE && type != InventoryType.PLAYER;
    }

    private static boolean isEmpty(ItemStack item) {
        return item == null || item.getType().isAir();
    }
}
