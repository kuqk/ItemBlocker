package pl.variant.itemblocker.restriction;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import pl.variant.itemblocker.message.Message;
import pl.variant.itemblocker.message.Placeholder;
import pl.variant.itemblocker.rule.Limit;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Keeps players within stack and inventory limits.
 *
 * <p>Picking up is limited before it happens. Everything else, such as taking items out of a
 * container or crafting, is corrected right after: stacks above the limit are split into free slots,
 * and items that do not fit or exceed the inventory limit are dropped at the player's feet. Items
 * are only ever moved, never deleted or copied.
 */
public final class LimitEnforcer {

    private final Enforcement enforcement;

    public LimitEnforcer(Enforcement enforcement) {
        this.enforcement = enforcement;
    }

    /**
     * How much of a stack a player may pick up.
     *
     * @param amount the number of items the player may take
     * @param denial why not all of them, if {@code amount} is smaller than the stack
     */
    public record Allowance(int amount, Optional<Denial> denial) {
    }

    /**
     * Returns whether any limit applies to the player where they are.
     */
    public boolean appliesTo(Player player) {
        return !enforcement.restrictions().limits(player).isEmpty();
    }

    public void notify(Player player, Denial denial) {
        enforcement.notify(player, denial);
    }

    public Allowance allowedPickup(Player player, ItemStack item) {
        PlayerLimits limits = enforcement.restrictions().limits(player);
        Material type = item.getType();
        int allowed = item.getAmount();
        Optional<Denial> denial = Optional.empty();

        Optional<Limit> inventoryLimit = limits.inventory(type);
        if (inventoryLimit.isPresent()) {
            int room = Math.max(0, inventoryLimit.get().amount() - count(player.getInventory(), type));
            if (room < allowed) {
                allowed = room;
                denial = Optional.of(inventoryDenial(type, inventoryLimit.get()));
            }
        }

        Optional<Limit> stackLimit = limits.stack(type);
        if (stackLimit.isPresent()) {
            int room = stackRoom(player.getInventory(), item, stackLimit.get().amount());
            if (room < allowed) {
                allowed = room;
                denial = Optional.of(stackDenial(type, stackLimit.get()));
            }
        }
        return new Allowance(allowed, denial);
    }

    /**
     * Returns whether the player may receive the given number of items of the type without going
     * over their inventory limit, and tells them if not.
     */
    public boolean mayReceive(Player player, ItemStack item, int amount) {
        if (item == null || item.getType().isAir()) {
            return true;
        }

        Optional<Limit> limit = enforcement.restrictions().limits(player).inventory(item.getType());
        if (limit.isPresent() && count(player.getInventory(), item.getType()) + amount > limit.get().amount()) {
            enforcement.notify(player, inventoryDenial(item.getType(), limit.get()));
            return false;
        }
        return true;
    }

    /**
     * Splits stacks above the limit and drops what exceeds the inventory limit or does not fit.
     */
    public void enforce(Player player) {
        PlayerLimits limits = enforcement.restrictions().limits(player);
        if (limits.isEmpty()) {
            return;
        }

        PlayerInventory inventory = player.getInventory();
        ItemStack[] storage = inventory.getStorageContents();
        ItemStack offHand = inventory.getItemInOffHand();
        Set<Material> types = new LinkedHashSet<>();
        for (ItemStack item : storage) {
            if (!isEmpty(item)) {
                types.add(item.getType());
            }
        }
        if (!isEmpty(offHand)) {
            types.add(offHand.getType());
        }

        List<ItemStack> excess = new ArrayList<>();
        Denial denial = null;
        for (Material type : types) {
            Optional<Limit> inventoryLimit = limits.inventory(type);
            if (inventoryLimit.isPresent()) {
                int over = count(inventory, type) - inventoryLimit.get().amount();
                if (over > 0) {
                    over = takeFromEnd(storage, type, over, excess);
                    if (over > 0 && offHand.getType() == type) {
                        int taken = Math.min(over, offHand.getAmount());
                        excess.add(withAmount(offHand, taken));
                        offHand = shrink(offHand, taken);
                    }
                    denial = inventoryDenial(type, inventoryLimit.get());
                }
            }

            Optional<Limit> stackLimit = limits.stack(type);
            if (stackLimit.isPresent()) {
                int maxStack = stackLimit.get().amount();
                List<ItemStack> overflow = new ArrayList<>();
                for (int slot = 0; slot < storage.length; slot++) {
                    storage[slot] = splitOff(storage[slot], type, maxStack, overflow);
                }
                offHand = splitOff(offHand, type, maxStack, overflow);
                if (!overflow.isEmpty()) {
                    placeInEmptySlots(storage, overflow, maxStack, excess);
                    denial = stackDenial(type, stackLimit.get());
                }
            }
        }

        if (denial == null) {
            return;
        }
        inventory.setStorageContents(storage);
        inventory.setItemInOffHand(offHand);
        excess.forEach(item -> player.getWorld().dropItemNaturally(player.getLocation(), item));
        enforcement.notify(player, denial);
    }

