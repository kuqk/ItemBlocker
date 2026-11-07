package pl.variant.itemblocker.command;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.Test;
import pl.variant.itemblocker.rule.Action;
import pl.variant.itemblocker.rule.ItemFilter;
import pl.variant.itemblocker.rule.ItemPattern;
import pl.variant.itemblocker.rule.ItemRule;
import pl.variant.itemblocker.rule.WorldScope;
import pl.variant.itemblocker.testing.PluginTest;
import pl.variant.itemblocker.testing.Text;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Regression cases discovered during the project audit. */
class CommandAuditTest extends PluginTest {

    @Test
    void blockBypassNoneRevokesAnExistingRuleExemption() {
        console("ib block tnt actions:place bypass:vip.tnt");
        assertEquals("vip.tnt", plugin.rules().rules().defaultPreset().item(Material.TNT)
                .orElseThrow().bypassPermission());

        console("ib block tnt actions:place bypass:none");

        assertNull(plugin.rules().rules().defaultPreset().item(Material.TNT)
                .orElseThrow().bypassPermission(), "The documented bypass:none option must revoke the permission");
    }

    @Test
    void addingActionsPreservesAnOmittedBypassAndAllowsExplicitReplacement() {
        console("ib block tnt actions:place bypass:vip.tnt");

        console("ib block tnt actions:crafting");

        ItemRule merged = plugin.rules().rules().defaultPreset().item(Material.TNT).orElseThrow();
        assertEquals(Set.of(Action.PLACE, Action.CRAFTING), merged.actions());
        assertEquals("vip.tnt", merged.bypassPermission());

        console("ib block tnt actions:drop bypass:staff.tnt");

        assertEquals("staff.tnt", plugin.rules().rules().defaultPreset().item(Material.TNT)
                .orElseThrow().bypassPermission());
    }

    @Test
    void handInsideAQuotedFilterNameIsTextRatherThanTheHandFlag() {
        List<String> output = console("ib filter add kings name:\"Sword hand of Kings\" actions:use");

        assertTrue(plugin.rules().rules().defaultPreset().filter("kings").isPresent(), output.toString());
        assertEquals("Sword hand of Kings", plugin.rules().rules().defaultPreset().filter("kings")
                .orElseThrow().filter().name());
    }

    @Test
    void standaloneHandAfterQuotedValuesStillCopiesTheHeldType() {
        player.setOp(true);
        player.getInventory().setItemInMainHand(new ItemStack(Material.STONE));

        server.dispatchCommand(player,
                "ib filter add kings name:\"Sword hand of Kings\" lore:\"hand made\" HAND actions:use");

        ItemFilter filter = plugin.rules().rules().defaultPreset().filter("kings")
                .orElseThrow(() -> new AssertionError(messages().toString())).filter();
        assertEquals(Material.STONE, filter.type());
        assertEquals("Sword hand of Kings", filter.name());
        assertEquals("hand made", filter.lore());
    }

    @Test
    void showReportsARestrictionIntroducedByAnItemGroup() {
        updateDefault(preset -> preset.withGroup(ItemPattern.parse("*_sword").orElseThrow(),
                ItemRule.blockEverything()));

        List<String> output = console("ib show diamond_sword");

        assertFalse(output.stream().anyMatch(line -> line.contains("No preset blocks")), output.toString());
        assertTrue(output.stream().anyMatch(line -> line.contains("default")), output.toString());
    }

    @Test
    void showKeepsGroupRulesWithDifferentWorldsSeparate() {
        updateDefault(preset -> preset
                .withGroup(ItemPattern.parse("*_sword").orElseThrow(),
                        new ItemRule(Set.of(Action.USE), WorldScope.of(List.of("arena"))))
                .withGroup(ItemPattern.parse("diamond_*").orElseThrow(),
                        new ItemRule(Set.of(Action.DROP), WorldScope.of(List.of("spawn")))));

        List<String> output = console("ib show diamond_sword");

        assertTrue(output.stream().anyMatch(line -> line.contains("use") && line.contains("arena")), output.toString());
        assertTrue(output.stream().anyMatch(line -> line.contains("drop") && line.contains("spawn")), output.toString());
        assertFalse(output.stream().anyMatch(line -> line.contains("No preset blocks")), output.toString());
    }

    @Test
    void showDoesNotReportGroupRestrictionsForAnAllowedItem() {
        updateDefault(preset -> preset.withGroup(ItemPattern.parse("*_sword").orElseThrow(),
                ItemRule.blockEverything()).withAllowed(Material.DIAMOND_SWORD));

        List<String> output = console("ib show diamond_sword");

        assertTrue(output.stream().anyMatch(line -> line.contains("No preset blocks")), output.toString());
    }

    private List<String> console(String command) {
        while (server.getConsoleSender().nextMessage() != null) {
            // Clear messages from earlier steps.
        }
        server.dispatchCommand(server.getConsoleSender(), command);
        List<String> output = new ArrayList<>();
        for (String line = server.getConsoleSender().nextMessage(); line != null;
             line = server.getConsoleSender().nextMessage()) {
            output.add(Text.plain(line));
        }
        return output;
    }
}
