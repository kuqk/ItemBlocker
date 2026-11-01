package pl.variant.itemblocker.gui;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

/**
 * A chest menu. The menu is the holder of its inventory, which is how {@link MenuListener} finds the
 * menu a click belongs to. Players can never take items out of a menu.
 */
public abstract class Menu implements InventoryHolder {

    private final int rows;
    private final String title;
    private Inventory inventory;

    protected Menu(int rows, String title) {
        this.rows = rows;
        this.title = title;
    }

    /**
     * Returns the menu's inventory, created on first use so that the menu is fully constructed
     * before it is handed to the server as the holder.
     */
    @Override
    public Inventory getInventory() {
        if (inventory == null) {
            inventory = Bukkit.createInventory(this, rows * 9, title);
        }
        return inventory;
    }

    /**
     * Fills the inventory with the current state.
     */
    protected abstract void render();

    /**
     * Handles a click on a slot of the menu.
     */
    protected abstract void click(Player player, int slot, ClickType click);

    /**
     * Handles a click on an item in the player's own inventory while the menu is open.
     */
    protected void clickOwnItem(Player player, ItemStack item) {
    }

    void open(Player player) {
        render();
        player.openInventory(getInventory());
    }
}
