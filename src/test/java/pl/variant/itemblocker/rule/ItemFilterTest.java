package pl.variant.itemblocker.rule;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;

import java.util.List;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ItemFilterTest {

    @BeforeEach
    void setUp() {
        MockBukkit.mock();
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    void matchesNameIgnoringCaseAndColors() {
        ItemFilter filter = filter(Material.DIAMOND_SWORD, "excalibur", null, null, null, null, null);

        assertTrue(filter.matches(item(Material.DIAMOND_SWORD, meta -> meta.setDisplayName(ChatColor.GOLD + "Legendary Excalibur"))));
        assertFalse(filter.matches(new ItemStack(Material.DIAMOND_SWORD)));
        assertFalse(filter.matches(item(Material.IRON_SWORD, meta -> meta.setDisplayName("Excalibur"))));
    }

    @Test
    void matchesLoreAndModel() {
        ItemFilter filter = filter(null, null, "soulbound", 1001, null, null, null);

        assertTrue(filter.matches(item(Material.STICK, meta -> {
            meta.setLore(List.of("Rare", ChatColor.RED + "Soulbound"));
            meta.setCustomModelData(1001);
        })));
        assertFalse(filter.matches(item(Material.STICK, meta -> meta.setLore(List.of("Soulbound")))));
    }

    @Test
    void matchesPersistentDataWithAndWithoutValue() {
        NamespacedKey key = NamespacedKey.fromString("nexo:id");
        ItemStack ruby = item(Material.PAPER, meta -> meta.getPersistentDataContainer().set(key, PersistentDataType.STRING, "ruby_sword"));

        assertTrue(filter(null, null, null, null, "nexo:id", null, null).matches(ruby));
        assertTrue(filter(null, null, null, null, "nexo:id", "RUBY_SWORD", null).matches(ruby));
        assertFalse(filter(null, null, null, null, "nexo:id", "emerald_sword", null).matches(ruby));
        assertFalse(filter(null, null, null, null, "oraxen:id", null, null).matches(ruby));
    }

    @Test
    void matchesEnchantments() {
        ItemStack sword = new ItemStack(Material.DIAMOND_SWORD);
        sword.addUnsafeEnchantment(Enchantment.SHARPNESS, 5);

        assertTrue(filter(null, null, null, null, null, null, "minecraft:sharpness").matches(sword));
        assertFalse(filter(null, null, null, null, null, null, "mending").matches(sword));
    }

    @Test
    void describesItsCriteria() {
        assertEquals("type=diamond_sword, name=Excalibur, data=nexo:id=ruby",
                filter(Material.DIAMOND_SWORD, "Excalibur", null, null, "nexo:id", "ruby", null).describe());
        assertTrue(filter(null, " ", null, null, null, "x", null).isEmpty());
    }

    private static ItemFilter filter(Material type, String name, String lore, Integer model, String dataKey, String dataValue, String enchantment) {
        return new ItemFilter(type, name, lore, model, dataKey, dataValue, enchantment);
    }

    private static ItemStack item(Material material, Consumer<ItemMeta> change) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        change.accept(meta);
        item.setItemMeta(meta);
        return item;
    }
}
