package pl.variant.itemblocker.scan;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import pl.variant.itemblocker.testing.PluginTest;
import pl.variant.itemblocker.rule.Action;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Expected-behavior regressions: failures identify defects, not accepted behavior. */
class ConfiscatedItemsAuditTest extends PluginTest {

    @TempDir
    Path folder;

    private static final Logger LOGGER = Logger.getLogger(ConfiscatedItemsAuditTest.class.getName());

    @Test
    void malformedStorageMustNotBeOverwrittenByANewConfiscation() throws Exception {
        Path file = folder.resolve("confiscated.yml");
        String broken = "previousOwner: [\n";
        Files.writeString(file, broken);
        ConfiscatedItems storage = new ConfiscatedItems(file, LOGGER);

        try {
            storage.add("newOwner", List.of(new ItemStack(Material.TNT)));
        } catch (RuntimeException expected) {
            // Rejecting the operation is safe provided the original data survives.
        }

        assertEquals(broken, Files.readString(file), "An unreadable confiscation file must be preserved");
    }

    @Test
    void failedSaveMustNotReturnItemsWhichRemainAvailableOnDisk() throws Exception {
        Path file = folder.resolve("confiscated.yml");
        List<ItemStack> items = List.of(new ItemStack(Material.DIAMOND, 8));
        ConfiscatedItems storage = new ConfiscatedItems(file, LOGGER);
        storage.add("Steve", items);

        // Use a directory at the file path for a deterministic write failure on Windows and Linux.
        Path backup = folder.resolve("confiscated.backup.yml");
        Files.move(file, backup);
        Files.createDirectory(file);
        Files.writeString(file.resolve("blocker"), "Prevent replacement of this directory on every platform");
        List<ItemStack> returned;
        try {
            returned = storage.take("Steve");
        } catch (RuntimeException expected) {
            return;
        }
        assertTrue(returned.isEmpty(),
                "Returning items after an uncommitted withdrawal allows duplicates after restart");
    }

    @Test
    void failedConfiscationSaveMustLeaveThePlayersItemsAvailable() throws Exception {
        Path blockedParent = folder.resolve("not-a-directory");
        Files.writeString(blockedParent, "This regular file prevents creating the storage directory");
        ConfiscatedItems storage = new ConfiscatedItems(blockedParent.resolve("confiscated.yml"), LOGGER);
        Scanner scanner = new Scanner(plugin.enforcement().restrictions(),
                (target, task) -> task.run(), (chunk, task) -> task.run(), storage);
        block(Material.TNT, Action.PICKUP);
        ItemStack original = new ItemStack(Material.TNT, 5);
        player.getInventory().setItem(0, original.clone());

        try {
            scanner.scanPlayers(List.of(player), Set.of(Action.PICKUP), ScanMode.CONFISCATE, ignored -> {});
        } catch (RuntimeException expected) {
            // An aborted confiscation must preserve the player's source items.
        }

        assertEquals(original, player.getInventory().getItem(0),
                "A failed durable save must not remove the only recoverable copy from its owner");
    }

    @Test
    void dottedWorldNameMustSurviveAStorageRoundTrip() {
        Path file = folder.resolve("confiscated.yml");
        String owner = "survival.2026 10 64 20";
        ConfiscatedItems storage = new ConfiscatedItems(file, LOGGER);
        storage.add(owner, List.of(new ItemStack(Material.DIAMOND, 8)));

        ConfiscatedItems reloaded = new ConfiscatedItems(file, LOGGER);

        assertEquals(Map.of(owner, 1), reloaded.owners());
        assertEquals(List.of(new ItemStack(Material.DIAMOND, 8)), reloaded.take(owner));
    }
}
