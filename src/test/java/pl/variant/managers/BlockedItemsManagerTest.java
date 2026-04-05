package pl.variant.managers;

import org.bukkit.Material;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import pl.variant.itemBlocker;
import pl.variant.model.BlockAction;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BlockedItemsManagerTest {

    private ServerMock server;
    private itemBlocker plugin;

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
        plugin = MockBukkit.load(itemBlocker.class);
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    void loadsLegacyItemListFromDefaultSection() throws IOException {
        Files.writeString(
                plugin.getDataFolder().toPath().resolve("blocked-items.yml"),
                """
                default:
                  items:
                    ENDER_PEARL:
                      - use
                      - drop
                """,
                StandardCharsets.UTF_8
        );

        plugin.getBlockedItemsManager().loadBlockedItems();

        var rule = plugin.getBlockedItemsManager().getGlobalItem(Material.ENDER_PEARL).orElseThrow();
        assertTrue(rule.getActions().contains(BlockAction.USE));
        assertTrue(rule.getActions().contains(BlockAction.DROP));
        assertFalse(rule.getActions().contains(BlockAction.PLACE));
    }
}
