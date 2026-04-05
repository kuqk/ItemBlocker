package pl.variant.listeners;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryAction;
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
import pl.variant.utils.EquipmentUtils;

import java.util.EnumSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class ArmorListener implements Listener {

    private static final EnumSet<EquipmentSlot> ARMOR_SLOTS = EnumSet.of(
            EquipmentSlot.HEAD,
            EquipmentSlot.CHEST,
            EquipmentSlot.LEGS,
            EquipmentSlot.FEET
    );

    private final itemBlocker plugin;
    private final Set<UUID> armorAdjustmentPlayers = ConcurrentHashMap.newKeySet();

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

        boolean blocked = plugin.getBlockService().blockIfNeeded(player, item, BlockAction.ARMOR, event);
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

    public void handleArmorChange(Player player, EquipmentSlot slot, ItemStack oldItem, ItemStack newItem) {
        if (player == null || armorAdjustmentPlayers.contains(player.getUniqueId())) {
            return;
        }

        if (!EquipmentUtils.isWearable(newItem)) {
            return;
        }

        var decision = plugin.getBlockService().inspect(player, newItem, BlockAction.ARMOR);
        if (!decision.blocked()) {
            return;
        }

        revertBlockedArmorEquip(player, slot, oldItem, newItem, decision);
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

        if (!event.isShiftClick() || event.getAction() != InventoryAction.MOVE_TO_OTHER_INVENTORY) {
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
            // Foreign inventories are ambiguous here: the player may only be trying to take
            // the item out of a UI, not explicitly equip it. Let the armor change event handle
            // the cases that really end up wearing the item.
            return false;
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

            var decision = plugin.getBlockService().inspect(player, equipped, BlockAction.ARMOR);
            if (!decision.blocked()) {
                continue;
            }

            EquipmentUtils.setEquippedItem(inventory, slot, null);
            storeInInventoryOrDrop(player, equipped.clone());
            plugin.getBlockService().sendBlockedDecision(player, decision);
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
            pl.variant.services.BlockService.ItemBlockDecision decision
    ) {
        if (slot == null) {
            return;
        }

        UUID playerId = player.getUniqueId();
        if (!armorAdjustmentPlayers.add(playerId)) {
            return;
        }

        try {
            EquipmentUtils.setEquippedItem(player.getInventory(), slot, cloneOrNull(oldItem));
            restoreBlockedArmorItem(player, newItem);
            plugin.getBlockService().sendBlockedDecision(player, decision);
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

        if (storeInFirstEmptyStorageSlot(inventory, restored)) {
            return;
        }

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

        if (storeInFirstEmptyStorageSlot(player.getInventory(), item)) {
            return;
        }

        player.getWorld().dropItemNaturally(player.getLocation(), item);
    }

    private boolean storeInFirstEmptyStorageSlot(PlayerInventory inventory, ItemStack item) {
        if (inventory == null || item == null || item.getType().isAir()) {
            return false;
        }

        ItemStack[] storage = inventory.getStorageContents();
        for (int index = 0; index < storage.length; index++) {
            ItemStack existing = storage[index];
            if (existing != null && !existing.getType().isAir()) {
                continue;
            }

            storage[index] = item;
            inventory.setStorageContents(storage);
            return true;
        }

        return false;
    }
}
