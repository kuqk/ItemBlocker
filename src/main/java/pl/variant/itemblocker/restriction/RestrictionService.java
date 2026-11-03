package pl.variant.itemblocker.restriction;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffectType;
import pl.variant.itemblocker.api.Restriction;
import pl.variant.itemblocker.api.event.ItemBlockedEvent;
import pl.variant.itemblocker.config.Settings;
import pl.variant.itemblocker.integration.Regions;
import pl.variant.itemblocker.message.Message;
import pl.variant.itemblocker.message.Placeholder;
import pl.variant.itemblocker.rule.Action;
import pl.variant.itemblocker.rule.EnchantmentMode;
import pl.variant.itemblocker.rule.Exemption;
import pl.variant.itemblocker.rule.Place;
import pl.variant.itemblocker.rule.PotionContents;
import pl.variant.itemblocker.rule.Preset;
import pl.variant.itemblocker.rule.RegistryKeys;
import pl.variant.itemblocker.rule.RuleBook;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Decides whether something is blocked, taking bypass permissions into account.
 */
public final class RestrictionService {

    public static final String BYPASS_PERMISSION = "itemblocker.bypass";
    public static final String ENCHANTMENT_BYPASS = "enchantment";
    public static final String POTION_BYPASS = "potion";
    public static final String LIMIT_BYPASS = "limit";
    public static final String PRESET_BYPASS_PREFIX = BYPASS_PERMISSION + ".preset.";

    private final Supplier<RuleBook> rules;
    private final Supplier<Settings> settings;
    private final Regions regions;
    private final TemporaryRules temporaryRules;
    private final Consumer<Event> events;

    /**
     * @param rules          the rules to check, including temporary rules for everyone
     * @param temporaryRules temporary rules, for the rules of single players
     * @param events         calls events, so other plugins can let a blocked action through
     */
    public RestrictionService(Supplier<RuleBook> rules, Supplier<Settings> settings, Regions regions,
                              TemporaryRules temporaryRules, Consumer<Event> events) {
        this.rules = rules;
        this.settings = settings;
        this.regions = regions;
        this.temporaryRules = temporaryRules;
        this.events = events;
    }

    /**
     * Checks whether the player may perform the action with the item. An item carrying a blocked
     * enchantment is blocked for every action.
     */
    public Optional<Denial> checkItem(Player player, ItemStack item, Action action) {
        if (isEmpty(item)) {
            return Optional.empty();
        }

        Place place = placeOf(player.getLocation());
        Optional<Denial> denial = bypasses(player, action.key())
                ? Optional.empty()
                : itemDenial(item, action, place, exemptionOf(player)).or(() -> playerRuleDenial(player, item, action, place));
        return denial.isPresent() ? confirmed(player, item, denial.get()) : checkEnchantedItem(player, item);
    }

    /**
     * Checks an item moved without a player, such as by a hopper. Bypass permissions do not apply.
     *
     * @param location where the item is moved, or {@code null} if unknown
     */
    public Optional<Denial> checkItem(ItemStack item, Action action, Location location) {
        if (isEmpty(item)) {
            return Optional.empty();
        }

        Place place = placeOf(location);
        Optional<Denial> denial = itemDenial(item, action, place, Exemption.NONE);
        return denial.isPresent() ? denial : enchantedItemDenial(item, place, Exemption.NONE);
    }

    /**
     * Checks an item someone holds, for scans: whether it is blocked for any of the actions or carries
     * a blocked enchantment.
     *
     * @param owner the player holding it, whose bypass permissions apply, or {@code null} for a container
     */
    public Optional<Denial> checkHeld(ItemStack item, Set<Action> actions, Location location, Player owner) {
        if (isEmpty(item)) {
            return Optional.empty();
        }

        Place place = placeOf(location);
        Exemption exemption = owner == null ? Exemption.NONE : exemptionOf(owner);
        for (Action action : actions) {
            if (owner == null || !bypasses(owner, action.key())) {
                Optional<Denial> denial = itemDenial(item, action, place, exemption);
                if (denial.isPresent()) {
                    return owner == null ? denial : confirmed(owner, item, denial.get());
                }
            }
        }
        if (owner == null) {
            return enchantedItemDenial(item, place, exemption);
        }
        if (bypasses(owner, ENCHANTMENT_BYPASS)) {
            return Optional.empty();
        }
        return enchantedItemDenial(item, place, exemption).flatMap(denial -> confirmed(owner, item, denial));
    }

