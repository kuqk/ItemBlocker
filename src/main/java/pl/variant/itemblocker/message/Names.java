package pl.variant.itemblocker.message;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import pl.variant.itemblocker.rule.ItemTypes;
import pl.variant.itemblocker.rule.RegistryKeys;

import java.lang.reflect.Method;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

/**
 * Turns item, enchantment and potion effect keys into readable names for messages, such as
 * {@code Diamond Chestplate} for {@code diamond_chestplate}. Items with a custom name, which is how
 * items from other plugins usually look, are shown by that name.
 */
public final class Names {

    private static final Set<String> LOWERCASE_WORDS = Set.of("a", "and", "of", "on", "the", "with");
    private static final Set<String> UPPERCASE_WORDS = Set.of("tnt");

    // ItemMeta#hasItemName and #getItemName only exist from 1.20.5. Calling them directly would keep
    // the jar from loading on older servers, so they are looked up once when available.
    private static final Optional<Method> HAS_ITEM_NAME = itemMetaMethod("hasItemName");
    private static final Optional<Method> GET_ITEM_NAME = itemMetaMethod("getItemName");

    private Names() {
    }

    public static String of(Material material) {
        return ofKey(ItemTypes.key(material));
    }

    /**
     * Returns the name a player sees for the item: its custom name if it has one, otherwise the
     * readable name of its type.
     */
    public static String of(ItemStack item) {
        return customName(item).orElseGet(() -> of(item.getType()));
    }

    /**
     * Returns the readable form of a key. A namespace such as {@code minecraft:} or the one of a
     * data pack is left out.
     */
    public static String ofKey(String key) {
        String normalized = RegistryKeys.normalize(key);
        String path = normalized.substring(normalized.lastIndexOf(':') + 1);

        StringBuilder name = new StringBuilder();
        for (String word : path.split("_")) {
            if (word.isEmpty()) {
                continue;
            }
            if (!name.isEmpty()) {
                name.append(' ');
            }
            if (UPPERCASE_WORDS.contains(word)) {
                name.append(word.toUpperCase(Locale.ROOT));
            } else if (!name.isEmpty() && LOWERCASE_WORDS.contains(word)) {
                name.append(word);
            } else {
                name.append(word.substring(0, 1).toUpperCase(Locale.ROOT)).append(word.substring(1));
            }
        }
        return name.toString();
    }

    private static Optional<String> customName(ItemStack item) {
        if (!item.hasItemMeta()) {
            return Optional.empty();
        }

        ItemMeta meta = item.getItemMeta();
        if (meta.hasDisplayName()) {
            return plain(meta.getDisplayName());
        }
        if (HAS_ITEM_NAME.isPresent() && GET_ITEM_NAME.isPresent()) {
            try {
                if ((boolean) HAS_ITEM_NAME.get().invoke(meta)) {
                    return plain((String) GET_ITEM_NAME.get().invoke(meta));
                }
            } catch (ReflectiveOperationException ignored) {
                // Fall back to the type name.
            }
        }
        return Optional.empty();
    }

    private static Optional<String> plain(String name) {
        String stripped = ChatColor.stripColor(name);
        return stripped == null || stripped.isBlank() ? Optional.empty() : Optional.of(stripped.trim());
    }

    private static Optional<Method> itemMetaMethod(String name) {
        try {
            return Optional.of(ItemMeta.class.getMethod(name));
        } catch (NoSuchMethodException exception) {
            return Optional.empty();
        }
    }
}
