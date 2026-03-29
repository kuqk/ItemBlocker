package pl.variant.listeners;

import com.destroystokyo.paper.event.player.PlayerArmorChangeEvent;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import pl.variant.itemBlocker;
import pl.variant.model.BlockAction;
import pl.variant.model.BlockCheckResult;
import pl.variant.utils.EquipmentUtils;

import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class ArmorListener implements Listener {

    private static final EnumSet<EquipmentSlot> ARMOR_SLOTS = EnumSet.of(
            EquipmentSlot.HEAD,
            EquipmentSlot.CHEST,
            EquipmentSlot.LEGS,
            EquipmentSlot.FEET
    );

    private final itemBlocker plugin;
    private final Set<UUID> armorAdjustmentPlayers = new HashSet<>();

    public ArmorListener(itemBlocker plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onArmorEquip(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        ItemStack item = resolveArmorEquipItem(event, player);
        if (item == null) {
            return;
        }

        boolean blocked = plugin.getBlockService().blockIfNeeded(player, item.getType(), BlockAction.ARMOR, event);
        if (blocked) {
            player.updateInventory();
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onWorldChange(PlayerChangedWorldEvent event) {
        enforceArmorRestrictions(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onJoin(PlayerJoinEvent event) {
        enforceArmorRestrictions(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPaperArmorChange(PlayerArmorChangeEvent event) {
        Player player = event.getPlayer();
        if (armorAdjustmentPlayers.contains(player.getUniqueId())) {
            return;
        }

        ItemStack newItem = event.getNewItem();
        if (!EquipmentUtils.isWearable(newItem)) {
            return;
        }

        BlockCheckResult result = plugin.getBlockService().check(player, newItem.getType(), BlockAction.ARMOR);
        if (!result.isBlocked()) {
            return;
        }

        revertBlockedArmorEquip(player, event.getSlot(), event.getOldItem(), newItem, result);
    }

    private ItemStack resolveArmorEquipItem(InventoryClickEvent event, Player player) {
        if (event.getSlotType() == InventoryType.SlotType.ARMOR) {
            if (event.getClick() == ClickType.NUMBER_KEY) {
                int hotbarButton = event.getHotbarButton();
                if (hotbarButton < 0) {
                    return null;
                }

                ItemStack hotbarItem = player.getInventory().getItem(hotbarButton);
                return isMatchingArmorSlot(event.getSlot(), hotbarItem) ? hotbarItem : null;
            }

            ItemStack cursor = event.getCursor();
            return isMatchingArmorSlot(event.getSlot(), cursor) ? cursor : null;
        }

        if (!event.isShiftClick()) {
            return null;
        }

        if (!canShiftClickEquip(event, player)) {
            return null;
        }

        ItemStack currentItem = event.getCurrentItem();
        EquipmentSlot slot = EquipmentUtils.getWearableSlot(currentItem);
        if (slot == null) {
            return null;
        }

        PlayerInventory inventory = player.getInventory();
        return EquipmentUtils.isArmorSlotEmpty(inventory, slot) ? currentItem : null;
    }

    private boolean isMatchingArmorSlot(int clickedSlot, ItemStack item) {
        if (!EquipmentUtils.isWearable(item)) {
            return false;
        }

        EquipmentSlot clickedArmorSlot = EquipmentUtils.getArmorSlotByInventoryIndex(clickedSlot);
        return clickedArmorSlot != null && clickedArmorSlot == EquipmentUtils.getWearableSlot(item);
    }

    private boolean canShiftClickEquip(InventoryClickEvent event, Player player) {
        if (event.getClickedInventory() == null) {
            return false;
        }

        if (event.getClickedInventory() != player.getInventory()) {
            return true;
        }

        InventoryType topType = event.getView().getTopInventory().getType();
        return topType == InventoryType.CRAFTING
                || topType == InventoryType.CREATIVE
                || topType == InventoryType.PLAYER;
    }

    private void enforceArmorRestrictions(Player player) {
        PlayerInventory inventory = player.getInventory();
        boolean changed = false;

        for (EquipmentSlot slot : ARMOR_SLOTS) {
            ItemStack equipped = EquipmentUtils.getEquippedItem(inventory, slot);
            if (!EquipmentUtils.isWearable(equipped)) {
                continue;
            }

            BlockCheckResult result = plugin.getBlockService().check(player, equipped.getType(), BlockAction.ARMOR);
            if (!result.isBlocked()) {
                continue;
            }

            EquipmentUtils.setEquippedItem(inventory, slot, null);
            storeInInventoryOrDrop(player, equipped.clone());
            plugin.getMessageManager().sendBlockedMessage(player, BlockAction.ARMOR, equipped.getType(), result);
            changed = true;
        }

        if (changed) {
            player.updateInventory();
        }
    }

    private void revertBlockedArmorEquip(
            Player player,
            EquipmentSlot slot,
            ItemStack oldItem,
            ItemStack newItem,
            BlockCheckResult result
    ) {
        UUID playerId = player.getUniqueId();
        if (!armorAdjustmentPlayers.add(playerId)) {
            return;
        }

        try {
            EquipmentUtils.setEquippedItem(player.getInventory(), slot, cloneOrNull(oldItem));
            restoreBlockedArmorItem(player, newItem);
            plugin.getMessageManager().sendBlockedMessage(player, BlockAction.ARMOR, newItem.getType(), result);
            player.updateInventory();
        } finally {
            armorAdjustmentPlayers.remove(playerId);
        }
    }

    private void restoreBlockedArmorItem(Player player, ItemStack item) {
        if (item == null || item.getType().isAir()) {
            return;
        }

        PlayerInventory inventory = player.getInventory();
        ItemStack restored = item.clone();

        ItemStack mainHand = inventory.getItemInMainHand();
        if (mainHand == null || mainHand.getType().isAir()) {
            inventory.setItemInMainHand(restored);
            return;
        }

        ItemStack offHand = inventory.getItemInOffHand();
        if (offHand == null || offHand.getType().isAir()) {
            inventory.setItemInOffHand(restored);
            return;
        }

        storeInInventoryOrDrop(player, restored);
    }

    private ItemStack cloneOrNull(ItemStack item) {
        if (item == null || item.getType().isAir()) {
            return null;
        }

        return item.clone();
    }

    private void storeInInventoryOrDrop(Player player, ItemStack item) {
        if (item == null || item.getType().isAir()) {
            return;
        }

        PlayerInventory inventory = player.getInventory();
        ItemStack[] storage = inventory.getStorageContents();

        for (int index = 0; index < storage.length; index++) {
            ItemStack existing = storage[index];
            if (existing != null && !existing.getType().isAir()) {
                continue;
            }

            storage[index] = item;
            inventory.setStorageContents(storage);
            return;
        }

        player.getWorld().dropItemNaturally(player.getLocation(), item);
    }
}
