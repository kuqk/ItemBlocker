package pl.variant.itemblocker.rule;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * Matches items by more than their type, so that one specific item, such as a sword from an RPG
 * plugin, can be blocked without blocking every item of its type. Every criterion that is set must
 * match; {@code null} criteria are ignored.
 *
 * @param type        item type
 * @param name        text the custom name contains, ignoring case and colors
 * @param lore        text one lore line contains, ignoring case and colors
 * @param model       custom model data
 * @param dataKey     persistent data key, such as {@code nexo:id}
 * @param dataValue   value stored under {@code dataKey}, or {@code null} for any value
 * @param enchantment enchantment key the item carries
 */
public record ItemFilter(
        Material type,
        String name,
        String lore,
        Integer model,
        String dataKey,
        String dataValue,
        String enchantment
) {

    // Namespaces of data the server itself adds, which never identifies a custom item.
    private static final Set<String> SERVER_NAMESPACES = Set.of("minecraft", "bukkit", "paper");

    public ItemFilter {
        name = blankToNull(name);
        lore = blankToNull(lore);
        dataKey = blankToNull(dataKey) == null ? null : dataKey.trim().toLowerCase(Locale.ROOT);
        dataValue = dataKey == null ? null : blankToNull(dataValue);
        enchantment = blankToNull(enchantment) == null ? null : RegistryKeys.normalize(enchantment);
    }

    /**
     * Copies what identifies the item: its type, custom name, custom model data and the first
     * persistent data entry that a plugin added, which is how plugins such as Nexo mark their items.
     *
     * @return the filter, or {@code null} for an empty hand
     */
    @SuppressWarnings("deprecation")
    public static ItemFilter of(ItemStack item) {
        if (item == null || item.getType().isAir()) {
            return null;
        }
        if (!item.hasItemMeta()) {
            return new ItemFilter(item.getType(), null, null, null, null, null, null);
        }

        ItemMeta meta = item.getItemMeta();
        String name = meta.hasDisplayName() ? ChatColor.stripColor(meta.getDisplayName()) : null;
        Integer model = meta.hasCustomModelData() ? meta.getCustomModelData() : null;
        PersistentDataContainer data = meta.getPersistentDataContainer();
        Optional<NamespacedKey> key = data.getKeys().stream()
                .filter(candidate -> !SERVER_NAMESPACES.contains(candidate.getNamespace()))
                .filter(candidate -> data.has(candidate, PersistentDataType.STRING))
                .findFirst();
        return new ItemFilter(item.getType(), name, null, model,
                key.map(NamespacedKey::toString).orElse(null),
                key.map(found -> data.get(found, PersistentDataType.STRING)).orElse(null),
                null);
    }

    public boolean isEmpty() {
        return type == null && name == null && lore == null && model == null && dataKey == null && enchantment == null;
    }

    public boolean matches(ItemStack item) {
        if (item == null || item.getType().isAir() || (type != null && item.getType() != type)) {
            return false;
        }
        if (name == null && lore == null && model == null && dataKey == null && enchantment == null) {
            return true;
        }
        if (!item.hasItemMeta()) {
            return false;
        }

        ItemMeta meta = item.getItemMeta();
        return (name == null || (meta.hasDisplayName() && contains(meta.getDisplayName(), name)))
                && (lore == null || hasLoreLine(meta))
                && (model == null || hasModel(meta))
                && (dataKey == null || hasData(meta.getPersistentDataContainer()))
                && (enchantment == null || hasEnchantment(meta));
    }

    private boolean hasLoreLine(ItemMeta meta) {
        List<String> lines = meta.hasLore() ? meta.getLore() : List.of();
        return lines != null && lines.stream().anyMatch(line -> contains(line, lore));
    }

    // Deprecated on newer servers in favour of the custom model data component, but it still
    // reads the first number of that component and exists on every supported version.
    @SuppressWarnings("deprecation")
    private boolean hasModel(ItemMeta meta) {
        return meta.hasCustomModelData() && meta.getCustomModelData() == model;
    }

    private boolean hasData(PersistentDataContainer data) {
        NamespacedKey key = NamespacedKey.fromString(dataKey);
        if (key == null || !data.getKeys().contains(key)) {
            return false;
        }
        if (dataValue == null) {
            return true;
        }
        if (data.has(key, PersistentDataType.STRING)) {
            return dataValue.equalsIgnoreCase(data.get(key, PersistentDataType.STRING));
        }
        if (data.has(key, PersistentDataType.INTEGER)) {
            return dataValue.equals(String.valueOf(data.get(key, PersistentDataType.INTEGER)));
        }
        return false;
    }

    private boolean hasEnchantment(ItemMeta meta) {
        if (meta.getEnchants().keySet().stream()
                .map(RegistryKeys::of)
                .anyMatch(enchantment::equals)) {
            return true;
        }
        return meta instanceof EnchantmentStorageMeta book && book.getStoredEnchants().keySet().stream()
                .map(RegistryKeys::of)
                .anyMatch(enchantment::equals);
    }

    private static boolean contains(String text, String part) {
        String plain = Objects.requireNonNullElse(ChatColor.stripColor(text), "");
        return plain.toLowerCase(Locale.ROOT).contains(part.toLowerCase(Locale.ROOT));
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    /**
     * Describes the criteria, for listings, such as {@code type=diamond_sword, name=Excalibur}.
     */
    public String describe() {
        StringBuilder text = new StringBuilder();
        append(text, "type", type == null ? null : ItemTypes.key(type));
        append(text, "name", name);
        append(text, "lore", lore);
        append(text, "model", model == null ? null : model.toString());
        append(text, "data", dataKey == null ? null : dataValue == null ? dataKey : dataKey + "=" + dataValue);
        append(text, "enchantment", enchantment);
        return text.toString();
    }

    private static void append(StringBuilder text, String key, String value) {
        if (value != null) {
            if (!text.isEmpty()) {
                text.append(", ");
            }
            text.append(key).append('=').append(value);
        }
    }
}
