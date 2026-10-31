package pl.variant.itemblocker.restriction;

import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffectType;
import pl.variant.itemblocker.config.Settings;
import pl.variant.itemblocker.message.Message;
import pl.variant.itemblocker.message.Names;
import pl.variant.itemblocker.message.Placeholder;
import pl.variant.itemblocker.rule.Action;
import pl.variant.itemblocker.rule.RegistryKeys;
import pl.variant.itemblocker.rule.RuleBook;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * Decides whether something is blocked, taking bypass permissions into account.
 */
public final class RestrictionService {

    public static final String BYPASS_PERMISSION = "itemblocker.bypass";
    public static final String ENCHANTMENT_BYPASS = "enchantment";
    public static final String POTION_BYPASS = "potion";

    private final Supplier<RuleBook> rules;
    private final Supplier<Settings> settings;

    public RestrictionService(Supplier<RuleBook> rules, Supplier<Settings> settings) {
        this.rules = rules;
        this.settings = settings;
    }

    /**
     * Checks whether the player may perform the action with the item. An item carrying a blocked
     * enchantment is blocked for every action.
     */
    public Optional<Denial> checkItem(Player player, ItemStack item, Action action) {
        if (isEmpty(item)) {
            return Optional.empty();
        }

        String world = player.getWorld().getName();
        Optional<Denial> denial = bypasses(player, action.key())
                ? Optional.empty()
                : itemDenial(item, action, world);
        return denial.isPresent() ? denial : checkEnchantedItem(player, item);
    }

    /**
     * Checks an item moved without a player, such as by a hopper. Bypass permissions do not apply.
     */
    public Optional<Denial> checkItem(ItemStack item, Action action, String world) {
        if (isEmpty(item)) {
            return Optional.empty();
        }

        Optional<Denial> denial = itemDenial(item, action, world);
        return denial.isPresent() ? denial : enchantedItemDenial(item, world);
    }

    /**
     * Checks whether the item carries an enchantment the player may not have.
     */
    public Optional<Denial> checkEnchantedItem(Player player, ItemStack item) {
        if (isEmpty(item) || bypasses(player, ENCHANTMENT_BYPASS)) {
            return Optional.empty();
        }
        return enchantedItemDenial(item, player.getWorld().getName());
    }

    public Optional<Denial> checkEnchanting(Player player, Enchantment enchantment, int level) {
        if (bypasses(player, ENCHANTMENT_BYPASS)) {
            return Optional.empty();
        }

        String key = RegistryKeys.of(enchantment);
        return rules.get()
                .findEnchantmentBlock(key, level, player.getWorld().getName())
                .map(preset -> new Denial(Message.RESTRICTION_ENCHANTING, preset, List.of(
                        Placeholder.of("enchantment", Names.ofKey(key)),
                        Placeholder.of("level", level)
                )));
    }

    public Optional<Denial> checkEffect(Player player, PotionEffectType type, int level) {
        if (bypasses(player, POTION_BYPASS)) {
            return Optional.empty();
        }

        String key = RegistryKeys.of(type);
        return rules.get()
                .findEffectBlock(key, level, player.getWorld().getName())
                .map(preset -> new Denial(Message.RESTRICTION_POTION, preset, List.of(
                        Placeholder.of("effect", Names.ofKey(key)),
                        Placeholder.of("level", level)
                )));
    }

    /**
     * Returns whether the player ignores restrictions of the given kind, which is an action key,
     * {@value #ENCHANTMENT_BYPASS} or {@value #POTION_BYPASS}.
     */
    public boolean bypasses(Player player, String kind) {
        Settings current = settings.get();
        return (current.bypassEnabled() && player.hasPermission(BYPASS_PERMISSION))
                || (current.perActionBypass() && player.hasPermission(BYPASS_PERMISSION + "." + kind));
    }

    private Optional<Denial> itemDenial(ItemStack item, Action action, String world) {
        return rules.get()
                .findItemBlock(item.getType(), action, world)
                .map(preset -> new Denial(Message.restriction(action), preset, List.of(
                        Placeholder.of("item", Names.of(item))
                )));
    }

    private Optional<Denial> enchantedItemDenial(ItemStack item, String world) {
        if (!item.hasItemMeta()) {
            return Optional.empty();
        }

        ItemMeta meta = item.getItemMeta();
        Optional<Denial> denial = enchantmentDenial(item, meta.getEnchants(), world);
        if (denial.isEmpty() && meta instanceof EnchantmentStorageMeta book) {
            denial = enchantmentDenial(item, book.getStoredEnchants(), world);
        }
        return denial;
    }

    private Optional<Denial> enchantmentDenial(ItemStack item, Map<Enchantment, Integer> enchantments, String world) {
        RuleBook book = rules.get();
        for (Map.Entry<Enchantment, Integer> entry : enchantments.entrySet()) {
            String key = RegistryKeys.of(entry.getKey());
            int level = entry.getValue();
            Optional<Denial> denial = book.findEnchantmentBlock(key, level, world)
                    .map(preset -> new Denial(Message.RESTRICTION_ENCHANTED_ITEM, preset, List.of(
                            Placeholder.of("item", Names.of(item)),
                            Placeholder.of("enchantment", Names.ofKey(key)),
                            Placeholder.of("level", level)
                    )));
            if (denial.isPresent()) {
                return denial;
            }
        }
        return Optional.empty();
    }

    private static boolean isEmpty(ItemStack item) {
        return item == null || item.getType().isAir() || item.getAmount() <= 0;
    }
}
