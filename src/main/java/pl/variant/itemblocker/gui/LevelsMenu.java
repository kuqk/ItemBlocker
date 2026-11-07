package pl.variant.itemblocker.gui;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;
import pl.variant.itemblocker.message.Formats;
import pl.variant.itemblocker.message.Message;
import pl.variant.itemblocker.message.Names;
import pl.variant.itemblocker.message.Placeholder;
import pl.variant.itemblocker.rule.LevelLimits;
import pl.variant.itemblocker.rule.Preset;
import pl.variant.itemblocker.rule.RegistryKeys;

import java.util.List;
import java.util.OptionalInt;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Every enchantment or every potion effect, blocked ones with a red name. Left click raises the
 * level it is blocked from, right click lowers it, shift + right click unblocks it.
 */
final class LevelsMenu extends ListMenu<String> {

    private static final int MAX_LEVEL = 255;

    enum Kind {
        ENCHANTMENTS(Message.GUI_ENCHANTMENTS_TITLE, Material.ENCHANTED_BOOK, RegistryKeys::enchantments,
                Preset::enchantments, Preset::withEnchantments),
        EFFECTS(Message.GUI_EFFECTS_TITLE, Material.POTION, RegistryKeys::effects,
                Preset::effects, Preset::withEffects);

        private final Message title;
        private final Material icon;
        private final Supplier<List<String>> keys;
        private final Function<Preset, LevelLimits> limits;
        private final BiFunction<Preset, LevelLimits, Preset> withLimits;

        Kind(Message title, Material icon, Supplier<List<String>> keys,
             Function<Preset, LevelLimits> limits, BiFunction<Preset, LevelLimits, Preset> withLimits) {
            this.title = title;
            this.icon = icon;
            this.keys = keys;
            this.limits = limits;
            this.withLimits = withLimits;
        }
    }

    private final Kind kind;

    LevelsMenu(Gui gui, String presetName, Kind kind, int page) {
        super(gui, presetName, page, gui.text(kind.title, Placeholder.of("preset", presetName)));
        this.kind = kind;
    }

    // Kept in key order so an entry stays where it is when clicked; blocked entries have a red name.
    @Override
    protected List<String> entries(Preset preset) {
        return kind.keys.get();
    }

    @Override
    protected ItemStack icon(Preset preset, String key) {
        LevelLimits limits = kind.limits.apply(preset);
        OptionalInt level = limits.minimumLevel(key);
        String levelText = level.isPresent() ? Integer.toString(level.getAsInt()) : gui.text(Message.GUI_NOT_BLOCKED);
        String name = (level.isPresent() ? ChatColor.RED : ChatColor.WHITE) + Names.ofKey(key);
        if (kind == Kind.ENCHANTMENTS) {
            return Gui.icon(kind.icon, name, gui.lines(Message.GUI_ENCHANTMENT_LORE, Placeholder.of("level", levelText),
                    Placeholder.of("mode", Formats.mode(gui.messages(), limits.mode(key)))));
        }
        return Gui.icon(kind.icon, name, gui.lines(Message.GUI_LEVEL_LORE, Placeholder.of("level", levelText)));
    }

    @Override
    protected void clickEntry(Player player, String key, ClickType click) {
        if (click != ClickType.LEFT && click != ClickType.RIGHT
                && click != ClickType.SHIFT_LEFT && click != ClickType.SHIFT_RIGHT) {
            return;
        }
        if (kind == Kind.ENCHANTMENTS && click == ClickType.SHIFT_LEFT) {
            // Shift + left click cycles what happens to items with a blocked enchantment.
            change(player, preset -> {
                LevelLimits limits = kind.limits.apply(preset);
                return limits.contains(key) ? kind.withLimits.apply(preset, limits.withMode(key, limits.mode(key).next())) : preset;
            });
            return;
        }
        change(player, preset -> {
            LevelLimits limits = kind.limits.apply(preset);
            int current = limits.minimumLevel(key).orElse(0);
            int level = switch (click) {
                case LEFT, SHIFT_LEFT -> Math.min(MAX_LEVEL, current + 1);
                case RIGHT -> current - 1;
                case SHIFT_RIGHT -> 0;
                default -> current;
            };
            return kind.withLimits.apply(preset, level <= 0 ? limits.without(key) : limits.with(key, level));
        });
    }

    @Override
    protected Menu atPage(int newPage) {
        return new LevelsMenu(gui, presetName, kind, newPage);
    }
}
