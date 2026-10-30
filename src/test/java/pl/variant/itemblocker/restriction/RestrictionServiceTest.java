package pl.variant.itemblocker.restriction;

import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.potion.PotionEffectType;
import org.junit.jupiter.api.Test;
import pl.variant.itemblocker.message.Message;
import pl.variant.itemblocker.rule.Action;
import pl.variant.itemblocker.rule.ItemRule;
import pl.variant.itemblocker.rule.Preset;
import pl.variant.itemblocker.rule.WorldScope;
import pl.variant.itemblocker.testing.PluginTest;

import java.util.EnumSet;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RestrictionServiceTest extends PluginTest {

    private RestrictionService restrictions() {
        return plugin.enforcement().restrictions();
    }

    @Test
    void blocksOnlyListedActions() {
        block(Material.TNT, Action.PLACE);

        Denial denial = restrictions().checkItem(player, new ItemStack(Material.TNT), Action.PLACE).orElseThrow();
        assertEquals(Message.RESTRICTION_PLACE, denial.message());
        assertEquals("default", denial.source().name());
        assertTrue(restrictions().checkItem(player, new ItemStack(Material.TNT), Action.DROP).isEmpty());
        assertTrue(restrictions().checkItem(player, null, Action.PLACE).isEmpty());
    }

    @Test
    void respectsWorldScopes() {
        updateRules(rules -> rules.with(Preset.empty("other")
                .withItem(Material.TNT, new ItemRule(EnumSet.of(Action.PLACE), WorldScope.of(List.of("nether"))))));

        assertTrue(restrictions().checkItem(player, new ItemStack(Material.TNT), Action.PLACE).isEmpty());
        assertTrue(restrictions().checkItem(new ItemStack(Material.TNT), Action.PLACE, "nether").isPresent());
    }

    @Test
    void perActionBypassSkipsOnlyThatAction() {
        block(Material.TNT, Action.PLACE, Action.DROP);
        player.addAttachment(plugin, "itemblocker.bypass.place", true);

        assertTrue(restrictions().checkItem(player, new ItemStack(Material.TNT), Action.PLACE).isEmpty());
        assertTrue(restrictions().checkItem(player, new ItemStack(Material.TNT), Action.DROP).isPresent());
    }

    @Test
    void globalBypassSkipsEverything() {
        block(Material.TNT, Action.PLACE);
        blockEffect("speed", 1);
        player.addAttachment(plugin, "itemblocker.bypass", true);

        assertTrue(restrictions().checkItem(player, new ItemStack(Material.TNT), Action.PLACE).isEmpty());
        assertTrue(restrictions().checkEffect(player, PotionEffectType.SPEED, 1).isEmpty());
    }

    @Test
    void bypassCanBeTurnedOff() {
        block(Material.TNT, Action.PLACE);
        plugin.getConfig().set("bypass.enabled", false);
        plugin.getConfig().set("bypass.per-action", false);
        plugin.saveConfig();
        reload();
        player.addAttachment(plugin, "itemblocker.bypass", true);

        assertTrue(restrictions().checkItem(player, new ItemStack(Material.TNT), Action.PLACE).isPresent());
    }

    @Test
    void itemWithBlockedEnchantmentIsBlockedForEveryAction() {
        blockEnchantment("sharpness", 3);
        ItemStack sword = new ItemStack(Material.DIAMOND_SWORD);
        sword.addUnsafeEnchantment(Enchantment.SHARPNESS, 3);

        Denial denial = restrictions().checkItem(player, sword, Action.PICKUP).orElseThrow();
        assertEquals(Message.RESTRICTION_ENCHANTED_ITEM, denial.message());

        sword.addUnsafeEnchantment(Enchantment.SHARPNESS, 2);
        assertTrue(restrictions().checkItem(player, sword, Action.PICKUP).isEmpty());
    }

    @Test
    void enchantedBookIsCheckedToo() {
        blockEnchantment("mending", 1);
        ItemStack book = new ItemStack(Material.ENCHANTED_BOOK);
        EnchantmentStorageMeta meta = (EnchantmentStorageMeta) book.getItemMeta();
        meta.addStoredEnchant(Enchantment.MENDING, 1, true);
        book.setItemMeta(meta);

        assertTrue(restrictions().checkItem(player, book, Action.INVENTORY).isPresent());
        player.addAttachment(plugin, "itemblocker.bypass.enchantment", true);
        assertTrue(restrictions().checkItem(player, book, Action.INVENTORY).isEmpty());
    }

    @Test
    void checksEnchantingAndEffectsByLevel() {
        blockEnchantment("sharpness", 5);
        blockEffect("strength", 2);

        assertTrue(restrictions().checkEnchanting(player, Enchantment.SHARPNESS, 5).isPresent());
        assertTrue(restrictions().checkEnchanting(player, Enchantment.SHARPNESS, 4).isEmpty());
        assertTrue(restrictions().checkEffect(player, PotionEffectType.STRENGTH, 2).isPresent());
        assertTrue(restrictions().checkEffect(player, PotionEffectType.STRENGTH, 1).isEmpty());
    }

    private void reload() {
        try {
            plugin.reload();
        } catch (java.io.IOException exception) {
            throw new java.io.UncheckedIOException(exception);
        }
    }
}
