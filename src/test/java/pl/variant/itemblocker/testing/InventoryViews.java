package pl.variant.itemblocker.testing;

import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.inventory.InventoryType.SlotType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.util.List;

/**
 * Inventory views with the raw slot layout of a real server. MockBukkit does not implement slot
 * conversion, which inventory click and drag events need.
 */
public final class InventoryViews {

    private static final int PLAYER_VIEW_TOP_SIZE = 5;

    private InventoryViews() {
    }

    /**
     * The player's own inventory: 0 crafting result, 1-4 crafting grid, 5-8 armor (head to feet),
     * 9-35 storage, 36-44 hotbar.
     */
    public static InventoryView playerInventory(Player player) {
        InventoryView delegate = player.getOpenInventory();
        Inventory crafting = delegate.getTopInventory() != null ? delegate.getTopInventory() : craftingGrid();
        return view(delegate, crafting, player, PLAYER_VIEW_TOP_SIZE, true);
    }

    /**
     * A container above the player's inventory: container slots first, then 27 storage slots, then
     * the 9 hotbar slots.
     */
    public static InventoryView container(Player player, Inventory top) {
        return view(player.getOpenInventory(), top, player, top.getSize(), false);
    }

    /**
     * A view of a specific type, such as an enchanting table view, that behaves like the player's
     * open inventory. For events that require a typed view but whose listeners do not use it.
     */
    public static <T extends InventoryView> T typed(Class<T> type, Player player) {
        InventoryView delegate = player.getOpenInventory();
        return type.cast(Proxy.newProxyInstance(
                type.getClassLoader(),
                new Class<?>[]{type},
                (proxy, method, args) -> {
                    try {
                        return method.invoke(delegate, args);
                    } catch (InvocationTargetException exception) {
                        throw exception.getCause();
                    }
                }
        ));
    }

    private static InventoryView view(InventoryView delegate, Inventory top, Player player, int topSize, boolean playerView) {
        return (InventoryView) Proxy.newProxyInstance(
                InventoryView.class.getClassLoader(),
                new Class<?>[]{InventoryView.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "getTopInventory" -> top;
                    case "getBottomInventory" -> player.getInventory();
                    case "getPlayer" -> player;
                    case "getType" -> top.getType();
                    case "countSlots" -> topSize + 36;
                    case "getCursor" -> player.getItemOnCursor();
                    case "convertSlot" -> convert((int) args[0], topSize, playerView);
                    case "getSlotType" -> slotType((int) args[0], topSize, playerView, top.getType());
                    case "getInventory" -> (int) args[0] < topSize ? top : player.getInventory();
                    case "getItem" -> inventoryAt((int) args[0], top, player, topSize)
                            .getItem(convert((int) args[0], topSize, playerView));
                    case "setItem" -> {
                        inventoryAt((int) args[0], top, player, topSize)
                                .setItem(convert((int) args[0], topSize, playerView), (ItemStack) args[1]);
                        yield null;
                    }
                    default -> {
                        try {
                            yield method.invoke(delegate, args);
                        } catch (InvocationTargetException exception) {
                            throw exception.getCause();
                        }
                    }
                }
        );
    }

    /**
     * The 2x2 crafting grid of the player's own inventory, which MockBukkit cannot create.
     */
    private static Inventory craftingGrid() {
        return (Inventory) Proxy.newProxyInstance(
                Inventory.class.getClassLoader(),
                new Class<?>[]{Inventory.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "getType" -> InventoryType.CRAFTING;
                    case "getSize" -> PLAYER_VIEW_TOP_SIZE;
                    case "getItem" -> null;
                    case "getViewers" -> List.of();
                    default -> throw new UnsupportedOperationException(method.getName());
                }
        );
    }

    private static Inventory inventoryAt(int rawSlot, Inventory top, Player player, int topSize) {
        return rawSlot < topSize ? top : player.getInventory();
    }

    private static int convert(int rawSlot, int topSize, boolean playerView) {
        if (rawSlot < topSize) {
            return rawSlot;
        }

        if (playerView) {
            if (rawSlot <= 8) {
                return 39 - (rawSlot - 5);
            }
            return rawSlot <= 35 ? rawSlot : rawSlot - 36;
        }

        int bottom = rawSlot - topSize;
        return bottom < 27 ? bottom + 9 : bottom - 27;
    }

    private static SlotType slotType(int rawSlot, int topSize, boolean playerView, InventoryType topType) {
        if (playerView) {
            if (rawSlot == 0) {
                return SlotType.RESULT;
            }
            if (rawSlot <= 4) {
                return SlotType.CRAFTING;
            }
            if (rawSlot <= 8) {
                return SlotType.ARMOR;
            }
            return rawSlot <= 35 ? SlotType.CONTAINER : SlotType.QUICKBAR;
        }

        if (rawSlot < topSize) {
            boolean result = rawSlot == 0 && (topType == InventoryType.WORKBENCH || topType == InventoryType.CRAFTING);
            return result ? SlotType.RESULT : SlotType.CONTAINER;
        }
        return rawSlot - topSize < 27 ? SlotType.CONTAINER : SlotType.QUICKBAR;
    }
}
