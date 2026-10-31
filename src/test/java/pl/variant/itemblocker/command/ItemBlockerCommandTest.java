package pl.variant.itemblocker.command;

import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.command.MessageTarget;
import pl.variant.itemblocker.rule.Action;
import pl.variant.itemblocker.rule.ItemRule;
import pl.variant.itemblocker.rule.Preset;
import pl.variant.itemblocker.rule.WorldScope;
import pl.variant.itemblocker.storage.RuleFile;
import pl.variant.itemblocker.testing.PluginTest;
import pl.variant.itemblocker.testing.Text;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ItemBlockerCommandTest extends PluginTest {

    @Test
    void blockCreatesRuleWithGivenActions() {
        assertEquals(List.of("ItemBlocker » Blocked TNT in default: place in all worlds."), console("ib block tnt actions:place"));

        assertEquals(new ItemRule(EnumSet.of(Action.PLACE), WorldScope.ALL), defaults().item(Material.TNT).orElseThrow());
    }

    @Test
    void blockWithoutOptionsBlocksEverything() {
        console("ib block minecraft:tnt");

        assertTrue(defaults().item(Material.TNT).orElseThrow().blocksEveryAction());
    }

    @Test
    void blockAddsToExistingRule() {
        console("ib block tnt actions:place worlds:world");

        assertEquals(List.of("ItemBlocker » Updated TNT in default: use, place in nether, world."),
                console("ib block tnt actions:use worlds:nether"));
        assertEquals(new ItemRule(EnumSet.of(Action.PLACE, Action.USE), WorldScope.of(List.of("world", "nether"))),
                defaults().item(Material.TNT).orElseThrow());
    }

    @Test
    void blockReportsInvalidInputWithoutChangingRules() {
        assertEquals(List.of("ItemBlocker » Unknown item: legacy_stone."), console("ib block legacy_stone"));
        assertEquals(List.of("ItemBlocker » Unknown item: water."), console("ib block water"));
        assertEquals(List.of("ItemBlocker » Preset missing does not exist."), console("ib block tnt in:missing"));
        assertEquals(List.of("ItemBlocker » Unknown option: fly."), console("ib block tnt fly"));
        assertEquals(List.of("ItemBlocker » Unknown action: fly. Available: crafting, pickup, drop, use, place, armor, inventory, hopper, smithing."),
                console("ib block tnt actions:fly"));

        assertTrue(defaults().items().isEmpty());
    }

    @Test
    void handUsesHeldItem() {
        player.setOp(true);
        player.getInventory().setItemInMainHand(new ItemStack(Material.DIAMOND));

        run(player, "ib block hand actions:drop");

        assertEquals(EnumSet.of(Action.DROP), defaults().item(Material.DIAMOND).orElseThrow().actions());
        assertEquals(List.of("ItemBlocker » Only players can do that."), console("ib block hand"));
    }

    @Test
    void editReplacesOnlyGivenParts() {
        console("ib block tnt actions:place worlds:world");

        assertEquals(List.of("ItemBlocker » Updated TNT in default: crafting, use in world."), console("ib edit tnt actions:crafting,use"));
        assertEquals(new ItemRule(EnumSet.of(Action.CRAFTING, Action.USE), WorldScope.of(List.of("world"))),
                defaults().item(Material.TNT).orElseThrow());
        assertEquals(List.of("ItemBlocker » Usage: /ib edit <item|hand> [actions:...] [worlds:...] [in:preset]"), console("ib edit tnt"));
        assertEquals(List.of("ItemBlocker » Stone is not blocked in default."), console("ib edit stone actions:use"));
    }

    @Test
    void unblockRemovesRule() {
        console("ib block tnt");

        assertEquals(List.of("ItemBlocker » Unblocked TNT in default."), console("ib unblock tnt"));
        assertEquals(List.of("ItemBlocker » TNT is not blocked in default."), console("ib unblock tnt"));
        assertTrue(defaults().items().isEmpty());
    }

    @Test
    void blocksAndUnblocksEnchantmentsInPreset() {
        console("ib preset create pvp");

        assertEquals(List.of("ItemBlocker » Blocked Sharpness from level 4 in pvp."), console("ib block enchantment Sharpness level:4 in:pvp"));
        assertEquals(Map.of("sharpness", 4), preset("pvp").enchantments().minimumLevels());
        assertEquals(List.of("ItemBlocker » Unblocked Sharpness in pvp."), console("ib unblock enchantment minecraft:sharpness in:pvp"));
        assertTrue(preset("pvp").enchantments().isEmpty());
    }

    @Test
    void reportsInvalidEnchantmentInput() {
        assertEquals(List.of("ItemBlocker » Unknown enchantment: sharpest."), console("ib block enchantment sharpest"));
        assertEquals(List.of("ItemBlocker » The level must be a whole number from 1, got 0."), console("ib block enchantment sharpness level:0"));
        assertEquals(List.of("ItemBlocker » Sharpness is not blocked in default."), console("ib unblock enchantment sharpness"));
    }

    @Test
    void blocksPotionEffects() {
        assertEquals(List.of("ItemBlocker » Blocked Strength from level 2 in default."), console("ib block potion strength level:2"));
        assertEquals(Map.of("strength", 2), defaults().effects().minimumLevels());
        assertEquals(List.of("ItemBlocker » Unknown potion effect: flying."), console("ib block potion flying"));
    }

    @Test
    void showListsEveryPresetBlockingAnItem() {
        console("ib preset create pvp");
        console("ib block tnt actions:place");
        console("ib block tnt in:pvp worlds:arena");

        assertEquals(List.of(
                "TNT - rules",
                " • default: place in all worlds",
                " • pvp: all actions in arena"
        ), console("ib show tnt"));
        assertEquals(List.of("Stone - rules", " No preset blocks Stone."), console("ib show stone"));
    }

    @Test
    void listShowsOverviewAndDetails() {
        console("ib block tnt");
        console("ib block enchantment mending");

        assertEquals(List.of(
                "ItemBlocker rules",
                " • default: 1 items, 1 enchantments, 0 potion effects (all worlds)"
        ), console("ib list"));
        assertEquals(List.of("Blocked items in default", " • TNT: all actions in all worlds"), console("ib list items in:default"));
        assertEquals(List.of("Blocked potion effects in default", " Nothing here."), console("ib list potions"));
    }

    @Test
    void managesPresets() {
        assertEquals(List.of("ItemBlocker » Created preset event."), console("ib preset create Event"));
        assertEquals(List.of("ItemBlocker » Preset event already exists."), console("ib preset create event"));
        assertEquals(List.of("ItemBlocker » Invalid preset name [x]. Use 1-32 characters: a-z, 0-9, _ or -."), console("ib preset create [x]"));

        console("ib preset edit event description Weekend event");
        console("ib preset edit event reason Event rules");
        assertEquals(List.of("ItemBlocker » Preset event now applies in arena, lobby."), console("ib preset edit event worlds arena lobby"));

        Preset event = preset("event");
        assertEquals("Weekend event", event.description());
        assertEquals("Event rules", event.reason());
        assertEquals(Set.of("arena", "lobby"), event.scope().worlds());

        assertEquals(List.of("ItemBlocker » The default preset cannot be deleted."), console("ib preset delete default"));
        assertEquals(List.of("ItemBlocker » Deleted preset event."), console("ib preset delete event"));
        assertTrue(plugin.rules().rules().preset("event").isEmpty());
    }

    @Test
    void reloadKeepsRulesWhenFileIsBroken() throws IOException {
        console("ib block tnt");
        Files.writeString(plugin.getDataFolder().toPath().resolve(RuleFile.FILE_NAME), "presets: [unclosed", StandardCharsets.UTF_8);

        List<String> messages = console("ib reload");

        assertEquals(1, messages.size());
        assertTrue(messages.getFirst().startsWith("ItemBlocker » Could not reload:"), messages.getFirst());
        assertTrue(defaults().item(Material.TNT).isPresent());
        assertTrue(console("ib block stone").getFirst().startsWith("ItemBlocker » Could not save the rules:"));
    }

    @Test
    void checksPermissionsPerAction() {
        player.addAttachment(plugin, "itemblocker.use", true);
        player.addAttachment(plugin, "itemblocker.list", true);

        assertEquals(List.of("ItemBlocker » You do not have permission to do that."), run(player, "ib block tnt"));
        assertEquals("ItemBlocker rules", run(player, "ib list").getFirst());
        assertEquals(List.of("help", "list", "preset"), complete(player, "ib", ""));
    }

    @Test
    void helpListsOnlyAvailableCommands() {
        player.addAttachment(plugin, "itemblocker.use", true);
        player.addAttachment(plugin, "itemblocker.info", true);

        List<String> help = run(player, "ib help");

        assertTrue(help.stream().anyMatch(line -> line.startsWith("/ib show")), help.toString());
        assertFalse(help.stream().anyMatch(line -> line.startsWith("/ib block")), help.toString());
    }

    @Test
    void unknownSubcommandPointsToHelp() {
        assertEquals(List.of("ItemBlocker » Unknown command. Use /ib help."), console("ib fly"));
    }

    @Test
    void limitsPresetsToRegions() {
        console("ib preset create spawn");

        assertEquals(List.of("ItemBlocker » Regions of spawn: arena, spawn."), console("ib preset edit spawn regions Spawn arena"));
        assertEquals(Set.of("spawn", "arena"), preset("spawn").regions().regions());
        assertTrue(console("ib preset show spawn").contains(" Regions: arena, spawn"));
        assertEquals(List.of("ItemBlocker » Regions of spawn: all regions."), console("ib preset edit spawn regions all"));
        assertTrue(preset("spawn").regions().isAll());
        assertEquals(List.of("spawn"), complete(server.getConsoleSender(), "ib", "preset", "edit", "spawn", "regions", "sp"));
    }

    @Test
    void completesSubcommandsSubjectsAndOptions() {
        CommandSender console = server.getConsoleSender();
        console("ib preset create pvp");

        assertEquals(List.of("block"), complete(console, "ib", "bl"));
        assertEquals(List.of("enchantment", "enchanted_book", "enchanted_golden_apple", "enchanting_table"),
                complete(console, "ib", "block", "ench"));
        assertTrue(complete(console, "ib", "block", "diamond_s").contains("diamond_sword"));
        assertEquals(List.of("sharpness"), complete(console, "ib", "block", "enchantment", "sharp"));
        assertEquals(List.of("actions:", "worlds:", "in:"), complete(console, "ib", "block", "tnt", ""));
        assertEquals(List.of("actions:place"), complete(console, "ib", "block", "tnt", "actions:pl"));
        assertEquals(List.of("actions:place,use"), complete(console, "ib", "block", "tnt", "actions:place,us"));
        assertEquals(List.of("in:pvp"), complete(console, "ib", "block", "tnt", "in:p"));
        assertEquals(List.of("worlds:"), complete(console, "ib", "block", "tnt", "actions:all", "in:pvp", ""));
    }

    private Preset defaults() {
        return plugin.rules().rules().defaultPreset();
    }

    private Preset preset(String name) {
        return plugin.rules().rules().preset(name).orElseThrow();
    }

    private List<String> console(String commandLine) {
        return run(server.getConsoleSender(), commandLine);
    }

    /**
     * Runs a command line as the sender and returns the messages it received, without colors.
     */
    private List<String> run(CommandSender sender, String commandLine) {
        MessageTarget target = (MessageTarget) sender;
        while (target.nextMessage() != null) {
            // Drop messages from earlier steps.
        }

        server.dispatchCommand(sender, commandLine);

        List<String> messages = new ArrayList<>();
        for (String message = target.nextMessage(); message != null; message = target.nextMessage()) {
            messages.add(Text.plain(message));
        }
        return messages;
    }

    private List<String> complete(CommandSender sender, String label, String... args) {
        ItemBlockerCommand command = new ItemBlockerCommand(plugin);
        return command.onTabComplete(sender, plugin.getCommand("itemblocker"), label, args);
    }
}