    private static int count(PlayerInventory inventory, Material type) {
        int total = 0;
        for (ItemStack item : inventory.getContents()) {
            if (item != null && item.getType() == type) {
                total += item.getAmount();
            }
        }
        return total;
    }

    /**
     * Returns how many of the item fit into the storage slots without any stack going over the limit.
     */
    private static int stackRoom(PlayerInventory inventory, ItemStack item, int maxStack) {
        int room = 0;
        for (ItemStack slot : inventory.getStorageContents()) {
            if (isEmpty(slot)) {
                room += maxStack;
            } else if (slot.isSimilar(item)) {
                room += Math.max(0, maxStack - slot.getAmount());
            }
        }
        return room;
    }

    /**
     * Takes items of the type from the last storage slot backwards, so the hotbar is emptied last.
     *
     * @return how many could not be taken
     */
    private static int takeFromEnd(ItemStack[] storage, Material type, int amount, List<ItemStack> taken) {
        int remaining = amount;
        for (int slot = storage.length - 1; slot >= 0 && remaining > 0; slot--) {
            ItemStack item = storage[slot];
            if (item != null && item.getType() == type) {
                int take = Math.min(remaining, item.getAmount());
                taken.add(withAmount(item, take));
                storage[slot] = shrink(item, take);
                remaining -= take;
            }
        }
        return remaining;
    }

    /**
     * Cuts a stack of the type down to the limit, adding the rest to the overflow.
     */
    private static ItemStack splitOff(ItemStack item, Material type, int maxStack, List<ItemStack> overflow) {
        if (item == null || item.getType() != type || item.getAmount() <= maxStack) {
            return item;
        }
        overflow.add(withAmount(item, item.getAmount() - maxStack));
        return withAmount(item, maxStack);
    }

    private static void placeInEmptySlots(ItemStack[] storage, List<ItemStack> overflow, int maxStack, List<ItemStack> excess) {
        for (ItemStack item : overflow) {
            int remaining = item.getAmount();
            for (int slot = 0; slot < storage.length && remaining > 0; slot++) {
                if (isEmpty(storage[slot])) {
                    int amount = Math.min(maxStack, remaining);
                    storage[slot] = withAmount(item, amount);
                    remaining -= amount;
                }
            }
            if (remaining > 0) {
                excess.add(withAmount(item, remaining));
            }
        }
    }

    private static ItemStack withAmount(ItemStack item, int amount) {
        ItemStack copy = item.clone();
        copy.setAmount(amount);
        return copy;
    }

    private static ItemStack shrink(ItemStack item, int amount) {
        return item.getAmount() <= amount ? null : withAmount(item, item.getAmount() - amount);
    }

    private static boolean isEmpty(ItemStack item) {
        return item == null || item.getType().isAir();
    }

    private static Denial stackDenial(Material type, Limit limit) {
        return new Denial(Message.RESTRICTION_STACK_LIMIT, limit.source(), List.of(
                Placeholder.item("item", type),
                Placeholder.of("limit", limit.amount())));
    }

    private static Denial inventoryDenial(Material type, Limit limit) {
        return new Denial(Message.RESTRICTION_INVENTORY_LIMIT, limit.source(), List.of(
                Placeholder.item("item", type),
                Placeholder.of("limit", limit.amount())));
    }
}
