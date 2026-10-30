package pl.variant.itemblocker.listener;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockDispenseArmorEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import pl.variant.itemblocker.restriction.ArmorEnforcer;
import pl.variant.itemblocker.restriction.Enforcement;
import pl.variant.itemblocker.restriction.Equipment;
import pl.variant.itemblocker.restriction.WearableSlots;
import pl.variant.itemblocker.rule.Action;

import java.util.Optional;

/**
 * Putting on armor through the inventory or a dispenser, and taking off blocked armor when a player
 * joins or changes worlds. Right click equipping is handled by {@link ItemListener}.
 */
public final class ArmorListener implements Listener {

    private final Enforcement enforcement;
    private final ArmorEnforcer armorEnforcer;
    private final WearableSlots wearableSlots;

    public ArmorListener(Enforcement enforcement, ArmorEnforcer armorEnforcer, WearableSlots wearableSlots) {
        this.enforcement = enforcement;
        this.armorEnforcer = armorEnforcer;
        this.wearableSlots = wearableSlots;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getWhoClicked() instanceof Player player) {
            ItemStack equipped = itemEquippedByClick(event, player);
            if (equipped != null && enforcement.denyItem(player, equipped, Action.ARMOR, event)) {
                player.updateInventory();
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        ItemStack item = event.getOldCursor();
        Optional<EquipmentSlot> wornIn = wearableSlots.armorSlotOf(item);
        if (wornIn.isPresent() && draggedOntoArmorSlot(event, wornIn.get())
                && enforcement.denyItem(player, item, Action.ARMOR, event)) {
            player.updateInventory();
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onDispense(BlockDispenseArmorEvent event) {
        if (event.getTargetEntity() instanceof Player player) {
            enforcement.denyItem(player, event.getItem(), Action.ARMOR, event);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        armorEnforcer.removeBlockedArmor(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onWorldChange(PlayerChangedWorldEvent event) {
        armorEnforcer.removeBlockedArmor(event.getPlayer());
    }

    private ItemStack itemEquippedByClick(InventoryClickEvent event, Player player) {
        PlayerInventory inventory = player.getInventory();
        if (event.getSlotType() == InventoryType.SlotType.ARMOR) {
            ItemStack incoming = switch (event.getClick()) {
                case NUMBER_KEY -> event.getHotbarButton() < 0 ? null : inventory.getItem(event.getHotbarButton());
                case SWAP_OFFHAND -> inventory.getItemInOffHand();
                default -> event.getCursor();
            };
            Optional<EquipmentSlot> clickedSlot = Equipment.armorSlotAtIndex(event.getSlot());
            return clickedSlot.isPresent() && clickedSlot.equals(wearableSlots.armorSlotOf(incoming)) ? incoming : null;
        }

        // Shift clicking armor in the player's own inventory puts it on if the slot is free.
        InventoryType topType = event.getInventory().getType();
        boolean ownInventoryOnly = topType == InventoryType.CRAFTING || topType == InventoryType.CREATIVE;
        if (event.isShiftClick() && ownInventoryOnly && event.getClickedInventory() == inventory) {
            ItemStack clicked = event.getCurrentItem();
            Optional<EquipmentSlot> slot = wearableSlots.armorSlotOf(clicked);
            if (slot.isPresent() && isEmpty(inventory.getItem(slot.get()))) {
                return clicked;
            }
        }
        return null;
    }

    // InventoryView changed from a class to an interface in 1.21, so calling it would break the jar
    // on 1.20. Only the player's own inventory view shows armor slots, at fixed raw slots.
    private static boolean draggedOntoArmorSlot(InventoryDragEvent event, EquipmentSlot wornIn) {
        return event.getInventory().getType() == InventoryType.CRAFTING
                && event.getRawSlots().stream()
                .anyMatch(rawSlot -> Equipment.armorSlotAtRawSlot(rawSlot).equals(Optional.of(wornIn)));
    }

    private static boolean isEmpty(ItemStack item) {
        return item == null || item.getType().isAir();
    }
}
