package pl.variant.itemblocker.rule;

import org.bukkit.Material;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ItemTypesTest {

    @BeforeEach
    void setUp() {
        MockBukkit.mock();
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    void parsesKeysInAnyCaseAndWithNamespace() {
        assertEquals(Optional.of(Material.TNT), ItemTypes.parse("tnt"));
        assertEquals(Optional.of(Material.DIAMOND_SWORD), ItemTypes.parse("DIAMOND_SWORD"));
        assertEquals(Optional.of(Material.ENDER_PEARL), ItemTypes.parse(" minecraft:ender_pearl "));
    }

    @Test
    void rejectsLegacyAndNonItemMaterials() {
        assertTrue(ItemTypes.parse("LEGACY_STONE").isEmpty());
        assertTrue(ItemTypes.parse("water").isEmpty());
        assertTrue(ItemTypes.parse("air").isEmpty());
        assertTrue(ItemTypes.parse("not_an_item").isEmpty());
        assertTrue(ItemTypes.parse(" ").isEmpty());
    }

    @Test
    void keysListOnlyBlockableItems() {
        assertTrue(ItemTypes.keys().contains("diamond_sword"));
        assertFalse(ItemTypes.keys().stream().anyMatch(key -> key.startsWith("legacy_")));
        assertFalse(ItemTypes.keys().contains("water"));
    }
}