    /**
     * Checks a potion a brewing stand is about to make: blocked if a rule blocks brewing the item, or
     * if a preset blocks one of the potion effects it gives.
     */
    public Optional<Denial> checkBrew(ItemStack result, Location location) {
        if (isEmpty(result)) {
            return Optional.empty();
        }

        Place place = placeOf(location);
        Optional<Denial> denial = itemDenial(result, Action.BREW, place, Exemption.NONE);
        if (denial.isPresent()) {
            return denial;
        }
        RuleBook book = rules.get();
        for (Map.Entry<String, Integer> effect : PotionContents.effectsOf(result).entrySet()) {
            Optional<Preset> preset = book.findEffectBlock(effect.getKey(), effect.getValue(), place);
            if (preset.isPresent()) {
                return Optional.of(new Denial(Message.RESTRICTION_BREW, preset.get(), List.of(Placeholder.item("item", result))));
            }
        }
        return Optional.empty();
    }

    /**
     * Checks whether the item carries an enchantment the player may not have.
     */
    public Optional<Denial> checkEnchantedItem(Player player, ItemStack item) {
        if (isEmpty(item) || bypasses(player, ENCHANTMENT_BYPASS)) {
            return Optional.empty();
        }
        return enchantedItemDenial(item, placeOf(player.getLocation()), exemptionOf(player))
                .flatMap(denial -> confirmed(player, item, denial));
    }

    public Optional<Denial> checkEnchanting(Player player, Enchantment enchantment, int level) {
        if (bypasses(player, ENCHANTMENT_BYPASS)) {
            return Optional.empty();
        }

        // Enchantments that are lowered, removed or confiscated may be enchanted; the item is fixed after.
        String key = RegistryKeys.of(enchantment);
        return rules.get()
                .findEnchantmentBlock(key, level, placeOf(player.getLocation()), exemptionOf(player))
                .filter(preset -> isBlockMode(preset, key))
                .map(preset -> new Denial(Message.RESTRICTION_ENCHANTING, preset, List.of(
                        Placeholder.enchantment("enchantment", key),
                        Placeholder.of("level", level)
                )))
                .flatMap(denial -> confirmed(player, null, denial));
    }

    public Optional<Denial> checkEffect(Player player, PotionEffectType type, int level) {
        if (bypasses(player, POTION_BYPASS)) {
            return Optional.empty();
        }

        String key = RegistryKeys.of(type);
        return rules.get()
                .findEffectBlock(key, level, placeOf(player.getLocation()), exemptionOf(player))
                .map(preset -> new Denial(Message.RESTRICTION_POTION, preset, List.of(
                        Placeholder.effect("effect", key),
                        Placeholder.of("level", level)
                )))
                .flatMap(denial -> confirmed(player, null, denial));
    }

    /**
     * Gives other plugins the chance to let the action through.
     *
     * @return the denial, or empty if a plugin cancelled the event
     */
    private Optional<Denial> confirmed(Player player, ItemStack item, Denial denial) {
        ItemBlockedEvent event = new ItemBlockedEvent(!Bukkit.isPrimaryThread(), player, item, restrictionOf(denial));
        events.accept(event);
        return event.isCancelled() ? Optional.empty() : Optional.of(denial);
    }

    public static Restriction restrictionOf(Denial denial) {
        return new Restriction(denial.action(), denial.subject(), denial.source().name(), denial.source().reason());
    }

    /**
     * Checks the temporary rules added for just this player.
     */
    private Optional<Denial> playerRuleDenial(Player player, ItemStack item, Action action, Place place) {
        for (Preset preset : temporaryRules.forPlayer(player.getUniqueId())) {
            if (preset.blocksItem(item.getType(), action, place, Exemption.NONE)) {
                return Optional.of(new Denial(Message.restriction(action), preset, List.of(Placeholder.item("item", item))));
            }
        }
        return Optional.empty();
    }

