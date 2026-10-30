package pl.variant.itemblocker.command;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffectType;
import pl.variant.itemblocker.message.Message;
import pl.variant.itemblocker.message.Placeholder;
import pl.variant.itemblocker.rule.Action;
import pl.variant.itemblocker.rule.ItemTypes;
import pl.variant.itemblocker.rule.Preset;
import pl.variant.itemblocker.rule.RegistryKeys;
import pl.variant.itemblocker.rule.RuleBook;
import pl.variant.itemblocker.rule.WorldScope;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Turns command arguments into rule values, throwing {@link CommandException} for invalid input.
 */
final class Arguments {

    static final String HAND = "hand";
    static final String ALL = "all";
    static final String NONE = "none";

    private Arguments() {
    }

    /**
     * Resolves an item key, or {@value #HAND} for the item in the player's main hand.
     */
    static Material item(CommandContext context, String input) {
        if (input.equalsIgnoreCase(HAND)) {
            ItemStack held = context.requirePlayer().getInventory().getItemInMainHand();
            if (held.getType().isAir()) {
                throw new CommandException(Message.ERROR_EMPTY_HAND);
            }
            return held.getType();
        }

        return ItemTypes.parse(input)
                .orElseThrow(() -> new CommandException(Message.ERROR_UNKNOWN_ITEM, Placeholder.of("input", input)));
    }

    static Enchantment enchantment(String input) {
        return Optional.ofNullable(namespacedKey(input))
                .map(Registry.ENCHANTMENT::get)
                .orElseThrow(() -> new CommandException(Message.ERROR_UNKNOWN_ENCHANTMENT, Placeholder.of("input", input)));
    }

    // Registry.EFFECT only exists from 1.20.3. PotionEffectType.getByKey is deprecated in favor of it,
    // but it is the lookup that works on every supported version.
    @SuppressWarnings("deprecation")
    static PotionEffectType effect(String input) {
        return Optional.ofNullable(namespacedKey(input))
                .map(PotionEffectType::getByKey)
                .orElseThrow(() -> new CommandException(Message.ERROR_UNKNOWN_POTION, Placeholder.of("input", input)));
    }

    /**
     * Resolves the preset named by the {@code in:} option, or the default preset.
     */
    static Preset preset(CommandContext context, Options options) {
        return preset(context, options.get(Options.PRESET).orElse(RuleBook.DEFAULT_PRESET));
    }

    static Preset preset(CommandContext context, String name) {
        return context.rules().preset(name)
                .orElseThrow(() -> new CommandException(Message.ERROR_UNKNOWN_PRESET, Placeholder.of("preset", name)));
    }

    /**
     * Parses a comma separated list of actions, where {@value #ALL} means every action and
     * {@value #NONE} means none.
     */
    static Set<Action> actions(String input) {
        EnumSet<Action> actions = EnumSet.noneOf(Action.class);
        for (String key : input.split(",")) {
            String trimmed = key.trim();
            if (trimmed.equalsIgnoreCase(ALL)) {
                return EnumSet.allOf(Action.class);
            }
            if (trimmed.equalsIgnoreCase(NONE) || trimmed.isEmpty()) {
                continue;
            }

            actions.add(Action.fromKey(trimmed).orElseThrow(() -> new CommandException(
                    Message.ERROR_UNKNOWN_ACTION,
                    Placeholder.of("input", trimmed),
                    Placeholder.of("actions", Arrays.stream(Action.values()).map(Action::key).collect(Collectors.joining(", ")))
            )));
        }
        return actions;
    }

    /**
     * Parses a comma separated list of worlds, where {@value #ALL} means every world.
     */
    static WorldScope worlds(String input) {
        return WorldScope.of(List.of(input.split(",")));
    }

    static int level(Options options) {
        String input = options.get(Options.LEVEL).orElse("1");
        try {
            int level = Integer.parseInt(input);
            if (level >= 1) {
                return level;
            }
        } catch (NumberFormatException ignored) {
            // Reported below together with values below one.
        }
        throw new CommandException(Message.ERROR_INVALID_LEVEL, Placeholder.of("input", input));
    }

    private static NamespacedKey namespacedKey(String input) {
        String key = RegistryKeys.normalize(input);
        return NamespacedKey.fromString(key.contains(":") ? key : NamespacedKey.MINECRAFT + ":" + key);
    }
}
