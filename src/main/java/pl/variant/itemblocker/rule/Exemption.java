package pl.variant.itemblocker.rule;

/**
 * Lets someone ignore a whole preset or a single rule, for example a rank allowed to use one item
 * that is blocked for everyone else.
 */
@FunctionalInterface
public interface Exemption {

    /**
     * Nobody is exempt, as for hoppers.
     */
    Exemption NONE = (preset, permission) -> false;

    /**
     * @param preset     the preset of the rule
     * @param permission the rule's own bypass permission, or {@code null} when asking about the
     *                   whole preset
     */
    boolean exempts(Preset preset, String permission);
}