    /**
     * Returns the item limits that apply to the player where they are, or none if they bypass them.
     */
    public PlayerLimits limits(Player player) {
        RuleBook book = rules.get();
        if (!book.usesLimits() || bypasses(player, LIMIT_BYPASS)) {
            return PlayerLimits.NONE;
        }
        return new PlayerLimits(book, placeOf(player.getLocation()), exemptionOf(player));
    }

    /**
     * Returns whether the player ignores restrictions of the given kind, which is an action key,
     * {@value #ENCHANTMENT_BYPASS}, {@value #POTION_BYPASS} or {@value #LIMIT_BYPASS}.
     */
    public boolean bypasses(Player player, String kind) {
        Settings current = settings.get();
        return (current.bypassEnabled() && player.hasPermission(BYPASS_PERMISSION))
                || (current.perActionBypass() && player.hasPermission(BYPASS_PERMISSION + "." + kind));
    }

    /**
     * A player is exempt from a preset with {@code itemblocker.bypass.preset.<name>} and from a single
     * rule with that rule's own permission. Both have to be granted explicitly, so operators are not
     * exempt just for being operators.
     */
    private static Exemption exemptionOf(Player player) {
        return (preset, permission) -> {
            String needed = permission == null ? PRESET_BYPASS_PREFIX + preset.name() : permission;
            return player.isPermissionSet(needed) && player.hasPermission(needed);
        };
    }

    /**
     * Returns the place of a location. Regions are only looked up when a preset is limited to them.
     */
    private Place placeOf(Location location) {
        if (location == null || location.getWorld() == null) {
            return Place.inWorld(null);
        }

        String world = location.getWorld().getName();
        return rules.get().usesRegions() ? new Place(world, regions.at(location)) : Place.inWorld(world);
    }

    private Optional<Denial> itemDenial(ItemStack item, Action action, Place place, Exemption exemption) {
        RuleBook book = rules.get();
        return book.findItemBlock(item.getType(), action, place, exemption)
                .or(() -> book.findFilterBlock(item, action, place, exemption))
                .map(preset -> new Denial(Message.restriction(action), preset, List.of(
                        Placeholder.item("item", item)
                )));
    }

    private Optional<Denial> enchantedItemDenial(ItemStack item, Place place, Exemption exemption) {
        if (!item.hasItemMeta()) {
            return Optional.empty();
        }

        ItemMeta meta = item.getItemMeta();
        Optional<Denial> denial = enchantmentDenial(item, meta.getEnchants(), place, exemption);
        if (denial.isEmpty() && meta instanceof EnchantmentStorageMeta book) {
            denial = enchantmentDenial(item, book.getStoredEnchants(), place, exemption);
        }
        return denial;
    }

    private Optional<Denial> enchantmentDenial(ItemStack item, Map<Enchantment, Integer> enchantments, Place place,
                                                Exemption exemption) {
        RuleBook book = rules.get();
        for (Map.Entry<Enchantment, Integer> entry : enchantments.entrySet()) {
            String key = RegistryKeys.of(entry.getKey());
            int level = entry.getValue();
            Optional<Denial> denial = book.findEnchantmentBlock(key, level, place, exemption)
                    .filter(preset -> isBlockMode(preset, key))
                    .map(preset -> new Denial(Message.RESTRICTION_ENCHANTED_ITEM, preset, List.of(
                            Placeholder.item("item", item),
                            Placeholder.enchantment("enchantment", key),
                            Placeholder.of("level", level)
                    )));
            if (denial.isPresent()) {
                return denial;
            }
        }
        return Optional.empty();
    }

    /**
     * Returns the preset whose limit lowers, removes or confiscates the enchantment at this level for
     * the player, if any. Blocking limits are handled by the item checks instead.
     */
    public Optional<Preset> enchantmentFix(Player player, String key, int level) {
        RuleBook book = rules.get();
        if (!book.usesEnchantmentModes() || bypasses(player, ENCHANTMENT_BYPASS)) {
            return Optional.empty();
        }
        return book.findEnchantmentBlock(key, level, placeOf(player.getLocation()), exemptionOf(player))
                .filter(preset -> !isBlockMode(preset, key));
    }

    private static boolean isBlockMode(Preset preset, String key) {
        return preset.enchantments().mode(key) == EnchantmentMode.BLOCK;
    }

    private static boolean isEmpty(ItemStack item) {
        return item == null || item.getType().isAir() || item.getAmount() <= 0;
    }
}
