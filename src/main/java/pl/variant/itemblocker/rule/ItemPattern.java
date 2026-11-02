package pl.variant.itemblocker.rule;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Tag;

import java.util.Locale;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * A group of item types: either item keys with {@code *} wildcards, such as {@code *_sword} or
 * {@code netherite_*}, or an item tag, such as {@code #swords} or {@code #minecraft:logs}.
 */
public final class ItemPattern {

    private static final Pattern GLOB = Pattern.compile("[a-z0-9_*]*\\*[a-z0-9_*]*");
    private static final Pattern TAG = Pattern.compile("#([a-z0-9_.-]+:)?[a-z0-9_./-]+");

    private final String text;
    private final Pattern glob;
    private final NamespacedKey tagKey;
    private volatile Tag<Material> tag;

    private ItemPattern(String text, Pattern glob, NamespacedKey tagKey) {
        this.text = text;
        this.glob = glob;
        this.tagKey = tagKey;
    }

    /**
     * Parses a pattern. Plain item keys without a wildcard are not patterns.
     */
    public static Optional<ItemPattern> parse(String input) {
        if (input == null) {
            return Optional.empty();
        }

        String text = input.trim().toLowerCase(Locale.ROOT);
        if (GLOB.matcher(text).matches()) {
            Pattern regex = Pattern.compile(text.replace("*", ".*"));
            return Optional.of(new ItemPattern(text, regex, null));
        }
        if (TAG.matcher(text).matches()) {
            NamespacedKey key = NamespacedKey.fromString(text.substring(1));
            return key == null ? Optional.empty() : Optional.of(new ItemPattern(text, null, key));
        }
        return Optional.empty();
    }

    public static boolean looksLikePattern(String input) {
        return input != null && (input.contains("*") || input.startsWith("#"));
    }

    public String text() {
        return text;
    }

    public boolean matches(Material material) {
        if (glob != null) {
            return glob.matcher(ItemTypes.key(material)).matches();
        }
        Tag<Material> resolved = tag();
        return resolved != null && resolved.isTagged(material);
    }

    // Tags come from data packs, so they are looked up when first needed rather than when parsed.
    private Tag<Material> tag() {
        Tag<Material> resolved = tag;
        if (resolved == null) {
            resolved = Bukkit.getTag(Tag.REGISTRY_ITEMS, tagKey, Material.class);
            tag = resolved;
        }
        return resolved;
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof ItemPattern pattern && pattern.text.equals(text);
    }

    @Override
    public int hashCode() {
        return text.hashCode();
    }

    @Override
    public String toString() {
        return text;
    }
}
