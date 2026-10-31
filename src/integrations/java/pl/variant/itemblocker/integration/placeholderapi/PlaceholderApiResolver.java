package pl.variant.itemblocker.integration.placeholderapi;

import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.entity.Player;
import pl.variant.itemblocker.integration.PlaceholderResolver;

/**
 * Fills in PlaceholderAPI placeholders such as {@code %player_name%}.
 */
public final class PlaceholderApiResolver implements PlaceholderResolver {

    @Override
    public String resolve(Player player, String text) {
        return PlaceholderAPI.setPlaceholders(player, text);
    }
}
