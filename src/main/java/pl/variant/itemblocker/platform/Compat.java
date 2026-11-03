package pl.variant.itemblocker.platform;

import org.bukkit.Keyed;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.file.YamlConfigurationOptions;
import org.bukkit.event.Event;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.potion.PotionEffectType;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Calls API methods that older supported servers do not have, with a fallback for those servers.
 */
public final class Compat {

    private static final MethodHandles.Lookup LOOKUP = MethodHandles.publicLookup();
    private static final MethodHandle SET_HEADER = find(YamlConfigurationOptions.class, "setHeader",
            MethodType.methodType(YamlConfigurationOptions.class, List.class));
    private static final MethodHandle EFFECT_BY_KEY = findStatic(PotionEffectType.class, "getByKey",
            MethodType.methodType(PotionEffectType.class, NamespacedKey.class));

    // Effects whose legacy Bukkit name differs from their key.
    private static final Map<String, String> LEGACY_NAMES = Map.of(
            "slowness", "slow",
            "haste", "fast_digging",
            "mining_fatigue", "slow_digging",
            "strength", "increase_damage",
            "instant_health", "heal",
            "instant_damage", "harm",
            "jump_boost", "jump",
            "nausea", "confusion",
            "resistance", "damage_resistance");

    private Compat() {
    }

    /**
     * The hand of an event that only has {@code getHand()} from 1.19, such as entity placement and
     * emptying a bucket. Older servers only let players do it with the main hand.
     */
    public static EquipmentSlot hand(Event event) {
        try {
            Object hand = event.getClass().getMethod("getHand").invoke(event);
            if (hand instanceof EquipmentSlot slot) {
                return slot;
            }
        } catch (ReflectiveOperationException | LinkageError ignored) {
            // No getHand() before 1.19.
        }
        return EquipmentSlot.HAND;
    }

    /**
     * Sets the header comment of a YAML file. {@code setHeader(List)} exists from 1.18, older servers
     * take the header as one string.
     */
    @SuppressWarnings("deprecation")
    public static void header(YamlConfigurationOptions options, List<String> lines) {
        if (SET_HEADER != null) {
            try {
                SET_HEADER.invoke(options, lines);
                return;
            } catch (Throwable ignored) {
                // Fall through to the string header.
            }
        }
        options.header(String.join("\n", lines));
    }

    /**
     * Looks up a potion effect by key. {@code getByKey} exists from 1.18, older servers look effects
     * up by their legacy name.
     */
    @SuppressWarnings("deprecation")
    public static PotionEffectType effect(NamespacedKey key) {
        if (EFFECT_BY_KEY != null) {
            try {
                return (PotionEffectType) EFFECT_BY_KEY.invoke(key);
            } catch (Throwable ignored) {
                // Fall through to the name lookup.
            }
        }
        if (!NamespacedKey.MINECRAFT.equals(key.getNamespace())) {
            return null;
        }
        String name = key.getKey();
        return PotionEffectType.getByName(LEGACY_NAMES.getOrDefault(name, name).toUpperCase(Locale.ROOT));
    }

    /**
     * The key of a potion effect type. Effect types are only {@link Keyed} from 1.18, older servers
     * name them by their legacy name.
     */
    @SuppressWarnings({"deprecation", "ConstantValue"})
    public static NamespacedKey effectKey(PotionEffectType type) {
        Object entry = type;
        if (entry instanceof Keyed keyed) {
            return keyed.getKey();
        }
        String name = type.getName().toLowerCase(Locale.ROOT);
        for (Map.Entry<String, String> legacy : LEGACY_NAMES.entrySet()) {
            if (legacy.getValue().equals(name)) {
                return NamespacedKey.minecraft(legacy.getKey());
            }
        }
        return NamespacedKey.minecraft(name);
    }

    private static MethodHandle find(Class<?> owner, String name, MethodType type) {
        try {
            return LOOKUP.findVirtual(owner, name, type);
        } catch (ReflectiveOperationException e) {
            return null;
        }
    }

    private static MethodHandle findStatic(Class<?> owner, String name, MethodType type) {
        try {
            return LOOKUP.findStatic(owner, name, type);
        } catch (ReflectiveOperationException e) {
            return null;
        }
    }
}
