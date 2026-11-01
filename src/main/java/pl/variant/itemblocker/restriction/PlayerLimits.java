package pl.variant.itemblocker.restriction;

import org.bukkit.Material;
import pl.variant.itemblocker.rule.Limit;
import pl.variant.itemblocker.rule.Place;
import pl.variant.itemblocker.rule.RuleBook;

import java.util.Optional;

/**
 * The item limits that apply to a player where they are.
 */
public final class PlayerLimits {

    static final PlayerLimits NONE = new PlayerLimits(RuleBook.empty(), Place.inWorld(null));

    private final RuleBook rules;
    private final Place place;

    PlayerLimits(RuleBook rules, Place place) {
        this.rules = rules;
        this.place = place;
    }

    public boolean isEmpty() {
        return !rules.usesLimits();
    }

    /**
     * Returns the stack limit for the item type, if it is smaller than the type's own stack size.
     */
    public Optional<Limit> stack(Material material) {
        return rules.stackLimit(material, place).filter(limit -> limit.amount() < material.getMaxStackSize());
    }

    public Optional<Limit> inventory(Material material) {
        return rules.inventoryLimit(material, place);
    }
}
