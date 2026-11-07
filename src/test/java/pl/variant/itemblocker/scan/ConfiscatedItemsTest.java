package pl.variant.itemblocker.scan;

import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import pl.variant.itemblocker.restriction.EnchantmentFixer;
import pl.variant.itemblocker.rule.Action;
import pl.variant.itemblocker.rule.EnchantmentMode;
import pl.variant.itemblocker.testing.PluginTest;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ConfiscatedItemsTest extends PluginTest {

    @TempDir
    Path folder;

    private ConfiscatedItems storage(Path file) {
        return new ConfiscatedItems(file, Logger.getLogger(ConfiscatedItemsTest.class.getName()));
    }

    @Test
    void failedReadCanBeRetriedAfterTheFileIsRepaired() throws IOException {
        Path file = folder.resolve("confiscated.yml");
        Files.writeString(file, "broken: [");
        ConfiscatedItems storage = storage(file);
        assertThrows(UncheckedIOException.class, storage::owners);

        YamlConfiguration repaired = new YamlConfiguration();
        repaired.set("Steve", List.of(new ItemStack(Material.DIAMOND, 4)));
        Files.writeString(file, repaired.saveToString());

        assertEquals(Map.of("Steve", 1), storage.owners());
        assertEquals(List.of(new ItemStack(Material.DIAMOND, 4)), storage.take("Steve"));
        assertEquals(Map.of(), storage(file).owners());
    }

    @Test
    void failedWithdrawalLeavesTheSameItemsAvailableForRetry() throws IOException {
        Path file = folder.resolve("confiscated.yml");
        ConfiscatedItems storage = storage(file);
        List<ItemStack> items = List.of(new ItemStack(Material.DIAMOND, 4));
        storage.add("Steve", items);
        Path original = folder.resolve("original.yml");
        Files.move(file, original);
        Files.createDirectory(file);
        Files.writeString(file.resolve("blocker"), "non-empty directory prevents replacement");

        assertThrows(UncheckedIOException.class, () -> storage.take("Steve"));
        assertEquals(Map.of("Steve", 1), storage.owners());

        Files.delete(file.resolve("blocker"));
        Files.delete(file);
        Files.move(original, file);
        assertEquals(items, storage.take("Steve"));
        assertEquals(Map.of(), storage(file).owners());
    }

    @Test
    void oldNestedOwnerKeysAreRecoveredAndSavedAsLiteralNames() throws IOException {
        Path file = folder.resolve("confiscated.yml");
        YamlConfiguration legacy = new YamlConfiguration();
        List<ItemStack> items = List.of(new ItemStack(Material.DIAMOND, 4));
        legacy.set("survival.2026 10 64 20", items);
        Files.writeString(file, legacy.saveToString());

        ConfiscatedItems storage = storage(file);
        assertEquals(Map.of("survival.2026 10 64 20", 1), storage.owners());
        storage.add("Alex", List.of(new ItemStack(Material.TNT)));
        assertEquals(items, storage(file).take("survival.2026 10 64 20"));
    }

    @Test
    void aConfiscationFailureIsReportedOnceWithoutCallingTheSuccessCallback() throws IOException {
        Path parent = folder.resolve("not-a-directory");
        Files.writeString(parent, "block writes");
        Scanner scanner = new Scanner(plugin.enforcement().restrictions(),
                (target, task) -> task.run(), (chunk, task) -> task.run(), storage(parent.resolve("confiscated.yml")));
        block(Material.TNT, Action.PICKUP);
        player.getInventory().setItem(0, new ItemStack(Material.TNT, 5));
        AtomicInteger success = new AtomicInteger();
        AtomicInteger failure = new AtomicInteger();

        scanner.scanPlayers(List.of(player), Set.of(Action.PICKUP), ScanMode.CONFISCATE,
                ignored -> success.incrementAndGet(), ignored -> failure.incrementAndGet());

        assertEquals(0, success.get());
        assertEquals(1, failure.get());
        assertEquals(new ItemStack(Material.TNT, 5), player.getInventory().getItem(0));
    }

    @Test
    void failedAutomaticConfiscationLeavesTheEnchantedItemInPlace() throws IOException {
        Path parent = folder.resolve("not-a-directory");
        Files.writeString(parent, "block writes");
        EnchantmentFixer fixer = new EnchantmentFixer(plugin.enforcement(), storage(parent.resolve("confiscated.yml")));
        updateDefault(preset -> preset.withEnchantments(preset.enchantments()
                .with("sharpness", 5).withMode("sharpness", EnchantmentMode.CONFISCATE)));
        ItemStack sword = new ItemStack(Material.DIAMOND_SWORD);
        sword.addUnsafeEnchantment(org.bukkit.enchantments.Enchantment.SHARPNESS, 5);
        player.getInventory().setItem(0, sword.clone());

        fixer.fixInventory(player);

        assertEquals(sword, player.getInventory().getItem(0));
    }
}
