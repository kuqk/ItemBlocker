package pl.variant.commands;

import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffectType;
import pl.variant.itemBlocker;
import pl.variant.model.BlockAction;
import pl.variant.model.ItemRule;
import pl.variant.model.WorldScopeMode;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

final class UnifiedCommandUiHandler {

    private static final String DEFAULT_TARGET = "default";
    private static final String ALL_TARGET = "all";

    private final itemBlocker plugin;
    private final CommandUiHandler legacyHandler;

    UnifiedCommandUiHandler(itemBlocker plugin) {
        this.plugin = plugin;
        this.legacyHandler = new CommandUiHandler(plugin);
    }

    boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        return switch (args[0].toLowerCase(Locale.ROOT)) {
            case "block" -> handleBlock(sender, command, label, args);
            case "edit" -> handleEdit(sender, command, label, args);
            case "unblock" -> handleUnblock(sender, command, label, args);
            case "show" -> handleShow(sender, command, label, args);
            case "list" -> handleList(sender, command, label, args);
            case "preset" -> handlePreset(sender, command, label, args);
            case "reload" -> legacy(sender, command, label, "reload");
            case "help" -> {
                sendHelp(sender);
                yield true;
            }
            default -> {
                plugin.getMessageManager().sendMessage(sender, "unknown-command");
                yield true;
            }
        };
    }

    List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return filterByPrefix(List.of("block", "edit", "unblock", "show", "list", "preset", "reload", "help"), args[0]);
        }

        return switch (args[0].toLowerCase(Locale.ROOT)) {
            case "block" -> completeBlock(args);
            case "edit" -> completeEdit(args);
            case "unblock" -> completeUnblock(args);
            case "show" -> completeShow(args);
            case "list" -> completeList(args);
            case "preset" -> completePreset(args);
            default -> List.of();
        };
    }

    private boolean handleBlock(CommandSender sender, Command command, String label, String[] args) {
        if (args.length < 2) {
            return sendUsage(sender, "usage-block");
        }

        String subject = args[1].toLowerCase(Locale.ROOT);
        return switch (subject) {
            case "enchantment" -> handleBlockThreshold(sender, command, label, args, true);
            case "potion" -> handleBlockThreshold(sender, command, label, args, false);
            default -> handleBlockItem(sender, command, label, args, 1);
        };
    }

    private boolean handleEdit(CommandSender sender, Command command, String label, String[] args) {
        if (args.length < 2) {
            return sendUsage(sender, "usage-edit");
        }

        return handleEditItem(sender, command, label, args, 1);
    }

    private boolean handleUnblock(CommandSender sender, Command command, String label, String[] args) {
        if (args.length < 2) {
            return sendUsage(sender, "usage-unblock");
        }

        String subject = args[1].toLowerCase(Locale.ROOT);
        return switch (subject) {
            case "enchantment" -> handleUnblockThreshold(sender, command, label, args, true);
            case "potion" -> handleUnblockThreshold(sender, command, label, args, false);
            default -> handleUnblockItem(sender, command, label, args, 1);
        };
    }

    private boolean handleShow(CommandSender sender, Command command, String label, String[] args) {
        if (args.length < 2) {
            return sendUsage(sender, "usage-show");
        }

        String subject = args[1].toLowerCase(Locale.ROOT);
        return switch (subject) {
            case "enchantment" -> handleShowThreshold(sender, command, label, args, true);
            case "potion" -> handleShowThreshold(sender, command, label, args, false);
            case "preset" -> handleShowTarget(sender, command, label, args);
            default -> handleShowItemOrTarget(sender, command, label, args);
        };
    }

    private void sendHelp(CommandSender sender) {
        plugin.getMessageManager().sendMessage(sender, "help-simple-header");
        plugin.getMessageManager().sendMessage(sender, "help-simple-block");
        plugin.getMessageManager().sendMessage(sender, "help-simple-edit");
        plugin.getMessageManager().sendMessage(sender, "help-simple-unblock");
        plugin.getMessageManager().sendMessage(sender, "help-simple-show");
        plugin.getMessageManager().sendMessage(sender, "help-simple-list");
        plugin.getMessageManager().sendMessage(sender, "help-simple-preset");
        plugin.getMessageManager().sendMessage(sender, "help-simple-reload");
        plugin.getMessageManager().sendMessage(sender, "help-simple-actions");
        plugin.getMessageManager().sendMessage(sender, "help-simple-defaults");
        plugin.getMessageManager().sendMessage(sender, "help-simple-example-1");
        plugin.getMessageManager().sendMessage(sender, "help-simple-example-2");
        plugin.getMessageManager().sendMessage(sender, "help-simple-example-3");
        plugin.getMessageManager().sendMessage(sender, "help-simple-example-4");
        plugin.getMessageManager().sendMessage(sender, "help-simple-example-5");
    }

    private boolean handleList(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 1) {
            return legacy(sender, command, label, "list");
        }

        String subject = args[1].toLowerCase(Locale.ROOT);
        if (subject.equals("presets")) {
            return legacy(sender, command, label, "preset", "list");
        }

        if (subject.equals("enchantments")) {
            TargetOption option = parseTargetOption(args, 2, true);
            if (!option.valid()) {
                return sendUsage(sender, "usage-list");
            }

            List<String> translated = new ArrayList<>(List.of("enchantment", "list"));
            if (option.target() != null && !option.target().equals(ALL_TARGET)) {
                translated.add(option.target());
            }
            return legacy(sender, command, label, translated);
        }

        if (subject.equals("potions")) {
            TargetOption option = parseTargetOption(args, 2, true);
            if (!option.valid()) {
                return sendUsage(sender, "usage-list");
            }

            List<String> translated = new ArrayList<>(List.of("potion", "list"));
            if (option.target() != null && !option.target().equals(ALL_TARGET)) {
                translated.add(option.target());
            }
            return legacy(sender, command, label, translated);
        }

        if (subject.equals("items")) {
            TargetOption option = parseTargetOption(args, 2, true);
            if (!option.valid()) {
                return sendUsage(sender, "usage-list");
            }

            if (option.target() == null || option.target().equals(ALL_TARGET)) {
                return legacy(sender, command, label, "list");
            }

            return legacy(sender, command, label, "item", "list", option.target());
        }

        String target = normalizeNamedTarget(args[1], true);
        if (target != null) {
            return legacy(sender, command, label, "item", "list", target);
        }

        return sendUsage(sender, "usage-list");
    }

    private boolean handlePreset(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 1 || args[1].equalsIgnoreCase("list")) {
            return legacy(sender, command, label, "preset", "list");
        }

        String action = args[1].toLowerCase(Locale.ROOT);
        switch (action) {
            case "create":
                if (args.length < 3) {
                    return sendUsage(sender, "usage-preset-simple");
                }
                return legacy(sender, command, label, "preset", args[2].toLowerCase(Locale.ROOT), "create");
            case "delete":
                if (args.length < 3) {
                    return sendUsage(sender, "usage-preset-simple");
                }
                return handlePresetDelete(sender, command, label, args[2]);
            case "show":
                if (args.length < 3) {
                    return sendUsage(sender, "usage-preset-simple");
                }
                return handlePresetShow(sender, command, label, args[2]);
            case "edit":
                if (args.length < 5) {
                    return sendUsage(sender, "usage-preset-simple");
                }
                return handlePresetEdit(sender, command, label, args);
            default:
                if (args.length == 2) {
                    return handlePresetShow(sender, command, label, args[1]);
                }
                return sendUsage(sender, "usage-preset-simple");
        }
    }

    private boolean handlePresetDelete(CommandSender sender, Command command, String label, String rawTarget) {
        String target = normalizeNamedTarget(rawTarget, false);
        if (target == null) {
            return sendUsage(sender, "usage-preset-simple");
        }

        return legacy(sender, command, label, "preset", target, "delete");
    }

    private boolean handlePresetShow(CommandSender sender, Command command, String label, String rawTarget) {
        String target = normalizeNamedTarget(rawTarget, true);
        if (target == null) {
            return sendUsage(sender, "usage-preset-simple");
        }

        return legacy(sender, command, label, "preset", target, "info");
    }

    private boolean handlePresetEdit(CommandSender sender, Command command, String label, String[] args) {
        String field = args[3].toLowerCase(Locale.ROOT);
        if (!field.equals("reason") && !field.equals("description") && !field.equals("worlds")) {
            return sendUsage(sender, "usage-preset-simple");
        }

        String target = normalizeNamedTarget(args[2], true);
        if (target == null) {
            return sendUsage(sender, "usage-preset-simple");
        }

        List<String> translated = new ArrayList<>();
        translated.add("preset");
        translated.add(target);
        translated.add("edit");
        translated.add(field);
        translated.addAll(Arrays.asList(args).subList(4, args.length));
        return legacy(sender, command, label, translated);
    }

    private boolean handleBlockItem(CommandSender sender, Command command, String label, String[] args, int itemIndex) {
        if (!sender.hasPermission("itemblocker.add")) {
            plugin.getMessageManager().sendMessage(sender, "no-permission");
            return true;
        }

        Material material = resolveItemMaterial(sender, args[itemIndex]);
        if (material == null) {
            return true;
        }

        ItemMutationOptions options = parseItemMutationOptions(args, itemIndex + 1);
        if (!options.valid()) {
            return sendUsage(sender, "usage-block");
        }

        EnumSet<BlockAction> actions = parseActions(sender, options.actions());
        ParsedWorldScope worldScope = parseWorldScope(options.worlds());
        if (actions == null || worldScope == null) {
            return sendUsage(sender, "usage-block");
        }

        String target = options.target() == null ? DEFAULT_TARGET : options.target();
        ItemRule existingRule = getItemRule(target, material);
        EnumSet<BlockAction> finalActions = existingRule == null
                ? actions
                : (options.actions() == null ? existingRule.getActions() : mergeActions(existingRule.getActions(), actions));
        ParsedWorldScope finalWorldScope = existingRule == null
                ? worldScope
                : (options.worlds() == null
                ? new ParsedWorldScope(existingRule.getWorldScopeMode(), existingRule.getWorlds())
                : mergeWorldScopes(existingRule.getWorldScopeMode(), existingRule.getWorlds(), worldScope.mode(), worldScope.worlds()));

        boolean saved = replaceItemRule(target, material, new ItemRule(finalActions, finalWorldScope.mode(), finalWorldScope.worlds()));
        if (!saved) {
            plugin.getMessageManager().sendMessage(sender, "preset-not-found", "{preset}", target);
            return true;
        }

        sendItemMutationMessages(sender, material, target, finalActions, finalWorldScope, options.worlds() != null, existingRule != null);
        return true;
    }

    private boolean handleEditItem(CommandSender sender, Command command, String label, String[] args, int itemIndex) {
        if (!sender.hasPermission("itemblocker.edit")) {
            plugin.getMessageManager().sendMessage(sender, "no-permission");
            return true;
        }

        Material material = resolveItemMaterial(sender, args[itemIndex]);
        if (material == null) {
            return true;
        }

        ItemMutationOptions options = parseItemMutationOptions(args, itemIndex + 1);
        if (!options.valid() || (options.actions() == null && options.worlds() == null)) {
            return sendUsage(sender, "usage-edit");
        }

        String target = options.target() == null ? DEFAULT_TARGET : options.target();
        ItemRule existingRule = getItemRule(target, material);
        if (existingRule == null) {
            plugin.getMessageManager().sendMessage(sender, "item-not-in-target", Map.of(
                    "{item}", material.name(),
                    "{target}", formatTargetLabel(target)
            ));
            return true;
        }

        EnumSet<BlockAction> actions = options.actions() == null
                ? existingRule.getActions()
                : parseActions(sender, options.actions());
        ParsedWorldScope worldScope = options.worlds() == null
                ? new ParsedWorldScope(existingRule.getWorldScopeMode(), existingRule.getWorlds())
                : parseWorldScope(options.worlds());
        if (actions == null || worldScope == null) {
            return sendUsage(sender, "usage-edit");
        }

        boolean saved = replaceItemRule(target, material, new ItemRule(actions, worldScope.mode(), worldScope.worlds()));
        if (!saved) {
            plugin.getMessageManager().sendMessage(sender, "preset-not-found", "{preset}", target);
            return true;
        }

        sendItemMutationMessages(sender, material, target, actions, worldScope, options.worlds() != null, true);
        return true;
    }

    private boolean handleUnblockItem(CommandSender sender, Command command, String label, String[] args, int itemIndex) {
        if (!sender.hasPermission("itemblocker.remove")) {
            plugin.getMessageManager().sendMessage(sender, "no-permission");
            return true;
        }

        ItemDeleteOptions options = parseItemDeleteOptions(args, itemIndex + 1);
        if (!options.valid()) {
            return sendUsage(sender, "usage-unblock");
        }

        Material material = resolveItemMaterial(sender, args[itemIndex]);
        if (material == null) {
            return true;
        }

        String target = options.target() == null ? DEFAULT_TARGET : options.target();
        boolean removed = removeItemFromTarget(target, material);
        plugin.getMessageManager().sendMessage(
                sender,
                removed ? "item-removed-target" : "item-not-in-target",
                Map.of(
                        "{item}", material.name(),
                        "{target}", formatTargetLabel(target)
                )
        );
        return true;
    }

    private boolean handleShowItemOrTarget(CommandSender sender, Command command, String label, String[] args) {
        TargetOption option = parseTargetOption(args, 2, true);
        if (!option.valid()) {
            return sendUsage(sender, "usage-show");
        }

        boolean itemReference = isHandToken(args[1]) || isItemReference(args[1]) || args.length > 2;
        if (itemReference) {
            Material material = resolveItemMaterial(sender, args[1]);
            if (material == null) {
                return true;
            }

            if (option.target() == null || option.target().equals(ALL_TARGET)) {
                return legacy(sender, command, label, "info", material.name());
            }

            return legacy(sender, command, label, "item", material.name(), "info", option.target());
        }

        String target = normalizeNamedTarget(args[1], true);
        if (target == null) {
            return sendUsage(sender, "usage-show");
        }

        return legacy(sender, command, label, "info", target);
    }

    private boolean handleShowTarget(CommandSender sender, Command command, String label, String[] args) {
        if (args.length < 3) {
            return sendUsage(sender, "usage-show");
        }

        String target = normalizeNamedTarget(args[2], true);
        if (target == null) {
            return sendUsage(sender, "usage-show");
        }

        return legacy(sender, command, label, "info", target);
    }

    private boolean handleBlockThreshold(CommandSender sender, Command command, String label, String[] args, boolean enchantment) {
        if (args.length < 3) {
            return sendUsage(sender, "usage-block");
        }

        ThresholdOptions options = parseThresholdOptions(args, 3, true, false);
        if (!options.valid()) {
            return sendUsage(sender, "usage-block");
        }

        List<String> translated = new ArrayList<>(List.of(enchantment ? "enchantment" : "potion", "add", args[2]));
        if (options.level() != null && options.level() > 1) {
            translated.add(String.valueOf(options.level()));
        }
        if (options.target() != null) {
            translated.add("preset:" + options.target());
        }
        return legacy(sender, command, label, translated);
    }

    private boolean handleUnblockThreshold(CommandSender sender, Command command, String label, String[] args, boolean enchantment) {
        if (args.length < 3) {
            return sendUsage(sender, "usage-unblock");
        }

        ThresholdOptions options = parseThresholdOptions(args, 3, false, false);
        if (!options.valid()) {
            return sendUsage(sender, "usage-unblock");
        }

        List<String> translated = new ArrayList<>(List.of(enchantment ? "enchantment" : "potion", "remove", args[2]));
        if (options.target() != null) {
            translated.add("preset:" + options.target());
        }
        return legacy(sender, command, label, translated);
    }

    private boolean handleShowThreshold(CommandSender sender, Command command, String label, String[] args, boolean enchantment) {
        if (args.length < 3) {
            return sendUsage(sender, "usage-show");
        }

        ThresholdOptions options = parseThresholdOptions(args, 3, false, true);
        if (!options.valid()) {
            return sendUsage(sender, "usage-show");
        }

        List<String> translated = new ArrayList<>(List.of(enchantment ? "enchantment" : "potion", "info", args[2]));
        if (options.target() != null && !options.target().equals(ALL_TARGET)) {
            translated.add(options.target());
        }
        return legacy(sender, command, label, translated);
    }

    private List<String> completeBlock(String[] args) {
        if (args.length == 2) {
            List<String> values = new ArrayList<>();
            values.add("enchantment");
            values.add("potion");
            values.add("hand");
            values.addAll(suggestMaterials(args[1]));
            return filterByPrefix(values, args[1]);
        }

        String subject = args[1].toLowerCase(Locale.ROOT);
        if (subject.equals("enchantment")) {
            if (args.length == 3) {
                return filterByPrefix(getEnchantmentSuggestions(), args[2]);
            }
            return suggestThresholdArguments(args[args.length - 1], true, false);
        }

        if (subject.equals("potion")) {
            if (args.length == 3) {
                return filterByPrefix(getPotionSuggestions(), args[2]);
            }
            return suggestThresholdArguments(args[args.length - 1], true, false);
        }

        return suggestItemArguments(args[args.length - 1]);
    }

    private List<String> completeEdit(String[] args) {
        if (args.length == 2) {
            List<String> values = new ArrayList<>();
            values.add("hand");
            values.addAll(suggestMaterials(args[1]));
            return filterByPrefix(values, args[1]);
        }

        return suggestItemArguments(args[args.length - 1]);
    }

    private List<String> completeUnblock(String[] args) {
        if (args.length == 2) {
            List<String> values = new ArrayList<>();
            values.add("enchantment");
            values.add("potion");
            values.add("hand");
            values.addAll(suggestMaterials(args[1]));
            return filterByPrefix(values, args[1]);
        }

        String subject = args[1].toLowerCase(Locale.ROOT);
        if (subject.equals("enchantment")) {
            if (args.length == 3) {
                return filterByPrefix(getEnchantmentSuggestions(), args[2]);
            }
            return suggestThresholdArguments(args[args.length - 1], false, false);
        }

        if (subject.equals("potion")) {
            if (args.length == 3) {
                return filterByPrefix(getPotionSuggestions(), args[2]);
            }
            return suggestThresholdArguments(args[args.length - 1], false, false);
        }

        return suggestItemDeleteArguments(args[args.length - 1]);
    }

    private List<String> completeShow(String[] args) {
        if (args.length == 2) {
            List<String> values = new ArrayList<>();
            values.add("enchantment");
            values.add("potion");
            values.add("preset");
            values.add("hand");
            values.add(DEFAULT_TARGET);
            values.addAll(plugin.getPresetManager().getPresetNames());
            values.addAll(suggestMaterials(args[1]));
            return filterByPrefix(values, args[1]);
        }

        String subject = args[1].toLowerCase(Locale.ROOT);
        if (subject.equals("enchantment")) {
            if (args.length == 3) {
                return filterByPrefix(getEnchantmentSuggestions(), args[2]);
            }
            return suggestThresholdArguments(args[args.length - 1], false, true);
        }

        if (subject.equals("potion")) {
            if (args.length == 3) {
                return filterByPrefix(getPotionSuggestions(), args[2]);
            }
            return suggestThresholdArguments(args[args.length - 1], false, true);
        }

        if (subject.equals("preset")) {
            if (args.length == 3) {
                return suggestTargets(args[2], false);
            }
            return List.of();
        }

        return suggestShowArguments(args[args.length - 1]);
    }

    private List<String> completeList(String[] args) {
        if (args.length == 2) {
            List<String> values = new ArrayList<>();
            values.add("items");
            values.add("presets");
            values.add("enchantments");
            values.add("potions");
            values.add(DEFAULT_TARGET);
            values.addAll(plugin.getPresetManager().getPresetNames());
            return filterByPrefix(values, args[1]);
        }

        String subject = args[1].toLowerCase(Locale.ROOT);
        if (subject.equals("items") || subject.equals("enchantments") || subject.equals("potions")) {
            return suggestTargetOption(args[args.length - 1], true);
        }

        return List.of();
    }

    private List<String> completePreset(String[] args) {
        if (args.length == 2) {
            List<String> values = new ArrayList<>();
            values.add("list");
            values.add("show");
            values.add("create");
            values.add("delete");
            values.add("edit");
            values.add(DEFAULT_TARGET);
            values.addAll(plugin.getPresetManager().getPresetNames());
            return filterByPrefix(values, args[1]);
        }

        String action = args[1].toLowerCase(Locale.ROOT);
        if (action.equals("create") || action.equals("delete") || action.equals("show")) {
            if (args.length == 3) {
                return suggestTargets(args[2], !action.equals("delete"));
            }
            return List.of();
        }

        if (action.equals("edit")) {
            if (args.length == 3) {
                return suggestTargets(args[2], true);
            }
            if (args.length == 4) {
                return filterByPrefix(List.of("reason", "description", "worlds"), args[3]);
            }
            if (args[3].equalsIgnoreCase("worlds")) {
                return suggestWorldValues(args[args.length - 1]);
            }
            return List.of();
        }

        return List.of();
    }

    private List<String> suggestItemArguments(String currentPrefix) {
        String normalized = currentPrefix == null ? "" : currentPrefix.trim().toLowerCase(Locale.ROOT);
        if (normalized.startsWith("actions:")) {
            return suggestActionValues(currentPrefix);
        }
        if (normalized.startsWith("worlds:")) {
            return suggestWorldValues(currentPrefix);
        }
        if (normalized.startsWith("in:")) {
            return suggestTargetOption(currentPrefix, false);
        }

        List<String> values = new ArrayList<>(List.of("actions:all", "actions:crafting", "worlds:all", "in:default"));
        return filterByPrefix(values, currentPrefix);
    }

    private List<String> suggestItemDeleteArguments(String currentPrefix) {
        String normalized = currentPrefix == null ? "" : currentPrefix.trim().toLowerCase(Locale.ROOT);
        if (normalized.startsWith("in:")) {
            return suggestTargetOption(currentPrefix, false);
        }
        return filterByPrefix(List.of("in:default"), currentPrefix);
    }

    private List<String> suggestShowArguments(String currentPrefix) {
        String normalized = currentPrefix == null ? "" : currentPrefix.trim().toLowerCase(Locale.ROOT);
        if (normalized.startsWith("in:")) {
            return suggestTargetOption(currentPrefix, true);
        }
        return filterByPrefix(List.of("in:all", "in:default"), currentPrefix);
    }

    private List<String> suggestThresholdArguments(String currentPrefix, boolean includeLevel, boolean allowAllTarget) {
        String normalized = currentPrefix == null ? "" : currentPrefix.trim().toLowerCase(Locale.ROOT);
        if (includeLevel && (normalized.isBlank() || isPositiveInteger(normalized))) {
            return filterByPrefix(List.of("level:1", "level:2", "level:3", "level:4", "in:default"), currentPrefix);
        }
        if (normalized.startsWith("level:")) {
            return filterByPrefix(List.of("level:1", "level:2", "level:3", "level:4", "level:5"), currentPrefix);
        }
        if (normalized.startsWith("in:")) {
            return suggestTargetOption(currentPrefix, allowAllTarget);
        }

        List<String> values = new ArrayList<>();
        if (includeLevel) {
            values.add("level:1");
            values.add("level:2");
        }
        values.add("in:default");
        if (allowAllTarget) {
            values.add("in:all");
        }
        return filterByPrefix(values, currentPrefix);
    }

    private List<String> suggestActionValues(String currentPrefix) {
        List<String> values = new ArrayList<>();
        values.add("all");
        values.addAll(Arrays.stream(BlockAction.values()).map(BlockAction::getKey).toList());
        return suggestCsvOption(currentPrefix, "actions", values);
    }

    private List<String> suggestWorldValues(String currentPrefix) {
        List<String> values = new ArrayList<>();
        values.add("all");
        values.add("disabled");
        values.addAll(plugin.getServer().getWorlds().stream()
                .map(world -> world.getName().toLowerCase(Locale.ROOT))
                .collect(Collectors.toCollection(LinkedHashSet::new)));
        values.add("world");
        values.add("world_nether");
        values.add("world_the_end");
        return suggestCsvOption(currentPrefix, "worlds", values);
    }

    private List<String> suggestTargetOption(String currentPrefix, boolean allowAll) {
        List<String> values = new ArrayList<>();
        if (allowAll) {
            values.add(ALL_TARGET);
        }
        values.add(DEFAULT_TARGET);
        values.addAll(plugin.getPresetManager().getPresetNames());
        return suggestSingleValueOption(currentPrefix, "in", values);
    }

    private List<String> suggestSingleValueOption(String currentPrefix, String key, List<String> values) {
        String prefix = key + ":";
        String normalized = currentPrefix == null ? "" : currentPrefix.trim();
        String rawValue = normalized.length() >= prefix.length()
                ? normalized.substring(prefix.length()).toLowerCase(Locale.ROOT)
                : "";

        return values.stream()
                .map(value -> prefix + value)
                .filter(value -> value.toLowerCase(Locale.ROOT).startsWith((prefix + rawValue).toLowerCase(Locale.ROOT)))
                .collect(Collectors.toList());
    }

    private List<String> suggestCsvOption(String currentPrefix, String key, List<String> values) {
        String prefix = key + ":";
        String normalized = currentPrefix == null ? "" : currentPrefix.trim();
        String rawValue = normalized.length() >= prefix.length()
                ? normalized.substring(prefix.length()).toLowerCase(Locale.ROOT)
                : "";

        String[] parts = rawValue.split(",", -1);
        List<String> committed = new ArrayList<>();
        int committedLimit = rawValue.endsWith(",") ? parts.length : Math.max(parts.length - 1, 0);
        for (int index = 0; index < committedLimit; index++) {
            String value = parts[index].trim();
            if (!value.isBlank()) {
                committed.add(value);
            }
        }

        String partial = rawValue.endsWith(",") || parts.length == 0 ? "" : parts[parts.length - 1].trim();
        LinkedHashSet<String> suggestions = new LinkedHashSet<>();
        for (String value : values) {
            String normalizedValue = value.toLowerCase(Locale.ROOT);
            if (committed.contains(normalizedValue) || !normalizedValue.startsWith(partial)) {
                continue;
            }

            List<String> suggestionValues = new ArrayList<>(committed);
            suggestionValues.add(normalizedValue);
            suggestions.add(prefix + String.join(",", suggestionValues));
        }
        return filterByPrefix(new ArrayList<>(suggestions), currentPrefix);
    }

    private ItemMutationOptions parseItemMutationOptions(String[] args, int startIndex) {
        String actions = null;
        String worlds = null;
        String target = null;

        for (int index = startIndex; index < args.length; index++) {
            String argument = args[index] == null ? "" : args[index].trim();
            if (argument.isBlank()) {
                continue;
            }

            int separatorIndex = argument.indexOf(':');
            if (separatorIndex < 0) {
                return ItemMutationOptions.invalid();
            }

            String key = argument.substring(0, separatorIndex).trim().toLowerCase(Locale.ROOT);
            String rawValue = argument.substring(separatorIndex + 1).trim();
            if (rawValue.isBlank()) {
                return ItemMutationOptions.invalid();
            }

            switch (key) {
                case "actions" -> actions = normalizeCsvValue(rawValue);
                case "worlds" -> worlds = normalizeCsvValue(rawValue);
                case "in" -> target = normalizeTargetValue(rawValue, false);
                default -> {
                    return ItemMutationOptions.invalid();
                }
            }

            if ((key.equals("actions") && actions == null)
                    || (key.equals("worlds") && worlds == null)
                    || (key.equals("in") && target == null)) {
                return ItemMutationOptions.invalid();
            }
        }

        return new ItemMutationOptions(actions, worlds, target, true);
    }

    private ItemDeleteOptions parseItemDeleteOptions(String[] args, int startIndex) {
        String target = null;

        for (int index = startIndex; index < args.length; index++) {
            String argument = args[index] == null ? "" : args[index].trim();
            if (argument.isBlank()) {
                continue;
            }

            int separatorIndex = argument.indexOf(':');
            if (separatorIndex < 0) {
                return ItemDeleteOptions.invalid();
            }

            String key = argument.substring(0, separatorIndex).trim().toLowerCase(Locale.ROOT);
            String rawValue = argument.substring(separatorIndex + 1).trim();
            if (rawValue.isBlank()) {
                return ItemDeleteOptions.invalid();
            }

            if (!key.equals("in")) {
                return ItemDeleteOptions.invalid();
            }

            target = normalizeTargetValue(rawValue, false);
            if (target == null) {
                return ItemDeleteOptions.invalid();
            }
        }

        return new ItemDeleteOptions(target, true);
    }

    private ThresholdOptions parseThresholdOptions(String[] args, int startIndex, boolean allowLevel, boolean allowAllTarget) {
        Integer level = 1;
        String target = null;
        boolean levelSeen = false;

        for (int index = startIndex; index < args.length; index++) {
            String argument = args[index] == null ? "" : args[index].trim();
            if (argument.isBlank()) {
                continue;
            }

            int separatorIndex = argument.indexOf(':');
            if (separatorIndex < 0) {
                Integer plainLevel = parsePositiveInteger(argument);
                if (allowLevel && !levelSeen && plainLevel != null) {
                    level = plainLevel;
                    levelSeen = true;
                    continue;
                }
                return ThresholdOptions.invalid();
            }

            String key = argument.substring(0, separatorIndex).trim().toLowerCase(Locale.ROOT);
            String rawValue = argument.substring(separatorIndex + 1).trim();
            if (rawValue.isBlank()) {
                return ThresholdOptions.invalid();
            }

            switch (key) {
                case "level" -> {
                    if (!allowLevel) {
                        return ThresholdOptions.invalid();
                    }
                    Integer parsedLevel = parsePositiveInteger(rawValue);
                    if (parsedLevel == null) {
                        return ThresholdOptions.invalid();
                    }
                    level = parsedLevel;
                    levelSeen = true;
                }
                case "in" -> {
                    target = normalizeTargetValue(rawValue, allowAllTarget);
                    if (target == null) {
                        return ThresholdOptions.invalid();
                    }
                }
                default -> {
                    return ThresholdOptions.invalid();
                }
            }
        }

        return new ThresholdOptions(level, target, true);
    }

    private TargetOption parseTargetOption(String[] args, int startIndex, boolean allowAll) {
        String target = null;

        for (int index = startIndex; index < args.length; index++) {
            String argument = args[index] == null ? "" : args[index].trim();
            if (argument.isBlank()) {
                continue;
            }

            int separatorIndex = argument.indexOf(':');
            if (separatorIndex < 0) {
                return TargetOption.invalid();
            }

            String key = argument.substring(0, separatorIndex).trim().toLowerCase(Locale.ROOT);
            String rawValue = argument.substring(separatorIndex + 1).trim();
            if (rawValue.isBlank()) {
                return TargetOption.invalid();
            }

            if (!key.equals("in")) {
                return TargetOption.invalid();
            }

            target = normalizeTargetValue(rawValue, allowAll);
            if (target == null) {
                return TargetOption.invalid();
            }
        }

        return new TargetOption(target, true);
    }

    private boolean legacy(CommandSender sender, Command command, String label, String... translatedArgs) {
        return legacyHandler.onCommand(sender, command, label, translatedArgs);
    }

    private boolean legacy(CommandSender sender, Command command, String label, List<String> translatedArgs) {
        return legacyHandler.onCommand(sender, command, label, translatedArgs.toArray(String[]::new));
    }

    private boolean sendUsage(CommandSender sender, String usageKey) {
        plugin.getMessageManager().sendMessage(sender, usageKey);
        return true;
    }

    private void sendItemMutationMessages(
            CommandSender sender,
            Material material,
            String target,
            Set<BlockAction> actions,
            ParsedWorldScope worldScope,
            boolean includeWorldMessage,
            boolean updated
    ) {
        plugin.getMessageManager().sendMessage(sender, updated ? "item-updated" : "item-saved", Map.of(
                "{item}", material.name(),
                "{target}", formatTargetLabel(target),
                "{actions}", formatActionKeys(actions)
        ));

        if (includeWorldMessage) {
            plugin.getMessageManager().sendMessage(sender, "item-worlds-set", Map.of(
                    "{item}", material.name(),
                    "{target}", formatTargetLabel(target),
                    "{worlds}", formatWorlds(worldScope.mode(), worldScope.worlds())
            ));
        }
    }

    private EnumSet<BlockAction> parseActions(CommandSender sender, String rawValue) {
        if (rawValue == null) {
            return EnumSet.allOf(BlockAction.class);
        }

        EnumSet<BlockAction> actions = EnumSet.noneOf(BlockAction.class);
        boolean explicitValuesProvided = false;
        for (String value : rawValue.split(",")) {
            String normalized = value.trim().toLowerCase(Locale.ROOT);
            if (normalized.isBlank()) {
                continue;
            }
            explicitValuesProvided = true;
            if (normalized.equals("all")) {
                return EnumSet.allOf(BlockAction.class);
            }
            if (normalized.equals("none")) {
                continue;
            }

            BlockAction action = BlockAction.fromKey(normalized).orElse(null);
            if (action == null) {
                plugin.getMessageManager().sendMessage(sender, "invalid-action", "{action}", value);
                return null;
            }
            actions.add(action);
        }

        return explicitValuesProvided ? actions : EnumSet.allOf(BlockAction.class);
    }

    private ParsedWorldScope parseWorldScope(String rawValue) {
        if (rawValue == null) {
            return new ParsedWorldScope(WorldScopeMode.DISABLED, Set.of());
        }

        Set<String> worlds = Arrays.stream(rawValue.split(","))
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .map(value -> value.toLowerCase(Locale.ROOT))
                .collect(Collectors.toCollection(LinkedHashSet::new));
        if (worlds.isEmpty() || worlds.contains("all") || worlds.contains("disabled")) {
            return new ParsedWorldScope(WorldScopeMode.DISABLED, Set.of());
        }

        return new ParsedWorldScope(WorldScopeMode.WHITELIST, worlds);
    }

    private EnumSet<BlockAction> mergeActions(Set<BlockAction> existingActions, Set<BlockAction> extraActions) {
        EnumSet<BlockAction> merged = existingActions == null || existingActions.isEmpty()
                ? EnumSet.noneOf(BlockAction.class)
                : EnumSet.copyOf(existingActions);
        if (extraActions != null && !extraActions.isEmpty()) {
            merged.addAll(extraActions);
        }
        return merged;
    }

    private ParsedWorldScope mergeWorldScopes(
            WorldScopeMode existingMode,
            Set<String> existingWorlds,
            WorldScopeMode newMode,
            Set<String> newWorlds
    ) {
        if (existingMode == WorldScopeMode.DISABLED
                || existingWorlds == null
                || existingWorlds.isEmpty()
                || newMode == WorldScopeMode.DISABLED
                || newWorlds == null
                || newWorlds.isEmpty()) {
            return new ParsedWorldScope(WorldScopeMode.DISABLED, Set.of());
        }

        LinkedHashSet<String> mergedWorlds = new LinkedHashSet<>(existingWorlds);
        mergedWorlds.addAll(newWorlds);
        return new ParsedWorldScope(WorldScopeMode.WHITELIST, mergedWorlds);
    }

    private boolean replaceItemRule(String target, Material material, ItemRule itemRule) {
        if (target.equals(DEFAULT_TARGET)) {
            return plugin.getBlockedItemsManager().replaceGlobalItemRule(material, itemRule);
        }

        return plugin.getPresetManager().replacePresetItemRule(target, material, itemRule);
    }

    private ItemRule getItemRule(String target, Material material) {
        if (target.equals(DEFAULT_TARGET)) {
            return plugin.getBlockedItemsManager().getGlobalItem(material).orElse(null);
        }

        return plugin.getPresetManager()
                .getPreset(target)
                .flatMap(preset -> preset.getItemRule(material))
                .orElse(null);
    }

    private boolean removeItemFromTarget(String target, Material material) {
        if (target.equals(DEFAULT_TARGET)) {
            return plugin.getBlockedItemsManager().removeGlobalItem(material);
        }

        return plugin.getPresetManager().removePresetItem(target, material);
    }

    private String formatTargetLabel(String target) {
        return target.equals(DEFAULT_TARGET)
                ? plugin.getMessageManager().getMessage("simple-list-label")
                : target;
    }

    private String formatActionKeys(Set<BlockAction> actions) {
        if (actions == null || actions.size() == BlockAction.values().length) {
            return "all";
        }
        if (actions.isEmpty()) {
            return "none";
        }

        List<String> values = new ArrayList<>();
        for (BlockAction action : BlockAction.values()) {
            if (actions.contains(action)) {
                values.add(action.getKey());
            }
        }
        return values.isEmpty() ? "all" : String.join(", ", values);
    }

    private String formatWorlds(WorldScopeMode mode, Set<String> worlds) {
        if (mode == WorldScopeMode.DISABLED || worlds == null || worlds.isEmpty()) {
            return plugin.getMessageManager().getMessage("info-all-worlds");
        }

        String formattedWorlds = worlds.stream().sorted().collect(Collectors.joining(", "));
        return plugin.getMessageManager().getMessage("info-world-list", "{worlds}", formattedWorlds);
    }

    private Material resolveItemMaterial(CommandSender sender, String rawItem) {
        if (isHandToken(rawItem)) {
            if (!(sender instanceof Player player)) {
                plugin.getMessageManager().sendMessage(sender, "only-players");
                return null;
            }

            ItemStack item = player.getInventory().getItemInMainHand();
            if (item.getType() == Material.AIR) {
                plugin.getMessageManager().sendMessage(sender, "no-item-in-hand");
                return null;
            }
            return item.getType();
        }

        Material material = parseMaterialOrNull(rawItem);
        if (material == null) {
            plugin.getMessageManager().sendMessage(sender, "invalid-item", "{item}", rawItem);
        }
        return material;
    }

    private boolean isItemReference(String value) {
        return parseMaterialOrNull(value) != null;
    }

    private boolean isTargetName(String value) {
        return normalizeNamedTarget(value, true) != null;
    }

    private String normalizeTargetValue(String rawValue, boolean allowAll) {
        if (rawValue == null || rawValue.isBlank()) {
            return null;
        }

        String normalized = rawValue.trim().toLowerCase(Locale.ROOT);
        if (allowAll && normalized.equals(ALL_TARGET)) {
            return ALL_TARGET;
        }
        return normalized;
    }

    private String normalizeNamedTarget(String rawValue, boolean allowDefault) {
        String normalized = normalizeTargetValue(rawValue, false);
        if (normalized == null) {
            return null;
        }
        if (allowDefault && normalized.equals(DEFAULT_TARGET)) {
            return DEFAULT_TARGET;
        }
        return plugin.getPresetManager().getPreset(normalized).isPresent() ? normalized : null;
    }

    private String normalizeCsvValue(String rawValue) {
        List<String> values = Arrays.stream(rawValue.split(","))
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .map(value -> value.toLowerCase(Locale.ROOT))
                .toList();
        if (values.isEmpty()) {
            return null;
        }
        return String.join(",", values);
    }

    private Material parseMaterialOrNull(String input) {
        if (input == null || input.isBlank()) {
            return null;
        }

        try {
            return Material.valueOf(input.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }

    private Integer parsePositiveInteger(String rawValue) {
        if (!isPositiveInteger(rawValue)) {
            return null;
        }

        try {
            int parsed = Integer.parseInt(rawValue);
            return parsed > 0 ? parsed : null;
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private boolean isPositiveInteger(String rawValue) {
        return rawValue != null && !rawValue.isBlank() && rawValue.chars().allMatch(Character::isDigit);
    }

    private boolean isHandToken(String value) {
        return value != null && value.equalsIgnoreCase("hand");
    }

    private List<String> suggestMaterials(String prefix) {
        String normalizedPrefix = prefix == null ? "" : prefix.toUpperCase(Locale.ROOT);
        return Arrays.stream(Material.values())
                .map(Material::name)
                .filter(material -> material.startsWith(normalizedPrefix))
                .sorted()
                .collect(Collectors.toList());
    }

    private List<String> getEnchantmentSuggestions() {
        return Arrays.stream(Enchantment.values())
                .filter(enchantment -> enchantment != null)
                .map(enchantment -> enchantment.getKey().getKey())
                .sorted()
                .collect(Collectors.toList());
    }

    private List<String> getPotionSuggestions() {
        return Arrays.stream(PotionEffectType.values())
                .filter(effectType -> effectType != null)
                .map(effectType -> effectType.getKey().getKey())
                .sorted()
                .collect(Collectors.toList());
    }

    private List<String> suggestTargets(String prefix, boolean includeAll) {
        List<String> values = new ArrayList<>();
        if (includeAll) {
            values.add(ALL_TARGET);
        }
        values.add(DEFAULT_TARGET);
        values.addAll(plugin.getPresetManager().getPresetNames());
        return filterByPrefix(values, prefix);
    }

    private List<String> filterByPrefix(List<String> values, String prefix) {
        String normalizedPrefix = prefix == null ? "" : prefix.toLowerCase(Locale.ROOT);
        return values.stream()
                .filter(value -> value.toLowerCase(Locale.ROOT).startsWith(normalizedPrefix))
                .collect(Collectors.toList());
    }

    private record ItemMutationOptions(String actions, String worlds, String target, boolean valid) {
        private static ItemMutationOptions invalid() {
            return new ItemMutationOptions(null, null, null, false);
        }
    }

    private record ItemDeleteOptions(String target, boolean valid) {
        private static ItemDeleteOptions invalid() {
            return new ItemDeleteOptions(null, false);
        }
    }

    private record ParsedWorldScope(WorldScopeMode mode, Set<String> worlds) {
    }

    private record ThresholdOptions(Integer level, String target, boolean valid) {
        private static ThresholdOptions invalid() {
            return new ThresholdOptions(null, null, false);
        }
    }

    private record TargetOption(String target, boolean valid) {
        private static TargetOption invalid() {
            return new TargetOption(null, false);
        }
    }
}
