package pl.variant.itemblocker.restriction;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.junit.jupiter.api.Test;
import pl.variant.itemblocker.message.Message;
import pl.variant.itemblocker.message.Placeholder;
import pl.variant.itemblocker.rule.Action;
import pl.variant.itemblocker.testing.Text;
import pl.variant.itemblocker.testing.PluginTest;

import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NotifierTest extends PluginTest {

    @Test
    void appendsPresetReason() {
        block(Material.TNT, Action.PLACE);
        updateDefault(preset -> preset.withReason("Raids are disabled"));

        notifyPlace(new Notifier(plugin.messages(), () -> Duration.ZERO));

        assertEquals(List.of("[ItemBlocker] You cannot place TNT. (Raids are disabled)"), messages());
    }

    @Test
    void omitsEmptyReason() {
        block(Material.TNT, Action.PLACE);

        notifyPlace(new Notifier(plugin.messages(), () -> Duration.ZERO));

        assertEquals(List.of("[ItemBlocker] You cannot place TNT."), messages());
    }

    @Test
    void sendsAtMostOneMessagePerCooldown() {
        block(Material.TNT, Action.PLACE);
        Notifier notifier = new Notifier(plugin.messages(), () -> Duration.ofHours(1));

        notifyPlace(notifier);
        notifyPlace(notifier);

        assertEquals(1, messages().size());
    }

    @Test
    void playersGetNamesTheirGameTranslatesAndTheConsoleGetsEnglish() {
        Placeholder item = Placeholder.item("item", Material.TNT);

        plugin.messages().send(player, Message.RESTRICTION_PLACE, item, Placeholder.of("reason", ""));
        plugin.messages().send(server.getConsoleSender(), Message.RESTRICTION_PLACE, item, Placeholder.of("reason", ""));

        assertTrue(player.nextMessage().contains("block.minecraft.tnt"));
        assertEquals("[ItemBlocker] You cannot place TNT.", Text.plain(server.getConsoleSender().nextMessage()));
    }

    @Test
    void customNamesAreNotTranslated() {
        ItemStack sword = new ItemStack(Material.DIAMOND_SWORD);
        ItemMeta meta = sword.getItemMeta();
        meta.setDisplayName("Excalibur");
        sword.setItemMeta(meta);

        plugin.messages().send(player, Message.RESTRICTION_PLACE, Placeholder.item("item", sword), Placeholder.of("reason", ""));

        assertEquals("[ItemBlocker] You cannot place Excalibur.", Text.plain(player.nextMessage()));
    }

    private void notifyPlace(Notifier notifier) {
        Denial denial = plugin.enforcement().restrictions()
                .checkItem(player, new ItemStack(Material.TNT), Action.PLACE)
                .orElseThrow();
        notifier.notify(player, denial);
    }
}
