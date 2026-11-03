package pl.variant.itemblocker.restriction;

import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.ItemMeta;
import pl.variant.itemblocker.message.Message;
import pl.variant.itemblocker.message.Placeholder;
import pl.variant.itemblocker.rule.EnchantmentMode;
import pl.variant.itemblocker.rule.Preset;
import pl.variant.itemblocker.rule.RegistryKeys;
import pl.variant.itemblocker.scan.ConfiscatedItems;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Applies enchantment limits that do not block: lowers the enchantment to the highest allowed level,
 * takes it off, or confiscates the whole item. Enchanted books are handled the same way through
 * their stored enchantments.
 */
public final class EnchantmentFixer {

    private final Enforcement enforcement;
    private final ConfiscatedItems confiscated;

    public EnchantmentFixer(Enforcement enforcement, ConfiscatedItems confiscated) {
        this.enforcement = enforcement;
        this.confiscated = confiscated;
    }

    /**
     * Fixes every item the player carries, including armor and the off hand.
     */
    public void fixInventory(Player player) {
        PlayerInventory inventory = player.getInventory();
        ItemStack[] contents = inventory.getContents();
        boolean changed = false;
        for (int slot = 0; slot < contents.length; slot++) {
            ItemStack item = contents[slot];
            if (item == null || item.getType().isAir() || !item.hasItemMeta()) {
                continue;
            }
            Optional<ItemStack> fixed = fix(player, item);
            if (fixed.isPresent() && !fixed.get().equals(item)) {
                contents[slot] = fixed.get().getType().isAir() ? null : fixed.get();
                changed = true;
            }
        }
        if (changed) {
            inventory.setContents(contents);
        }
    }

    /**
     * Returns the item as it may stay, an air item if it is confiscated, or empty if nothing changes.
     */
    Optional<ItemStack> fix(Player player, ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        boolean stored = meta instanceof EnchantmentStorageMeta;
        Map<Enchantment, Integer> enchantments = stored ? ((EnchantmentStorageMeta) meta).getStoredEnchants() : meta.getEnchants();
        boolean changed = false;
        List<Denial> notices = new ArrayList<>();

        for (Map.Entry<Enchantment, Integer> entry : enchantments.entrySet()) {
            String key = RegistryKeys.of(entry.getKey());
            Optional<Preset> preset = enforcement.restrictions().enchantmentFix(player, key, entry.getValue());
            if (preset.isEmpty()) {
                continue;
            }

            EnchantmentMode mode = preset.get().enchantments().mode(key);
            int allowed = preset.get().enchantments().minimumLevel(key).orElse(1) - 1;
            if (mode == EnchantmentMode.CONFISCATE) {
                confiscated.add(player.getName(), List.of(item.clone()));
                enforcement.notify(player, notice(Message.RESTRICTION_ENCHANTMENT_CONFISCATED, preset.get(), item, key, entry.getValue()));
                return Optional.of(new ItemStack(Material.AIR));
            }

            remove(meta, stored, entry.getKey());
            if (mode == EnchantmentMode.DOWNGRADE && allowed >= 1) {
                add(meta, stored, entry.getKey(), allowed);
                notices.add(notice(Message.RESTRICTION_ENCHANTMENT_DOWNGRADED, preset.get(), item, key, allowed));
            } else {
                notices.add(notice(Message.RESTRICTION_ENCHANTMENT_REMOVED, preset.get(), item, key, entry.getValue()));
            }
            changed = true;
        }

        if (!changed) {
            return Optional.empty();
        }
        ItemStack fixed = item.clone();
        fixed.setItemMeta(meta);
        notices.forEach(notice -> enforcement.notify(player, notice));
        return Optional.of(fixed);
    }

    private static void remove(ItemMeta meta, boolean stored, Enchantment enchantment) {
        if (stored) {
            ((EnchantmentStorageMeta) meta).removeStoredEnchant(enchantment);
        } else {
            meta.removeEnchant(enchantment);
        }
    }

    private static void add(ItemMeta meta, boolean stored, Enchantment enchantment, int level) {
        if (stored) {
            ((EnchantmentStorageMeta) meta).addStoredEnchant(enchantment, level, true);
        } else {
            meta.addEnchant(enchantment, level, true);
        }
    }

    private static Denial notice(Message message, Preset preset, ItemStack item, String key, int level) {
        return new Denial(message, preset, List.of(
                Placeholder.item("item", item),
                Placeholder.enchantment("enchantment", key),
                Placeholder.of("level", level)));
    }
}
