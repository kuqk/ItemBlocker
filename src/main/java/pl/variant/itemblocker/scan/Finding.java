package pl.variant.itemblocker.scan;

import org.bukkit.inventory.ItemStack;

/**
 * A forbidden item found by a scan.
 *
 * @param holder the player name or the container location
 * @param item   the item
 * @param preset the preset that forbids it
 */
public record Finding(String holder, ItemStack item, String preset) {
}
