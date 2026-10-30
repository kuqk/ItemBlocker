package pl.variant.itemblocker.restriction;

import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import pl.variant.itemblocker.rule.Action;

import java.util.Optional;

/**
 * Takes off armor a player is not allowed to wear, for example after joining or changing worlds.
 *
 * <p>The worn item is moved, never copied: it is taken out of its slot and put into the inventory,
 * or dropped at the player's feet if the inventory is full.
 */
public final class ArmorEnforcer {

    private final Enforcement enforcement;

    public ArmorEnforcer(Enforcement enforcement) {
        this.enforcement = enforcement;
    }

    public void removeBlockedArmor(Player player) {
        PlayerInventory inventory = player.getInventory();
        for (EquipmentSlot slot : Equipment.ARMOR_SLOTS) {
            ItemStack worn = inventory.getItem(slot);
            Optional<Denial> denial = enforcement.restrictions().checkItem(player, worn, Action.ARMOR);
            if (denial.isEmpty()) {
                continue;
            }

            inventory.setItem(slot, null);
            inventory.addItem(worn).values()
                    .forEach(leftover -> player.getWorld().dropItemNaturally(player.getLocation(), leftover));
            enforcement.notify(player, denial.get());
        }
    }
}
