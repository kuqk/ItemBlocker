package pl.variant.itemblocker.api.event;

import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.ItemStack;
import pl.variant.itemblocker.api.Restriction;

/**
 * Called when ItemBlocker is about to block something for a player. Cancelling it lets the action
 * through, for example for a player in a quest or a minigame that has its own rules.
 */
public class ItemBlockedEvent extends Event implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();

    private final Player player;
    private final ItemStack item;
    private final Restriction restriction;
    private boolean cancelled;

    public ItemBlockedEvent(boolean async, Player player, ItemStack item, Restriction restriction) {
        super(async);
        this.player = player;
        this.item = item;
        this.restriction = restriction;
    }

    public Player getPlayer() {
        return player;
    }

    /**
     * The item involved, or {@code null} for enchanting and potion effects.
     */
    public ItemStack getItem() {
        return item == null ? null : item.clone();
    }

    public Restriction getRestriction() {
        return restriction;
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
