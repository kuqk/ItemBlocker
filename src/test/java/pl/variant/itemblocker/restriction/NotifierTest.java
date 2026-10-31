package pl.variant.itemblocker.restriction;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.Test;
import pl.variant.itemblocker.rule.Action;
import pl.variant.itemblocker.testing.PluginTest;

import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class NotifierTest extends PluginTest {

    @Test
    void appendsPresetReason() {
        block(Material.TNT, Action.PLACE);
        updateDefault(preset -> preset.withReason("Raids are disabled"));

        notifyPlace(new Notifier(plugin.messages(), () -> Duration.ZERO));

        assertEquals(List.of("ItemBlocker » You cannot place TNT. (Raids are disabled)"), messages());
    }

    @Test
    void omitsEmptyReason() {
        block(Material.TNT, Action.PLACE);

        notifyPlace(new Notifier(plugin.messages(), () -> Duration.ZERO));

        assertEquals(List.of("ItemBlocker » You cannot place TNT."), messages());
    }

    @Test
    void sendsAtMostOneMessagePerCooldown() {
        block(Material.TNT, Action.PLACE);
        Notifier notifier = new Notifier(plugin.messages(), () -> Duration.ofHours(1));

        notifyPlace(notifier);
        notifyPlace(notifier);

        assertEquals(1, messages().size());
    }

    private void notifyPlace(Notifier notifier) {
        Denial denial = plugin.enforcement().restrictions()
                .checkItem(player, new ItemStack(Material.TNT), Action.PLACE)
                .orElseThrow();
        notifier.notify(player, denial);
    }
}
