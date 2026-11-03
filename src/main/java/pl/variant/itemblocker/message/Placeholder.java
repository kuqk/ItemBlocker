package pl.variant.itemblocker.message;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.Objects;

/**
 * A value that replaces {@code {name}} in a message.
 *
 * @param translationKey a Minecraft translation key, such as {@code item.minecraft.apple}, that
 *                       players see translated into the language of their own game, or {@code null};
 *                       {@code value} is then what the console and logs get
 */
public record Placeholder(String name, String value, String translationKey) {

    public Placeholder {
        Objects.requireNonNull(name, "name");
        value = Objects.requireNonNullElse(value, "");
    }

    public Placeholder(String name, String value) {
        this(name, value, null);
    }

    public static Placeholder of(String name, Object value) {
        return new Placeholder(name, value == null ? "" : value.toString());
    }

    /**
     * The name of an item type, translated for players.
     */
    public static Placeholder item(String name, Material material) {
        return new Placeholder(name, Names.of(material), Names.translationKey(material));
    }

    /**
     * The name of an item: its custom name if it has one, otherwise the name of its type, translated
     * for players.
     */
    public static Placeholder item(String name, ItemStack item) {
        return Names.customName(item)
                .map(custom -> new Placeholder(name, custom))
                .orElseGet(() -> item(name, item.getType()));
    }

    /**
     * The name of an enchantment, translated for players unless it comes from a data pack.
     */
    public static Placeholder enchantment(String name, String key) {
        return new Placeholder(name, Names.ofKey(key), Names.translationKey("enchantment", key));
    }

    /**
     * The name of a potion effect, translated for players unless it comes from a data pack.
     */
    public static Placeholder effect(String name, String key) {
        return new Placeholder(name, Names.ofKey(key), Names.translationKey("effect", key));
    }
}
