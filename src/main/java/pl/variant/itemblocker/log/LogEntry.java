package pl.variant.itemblocker.log;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/**
 * One blocked action.
 *
 * @param time    when it happened
 * @param player  the player's name
 * @param action  what was blocked, such as {@code place} or {@code enchanting}
 * @param subject the item, enchantment or effect, such as {@code TNT} or {@code Sharpness 5}
 * @param world   the world it happened in
 * @param preset  the preset whose rule blocked it
 */
public record LogEntry(Instant time, String player, String action, String subject, String world, String preset) {

    private static final DateTimeFormatter LINE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.systemDefault());

    /**
     * The entry as one line of a log file.
     */
    public String toLine() {
        return String.join(" | ", LINE_TIME.format(time), player, action, subject, world, preset);
    }
}
