package pl.variant.itemblocker.api;

/**
 * Why something is blocked.
 *
 * @param action  what is blocked, such as {@code place}, {@code enchanted-item} or {@code enchanting}
 * @param subject the item, or the enchantment or effect with its level, as shown to players
 * @param preset  the name of the preset whose rule blocks it
 * @param reason  the preset's reason, may be empty
 */
public record Restriction(String action, String subject, String preset, String reason) {
}
