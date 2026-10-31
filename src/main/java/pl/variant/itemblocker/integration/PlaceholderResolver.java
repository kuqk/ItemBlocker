package pl.variant.itemblocker.integration;

import org.bukkit.entity.Player;

/**
 * Fills in placeholders of other plugins, which PlaceholderAPI provides when it is installed.
 */
@FunctionalInterface
public interface PlaceholderResolver {

    /**
     * Used when PlaceholderAPI is not installed: text is left as it is.
     */
    PlaceholderResolver NONE = (player, text) -> text;

    String resolve(Player player, String text);
}
