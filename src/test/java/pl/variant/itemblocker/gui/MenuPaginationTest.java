package pl.variant.itemblocker.gui;

import org.bukkit.Material;
import org.bukkit.event.inventory.ClickType;
import org.junit.jupiter.api.Test;
import pl.variant.itemblocker.message.Message;
import pl.variant.itemblocker.rule.Preset;
import pl.variant.itemblocker.testing.PluginTest;

import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MenuPaginationTest extends PluginTest {

    @Test
    void presetsBeyondTheFirstFortyFiveCanBeOpenedAndNavigationReturnsToTheFirstPage() {
        updateRules(rules -> {
            for (int index = 0; index < 50; index++) {
                rules = rules.with(Preset.empty(String.format(Locale.ROOT, "preset%02d", index)));
            }
            return rules;
        });
        PresetsMenu first = new PresetsMenu(plugin.gui());
        first.open(player);

        first.click(player, PresetsMenu.NEXT, ClickType.LEFT);
        server.getScheduler().performOneTick();
        PresetsMenu second = assertInstanceOf(PresetsMenu.class, player.getOpenInventory().getTopInventory().getHolder());
        assertTrue(second.getInventory().getItem(0).getItemMeta().getDisplayName().contains("preset44"));

        second.click(player, 0, ClickType.LEFT);
        server.getScheduler().performOneTick();
        assertInstanceOf(PresetMenu.class, player.getOpenInventory().getTopInventory().getHolder());
        assertTrue(player.getOpenInventory().getTitle().contains("preset44"));

        second.open(player);
        second.click(player, PresetsMenu.PREVIOUS, ClickType.LEFT);
        server.getScheduler().performOneTick();
        PresetsMenu returned = assertInstanceOf(PresetsMenu.class, player.getOpenInventory().getTopInventory().getHolder());
        assertEquals(Material.ENCHANTED_BOOK, returned.getInventory().getItem(0).getType());
    }

    @Test
    void nameSelectionsSurviveNavigationAcrossThreePages() {
        List<String> names = IntStream.range(0, 100).mapToObj(index -> "region" + index).toList();
        AtomicReference<Set<String>> selected = new AtomicReference<>(Set.of());
        NamePickerMenu menu = new NamePickerMenu(plugin.gui(), Message.GUI_REGIONS_TITLE, names,
                ignored -> Material.OAK_FENCE, selected::get, selected::set, () -> new PresetsMenu(plugin.gui()));
        menu.open(player);

        menu.click(player, 0, ClickType.LEFT);
        menu.click(player, NamePickerMenu.NEXT, ClickType.LEFT);
        menu.click(player, 0, ClickType.LEFT);
        menu.click(player, NamePickerMenu.NEXT, ClickType.LEFT);
        menu.click(player, 0, ClickType.LEFT);
        assertEquals(Set.of("region0", "region45", "region90"), selected.get());

        menu.click(player, NamePickerMenu.PREVIOUS, ClickType.LEFT);
        menu.click(player, 0, ClickType.LEFT);
        assertEquals(Set.of("region0", "region90"), selected.get());
        menu.click(player, NamePickerMenu.EVERYWHERE, ClickType.LEFT);
        assertTrue(selected.get().isEmpty());
    }

    @Test
    void unsupportedGesturesDoNotRemovePotionRestrictions() {
        blockEffect("strength", 2);
        LevelsMenu menu = new LevelsMenu(plugin.gui(), "default", LevelsMenu.Kind.EFFECTS, 0);
        menu.open(player);
        int index = menu.entries(plugin.rules().rules().defaultPreset()).indexOf("strength");
        ListMenu<?> current = menu;
        if (index >= ListMenu.PAGE_SIZE) {
            current = (ListMenu<?>) menu.atPage(index / ListMenu.PAGE_SIZE);
            current.open(player);
        }
        int slot = index % ListMenu.PAGE_SIZE;
        for (ClickType gesture : List.of(ClickType.NUMBER_KEY, ClickType.MIDDLE, ClickType.DROP,
                ClickType.CONTROL_DROP, ClickType.DOUBLE_CLICK, ClickType.SWAP_OFFHAND)) {
            current.click(player, slot, gesture);
            assertEquals(2, plugin.rules().rules().defaultPreset().effects().minimumLevel("strength").orElse(0), gesture.name());
        }
        current.click(player, slot, ClickType.SHIFT_RIGHT);
        assertTrue(plugin.rules().rules().defaultPreset().effects().minimumLevel("strength").isEmpty());
    }
}
