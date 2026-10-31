package pl.variant.itemblocker.message;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;

import static org.junit.jupiter.api.Assertions.assertEquals;

class NamesTest {

    @BeforeEach
    void setUp() {
        MockBukkit.mock();
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    void turnsItemTypesIntoReadableNames() {
        assertEquals("Diamond Chestplate", Names.of(Material.DIAMOND_CHESTPLATE));
        assertEquals("Heart of the Sea", Names.of(Material.HEART_OF_THE_SEA));
        assertEquals("TNT Minecart", Names.of(Material.TNT_MINECART));
    }

    @Test
    void leavesOutNamespaces() {
        assertEquals("Fire Aspect", Names.ofKey("minecraft:fire_aspect"));
        assertEquals("Frost Bite", Names.ofKey("custom:frost_bite"));
        assertEquals("Strength", Names.ofKey("STRENGTH"));
    }

    @Test
    void prefersTheCustomNameWithoutColors() {
        ItemStack item = new ItemStack(Material.DIAMOND_SWORD);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.GOLD + "Excalibur");
        item.setItemMeta(meta);

        assertEquals("Excalibur", Names.of(item));
        assertEquals("Diamond Sword", Names.of(new ItemStack(Material.DIAMOND_SWORD)));
    }
}
