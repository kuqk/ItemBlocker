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

import java.io.UncheckedIOException;
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

    private record Fix(ItemStack item, List<Denial> notices) {
    }

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
        for (int slot = 0; slot < contents.length; slot++) {
            ItemStack item = contents[slot];
            if (item == null || item.getType().isAir() || !item.hasItemMeta()) {
                continue;
            }
            Optional<Fix> fixed;
            try {
                fixed = fix(player, item);
            } catch (UncheckedIOException exception) {
                // The storage already logged the failure. Leave this and all remaining items intact.
                return;
            }
            if (fixed.isPresent() && !fixed.get().item().equals(item)) {
                // Commit before notifying: a failed expansion or logger must not leave an item
                // both in the source inventory and in the confiscation file.
                ItemStack result = fixed.get().item();
                inventory.setItem(slot, result.getType().isAir() ? null : result);
                fixed.get().notices().forEach(notice -> enforcement.notify(player, notice));
            }
        }
    }

    /**
     * Computes a replacement and notices, or empty if nothing changes. Notices are sent only
     * after the replacement has been committed to the inventory.
     */
    private Optional<Fix> fix(Player player, ItemStack item) {
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
                Fix result = new Fix(new ItemStack(Material.AIR), List.of(
                        notice(Message.RESTRICTION_ENCHANTMENT_CONFISCATED, preset.get(), item, key, entry.getValue())));
                confiscated.add(player.getName(), List.of(item.clone()));
                return Optional.of(result);
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
        return Optional.of(new Fix(fixed, notices));
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
