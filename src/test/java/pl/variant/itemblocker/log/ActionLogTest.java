package pl.variant.itemblocker.log;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.Test;
import pl.variant.itemblocker.restriction.Denial;
import pl.variant.itemblocker.rule.Action;
import pl.variant.itemblocker.testing.PluginTest;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ActionLogTest extends PluginTest {

    @Test
    void recordsBlockedActionsOnceAndWritesThemToTheDailyFile() throws IOException {
        block(Material.TNT, Action.PLACE);
        Denial denial = plugin.enforcement().restrictions().checkItem(player, new ItemStack(Material.TNT), Action.PLACE).orElseThrow();

        plugin.enforcement().notify(player, denial);
        plugin.enforcement().notify(player, denial);

        List<LogEntry> recent = plugin.actionLog().recent(null, 10);
        assertEquals(1, recent.size());
        assertEquals("place", recent.get(0).action());
        assertEquals("TNT", recent.get(0).subject());
        assertEquals("default", recent.get(0).preset());
        assertTrue(plugin.actionLog().recent("someone else", 10).isEmpty());

        plugin.actionLog().close();
        try (var files = Files.list(plugin.getDataFolder().toPath().resolve("logs"))) {
            Path file = files.findFirst().orElseThrow();
            String line = Files.readString(file, StandardCharsets.UTF_8).trim();
            assertTrue(line.endsWith(" | " + player.getName() + " | place | TNT | " + player.getWorld().getName() + " | default"), line);
        }
    }

    @Test
    void discordAlertsCannotMentionAnyone() {
        LogEntry entry = new LogEntry(Instant.EPOCH, "Steve", "use", "@everyone \"*bold*\"", "world", "pvp");

        assertEquals("{\"content\":\"**Steve** - use **@everyone \\\"\\\\*bold\\\\*\\\"** in `world` (preset `pvp`)\","
                + "\"allowed_mentions\":{\"parse\":[]}}", DiscordWebhook.payload(entry));
    }
}
