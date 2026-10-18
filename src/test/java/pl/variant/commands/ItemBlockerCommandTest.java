package pl.variant.commands;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.command.MessageTarget;
import org.mockbukkit.mockbukkit.entity.PlayerMock;
import pl.variant.itemBlocker;
import pl.variant.model.WorldScopeMode;

import java.util.ArrayList;
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

    @Test
    void blockEnchantmentInMissingPresetIsNotReportedAsSaved() {
        List<String> messages = run(server.getConsoleSender(), "block", "enchantment", "efficiency", "in:missing");

        assertEquals(List.of(message("preset-not-found", "{preset}", "missing")), messages);
        assertFalse(plugin.getBlockedItemsManager().getGlobalEnchantments().contains("efficiency"));
    }

    @Test
    void blockPotionInMissingPresetIsNotReportedAsSaved() {
        List<String> messages = run(server.getConsoleSender(), "block", "potion", "haste", "in:missing");

        assertEquals(List.of(message("preset-not-found", "{preset}", "missing")), messages);
        assertFalse(plugin.getBlockedItemsManager().getGlobalPotions().contains("haste"));
    }

    @Test
    void blockEnchantmentInExistingPresetIsSaved() {
        run(server.getConsoleSender(), "preset", "create", "pvp");
        run(server.getConsoleSender(), "block", "enchantment", "sharpness", "level:5", "in:pvp");

        var preset = plugin.getPresetManager().getPreset("pvp").orElseThrow();
        assertEquals(5, preset.getEnchantmentRules().getThreshold("sharpness").orElseThrow());
    }

    @Test
    void presetCreateRejectsReservedAndUnsafeNames() {
        int presetCount = plugin.getPresetManager().getPresetCount();
        for (String name : List.of("list", "set", "global", "all", "[broken]", "#comment", "a:b")) {
            List<String> messages = run(server.getConsoleSender(), "preset", "create", name);
            assertEquals(List.of(message("preset-invalid-name", "{preset}", name)), messages, name);
        }

        assertEquals(presetCount, plugin.getPresetManager().getPresetCount());
    }

    @Test
    void presetCreateAcceptsSimpleNames() {
        run(server.getConsoleSender(), "preset", "create", "Event-1");

        assertTrue(plugin.getPresetManager().getPreset("event-1").isPresent());
    }

    @Test
    void presetNamedLikeLegacySubcommandStillWorks() throws Exception {
        java.nio.file.Files.writeString(
                plugin.getDataFolder().toPath().resolve("presets.yml"),
                """
                presets:
                  list:
                    items:
                      TNT:
                        actions: all
                        worlds: all
                """,
                java.nio.charset.StandardCharsets.UTF_8
        );
        plugin.getPresetManager().loadPresets();

        run(server.getConsoleSender(), "block", "DIAMOND", "in:list");
        assertTrue(plugin.getPresetManager().getPreset("list").orElseThrow().references(Material.DIAMOND));

        run(server.getConsoleSender(), "preset", "delete", "list");
        assertTrue(plugin.getPresetManager().getPreset("list").isEmpty());
    }

    @Test
    void legacyAndNonItemMaterialsAreRejected() {
        int itemCount = plugin.getBlockedItemsManager().getBlockedItemsCount();
        List<String> legacy = run(server.getConsoleSender(), "block", "LEGACY_STONE");
        List<String> water = run(server.getConsoleSender(), "block", "WATER");

        assertEquals(List.of(message("invalid-item", "{item}", "LEGACY_STONE")), legacy);
        assertEquals(List.of(message("invalid-item", "{item}", "WATER")), water);
        assertEquals(itemCount, plugin.getBlockedItemsManager().getBlockedItemsCount());
    }

    @Test
    void namespacedMaterialIsAccepted() {
        run(server.getConsoleSender(), "block", "minecraft:ender_pearl", "actions:place");

        var rule = plugin.getBlockedItemsManager().getGlobalItem(Material.ENDER_PEARL).orElseThrow();
        assertEquals(java.util.EnumSet.of(pl.variant.model.BlockAction.PLACE), rule.getActions());
    }

    @Test
    void materialSuggestionsSkipLegacyMaterials() {
        List<String> suggestions = commandExecutor.onTabComplete(
                server.getConsoleSender(),
                command,
                "ib",
                new String[]{"block", "LEGACY_"}
        );

        assertTrue(suggestions.isEmpty(), suggestions.toString());
    }

    @Test
    void listPermissionAllowsReadOnlyListing() {
        PlayerMock player = server.addPlayer();
        player.setOp(false);
        player.addAttachment(plugin, "itemblocker.list", true);
        String noPermission = message("no-permission");

        assertFalse(run(player, "list", "presets").contains(noPermission));
        assertFalse(run(player, "list", "enchantments").contains(noPermission));
        assertFalse(run(player, "list", "potions").contains(noPermission));
        assertEquals(List.of(noPermission), run(player, "block", "enchantment", "sharpness"));
    }

    @Test
    void infoPermissionAllowsShowingThresholdRules() {
        PlayerMock player = server.addPlayer();
        player.setOp(false);
        player.addAttachment(plugin, "itemblocker.info", true);

        assertFalse(run(player, "show", "enchantment", "sharpness").contains(message("no-permission")));
    }

    private List<String> run(CommandSender sender, String... args) {
        MessageTarget target = (MessageTarget) sender;
        while (target.nextMessage() != null) {
            // Drop messages from earlier steps.
        }

        assertTrue(commandExecutor.onCommand(sender, command, "ib", args));

        List<String> messages = new ArrayList<>();
        for (String next = target.nextMessage(); next != null; next = target.nextMessage()) {
            messages.add(ChatColor.stripColor(next));
        }
        return messages;
    }

    private String message(String path) {
        return ChatColor.stripColor(plugin.getMessageManager().getMessage(path));
    }

    private String message(String path, String placeholder, String value) {
        return ChatColor.stripColor(plugin.getMessageManager().getMessage(path, placeholder, value));
    }
}
