package pl.variant.itemblocker.gui;

import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType.SlotType;
import org.junit.jupiter.api.Test;
import pl.variant.itemblocker.testing.InventoryViews;
import pl.variant.itemblocker.testing.PluginTest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Regression cases discovered during the project audit. */
class MenuAuditTest extends PluginTest {

    @Test
    void pressingAHotbarKeyDoesNotDeleteAnEnchantmentRestriction() {
        blockEnchantment("sharpness", 5);
        LevelsMenu menu = new LevelsMenu(plugin.gui(), "default", LevelsMenu.Kind.ENCHANTMENTS, 0);
        menu.open(player);
        int slot = menu.entries(plugin.rules().rules().defaultPreset()).indexOf("sharpness");
        assertTrue(slot >= 0 && slot < ListMenu.PAGE_SIZE);

        InventoryClickEvent event = new InventoryClickEvent(
                InventoryViews.container(player, menu.getInventory()), SlotType.CONTAINER, slot,
                ClickType.NUMBER_KEY, InventoryAction.HOTBAR_SWAP, 0);
        server.getPluginManager().callEvent(event);

        assertTrue(event.isCancelled());
        assertEquals(5, plugin.rules().rules().defaultPreset().enchantments()
                .minimumLevel("sharpness").orElse(0),
                "Only the documented removal gesture should remove an enchantment restriction");
    }
}
