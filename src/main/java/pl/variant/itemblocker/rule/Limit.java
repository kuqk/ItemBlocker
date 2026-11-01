package pl.variant.itemblocker.rule;

/**
 * A limit in effect at some place: the amount and the preset it comes from.
 *
 * @param amount the largest allowed amount, at least 1
 * @param source the preset that sets it
 */
public record Limit(int amount, Preset source) {
}
