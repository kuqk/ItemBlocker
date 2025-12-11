package pl.variant.commands;

import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import pl.variant.itemBlocker;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class ItemBlockerCommand implements CommandExecutor, TabCompleter {
    
    private final itemBlocker plugin;
    
    public ItemBlockerCommand(itemBlocker plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }
        
        switch (args[0].toLowerCase()) {
            case "add":
                return handleAdd(sender, args);
            case "remove":
                return handleRemove(sender, args);
            case "list":
                return handleList(sender);
            case "reload":
                return handleReload(sender);
            case "addhand":
                return handleAddHand(sender);
            case "check":
                return handleCheck(sender, args);
            case "help":
                sendHelp(sender);
                return true;
            default:
                sender.sendMessage(plugin.getMessageManager().getMessage("unknown-command"));
                return true;
        }
    }
    
    private boolean handleAdd(CommandSender sender, String[] args) {
        if (!sender.hasPermission("itemblocker.add")) {
            sender.sendMessage(plugin.getMessageManager().getMessage("no-permission"));
            return true;
        }
        
        if (args.length < 2) {
            sender.sendMessage(plugin.getMessageManager().getMessage("usage-add"));
            return true;
        }
        
        Material material;
        try {
            material = Material.valueOf(args[1].toUpperCase());
        } catch (IllegalArgumentException e) {
            sender.sendMessage(plugin.getMessageManager().getMessage("invalid-item")
                    .replace("{item}", args[1]));
            return true;
        }
        
        if (plugin.getBlockedItemsManager().addBlockedItem(material)) {
            sender.sendMessage(plugin.getMessageManager().getMessage("item-added")
                    .replace("{item}", material.name()));
        } else {
            sender.sendMessage(plugin.getMessageManager().getMessage("item-already-blocked")
                    .replace("{item}", material.name()));
        }
        
        return true;
    }
    
    private boolean handleRemove(CommandSender sender, String[] args) {
        if (!sender.hasPermission("itemblocker.remove")) {
            sender.sendMessage(plugin.getMessageManager().getMessage("no-permission"));
            return true;
        }
        
        if (args.length < 2) {
            sender.sendMessage(plugin.getMessageManager().getMessage("usage-remove"));
            return true;
        }
        
        Material material;
        try {
            material = Material.valueOf(args[1].toUpperCase());
        } catch (IllegalArgumentException e) {
            sender.sendMessage(plugin.getMessageManager().getMessage("invalid-item")
                    .replace("{item}", args[1]));
            return true;
        }
        
        if (plugin.getBlockedItemsManager().removeBlockedItem(material)) {
            sender.sendMessage(plugin.getMessageManager().getMessage("item-removed")
                    .replace("{item}", material.name()));
        } else {
            sender.sendMessage(plugin.getMessageManager().getMessage("item-not-blocked")
                    .replace("{item}", material.name()));
        }
        
        return true;
    }
    
    private boolean handleList(CommandSender sender) {
        if (!sender.hasPermission("itemblocker.list")) {
            sender.sendMessage(plugin.getMessageManager().getMessage("no-permission"));
            return true;
        }
        
        int count = plugin.getBlockedItemsManager().getBlockedItemsCount();
        sender.sendMessage(plugin.getMessageManager().getMessage("list-header")
                .replace("{count}", String.valueOf(count)));
        
        if (count > 0) {
            StringBuilder items = new StringBuilder();
            for (Material material : plugin.getBlockedItemsManager().getBlockedItems()) {
                items.append("§7- §e").append(material.name()).append("\n");
            }
            sender.sendMessage(items.toString().trim());
        }
        
        return true;
    }
    
    private boolean handleReload(CommandSender sender) {
        if (!sender.hasPermission("itemblocker.reload")) {
            sender.sendMessage(plugin.getMessageManager().getMessage("no-permission"));
            return true;
        }
        
        plugin.getConfigManager().reloadConfiguration();
        plugin.getMessageManager().reloadMessages();
        plugin.getBlockedItemsManager().loadBlockedItems();
        
        sender.sendMessage(plugin.getMessageManager().getMessage("reloaded"));
        return true;
    }
    
    private boolean handleAddHand(CommandSender sender) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(plugin.getMessageManager().getMessage("only-players"));
            return true;
        }
        
        if (!sender.hasPermission("itemblocker.addhand")) {
            sender.sendMessage(plugin.getMessageManager().getMessage("no-permission"));
            return true;
        }
        
        Player player = (Player) sender;
        ItemStack item = player.getInventory().getItemInMainHand();
        
        if (item.getType() == Material.AIR) {
            player.sendMessage(plugin.getMessageManager().getMessage("no-item-in-hand"));
            return true;
        }
        
        if (plugin.getBlockedItemsManager().addBlockedItem(item.getType())) {
            player.sendMessage(plugin.getMessageManager().getMessage("item-added")
                    .replace("{item}", item.getType().name()));
        } else {
            player.sendMessage(plugin.getMessageManager().getMessage("item-already-blocked")
                    .replace("{item}", item.getType().name()));
        }
        
        return true;
    }
    
    private boolean handleCheck(CommandSender sender, String[] args) {
        if (!sender.hasPermission("itemblocker.check")) {
            sender.sendMessage(plugin.getMessageManager().getMessage("no-permission"));
            return true;
        }
        
        if (args.length < 2) {
            sender.sendMessage(plugin.getMessageManager().getMessage("usage-check"));
            return true;
        }
        
        Material material;
        try {
            material = Material.valueOf(args[1].toUpperCase());
        } catch (IllegalArgumentException e) {
            sender.sendMessage(plugin.getMessageManager().getMessage("invalid-item")
                    .replace("{item}", args[1]));
            return true;
        }
        
        if (plugin.getBlockedItemsManager().isBlocked(material)) {
            sender.sendMessage(plugin.getMessageManager().getMessage("item-is-blocked")
                    .replace("{item}", material.name()));
        } else {
            sender.sendMessage(plugin.getMessageManager().getMessage("item-not-blocked")
                    .replace("{item}", material.name()));
        }
        
        return true;
    }
    
    private void sendHelp(CommandSender sender) {
        sender.sendMessage(plugin.getMessageManager().getMessage("help-header"));
        sender.sendMessage(plugin.getMessageManager().getMessage("help-add"));
        sender.sendMessage(plugin.getMessageManager().getMessage("help-remove"));
        sender.sendMessage(plugin.getMessageManager().getMessage("help-addhand"));
        sender.sendMessage(plugin.getMessageManager().getMessage("help-list"));
        sender.sendMessage(plugin.getMessageManager().getMessage("help-check"));
        sender.sendMessage(plugin.getMessageManager().getMessage("help-reload"));
    }
    
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        
        if (args.length == 1) {
            completions.addAll(Arrays.asList("add", "remove", "list", "reload", "addhand", "check", "help"));
            return completions.stream()
                    .filter(s -> s.toLowerCase().startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }
        
        if (args.length == 2 && (args[0].equalsIgnoreCase("add") || 
                args[0].equalsIgnoreCase("remove") || 
                args[0].equalsIgnoreCase("check"))) {
            return Arrays.stream(Material.values())
                    .map(Material::name)
                    .filter(s -> s.toLowerCase().startsWith(args[1].toLowerCase()))
                    .collect(Collectors.toList());
        }
        
        return completions;
    }
}

