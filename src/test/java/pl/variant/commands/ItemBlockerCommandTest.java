package pl.variant.commands;

import org.bukkit.Material;
import org.bukkit.command.Command;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import pl.variant.itemBlocker;
import pl.variant.model.WorldScopeMode;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ItemBlockerCommandTest {

    private ServerMock server;
    private itemBlocker plugin;
    private ItemBlockerCommand commandExecutor;
    private Command command;

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
        plugin = MockBukkit.load(itemBlocker.class);
        commandExecutor = new ItemBlockerCommand(plugin);
        command = plugin.getCommand("itemblocker");
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    void blockSuggestionsDoNotSingleOutSmithing() {
        assertNotNull(command);

        List<String> suggestions = commandExecutor.onTabComplete(
                server.getConsoleSender(),
                command,
                "ib",
                new String[]{"block", "TNT", ""}
        );

        assertTrue(suggestions.contains("actions:all"));
        assertTrue(suggestions.contains("actions:crafting"));
        assertFalse(suggestions.contains("actions:smithing"));
    }

    @Test
    void topLevelSuggestionsDoNotExposeLegacyNetheriteCommand() {
        assertNotNull(command);

        List<String> suggestions = commandExecutor.onTabComplete(
                server.getConsoleSender(),
                command,
                "ib",
                new String[]{""}
        );

        assertFalse(suggestions.contains("netherite"));
    }

    @Test
    void blockSuggestionsIncludeDisabledWorldShortcut() {
        assertNotNull(command);

        List<String> suggestions = commandExecutor.onTabComplete(
                server.getConsoleSender(),
                command,
                "ib",
                new String[]{"block", "TNT", "worlds:d"}
        );

        assertTrue(suggestions.contains("worlds:disabled"));
    }

    @Test
    void blockCommandTreatsDisabledWorldsAsGlobalScope() {
        assertNotNull(command);

        boolean handled = commandExecutor.onCommand(
                server.getConsoleSender(),
                command,
                "ib",
                new String[]{"block", "TNT", "worlds:disabled"}
        );

        assertTrue(handled);

        var rule = plugin.getBlockedItemsManager().getGlobalItem(Material.TNT).orElseThrow();
        assertEquals(WorldScopeMode.DISABLED, rule.getWorldScopeMode());
        assertTrue(rule.getWorlds().isEmpty());
    }
}
