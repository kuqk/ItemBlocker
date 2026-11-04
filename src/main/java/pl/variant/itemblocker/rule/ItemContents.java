package pl.variant.itemblocker.rule;

import org.bukkit.block.Container;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.bukkit.inventory.meta.BundleMeta;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

/**
 * Items carried inside another item: the contents of a shulker box or any other container item, and
 * of a bundle. Without looking inside, a blocked item could be moved around packed in one of them.
 */
public final class ItemContents {

    private ItemContents() {
    }

    /**
     * Returns the items directly inside the item, without empty slots. Items inside those are not
     * included.
     */
    public static List<ItemStack> of(ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return List.of();
        }

        ItemMeta meta = item.getItemMeta();
        List<ItemStack> contents = new ArrayList<>();
        if (meta instanceof BlockStateMeta blockMeta && blockMeta.hasBlockState()
                && blockMeta.getBlockState() instanceof Container container) {
            for (ItemStack inside : container.getSnapshotInventory().getContents()) {
                add(contents, inside);
            }
        }
        if (meta instanceof BundleMeta bundle && bundle.hasItems()) {
            for (ItemStack inside : bundle.getItems()) {
                add(contents, inside);
            }
        }
        return contents;
    }

    private static void add(List<ItemStack> contents, ItemStack item) {
        if (item != null && !item.getType().isAir()) {
            contents.add(item);
        }
    }
}
