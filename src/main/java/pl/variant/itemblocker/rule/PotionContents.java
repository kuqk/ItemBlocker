package pl.variant.itemblocker.rule;

import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.lang.reflect.Method;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * The potion effects a potion, splash potion, lingering potion or tipped arrow gives.
 */
public final class PotionContents {

    // The base potion moved from getBasePotionData (up to 1.20.4) to getBasePotionType (from 1.20.2).
    // Neither exists on every supported version, so both are looked up when available.
    private static final Optional<Method> BASE_TYPE = method(PotionMeta.class, "getBasePotionType");
    private static final Optional<Method> BASE_DATA = method(PotionMeta.class, "getBasePotionData");

    private PotionContents() {
    }

    /**
     * Returns the effect keys of the item with their levels, starting at 1.
     */
    public static Map<String, Integer> effectsOf(ItemStack item) {
        Map<String, Integer> effects = new LinkedHashMap<>();
        if (item == null || !item.hasItemMeta()) {
            return effects;
        }

        ItemMeta meta = item.getItemMeta();
        if (!(meta instanceof PotionMeta potion)) {
            return effects;
        }
        for (PotionEffect effect : potion.getCustomEffects()) {
            effects.merge(RegistryKeys.of(effect.getType()), effect.getAmplifier() + 1, Math::max);
        }
        baseEffects(potion, effects);
        return effects;
    }

    private static void baseEffects(PotionMeta potion, Map<String, Integer> effects) {
        try {
            Object type = BASE_TYPE.isPresent() ? BASE_TYPE.get().invoke(potion) : null;
            if (type != null) {
                Optional<Method> list = method(type.getClass(), "getPotionEffects");
                if (list.isPresent()) {
                    for (Object effect : (List<?>) list.get().invoke(type)) {
                        PotionEffect potionEffect = (PotionEffect) effect;
                        effects.merge(RegistryKeys.of(potionEffect.getType()), potionEffect.getAmplifier() + 1, Math::max);
                    }
                    return;
                }
            }

            Object data = BASE_DATA.isPresent() ? BASE_DATA.get().invoke(potion) : null;
            if (data != null) {
                Object dataType = data.getClass().getMethod("getType").invoke(data);
                Object effectType = dataType.getClass().getMethod("getEffectType").invoke(dataType);
                boolean upgraded = (boolean) data.getClass().getMethod("isUpgraded").invoke(data);
                if (effectType instanceof PotionEffectType potionEffectType) {
                    effects.merge(RegistryKeys.of(potionEffectType), upgraded ? 2 : 1, Math::max);
                }
            }
        } catch (ReflectiveOperationException | ClassCastException exception) {
            // A base potion this server describes differently has no effects we can read.
        }
    }

    private static Optional<Method> method(Class<?> type, String name) {
        try {
            return Optional.of(type.getMethod(name));
        } catch (NoSuchMethodException exception) {
            return Optional.empty();
        }
    }
}
