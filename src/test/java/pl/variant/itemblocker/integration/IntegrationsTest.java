package pl.variant.itemblocker.integration;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.Test;
import pl.variant.itemblocker.message.Message;
import pl.variant.itemblocker.rule.Action;
import pl.variant.itemblocker.rule.ItemRule;
import pl.variant.itemblocker.rule.Preset;
import pl.variant.itemblocker.rule.RegionScope;
import pl.variant.itemblocker.testing.PluginTest;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class IntegrationsTest extends PluginTest {

    @Test
    void regionPresetOnlyAppliesInsideItsRegions() {
        updateRules(rules -> rules.with(Preset.empty("spawn")
                .withRegions(RegionScope.of(List.of("spawn")))
                .withItem(Material.TNT, ItemRule.blockEverything())));
        ItemStack tnt = new ItemStack(Material.TNT);

        plugin.fakeRegions().setCurrent("arena");
        assertTrue(plugin.enforcement().restrictions().checkItem(player, tnt, Action.PLACE).isEmpty());

        plugin.fakeRegions().setCurrent("arena", "spawn");
        assertTrue(plugin.enforcement().restrictions().checkItem(player, tnt, Action.PLACE).isPresent());
    }

    @Test
    void fillsInPlaceholdersOfOtherPluginsForPlayers() throws IOException {
        Path english = plugin.getDataFolder().toPath().resolve("lang").resolve("en.yml");
        Files.writeString(english, "reloaded: \"Hi %player_name%\"\n", StandardCharsets.UTF_8);
        plugin.reload();

        plugin.messages().send(player, Message.RELOADED);
        plugin.messages().send(server.getConsoleSender(), Message.RELOADED);

        assertEquals(List.of("Hi " + player.getName()), messages());
        assertEquals("Hi %player_name%", server.getConsoleSender().nextMessage());
        assertFalse(plugin.rules().rules().usesRegions());
    }
}
