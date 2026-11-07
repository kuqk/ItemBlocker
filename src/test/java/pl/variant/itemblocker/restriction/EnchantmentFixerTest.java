package pl.variant.itemblocker.restriction;

import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import pl.variant.itemblocker.message.Messages;
import pl.variant.itemblocker.rule.Action;
import pl.variant.itemblocker.rule.EnchantmentMode;
import pl.variant.itemblocker.testing.PluginTest;

import java.util.List;
import java.util.Map;
import java.nio.file.Path;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

class EnchantmentFixerTest extends PluginTest {

    @Test
    void downgradeLowersTheEnchantmentToTheHighestAllowedLevel() {
        limit(EnchantmentMode.DOWNGRADE);
        player.getInventory().setItem(0, sword(5));

        join();

        assertEquals(4, player.getInventory().getItem(0).getEnchantmentLevel(Enchantment.SHARPNESS));
        assertEquals(List.of("[ItemBlocker] Diamond Sword: Sharpness was lowered to level 4."), messages());
    }

    @Test
    void removeTakesTheEnchantmentOff() {
        limit(EnchantmentMode.REMOVE);
        player.getInventory().setItem(0, sword(5));

        join();

        assertTrue(player.getInventory().getItem(0).getEnchantments().isEmpty());
    }

    @Test
    void confiscateTakesTheItemAway() {
        limit(EnchantmentMode.CONFISCATE);
        player.getInventory().setItem(0, sword(5));

        join();

        assertNull(player.getInventory().getItem(0));
        assertEquals(Map.of(player.getName(), 1), plugin.confiscated().owners());
    }

    @Test
    void failedNotificationCannotLeaveAConfiscatedItemInBothInventories(@TempDir Path folder) {
        limit(EnchantmentMode.CONFISCATE);
        player.getInventory().setItem(0, sword(5));
        player.getInventory().setItem(1, sword(5));
        Messages brokenMessages = new Messages(folder.resolve("lang"), plugin::getResource, plugin.getLogger(),
                (target, text) -> { throw new IllegalStateException("Broken placeholder expansion"); });
        brokenMessages.load("en");
        Enforcement enforcement = new Enforcement(plugin.enforcement().restrictions(),
                new Notifier(brokenMessages, () -> Duration.ZERO), plugin.actionLog());
        EnchantmentFixer fixer = new EnchantmentFixer(enforcement, plugin.confiscated());

        assertThrows(IllegalStateException.class, () -> fixer.fixInventory(player));

        assertNull(player.getInventory().getItem(0));
        assertEquals(sword(5), player.getInventory().getItem(1));
        assertEquals(Map.of(player.getName(), 1), plugin.confiscated().owners());
    }

    @Test
    void enchantedBooksAreLoweredToo() {
        limit(EnchantmentMode.DOWNGRADE);
        ItemStack book = new ItemStack(Material.ENCHANTED_BOOK);
        EnchantmentStorageMeta meta = (EnchantmentStorageMeta) book.getItemMeta();
        meta.addStoredEnchant(Enchantment.SHARPNESS, 5, true);
        book.setItemMeta(meta);
        player.getInventory().setItem(0, book);

        join();

        assertEquals(4, ((EnchantmentStorageMeta) player.getInventory().getItem(0).getItemMeta()).getStoredEnchantLevel(Enchantment.SHARPNESS));
    }

    @Test
    void onlyBlockModeBlocksTheItemItself() {
        limit(EnchantmentMode.DOWNGRADE);
        assertTrue(plugin.enforcement().restrictions().checkItem(player, sword(5), Action.USE).isEmpty());

        limit(EnchantmentMode.BLOCK);
        assertTrue(plugin.enforcement().restrictions().checkItem(player, sword(5), Action.USE).isPresent());
        player.getInventory().setItem(0, sword(5));
        join();
        assertEquals(5, player.getInventory().getItem(0).getEnchantmentLevel(Enchantment.SHARPNESS));
    }

    @Test
    void lowerLevelsAreLeftAlone() {
        limit(EnchantmentMode.REMOVE);
        player.getInventory().setItem(0, sword(4));

        join();

        assertEquals(4, player.getInventory().getItem(0).getEnchantmentLevel(Enchantment.SHARPNESS));
    }

    private void limit(EnchantmentMode mode) {
        updateDefault(preset -> preset.withEnchantments(preset.enchantments().with("sharpness", 5).withMode("sharpness", mode)));
    }

    private static ItemStack sword(int sharpness) {
        ItemStack sword = new ItemStack(Material.DIAMOND_SWORD);
        sword.addUnsafeEnchantment(Enchantment.SHARPNESS, sharpness);
        return sword;
    }

    private void join() {
        server.getPluginManager().callEvent(new PlayerJoinEvent(player, Component.text("joined")));
        server.getScheduler().performOneTick();
    }
}
