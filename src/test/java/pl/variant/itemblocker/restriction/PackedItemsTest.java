package pl.variant.itemblocker.restriction;

import org.bukkit.Material;
import org.bukkit.block.ShulkerBox;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.bukkit.inventory.meta.BundleMeta;
import org.junit.jupiter.api.Test;
import pl.variant.itemblocker.rule.Action;
import pl.variant.itemblocker.testing.PluginTest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PackedItemsTest extends PluginTest {

    @Test
    void aShulkerBoxWithABlockedItemInsideIsBlockedWhenMoved() {
        block(Material.TNT, Action.PICKUP, Action.INVENTORY, Action.HOPPER);
        ItemStack box = shulkerBoxWith(new ItemStack(Material.TNT));
        RestrictionService restrictions = plugin.enforcement().restrictions();

        Denial denial = restrictions.checkItem(player, box, Action.PICKUP).orElseThrow();

        assertEquals("TNT", denial.placeholders().get(0).value());
        assertTrue(restrictions.checkItem(player, box, Action.INVENTORY).isPresent());
        assertTrue(restrictions.checkItem(box, Action.HOPPER, null).isPresent());
    }

    @Test
    void packedItemsDoNotCountForActionsThatDoNotMoveTheBox() {
        block(Material.TNT, Action.PLACE, Action.USE);
        ItemStack box = shulkerBoxWith(new ItemStack(Material.TNT));
        RestrictionService restrictions = plugin.enforcement().restrictions();

        assertTrue(restrictions.checkItem(player, box, Action.PLACE).isEmpty());
        assertTrue(restrictions.checkItem(player, box, Action.USE).isEmpty());
    }

    @Test
    void aShulkerBoxWithOnlyAllowedItemsCanBeMoved() {
        block(Material.TNT, Action.PICKUP);

        assertTrue(plugin.enforcement().restrictions()
                .checkItem(player, shulkerBoxWith(new ItemStack(Material.STONE, 64)), Action.PICKUP).isEmpty());
    }

    @Test
    void aBundleWithABlockedItemInsideIsBlockedWhenMoved() {
        block(Material.TNT, Action.INVENTORY);
        ItemStack bundle = new ItemStack(Material.BUNDLE);
        BundleMeta meta = (BundleMeta) bundle.getItemMeta();
        meta.setItems(List.of(new ItemStack(Material.TNT)));
        bundle.setItemMeta(meta);

        assertTrue(plugin.enforcement().restrictions().checkItem(player, bundle, Action.INVENTORY).isPresent());
    }

    private static ItemStack shulkerBoxWith(ItemStack inside) {
        ItemStack box = new ItemStack(Material.SHULKER_BOX);
        BlockStateMeta meta = (BlockStateMeta) box.getItemMeta();
        ShulkerBox state = (ShulkerBox) meta.getBlockState();
        state.getInventory().addItem(inside);
        meta.setBlockState(state);
        box.setItemMeta(meta);
        return box;
    }
}
