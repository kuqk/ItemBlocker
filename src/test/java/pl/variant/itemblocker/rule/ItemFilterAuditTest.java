package pl.variant.itemblocker.rule;

import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;

import static org.junit.jupiter.api.Assertions.assertTrue;

/** Opt-in regression cases discovered during the project audit. */
class ItemFilterAuditTest {

    @BeforeEach
    void setUp() {
        MockBukkit.mock();
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    void enchantmentCriterionMatchesStoredBookEnchantments() {
        ItemStack book = new ItemStack(Material.ENCHANTED_BOOK);
        EnchantmentStorageMeta meta = (EnchantmentStorageMeta) book.getItemMeta();
        meta.addStoredEnchant(Enchantment.SHARPNESS, 5, true);
        book.setItemMeta(meta);
        ItemFilter filter = new ItemFilter(Material.ENCHANTED_BOOK, null, null, null,
                null, null, "sharpness");

        assertTrue(filter.matches(book), "A Sharpness enchanted book should match the Sharpness criterion");
    }
}
