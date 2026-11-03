package pl.variant.itemblocker.listener;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.damage.DamageSource;
import org.bukkit.damage.DamageType;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockDispenseEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.world.LootGenerateEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.loot.LootContext;
import org.bukkit.potion.PotionType;
import org.bukkit.util.Vector;
import org.junit.jupiter.api.Test;
import pl.variant.itemblocker.rule.Action;
import pl.variant.itemblocker.rule.ItemRule;
import pl.variant.itemblocker.rule.PotionContents;
import pl.variant.itemblocker.rule.WorldScope;
import pl.variant.itemblocker.testing.PluginTest;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NewActionsTest extends PluginTest {

    @Test
    @SuppressWarnings({"deprecation", "removal"})
    void attackingWithABlockedWeaponIsDenied() {
        block(Material.MACE, Action.ATTACK);
        LivingEntity zombie = (LivingEntity) player.getWorld().spawnEntity(player.getLocation(), EntityType.ZOMBIE);
        player.getInventory().setItemInMainHand(new ItemStack(Material.MACE));

        EntityDamageByEntityEvent event = new EntityDamageByEntityEvent(player, zombie, DamageCause.ENTITY_ATTACK, 5);
        server.getPluginManager().callEvent(event);

        assertTrue(event.isCancelled());
        assertEquals(List.of("[ItemBlocker] You cannot attack with Mace."), messages());
    }

    @Test
    void breakingABlockedBlockIsDenied() {
        block(Material.SPAWNER, Action.BREAK);

        assertTrue(breakBlock(blockAt(0, Material.SPAWNER)).isCancelled());
        assertFalse(breakBlock(blockAt(1, Material.STONE)).isCancelled());
        assertEquals(List.of("[ItemBlocker] You cannot break Spawner."), messages());
    }

    @Test
    void breakingWithABlockedToolIsDenied() {
        block(Material.NETHERITE_PICKAXE, Action.BREAK);
        Block stone = blockAt(1, Material.STONE);

        player.getInventory().setItemInMainHand(new ItemStack(Material.NETHERITE_PICKAXE));
        assertTrue(breakBlock(stone).isCancelled());
        assertEquals(List.of("[ItemBlocker] You cannot break blocks with Netherite Pickaxe."), messages());

        player.getInventory().setItemInMainHand(new ItemStack(Material.IRON_PICKAXE));
        assertFalse(breakBlock(stone).isCancelled());
    }

    @Test
    void dispensersCannotUseBlockedItems() {
        block(Material.TNT, Action.DISPENSE);
        Block dispenser = blockAt(0, Material.DISPENSER);

        BlockDispenseEvent blocked = new BlockDispenseEvent(dispenser, new ItemStack(Material.TNT), new Vector());
        BlockDispenseEvent allowed = new BlockDispenseEvent(dispenser, new ItemStack(Material.ARROW), new Vector());
        server.getPluginManager().callEvent(blocked);
        server.getPluginManager().callEvent(allowed);

        assertTrue(blocked.isCancelled());
        assertFalse(allowed.isCancelled());
    }

    @Test
    void blockedItemsAreTakenOutOfLoot() {
        block(Material.ENCHANTED_GOLDEN_APPLE, Action.LOOT);
        List<ItemStack> loot = new ArrayList<>(List.of(new ItemStack(Material.ENCHANTED_GOLDEN_APPLE), new ItemStack(Material.BREAD)));

        server.getPluginManager().callEvent(new LootGenerateEvent(player.getWorld(), null, null, null,
                new LootContext.Builder(player.getLocation()).build(), loot, false));

        assertEquals(List.of(new ItemStack(Material.BREAD)), loot);
    }

    @Test
    @SuppressWarnings({"deprecation", "removal"})
    void mobsDoNotDropBlockedItemsButPlayersKeepTheirDrops() {
        updateDefault(preset -> preset.withItem(Material.TOTEM_OF_UNDYING, new ItemRule(EnumSet.of(Action.ENTITY_DROP), WorldScope.ALL)));
        LivingEntity evoker = (LivingEntity) player.getWorld().spawnEntity(player.getLocation(), EntityType.EVOKER);
        List<ItemStack> mobDrops = new ArrayList<>(List.of(new ItemStack(Material.TOTEM_OF_UNDYING), new ItemStack(Material.EMERALD)));
        List<ItemStack> playerDrops = new ArrayList<>(List.of(new ItemStack(Material.TOTEM_OF_UNDYING)));

        server.getPluginManager().callEvent(new EntityDeathEvent(evoker, DamageSource.builder(DamageType.GENERIC).build(), mobDrops));
        server.getPluginManager().callEvent(new EntityDeathEvent(player, DamageSource.builder(DamageType.GENERIC).build(), playerDrops));

        assertEquals(List.of(new ItemStack(Material.EMERALD)), mobDrops);
        assertEquals(1, playerDrops.size());
    }

    @Test
    void brewingIsBlockedByTheItemOrByABlockedEffect() {
        ItemStack swiftness = potion(Material.POTION, PotionType.SWIFTNESS);
        Location stand = player.getLocation();
        assertEquals(Map.of("speed", 1), PotionContents.effectsOf(swiftness));
        assertTrue(plugin.enforcement().restrictions().checkBrew(swiftness, stand).isEmpty());

        blockEffect("speed", 1);
        assertTrue(plugin.enforcement().restrictions().checkBrew(swiftness, stand).isPresent());

        block(Material.SPLASH_POTION, Action.BREW);
        assertTrue(plugin.enforcement().restrictions().checkBrew(potion(Material.SPLASH_POTION, PotionType.WATER), stand).isPresent());
    }

    @Test
    void obtainCoversEveryWayOfGettingAnItem() {
        assertEquals(EnumSet.of(Action.CRAFTING, Action.CRAFTER, Action.SMITHING, Action.BREW, Action.TRADE,
                Action.LOOT, Action.ENTITY_DROP, Action.PICKUP), Action.OBTAIN);
        assertFalse(Action.OBTAIN.contains(Action.USE));
    }

    private Block blockAt(int x, Material type) {
        Block block = player.getWorld().getBlockAt(x, 5, 0);
        block.setType(type);
        return block;
    }

    private BlockBreakEvent breakBlock(Block block) {
        BlockBreakEvent event = new BlockBreakEvent(block, player);
        server.getPluginManager().callEvent(event);
        return event;
    }

    private static ItemStack potion(Material material, PotionType type) {
        ItemStack item = new ItemStack(material);
        PotionMeta meta = (PotionMeta) item.getItemMeta();
        meta.setBasePotionType(type);
        item.setItemMeta(meta);
        return item;
    }
}
