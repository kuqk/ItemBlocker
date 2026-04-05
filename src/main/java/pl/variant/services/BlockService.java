package pl.variant.services;

import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffectType;
import pl.variant.itemBlocker;
import pl.variant.model.BlockAction;
import pl.variant.model.BlockCheckResult;
import pl.variant.utils.TextUtils;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

public class BlockService {

    private final itemBlocker plugin;

    public BlockService(itemBlocker plugin) {
        this.plugin = plugin;
    }

    public ItemBlockDecision inspect(Player player, ItemStack item, BlockAction action) {
        if (player == null || isEmpty(item)) {
            return ItemBlockDecision.allowed();
        }

        if (plugin.getConfigManager().canBypassAll(player)) {
            return ItemBlockDecision.allowed();
        }

        String worldName = player.getWorld().getName();
        if (!plugin.getConfigManager().canBypass(player, action)) {
            BlockCheckResult actionResult = plugin.getBlockedItemsManager().check(item.getType(), action, worldName);
            if (actionResult.isBlocked()) {
                return createItemActionDecision(item, action, actionResult);
            }
        }

        if (!plugin.getConfigManager().canBypass(player, "enchantment")) {
            ItemBlockDecision enchantmentDecision = inspectEnchantments(item, worldName);
            if (enchantmentDecision.blocked()) {
                return enchantmentDecision;
            }
        }

        return ItemBlockDecision.allowed();
    }

    public ItemBlockDecision inspect(ItemStack item, BlockAction action, String worldName) {
        if (isEmpty(item)) {
            return ItemBlockDecision.allowed();
        }

        BlockCheckResult actionResult = plugin.getBlockedItemsManager().check(item.getType(), action, worldName);
        if (actionResult.isBlocked()) {
            return createItemActionDecision(item, action, actionResult);
        }

        ItemBlockDecision enchantmentDecision = inspectEnchantments(item, worldName);
        if (enchantmentDecision.blocked()) {
            return enchantmentDecision;
        }

        return ItemBlockDecision.allowed();
    }

    public BlockCheckResult checkEnchantment(Player player, Enchantment enchantment, int level) {
        if (player == null || enchantment == null) {
            return BlockCheckResult.allowed();
        }

        if (plugin.getConfigManager().canBypass(player, "enchantment")) {
            return BlockCheckResult.allowed();
        }

        return plugin.getBlockedItemsManager().checkEnchantment(enchantment, level, player.getWorld().getName());
    }

    public BlockCheckResult checkPotionEffect(Player player, PotionEffectType effectType, int level) {
        if (player == null || effectType == null) {
            return BlockCheckResult.allowed();
        }

        if (plugin.getConfigManager().canBypass(player, "potion")) {
            return BlockCheckResult.allowed();
        }

        return plugin.getBlockedItemsManager().checkPotionEffect(effectType, level, player.getWorld().getName());
    }

    public boolean blockIfNeeded(Player player, ItemStack item, BlockAction action, Cancellable cancellable) {
        ItemBlockDecision decision = inspect(player, item, action);
        if (!decision.blocked()) {
            return false;
        }

        cancellable.setCancelled(true);
        sendBlockedDecision(player, decision);
        return true;
    }

    public void sendBlockedDecision(Player player, ItemBlockDecision decision) {
        if (player == null || decision == null || !decision.blocked()) {
            return;
        }

        plugin.getMessageManager().sendCustomBlockedMessage(
                player,
                decision.messageKey(),
                decision.placeholders(),
                decision.result()
        );
    }

    private ItemBlockDecision inspectEnchantments(ItemStack item, String worldName) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return ItemBlockDecision.allowed();
        }

        Optional<ItemBlockDecision> directEnchantments = findBlockedEnchantment(item, meta.getEnchants(), worldName);
        if (directEnchantments.isPresent()) {
            return directEnchantments.get();
        }

        if (meta instanceof EnchantmentStorageMeta storageMeta) {
            Optional<ItemBlockDecision> storedEnchantments = findBlockedEnchantment(item, storageMeta.getStoredEnchants(), worldName);
            if (storedEnchantments.isPresent()) {
                return storedEnchantments.get();
            }
        }

        return ItemBlockDecision.allowed();
    }

    private Optional<ItemBlockDecision> findBlockedEnchantment(
            ItemStack item,
            Map<Enchantment, Integer> enchantments,
            String worldName
    ) {
        for (Map.Entry<Enchantment, Integer> entry : enchantments.entrySet()) {
            BlockCheckResult result = plugin.getBlockedItemsManager().checkEnchantment(entry.getKey(), entry.getValue(), worldName);
            if (result.isBlocked()) {
                return Optional.of(createEnchantmentDecision(item, entry.getKey(), entry.getValue(), result));
            }
        }

        return Optional.empty();
    }

    private ItemBlockDecision createItemActionDecision(ItemStack item, BlockAction action, BlockCheckResult result) {
        return new ItemBlockDecision(
                true,
                result,
                action.getMessageKey(),
                Map.of(
                        "{item}", item.getType().name(),
                        "{item_pretty}", TextUtils.formatEnumName(item.getType().name())
                )
        );
    }

    private ItemBlockDecision createEnchantmentDecision(
            ItemStack item,
            Enchantment enchantment,
            int level,
            BlockCheckResult result
    ) {
        String key = enchantment.getKey().getKey();
        return new ItemBlockDecision(
                true,
                result,
                "blocked-enchantment-item",
                Map.of(
                        "{item}", item.getType().name(),
                        "{item_pretty}", TextUtils.formatEnumName(item.getType().name()),
                        "{enchantment}", key,
                        "{enchantment_pretty}", formatPrettyKey(key),
                        "{level}", String.valueOf(level)
                )
        );
    }

    private boolean isEmpty(ItemStack item) {
        return item == null || item.getType() == Material.AIR;
    }

    private String formatPrettyKey(String key) {
        return TextUtils.formatEnumName((key == null ? "" : key).toUpperCase());
    }

    public record ItemBlockDecision(
            boolean blocked,
            BlockCheckResult result,
            String messageKey,
            Map<String, String> placeholders
    ) {
        private static final ItemBlockDecision ALLOWED = new ItemBlockDecision(false, BlockCheckResult.allowed(), "", Map.of());

        public static ItemBlockDecision allowed() {
            return ALLOWED;
        }

        public ItemBlockDecision {
            placeholders = placeholders == null ? Map.of() : Map.copyOf(new LinkedHashMap<>(placeholders));
        }
    }
}
