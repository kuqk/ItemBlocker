package pl.variant.itemblocker.config;

import org.bukkit.configuration.ConfigurationSection;

import java.time.Duration;
import java.util.Locale;

/**
 * Values read from {@code config.yml}.
 *
 * @param language           language code of the message file to use
 * @param messageCooldown    minimum time between two restriction messages to the same player
 * @param bypassEnabled      whether {@code itemblocker.bypass} skips every restriction
 * @param perActionBypass    whether {@code itemblocker.bypass.<action>} skips restrictions for one action
 */
public record Settings(String language, Duration messageCooldown, boolean bypassEnabled, boolean perActionBypass) {

    public static final int CONFIG_VERSION = 2;

    public static final String LANGUAGE = "language";
    public static final String MESSAGE_COOLDOWN = "messages.cooldown-ms";
    public static final String BYPASS_ENABLED = "bypass.enabled";
    public static final String PER_ACTION_BYPASS = "bypass.per-action";

    public static final Settings DEFAULTS = new Settings("en", Duration.ofSeconds(1), true, true);

    public static Settings from(ConfigurationSection config) {
        return new Settings(
                config.getString(LANGUAGE, DEFAULTS.language()).trim().toLowerCase(Locale.ROOT),
                Duration.ofMillis(Math.max(0, config.getLong(MESSAGE_COOLDOWN, DEFAULTS.messageCooldown().toMillis()))),
                config.getBoolean(BYPASS_ENABLED, DEFAULTS.bypassEnabled()),
                config.getBoolean(PER_ACTION_BYPASS, DEFAULTS.perActionBypass())
        );
    }
}
