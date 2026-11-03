package pl.variant.itemblocker.listener;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;
import pl.variant.itemblocker.message.Message;
import pl.variant.itemblocker.restriction.Denial;
import pl.variant.itemblocker.restriction.Enforcement;
import pl.variant.itemblocker.rule.Action;

/**
 * Attacking with an item, and breaking blocks: either a blocked block, or any block with a blocked
 * tool.
 */
public final class CombatListener implements Listener {

    private final Enforcement enforcement;

    public CombatListener(Enforcement enforcement) {
        this.enforcement = enforcement;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onAttack(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Player player) {
            ItemStack weapon = player.getInventory().getItemInMainHand();
            if (!weapon.getType().isAir()) {
                enforcement.denyItem(player, weapon, Action.ATTACK, event);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        Material block = event.getBlock().getType();
        if (block.isItem() && enforcement.denyItem(player, new ItemStack(block), Action.BREAK, event)) {
            return;
        }

        ItemStack tool = player.getInventory().getItemInMainHand();
        if (tool.getType().isAir()) {
            return;
        }
        enforcement.restrictions().checkItem(player, tool, Action.BREAK).ifPresent(denial -> {
            event.setCancelled(true);
            // The rule is on the tool, not on the block, which the message has to say.
            Denial withTool = denial.message() == Message.RESTRICTION_BREAK
                    ? new Denial(Message.RESTRICTION_BREAK_WITH, denial.source(), denial.placeholders())
                    : denial;
            enforcement.notify(player, withTool);
        });
    }
}
