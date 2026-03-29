package pl.variant.commands;

import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import pl.variant.itemBlocker;
import pl.variant.model.BlockAction;
import pl.variant.model.ItemRule;
import pl.variant.model.RulePreset;
import pl.variant.model.WorldScopeMode;
import pl.variant.utils.TextUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

final class CommandUiHandler {

    private static final String DEFAULT_TARGET = "default";
    private static final String LEGACY_DEFAULT_TARGET = "global";
    private static final String INVALID_TARGET = "__invalid_target__";

    private final itemBlocker plugin;

    CommandUiHandler(itemBlocker plugin) {
        this.plugin = plugin;
    }

    boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sendHelpOverview(sender);
            return true;
        }

        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "add":
                return handleAdd(sender, args);
            case "addhand":
                return handleAddHand(sender, args);
            case "info":
                return handleInfo(sender, args);
            case "item":
                return handleItem(sender, args);
            case "preset":
                return handlePreset(sender, args);
            case "list":
                return handleList(sender);
            case "reload":
                return handleReload(sender);
            case "help":
                return handleHelp(sender, args);
            default:
                plugin.getMessageManager().sendMessage(sender, "unknown-command");
                return true;
        }
    }

    List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return filterByPrefix(getVisibleTopLevelCommands(sender), args[0]);
        }

        String subCommand = args[0].toLowerCase(Locale.ROOT);
        switch (subCommand) {
            case "add":
                if (args.length == 2) {
                    return suggestMaterials(args[1]);
                }
                return suggestItemMutationArguments(args, 2);
            case "addhand":
                return suggestItemMutationArguments(args, 1);
            case "info":
                if (args.length == 2) {
                    List<String> values = new ArrayList<>(suggestMaterials(args[1]));
                    values.add(DEFAULT_TARGET);
                    values.addAll(getPresetReferenceSuggestions());
                    return filterByPrefix(values, args[1]);
                }
                return List.of();
            case "item":
                return completeItemCommand(args);
            case "preset":
                return completePresetCommand(args);
            case "help":
                if (args.length == 2) {
                    return filterByPrefix(getVisibleHelpTopics(sender), args[1]);
                }
                return List.of();
            default:
                return List.of();
        }
    }

    private boolean handleAdd(CommandSender sender, String[] args) {
        if (!sender.hasPermission("itemblocker.add")) {
            plugin.getMessageManager().sendMessage(sender, "no-permission");
            return true;
        }

        if (args.length < 2) {
            plugin.getMessageManager().sendMessage(sender, "usage-add");
            return true;
        }

        Material material = parseMaterial(sender, stripItemPrefix(args[1]));
        if (material == null) {
            return true;
        }

        ParsedItemMutation mutation = parseItemMutation(sender, args, 2, "usage-add");
        if (mutation == null) {
            return true;
        }

        return applyItemMutation(sender, material, mutation, "item-saved", true, false);
    }

    private boolean handleAddHand(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            plugin.getMessageManager().sendMessage(sender, "only-players");
            return true;
        }

        if (!sender.hasPermission("itemblocker.addhand")) {
            plugin.getMessageManager().sendMessage(sender, "no-permission");
            return true;
        }

        ItemStack item = player.getInventory().getItemInMainHand();
        if (item.getType() == Material.AIR) {
            plugin.getMessageManager().sendMessage(player, "no-item-in-hand");
            return true;
        }

        ParsedItemMutation mutation = parseItemMutation(sender, args, 1, "usage-addhand");
        if (mutation == null) {
            return true;
        }

        return applyItemMutation(sender, item.getType(), mutation, "item-saved", true, false);
    }

    private boolean handleInfo(CommandSender sender, String[] args) {
        if (!sender.hasPermission("itemblocker.info")) {
            plugin.getMessageManager().sendMessage(sender, "no-permission");
            return true;
        }

        if (args.length < 2) {
            plugin.getMessageManager().sendMessage(sender, "usage-info");
            return true;
        }

        String rawReference = args[1];
        if (isItemReference(rawReference)) {
            Material referencedMaterial = parseMaterial(sender, rawReference);
            if (referencedMaterial == null) {
                return true;
            }
            return sendItemInfo(sender, referencedMaterial, null);
        }

        Material material = parseMaterialOrNull(rawReference);
        if (material != null) {
            return sendItemInfo(sender, material, null);
        }

        return sendTargetInfo(sender, rawReference);
    }

    private boolean handleItem(CommandSender sender, String[] args) {
        if (args.length < 2) {
            plugin.getMessageManager().sendMessage(sender, "usage-item");
            return true;
        }

        if (args[1].equalsIgnoreCase("list")) {
            return handleItemList(sender, args);
        }

        Material material = parseMaterial(sender, args[1]);
        if (material == null) {
            return true;
        }

        if (args.length < 3) {
            plugin.getMessageManager().sendMessage(sender, "usage-item");
            return true;
        }

        String action = args[2].toLowerCase(Locale.ROOT);
        switch (action) {
            case "create":
                if (!sender.hasPermission("itemblocker.add")) {
                    plugin.getMessageManager().sendMessage(sender, "no-permission");
                    return true;
                }

                ParsedItemMutation createMutation = parseItemMutation(sender, args, 3, "usage-item-create");
                if (createMutation == null) {
                    return true;
                }
                return applyItemMutation(sender, material, createMutation, "item-saved", true, true);
            case "delete":
                if (!sender.hasPermission("itemblocker.remove")) {
                    plugin.getMessageManager().sendMessage(sender, "no-permission");
                    return true;
                }
                return handleItemDelete(sender, args, material);
            case "info":
            case "list":
                if (!sender.hasPermission("itemblocker.info")) {
                    plugin.getMessageManager().sendMessage(sender, "no-permission");
                    return true;
                }
                return handleItemInfo(sender, args, material);
            case "edit":
                if (!sender.hasPermission("itemblocker.edit")) {
                    plugin.getMessageManager().sendMessage(sender, "no-permission");
                    return true;
                }
                return handleItemEdit(sender, args, material);
            default:
                plugin.getMessageManager().sendMessage(sender, "usage-item");
                return true;
        }
    }

    private boolean handleItemInfo(CommandSender sender, String[] args, Material material) {
        String targetFilter = resolveOptionalInfoTarget(sender, args, 3);
        if (INVALID_TARGET.equals(targetFilter)) {
            return true;
        }

        return sendItemInfo(sender, material, targetFilter);
    }

    private boolean handleItemEdit(CommandSender sender, String[] args, Material material) {
        if (args.length < 4) {
            plugin.getMessageManager().sendMessage(sender, "usage-item-edit");
            return true;
        }

        if (args[3].equalsIgnoreCase("worlds") || args[3].equalsIgnoreCase("world")) {
            ParsedTargetedWorldScope worldUpdate = parseTargetedWorldScope(sender, args, 4, "usage-item-worlds");
            if (worldUpdate == null) {
                return true;
            }

            ItemRule existingRule = getItemRule(worldUpdate.target(), material);
            if (existingRule == null) {
                plugin.getMessageManager().sendMessage(sender, "item-not-in-target", Map.of(
                        "{item}", material.name(),
                        "{target}", formatTargetLabel(worldUpdate.target())
                ));
                return true;
            }

            Integer scopeIndex = existingRule.hasMultipleScopes() ? null : 1;
            if (scopeIndex == null) {
                plugin.getMessageManager().sendMessage(sender, "item-scope-required", Map.of(
                        "{item}", material.name(),
                        "{target}", formatTargetLabel(worldUpdate.target()),
                        "{count}", String.valueOf(existingRule.getScopedRules().size())
                ));
                return true;
            }

            replaceItemScope(
                    worldUpdate.target(),
                    material,
                    existingRule,
                    scopeIndex,
                    null,
                    new ParsedWorldScope(worldUpdate.mode(), worldUpdate.worlds())
            );
            plugin.getMessageManager().sendMessage(sender, "item-scope-updated", Map.of(
                    "{item}", material.name(),
                    "{target}", formatTargetLabel(worldUpdate.target()),
                    "{scope}", String.valueOf(scopeIndex),
                    "{rule}", formatScopedRule(getItemRule(worldUpdate.target(), material).getScopedRules().get(scopeIndex - 1))
            ));
            return true;
        }

        ParsedItemPatch editPatch = parseItemPatch(sender, args, 3, "usage-item-edit");
        if (editPatch == null) {
            return true;
        }
        return applyItemPatch(sender, material, editPatch);
    }

    private boolean handleItemDelete(CommandSender sender, String[] args, Material material) {
        ParsedItemDelete deleteOptions = parseItemDelete(sender, args, 3, "usage-item-delete");
        if (deleteOptions == null) {
            return true;
        }

        if (deleteOptions.scopeIndex() == null) {
            boolean removed = removeItemFromTarget(deleteOptions.target(), material);
            plugin.getMessageManager().sendMessage(
                    sender,
                    removed ? "item-removed-target" : "item-not-in-target",
                    Map.of(
                            "{item}", material.name(),
                            "{target}", formatTargetLabel(deleteOptions.target())
                    )
            );
            return true;
        }

        ItemRule existingRule = getItemRule(deleteOptions.target(), material);
        if (existingRule == null) {
            plugin.getMessageManager().sendMessage(sender, "item-not-in-target", Map.of(
                    "{item}", material.name(),
                    "{target}", formatTargetLabel(deleteOptions.target())
            ));
            return true;
        }

        int scopeIndex = deleteOptions.scopeIndex();
        if (scopeIndex < 1 || scopeIndex > existingRule.getScopedRules().size()) {
            plugin.getMessageManager().sendMessage(sender, "item-scope-invalid", Map.of(
                    "{item}", material.name(),
                    "{target}", formatTargetLabel(deleteOptions.target()),
                    "{scope}", String.valueOf(scopeIndex),
                    "{count}", String.valueOf(existingRule.getScopedRules().size())
            ));
            return true;
        }

        if (existingRule.getScopedRules().size() == 1) {
            removeItemFromTarget(deleteOptions.target(), material);
        } else {
            List<ItemRule.ScopedRule> updatedScopes = new ArrayList<>(existingRule.getScopedRules());
            updatedScopes.remove(scopeIndex - 1);
            replaceItemRule(deleteOptions.target(), material, new ItemRule(updatedScopes));
        }

        plugin.getMessageManager().sendMessage(sender, "item-scope-removed", Map.of(
                "{item}", material.name(),
                "{target}", formatTargetLabel(deleteOptions.target()),
                "{scope}", String.valueOf(scopeIndex)
        ));
        return true;
    }

    private boolean handleItemList(CommandSender sender, String[] args) {
        if (!sender.hasPermission("itemblocker.list")) {
            plugin.getMessageManager().sendMessage(sender, "no-permission");
            return true;
        }

        if (args.length < 3 || args[2].equalsIgnoreCase("all")) {
            return handleList(sender);
        }

        return sendTargetInfo(sender, args[2]);
    }

    private boolean handlePreset(CommandSender sender, String[] args) {
        if (!sender.hasPermission("itemblocker.preset")) {
            plugin.getMessageManager().sendMessage(sender, "no-permission");
            return true;
        }

        if (args.length < 2 || args[1].equalsIgnoreCase("list")) {
            sendPresetOverview(sender);
            return true;
        }

        if (args[1].equalsIgnoreCase("set")) {
            if (args.length < 3) {
                plugin.getMessageManager().sendMessage(sender, "usage-preset");
                return true;
            }

            String setTarget = resolveTarget(sender, args[2], false);
            if (setTarget == null) {
                return true;
            }

            return applyPresetToServer(sender, setTarget);
        }

        String rawTarget = args[1];
        String targetIdentifier = normalizeTargetIdentifier(rawTarget);
        if (targetIdentifier == null) {
            plugin.getMessageManager().sendMessage(sender, "usage-preset");
            return true;
        }

        if (args.length < 3) {
            return sendTargetInfo(sender, rawTarget);
        }

        String action = args[2].toLowerCase(Locale.ROOT);
        switch (action) {
            case "create":
                if (isDefaultTarget(targetIdentifier)) {
                    plugin.getMessageManager().sendMessage(sender, "preset-edit-global");
                    return true;
                }

                boolean created = plugin.getPresetManager().createPreset(targetIdentifier);
                plugin.getMessageManager().sendMessage(
                        sender,
                        created ? "preset-created" : "preset-create-failed",
                        "{preset}",
                        targetIdentifier
                );
                return true;
            case "delete":
                if (isDefaultTarget(targetIdentifier)) {
                    plugin.getMessageManager().sendMessage(sender, "preset-edit-global");
                    return true;
                }

                boolean deleted = plugin.getPresetManager().deletePreset(targetIdentifier);
                plugin.getMessageManager().sendMessage(
                        sender,
                        deleted ? "preset-deleted" : "preset-not-found",
                        "{preset}",
                        targetIdentifier
                );
                return true;
            case "info":
            case "list":
                return sendTargetInfo(sender, rawTarget);
            case "edit":
                return handlePresetEdit(sender, rawTarget, args, 3);
            default:
                plugin.getMessageManager().sendMessage(sender, "usage-preset");
                return true;
        }
    }

    private boolean applyPresetToServer(CommandSender sender, String presetName) {
        Set<String> serverWorlds = plugin.getServer().getWorlds().stream()
                .map(world -> world.getName().toLowerCase(Locale.ROOT))
                .collect(Collectors.toCollection(LinkedHashSet::new));

        if (serverWorlds.isEmpty()) {
            plugin.getMessageManager().sendMessage(sender, "usage-preset");
            return true;
        }

        plugin.getPresetManager().updatePresetWorlds(presetName, WorldScopeMode.WHITELIST, serverWorlds);
        plugin.getMessageManager().sendMessage(sender, "preset-applied-server", Map.of(
                "{preset}", presetName,
                "{worlds}", String.join(", ", serverWorlds)
        ));
        return true;
    }

    private boolean handlePresetEdit(CommandSender sender, String rawTarget, String[] args, int startIndex) {
        String target = resolveTarget(sender, rawTarget, true);
        if (target == null) {
            return true;
        }

        if (args.length <= startIndex) {
            plugin.getMessageManager().sendMessage(sender, "usage-preset-edit");
            return true;
        }

        String mode = args[startIndex].toLowerCase(Locale.ROOT);
        switch (mode) {
            case "description":
                if (isDefaultTarget(target)) {
                    plugin.getMessageManager().sendMessage(sender, "preset-edit-global");
                    return true;
                }

                if (args.length <= startIndex + 1) {
                    plugin.getMessageManager().sendMessage(sender, "usage-preset-edit");
                    return true;
                }

                plugin.getPresetManager().updatePresetDescription(
                        target,
                        String.join(" ", Arrays.copyOfRange(args, startIndex + 1, args.length))
                );
                plugin.getMessageManager().sendMessage(sender, "preset-description-set", "{preset}", target);
                return true;
            case "reason":
                if (args.length <= startIndex + 1) {
                    plugin.getMessageManager().sendMessage(sender, "usage-preset-edit");
                    return true;
                }

                String reason = String.join(" ", Arrays.copyOfRange(args, startIndex + 1, args.length));
                if (isDefaultTarget(target)) {
                    plugin.getBlockedItemsManager().setGlobalReason(reason);
                    plugin.getMessageManager().sendMessage(sender, "global-reason-set", Map.of("{reason}", reason));
                    return true;
                }

                plugin.getPresetManager().updatePresetReason(target, reason);
                plugin.getMessageManager().sendMessage(sender, "preset-reason-set", "{preset}", target);
                return true;
            case "worlds":
            case "world":
                if (isDefaultTarget(target)) {
                    plugin.getMessageManager().sendMessage(sender, "preset-edit-global");
                    return true;
                }

                ParsedWorldScope worldScope = parseWorldScope(sender, args, startIndex + 1, "usage-preset-worlds");
                if (worldScope == null) {
                    return true;
                }

                plugin.getPresetManager().updatePresetWorlds(target, worldScope.mode(), worldScope.worlds());
                plugin.getMessageManager().sendMessage(sender, "preset-worlds-set", Map.of(
                        "{preset}", target,
                        "{worlds}", formatWorlds(worldScope.mode(), worldScope.worlds())
                ));
                return true;
            default:
                plugin.getMessageManager().sendMessage(sender, "usage-preset-edit");
                return true;
        }
    }

    private boolean handleList(CommandSender sender) {
        if (!sender.hasPermission("itemblocker.list")) {
            plugin.getMessageManager().sendMessage(sender, "no-permission");
            return true;
        }

        Map<Material, ItemRule> globalItems = plugin.getBlockedItemsManager().getGlobalItems();
        List<RulePreset> presets = plugin.getPresetManager().getPresets();

        plugin.getMessageManager().sendMessage(sender, "list-header", Map.of(
                "{simple_count}", String.valueOf(globalItems.size()),
                "{rule_count}", String.valueOf(presets.size())
        ));

        if (globalItems.isEmpty()) {
            plugin.getMessageManager().sendMessage(sender, "list-simple-empty");
        } else {
            plugin.getMessageManager().sendMessage(sender, "list-simple-header");
            globalItems.entrySet().stream()
                    .sorted(Map.Entry.comparingByKey(Comparator.comparing(Material::name)))
                    .forEach(entry -> sender.sendMessage(plugin.getMessageManager().getMessage(
                            "list-simple-entry",
                            Map.of(
                                    "{item}", entry.getKey().name(),
                                    "{actions}", formatActions(entry.getValue().getActions())
                            )
                    )));
        }

        if (presets.isEmpty()) {
            plugin.getMessageManager().sendMessage(sender, "list-rules-empty");
            return true;
        }

        plugin.getMessageManager().sendMessage(sender, "list-rules-header");
        for (RulePreset preset : presets) {
            sender.sendMessage(plugin.getMessageManager().getMessage(
                    "list-preset-entry",
                    Map.of(
                            "{preset}", preset.getName(),
                            "{count}", String.valueOf(preset.getItemCount()),
                            "{worlds}", formatWorlds(preset)
                    )
            ));
        }

        return true;
    }

    private boolean handleReload(CommandSender sender) {
        if (!sender.hasPermission("itemblocker.reload")) {
            plugin.getMessageManager().sendMessage(sender, "no-permission");
            return true;
        }

        plugin.getConfigManager().reloadConfiguration();
        plugin.getMessageManager().reloadMessages();
        plugin.getBlockedItemsManager().loadBlockedItems();
        plugin.getPresetManager().loadPresets();

        plugin.getMessageManager().sendMessage(sender, "reloaded");
        return true;
    }

    private boolean sendTargetInfo(CommandSender sender, String rawTarget) {
        String target = resolveTarget(sender, rawTarget, true);
        if (target == null) {
            return true;
        }

        if (isDefaultTarget(target)) {
            sendDefaultInfo(sender);
            return true;
        }

        RulePreset preset = plugin.getPresetManager().getPreset(target).orElse(null);
        if (preset == null) {
            plugin.getMessageManager().sendMessage(sender, "preset-not-found", "{preset}", rawTarget);
            return true;
        }

        plugin.getMessageManager().sendMessage(sender, "preset-info-header", "{preset}", preset.getName());
        plugin.getMessageManager().sendMessage(sender, "preset-info-description", Map.of(
                "{description}", preset.getDescription().isBlank()
                        ? plugin.getMessageManager().getMessage("preset-no-description")
                        : preset.getDescription()
        ));
        plugin.getMessageManager().sendMessage(sender, "preset-info-reason", Map.of(
                "{reason}", preset.getReason().isBlank() ? "-" : preset.getReason()
        ));
        plugin.getMessageManager().sendMessage(sender, "preset-info-worlds", Map.of(
                "{worlds}", formatWorlds(preset)
        ));
        plugin.getMessageManager().sendMessage(sender, "preset-info-items", Map.of(
                "{count}", String.valueOf(preset.getItemCount())
        ));
        sendItemList(sender, preset.getItemRules(), "preset-info-items-list", "preset-info-empty");
        return true;
    }

    private boolean sendItemInfo(CommandSender sender, Material material, String targetFilter) {
        plugin.getMessageManager().sendMessage(sender, "info-header", Map.of(
                "{item}", material.name(),
                "{item_pretty}", TextUtils.formatEnumName(material.name())
        ));

        if (targetFilter == null) {
            ItemRule globalRule = plugin.getBlockedItemsManager().getGlobalItem(material).orElse(null);
            List<RulePreset> matchingPresets = plugin.getPresetManager().getMatchingPresets(material);

            if (globalRule == null && matchingPresets.isEmpty()) {
                plugin.getMessageManager().sendMessage(sender, "info-no-rules");
                return true;
            }

            if (globalRule != null) {
                sendDefaultItemInfo(sender, globalRule);
            }

            for (RulePreset preset : matchingPresets) {
                ItemRule itemRule = preset.getItemRule(material).orElse(null);
                if (itemRule != null) {
                    sendPresetItemInfo(sender, preset, itemRule);
                }
            }

            return true;
        }

        if (isDefaultTarget(targetFilter)) {
            ItemRule globalRule = plugin.getBlockedItemsManager().getGlobalItem(material).orElse(null);
            if (globalRule == null) {
                plugin.getMessageManager().sendMessage(sender, "item-not-in-target", Map.of(
                        "{item}", material.name(),
                        "{target}", formatTargetLabel(targetFilter)
                ));
                return true;
            }

            sendDefaultItemInfo(sender, globalRule);
            return true;
        }

        RulePreset preset = plugin.getPresetManager().getPreset(targetFilter).orElse(null);
        if (preset == null) {
            plugin.getMessageManager().sendMessage(sender, "preset-not-found", "{preset}", targetFilter);
            return true;
        }

        ItemRule itemRule = preset.getItemRule(material).orElse(null);
        if (itemRule == null) {
            plugin.getMessageManager().sendMessage(sender, "item-not-in-target", Map.of(
                    "{item}", material.name(),
                    "{target}", formatTargetLabel(targetFilter)
            ));
            return true;
        }

        sendPresetItemInfo(sender, preset, itemRule);
        return true;
    }

    private void sendDefaultItemInfo(CommandSender sender, ItemRule globalRule) {
        plugin.getMessageManager().sendMessage(sender, "info-simple-list");

        String simpleListReason = plugin.getBlockedItemsManager().getSimpleListReason();
        if (!simpleListReason.isBlank()) {
            plugin.getMessageManager().sendMessage(sender, "info-reason", Map.of(
                    "{reason}", simpleListReason
            ));
        }

        sendItemScopeDetails(sender, globalRule);
    }

    private void sendPresetItemInfo(CommandSender sender, RulePreset preset, ItemRule itemRule) {
        plugin.getMessageManager().sendMessage(sender, "info-rule-entry", Map.of(
                "{id}", preset.getName(),
                "{worlds}", formatWorlds(preset)
        ));

        if (!preset.getDescription().isBlank()) {
            plugin.getMessageManager().sendMessage(sender, "info-description", Map.of(
                    "{description}", preset.getDescription()
            ));
        }

        if (!preset.getReason().isBlank()) {
            plugin.getMessageManager().sendMessage(sender, "info-reason", Map.of(
                    "{reason}", preset.getReason()
            ));
        }

        sendItemScopeDetails(sender, itemRule);
    }

    private void sendDefaultInfo(CommandSender sender) {
        plugin.getMessageManager().sendMessage(sender, "global-info-header");

        String reason = plugin.getBlockedItemsManager().getSimpleListReason();
        if (reason != null && !reason.isBlank()) {
            plugin.getMessageManager().sendMessage(sender, "global-info-reason", Map.of("{reason}", reason));
        }

        plugin.getMessageManager().sendMessage(sender, "global-info-items", Map.of(
                "{count}", String.valueOf(plugin.getBlockedItemsManager().getBlockedItemsCount())
        ));
        sendItemList(
                sender,
                plugin.getBlockedItemsManager().getGlobalItems(),
                "global-info-items-list",
                "global-info-empty"
        );
    }

    private void sendPresetOverview(CommandSender sender) {
        plugin.getMessageManager().sendMessage(sender, "preset-header");
        plugin.getMessageManager().sendMessage(sender, "preset-entry", Map.of(
                "{preset}", DEFAULT_TARGET,
                "{description}", plugin.getMessageManager().getMessage("simple-list-label"),
                "{count}", String.valueOf(plugin.getBlockedItemsManager().getBlockedItemsCount())
        ));

        for (RulePreset preset : plugin.getPresetManager().getPresets()) {
            plugin.getMessageManager().sendMessage(sender, "preset-entry", Map.of(
                    "{preset}", preset.getName(),
                    "{description}", preset.getDescription().isBlank()
                            ? plugin.getMessageManager().getMessage("preset-no-description")
                            : preset.getDescription(),
                    "{count}", String.valueOf(preset.getItemCount())
            ));
        }
    }

    private boolean handleHelp(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sendHelpOverview(sender);
            return true;
        }

        switch (args[1].toLowerCase(Locale.ROOT)) {
            case "add":
                if (!canUseAdd(sender) && !canUseAddHand(sender)) {
                    plugin.getMessageManager().sendMessage(sender, "no-permission");
                    return true;
                }
                sendHelpAdd(sender);
                return true;
            case "item":
                if (!canUseItem(sender)) {
                    plugin.getMessageManager().sendMessage(sender, "no-permission");
                    return true;
                }
                sendHelpItem(sender);
                return true;
            case "preset":
                if (!canUsePreset(sender)) {
                    plugin.getMessageManager().sendMessage(sender, "no-permission");
                    return true;
                }
                sendHelpPreset(sender);
                return true;
            default:
                plugin.getMessageManager().sendMessage(sender, "help-topic-unknown", "{topic}", args[1]);
                sendHelpOverview(sender);
                return true;
        }
    }

    private void sendHelpOverview(CommandSender sender) {
        plugin.getMessageManager().sendMessage(sender, "help-main-header");
        boolean basicHeaderShown = false;
        if (canUseAdd(sender) || canUseAddHand(sender) || canUseInfo(sender) || canUseList(sender) || canUseReload(sender)) {
            plugin.getMessageManager().sendMessage(sender, "help-main-basic-header");
            basicHeaderShown = true;
        }

        if (canUseAdd(sender)) {
            plugin.getMessageManager().sendMessage(sender, "help-main-add");
        }
        if (canUseAddHand(sender)) {
            plugin.getMessageManager().sendMessage(sender, "help-main-addhand");
        }
        if (canUseInfo(sender)) {
            plugin.getMessageManager().sendMessage(sender, "help-main-info");
        }
        if (canUseList(sender)) {
            plugin.getMessageManager().sendMessage(sender, "help-main-list");
        }
        if (canUseReload(sender)) {
            plugin.getMessageManager().sendMessage(sender, "help-main-reload");
        }

        if (canUseItem(sender) || canUsePreset(sender)) {
            plugin.getMessageManager().sendMessage(sender, "help-main-manage-header");
            if (canUseItem(sender)) {
                plugin.getMessageManager().sendMessage(sender, "help-main-item");
            }
            if (canUsePreset(sender)) {
                plugin.getMessageManager().sendMessage(sender, "help-main-preset");
            }
        }

        if (!getVisibleHelpTopics(sender).isEmpty()) {
            plugin.getMessageManager().sendMessage(sender, "help-main-more");
        } else if (!basicHeaderShown) {
            plugin.getMessageManager().sendMessage(sender, "no-permission");
        }
    }

    private void sendHelpAdd(CommandSender sender) {
        plugin.getMessageManager().sendMessage(sender, "help-add-header");
        plugin.getMessageManager().sendMessage(sender, "help-add-usage");
        plugin.getMessageManager().sendMessage(sender, "help-add-actions");
        plugin.getMessageManager().sendMessage(sender, "help-add-target");
        plugin.getMessageManager().sendMessage(sender, "help-add-worlds");
        plugin.getMessageManager().sendMessage(sender, "help-add-addhand");
        plugin.getMessageManager().sendMessage(sender, "help-add-example");
    }

    private void sendHelpItem(CommandSender sender) {
        plugin.getMessageManager().sendMessage(sender, "help-item-header");
        plugin.getMessageManager().sendMessage(sender, "help-item-usage");
        plugin.getMessageManager().sendMessage(sender, "help-item-create");
        plugin.getMessageManager().sendMessage(sender, "help-item-delete");
        plugin.getMessageManager().sendMessage(sender, "help-item-info");
        plugin.getMessageManager().sendMessage(sender, "help-item-list");
        plugin.getMessageManager().sendMessage(sender, "help-item-edit");
        plugin.getMessageManager().sendMessage(sender, "help-item-example");
    }

    private void sendHelpPreset(CommandSender sender) {
        plugin.getMessageManager().sendMessage(sender, "help-preset-header");
        plugin.getMessageManager().sendMessage(sender, "help-preset-usage");
        plugin.getMessageManager().sendMessage(sender, "help-preset-manage");
        plugin.getMessageManager().sendMessage(sender, "help-preset-create");
        plugin.getMessageManager().sendMessage(sender, "help-preset-delete");
        plugin.getMessageManager().sendMessage(sender, "help-preset-info");
        plugin.getMessageManager().sendMessage(sender, "help-preset-list");
        plugin.getMessageManager().sendMessage(sender, "help-preset-edit");
        plugin.getMessageManager().sendMessage(sender, "help-preset-default");
        plugin.getMessageManager().sendMessage(sender, "help-preset-example");
    }

    private boolean applyItemMutation(
            CommandSender sender,
            Material material,
            ParsedItemMutation mutation,
            String messageKey,
            boolean append,
            boolean keepSeparateScope
    ) {
        boolean saved;
        if (append) {
            ParsedWorldScope worldScope = mutation.worldScope() == null
                    ? new ParsedWorldScope(WorldScopeMode.DISABLED, Set.of())
                    : mutation.worldScope();
            saved = saveMergedItemToTarget(
                    mutation.target(),
                    material,
                    mutation.actions(),
                    worldScope.mode(),
                    worldScope.worlds(),
                    keepSeparateScope
            );
        } else {
            saved = saveItemToTarget(mutation.target(), material, mutation.actions());
            if (saved && mutation.worldScope() != null) {
                saved = updateItemWorlds(mutation.target(), material, mutation.worldScope().mode(), mutation.worldScope().worlds());
            }
        }

        if (!saved) {
            plugin.getMessageManager().sendMessage(sender, "preset-not-found", "{preset}", mutation.target());
            return true;
        }

        plugin.getMessageManager().sendMessage(sender, messageKey, Map.of(
                "{item}", material.name(),
                "{target}", formatTargetLabel(mutation.target()),
                "{actions}", formatActions(mutation.actions())
        ));

        if (mutation.worldScope() != null) {
            plugin.getMessageManager().sendMessage(sender, "item-worlds-set", Map.of(
                    "{item}", material.name(),
                    "{target}", formatTargetLabel(mutation.target()),
                    "{worlds}", formatWorlds(mutation.worldScope().mode(), mutation.worldScope().worlds())
            ));
        }

        return true;
    }

    private boolean applyItemPatch(CommandSender sender, Material material, ParsedItemPatch patch) {
        ItemRule existingRule = getItemRule(patch.target(), material);
        if (existingRule == null) {
            plugin.getMessageManager().sendMessage(sender, "item-not-in-target", Map.of(
                    "{item}", material.name(),
                    "{target}", formatTargetLabel(patch.target())
            ));
            return true;
        }

        int scopeIndex = resolveScopeIndex(sender, material, patch.target(), existingRule, patch.scopeIndex());
        if (scopeIndex < 0) {
            return true;
        }

        replaceItemScope(
                patch.target(),
                material,
                existingRule,
                scopeIndex,
                patch.actions(),
                patch.worldScope()
        );

        ItemRule updatedRule = getItemRule(patch.target(), material);
        if (updatedRule == null) {
            plugin.getMessageManager().sendMessage(sender, "item-not-in-target", Map.of(
                    "{item}", material.name(),
                    "{target}", formatTargetLabel(patch.target())
            ));
            return true;
        }

        plugin.getMessageManager().sendMessage(sender, "item-scope-updated", Map.of(
                "{item}", material.name(),
                "{target}", formatTargetLabel(patch.target()),
                "{scope}", String.valueOf(scopeIndex),
                "{rule}", formatScopedRule(updatedRule.getScopedRules().get(scopeIndex - 1))
        ));
        return true;
    }

    private void replaceItemScope(
            String target,
            Material material,
            ItemRule existingRule,
            int scopeIndex,
            Set<BlockAction> actions,
            ParsedWorldScope worldScope
    ) {
        List<ItemRule.ScopedRule> scopes = new ArrayList<>(existingRule.getScopedRules());
        ItemRule.ScopedRule currentScope = scopes.get(scopeIndex - 1);
        Set<BlockAction> resolvedActions = actions == null ? currentScope.actions() : actions;
        ParsedWorldScope resolvedWorldScope = worldScope == null
                ? new ParsedWorldScope(currentScope.mode(), currentScope.worlds())
                : worldScope;

        scopes.set(
                scopeIndex - 1,
                new ItemRule.ScopedRule(resolvedActions, resolvedWorldScope.mode(), resolvedWorldScope.worlds())
        );
        replaceItemRule(target, material, new ItemRule(scopes));
    }

    private int resolveScopeIndex(
            CommandSender sender,
            Material material,
            String target,
            ItemRule itemRule,
            Integer requestedScopeIndex
    ) {
        int scopeCount = itemRule.getScopedRules().size();
        if (requestedScopeIndex != null) {
            if (requestedScopeIndex < 1 || requestedScopeIndex > scopeCount) {
                plugin.getMessageManager().sendMessage(sender, "item-scope-invalid", Map.of(
                        "{item}", material.name(),
                        "{target}", formatTargetLabel(target),
                        "{scope}", String.valueOf(requestedScopeIndex),
                        "{count}", String.valueOf(scopeCount)
                ));
                return -1;
            }

            return requestedScopeIndex;
        }

        if (scopeCount == 1) {
            return 1;
        }

        plugin.getMessageManager().sendMessage(sender, "item-scope-required", Map.of(
                "{item}", material.name(),
                "{target}", formatTargetLabel(target),
                "{count}", String.valueOf(scopeCount)
        ));
        return -1;
    }

    private boolean saveItemToTarget(String target, Material material, Set<BlockAction> actions) {
        if (isDefaultTarget(target)) {
            return plugin.getBlockedItemsManager().upsertGlobalItem(material, actions);
        }

        return plugin.getPresetManager().upsertPresetItem(target, material, actions);
    }

    private ItemRule getItemRule(String target, Material material) {
        if (isDefaultTarget(target)) {
            return plugin.getBlockedItemsManager().getGlobalItem(material).orElse(null);
        }

        return plugin.getPresetManager()
                .getPreset(target)
                .flatMap(preset -> preset.getItemRule(material))
                .orElse(null);
    }

    private boolean replaceItemRule(String target, Material material, ItemRule itemRule) {
        if (isDefaultTarget(target)) {
            return plugin.getBlockedItemsManager().replaceGlobalItemRule(material, itemRule);
        }

        return plugin.getPresetManager().replacePresetItemRule(target, material, itemRule);
    }

    private boolean saveMergedItemToTarget(
            String target,
            Material material,
            Set<BlockAction> actions,
            WorldScopeMode mode,
            Set<String> worlds,
            boolean keepSeparateScope
    ) {
        if (isDefaultTarget(target)) {
            return keepSeparateScope
                    ? plugin.getBlockedItemsManager().appendGlobalItem(material, actions, mode, worlds)
                    : plugin.getBlockedItemsManager().mergeGlobalItem(material, actions, mode, worlds);
        }

        return keepSeparateScope
                ? plugin.getPresetManager().appendPresetItem(target, material, actions, mode, worlds)
                : plugin.getPresetManager().mergePresetItem(target, material, actions, mode, worlds);
    }

    private boolean updateItemWorlds(String target, Material material, WorldScopeMode mode, Set<String> worlds) {
        if (isDefaultTarget(target)) {
            return plugin.getBlockedItemsManager().updateGlobalItemWorlds(material, mode, worlds);
        }

        return plugin.getPresetManager().updatePresetItemWorlds(target, material, mode, worlds);
    }

    private boolean removeItemFromTarget(String target, Material material) {
        if (isDefaultTarget(target)) {
            return plugin.getBlockedItemsManager().removeGlobalItem(material);
        }

        return plugin.getPresetManager().removePresetItem(target, material);
    }

    private String formatActions(Set<BlockAction> actions) {
        if (actions == null || actions.isEmpty() || actions.size() == BlockAction.values().length) {
            return "all";
        }

        return actions.stream()
                .map(BlockAction::getKey)
                .map(TextUtils::formatEnumName)
                .collect(Collectors.joining(", "));
    }

    private String formatWorlds(RulePreset preset) {
        return formatWorlds(preset.getWorldScopeMode(), preset.getWorlds());
    }

    private String formatWorlds(WorldScopeMode mode, Set<String> worlds) {
        if (mode == WorldScopeMode.DISABLED || worlds.isEmpty()) {
            return plugin.getMessageManager().getMessage("info-all-worlds");
        }

        String formattedWorlds = worlds.stream().sorted().collect(Collectors.joining(", "));
        return plugin.getMessageManager().getMessage("info-world-list", "{worlds}", formattedWorlds);
    }

    private String formatTargetLabel(String target) {
        return isDefaultTarget(target)
                ? plugin.getMessageManager().getMessage("simple-list-label")
                : target;
    }

    private void sendItemScopeDetails(CommandSender sender, ItemRule itemRule) {
        List<ItemRule.ScopedRule> scopes = itemRule.getScopedRules();
        if (scopes.size() == 1) {
            plugin.getMessageManager().sendMessage(sender, "info-actions", Map.of(
                    "{actions}", formatScopedRule(scopes.getFirst())
            ));
            return;
        }

        plugin.getMessageManager().sendMessage(sender, "info-scopes-header", Map.of(
                "{count}", String.valueOf(scopes.size())
        ));

        for (int index = 0; index < scopes.size(); index++) {
            plugin.getMessageManager().sendMessage(sender, "info-scope-entry", Map.of(
                    "{scope}", String.valueOf(index + 1),
                    "{rule}", formatScopedRule(scopes.get(index))
            ));
        }
    }

    private String formatRuleSummary(ItemRule itemRule) {
        List<ItemRule.ScopedRule> scopes = itemRule.getScopedRules();
        if (scopes.size() == 1) {
            return formatScopedRule(scopes.getFirst());
        }

        List<String> parts = new ArrayList<>();
        for (int index = 0; index < scopes.size(); index++) {
            parts.add("#" + (index + 1) + " " + formatScopedRule(scopes.get(index)));
        }
        return String.join("; ", parts);
    }

    private String formatScopedRule(ItemRule.ScopedRule scopedRule) {
        return formatActions(scopedRule.actions()) + " @ " + formatWorlds(scopedRule.mode(), scopedRule.worlds());
    }

    private void sendItemList(
            CommandSender sender,
            Map<Material, ItemRule> items,
            String headerKey,
            String emptyKey
    ) {
        if (items.isEmpty()) {
            plugin.getMessageManager().sendMessage(sender, emptyKey);
            return;
        }

        plugin.getMessageManager().sendMessage(sender, headerKey);
        items.entrySet().stream()
                .sorted(Map.Entry.comparingByKey(Comparator.comparing(Material::name)))
                .forEach(entry -> sender.sendMessage(plugin.getMessageManager().getMessage(
                        "info-item-entry",
                        Map.of(
                                "{item}", entry.getKey().name(),
                                "{actions}", formatRuleSummary(entry.getValue()),
                                "{worlds_suffix}", ""
                        )
                )));
    }

    private Material parseMaterial(CommandSender sender, String input) {
        Material material = parseMaterialOrNull(input);
        if (material == null) {
            plugin.getMessageManager().sendMessage(sender, "invalid-item", "{item}", input);
        }
        return material;
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

    private ParsedItemMutation parseItemMutation(CommandSender sender, String[] args, int startIndex, String usageKey) {
        if (usesKeyedItemMutationSyntax(args, startIndex)) {
            return parseKeyedItemMutation(sender, args, startIndex, usageKey);
        }

        String target = DEFAULT_TARGET;
        LinkedHashSet<String> actionTokens = new LinkedHashSet<>();
        LinkedHashSet<String> worldTokens = new LinkedHashSet<>();
        WorldScopeMode worldMode = null;
        boolean worldSectionStarted = false;
        boolean worldModeExpected = false;

        for (int index = startIndex; index < args.length; index++) {
            for (String rawChunk : splitArgument(args[index])) {
                String token = rawChunk.toLowerCase(Locale.ROOT);
                if (token.isBlank()) {
                    continue;
                }

                if (!worldSectionStarted) {
                    String resolvedTarget = findCommandTargetName(rawChunk);
                    if (resolvedTarget != null) {
                        target = resolvedTarget;
                        continue;
                    }

                    if (token.equals("world") || token.equals("worlds")) {
                        worldSectionStarted = true;
                        worldModeExpected = true;
                        continue;
                    }

                    if (token.equals("all") || BlockAction.fromKey(token).isPresent()) {
                        actionTokens.add(token);
                        continue;
                    }

                    worldSectionStarted = true;
                    if (token.equals("disabled")) {
                        worldMode = WorldScopeMode.DISABLED;
                    } else {
                        worldMode = WorldScopeMode.WHITELIST;
                        worldTokens.add(token);
                    }
                    continue;
                }

                if (worldModeExpected) {
                    if (token.equals("all") || token.equals("disabled")) {
                        worldMode = WorldScopeMode.DISABLED;
                        worldModeExpected = false;
                        continue;
                    }

                    worldMode = WorldScopeMode.WHITELIST;
                    worldTokens.add(token);
                    worldModeExpected = false;
                    continue;
                }

                worldTokens.add(token);
            }
        }

        EnumSet<BlockAction> actions = actionTokens.isEmpty()
                ? EnumSet.allOf(BlockAction.class)
                : parseActions(sender, actionTokens);
        if (actions == null) {
            return null;
        }

        ParsedWorldScope worldScope = null;
        if (worldSectionStarted) {
            if (worldModeExpected) {
                plugin.getMessageManager().sendMessage(sender, usageKey);
                return null;
            }

            String trailingTarget = extractTrailingTarget(worldTokens);
            if (trailingTarget != null) {
                target = trailingTarget;
            }

            if (worldMode == null) {
                worldMode = worldTokens.isEmpty() ? WorldScopeMode.DISABLED : WorldScopeMode.WHITELIST;
            }

            if (worldMode != WorldScopeMode.DISABLED && worldTokens.isEmpty()) {
                plugin.getMessageManager().sendMessage(sender, usageKey);
                return null;
            }

            worldScope = new ParsedWorldScope(worldMode, worldTokens);
        }

        return new ParsedItemMutation(target, actions, worldScope, null);
    }

    private boolean usesKeyedItemMutationSyntax(String[] args, int startIndex) {
        for (int index = startIndex; index < args.length; index++) {
            String argument = args[index];
            if (argument == null) {
                continue;
            }

            String normalized = argument.trim().toLowerCase(Locale.ROOT);
            if (normalized.startsWith("actions:")
                    || normalized.startsWith("worlds:")
                    || normalized.startsWith("world:")
                    || normalized.startsWith("preset:")
                    || normalized.startsWith("target:")) {
                return true;
            }
        }
        return false;
    }

    private ParsedItemMutation parseKeyedItemMutation(CommandSender sender, String[] args, int startIndex, String usageKey) {
        String target = DEFAULT_TARGET;
        LinkedHashSet<String> actionTokens = new LinkedHashSet<>();
        LinkedHashSet<String> worldTokens = new LinkedHashSet<>();
        WorldScopeMode worldMode = null;
        boolean worldsSpecified = false;

        for (int index = startIndex; index < args.length; index++) {
            String argument = args[index] == null ? "" : args[index].trim();
            if (argument.isBlank()) {
                continue;
            }

            int separatorIndex = argument.indexOf(':');
            if (separatorIndex < 0) {
                String resolvedTarget = findCommandTargetName(argument);
                if (resolvedTarget != null) {
                    target = resolvedTarget;
                    continue;
                }

                plugin.getMessageManager().sendMessage(sender, usageKey);
                return null;
            }

            String key = argument.substring(0, separatorIndex).trim().toLowerCase(Locale.ROOT);
            String rawValue = argument.substring(separatorIndex + 1).trim();
            if (rawValue.isBlank()) {
                plugin.getMessageManager().sendMessage(sender, usageKey);
                return null;
            }

            switch (key) {
                case "actions" -> {
                    List<String> values = splitKeyedValues(rawValue);
                    if (values.isEmpty()) {
                        plugin.getMessageManager().sendMessage(sender, usageKey);
                        return null;
                    }

                    if (values.stream().anyMatch(value -> value.equalsIgnoreCase("all"))) {
                        actionTokens.clear();
                        actionTokens.add("all");
                    } else if (!actionTokens.contains("all")) {
                        values.stream()
                                .map(value -> value.toLowerCase(Locale.ROOT))
                                .forEach(actionTokens::add);
                    }
                }
                case "world", "worlds" -> {
                    List<String> values = splitKeyedValues(rawValue);
                    if (values.isEmpty()) {
                        plugin.getMessageManager().sendMessage(sender, usageKey);
                        return null;
                    }

                    worldsSpecified = true;
                    if (values.stream().anyMatch(value -> value.equalsIgnoreCase("all") || value.equalsIgnoreCase("disabled"))) {
                        worldMode = WorldScopeMode.DISABLED;
                        worldTokens.clear();
                    } else {
                        worldMode = WorldScopeMode.WHITELIST;
                        values.stream()
                                .map(value -> value.toLowerCase(Locale.ROOT))
                                .forEach(worldTokens::add);
                    }
                }
                case "preset", "target" -> {
                    String resolvedTarget = resolveTarget(sender, rawValue, true);
                    if (resolvedTarget == null) {
                        return null;
                    }
                    target = resolvedTarget;
                }
                default -> {
                    plugin.getMessageManager().sendMessage(sender, usageKey);
                    return null;
                }
            }
        }

        EnumSet<BlockAction> actions = actionTokens.isEmpty()
                ? EnumSet.allOf(BlockAction.class)
                : parseActions(sender, actionTokens);
        if (actions == null) {
            return null;
        }

        ParsedWorldScope worldScope = null;
        if (worldsSpecified) {
            worldScope = new ParsedWorldScope(
                    worldMode == null ? WorldScopeMode.DISABLED : worldMode,
                    new LinkedHashSet<>(worldTokens)
            );
        }

        return new ParsedItemMutation(target, actions, worldScope, null);
    }

    private ParsedItemPatch parseItemPatch(CommandSender sender, String[] args, int startIndex, String usageKey) {
        String target = DEFAULT_TARGET;
        LinkedHashSet<String> actionTokens = new LinkedHashSet<>();
        LinkedHashSet<String> worldTokens = new LinkedHashSet<>();
        WorldScopeMode worldMode = null;
        boolean worldsSpecified = false;
        Integer scopeIndex = null;

        for (int index = startIndex; index < args.length; index++) {
            String argument = args[index] == null ? "" : args[index].trim();
            if (argument.isBlank()) {
                continue;
            }

            int separatorIndex = argument.indexOf(':');
            if (separatorIndex < 0) {
                String resolvedTarget = findCommandTargetName(argument);
                if (resolvedTarget != null) {
                    target = resolvedTarget;
                    continue;
                }

                plugin.getMessageManager().sendMessage(sender, usageKey);
                return null;
            }

            String key = argument.substring(0, separatorIndex).trim().toLowerCase(Locale.ROOT);
            String rawValue = argument.substring(separatorIndex + 1).trim();
            if (rawValue.isBlank()) {
                plugin.getMessageManager().sendMessage(sender, usageKey);
                return null;
            }

            switch (key) {
                case "actions" -> {
                    List<String> values = splitKeyedValues(rawValue);
                    if (values.isEmpty()) {
                        plugin.getMessageManager().sendMessage(sender, usageKey);
                        return null;
                    }

                    if (values.stream().anyMatch(value -> value.equalsIgnoreCase("all"))) {
                        actionTokens.clear();
                        actionTokens.add("all");
                    } else if (!actionTokens.contains("all")) {
                        values.stream()
                                .map(value -> value.toLowerCase(Locale.ROOT))
                                .forEach(actionTokens::add);
                    }
                }
                case "world", "worlds" -> {
                    List<String> values = splitKeyedValues(rawValue);
                    if (values.isEmpty()) {
                        plugin.getMessageManager().sendMessage(sender, usageKey);
                        return null;
                    }

                    worldsSpecified = true;
                    if (values.stream().anyMatch(value -> value.equalsIgnoreCase("all") || value.equalsIgnoreCase("disabled"))) {
                        worldMode = WorldScopeMode.DISABLED;
                        worldTokens.clear();
                    } else {
                        worldMode = WorldScopeMode.WHITELIST;
                        values.stream()
                                .map(value -> value.toLowerCase(Locale.ROOT))
                                .forEach(worldTokens::add);
                    }
                }
                case "preset", "target" -> {
                    String resolvedTarget = resolveTarget(sender, rawValue, true);
                    if (resolvedTarget == null) {
                        return null;
                    }
                    target = resolvedTarget;
                }
                case "scope" -> {
                    Integer parsedScopeIndex = parseScopeIndex(rawValue);
                    if (parsedScopeIndex == null) {
                        plugin.getMessageManager().sendMessage(sender, usageKey);
                        return null;
                    }
                    scopeIndex = parsedScopeIndex;
                }
                default -> {
                    plugin.getMessageManager().sendMessage(sender, usageKey);
                    return null;
                }
            }
        }

        EnumSet<BlockAction> actions = null;
        if (!actionTokens.isEmpty()) {
            actions = parseActions(sender, actionTokens);
            if (actions == null) {
                return null;
            }
        }

        ParsedWorldScope worldScope = null;
        if (worldsSpecified) {
            worldScope = new ParsedWorldScope(
                    worldMode == null ? WorldScopeMode.DISABLED : worldMode,
                    new LinkedHashSet<>(worldTokens)
            );
        }

        if (actions == null && worldScope == null) {
            plugin.getMessageManager().sendMessage(sender, usageKey);
            return null;
        }

        return new ParsedItemPatch(target, actions, worldScope, scopeIndex);
    }

    private ParsedItemDelete parseItemDelete(CommandSender sender, String[] args, int startIndex, String usageKey) {
        String target = DEFAULT_TARGET;
        Integer scopeIndex = null;

        for (int index = startIndex; index < args.length; index++) {
            String argument = args[index] == null ? "" : args[index].trim();
            if (argument.isBlank()) {
                continue;
            }

            int separatorIndex = argument.indexOf(':');
            if (separatorIndex >= 0) {
                String key = argument.substring(0, separatorIndex).trim().toLowerCase(Locale.ROOT);
                String rawValue = argument.substring(separatorIndex + 1).trim();
                if (rawValue.isBlank()) {
                    plugin.getMessageManager().sendMessage(sender, usageKey);
                    return null;
                }

                switch (key) {
                    case "preset", "target" -> {
                        String resolvedTarget = resolveTarget(sender, rawValue, true);
                        if (resolvedTarget == null) {
                            return null;
                        }
                        target = resolvedTarget;
                    }
                    case "scope" -> {
                        Integer parsedScopeIndex = parseScopeIndex(rawValue);
                        if (parsedScopeIndex == null) {
                            plugin.getMessageManager().sendMessage(sender, usageKey);
                            return null;
                        }
                        scopeIndex = parsedScopeIndex;
                    }
                    default -> {
                        plugin.getMessageManager().sendMessage(sender, usageKey);
                        return null;
                    }
                }
                continue;
            }

            String resolvedTarget = findCommandTargetName(argument);
            if (resolvedTarget != null) {
                target = resolvedTarget;
                continue;
            }

            plugin.getMessageManager().sendMessage(sender, usageKey);
            return null;
        }

        return new ParsedItemDelete(target, scopeIndex);
    }

    private List<String> splitKeyedValues(String rawValue) {
        return Arrays.stream(rawValue.split(","))
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .collect(Collectors.toList());
    }

    private Integer parseScopeIndex(String rawValue) {
        if (rawValue == null || rawValue.isBlank()) {
            return null;
        }

        try {
            int parsed = Integer.parseInt(rawValue.trim());
            return parsed > 0 ? parsed : null;
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private ParsedTargetedWorldScope parseTargetedWorldScope(CommandSender sender, String[] args, int startIndex, String usageKey) {
        if (args.length <= startIndex) {
            plugin.getMessageManager().sendMessage(sender, usageKey);
            return null;
        }

        LinkedHashSet<String> worldTokens = new LinkedHashSet<>();
        String target = DEFAULT_TARGET;
        String firstToken = args[startIndex].toLowerCase(Locale.ROOT);

        WorldScopeMode worldMode;
        int worldsStartIndex;
        if (firstToken.equals("all") || firstToken.equals("disabled")) {
            worldMode = WorldScopeMode.DISABLED;
            worldsStartIndex = startIndex + 1;
        } else {
            worldMode = WorldScopeMode.WHITELIST;
            worldsStartIndex = startIndex;
        }

        for (int index = worldsStartIndex; index < args.length; index++) {
            for (String rawChunk : splitArgument(args[index])) {
                worldTokens.add(rawChunk.toLowerCase(Locale.ROOT));
            }
        }

        String trailingTarget = extractTrailingTarget(worldTokens);
        if (trailingTarget != null) {
            target = trailingTarget;
        }

        if (worldMode != WorldScopeMode.DISABLED && worldTokens.isEmpty()) {
            plugin.getMessageManager().sendMessage(sender, usageKey);
            return null;
        }

        return new ParsedTargetedWorldScope(target, worldMode, worldTokens);
    }

    private ParsedWorldScope parseWorldScope(CommandSender sender, String[] args, int modeIndex, String usageKey) {
        if (args.length <= modeIndex) {
            plugin.getMessageManager().sendMessage(sender, usageKey);
            return null;
        }

        String firstToken = args[modeIndex].toLowerCase(Locale.ROOT);
        if (firstToken.equals("all") || firstToken.equals("disabled")) {
            return new ParsedWorldScope(WorldScopeMode.DISABLED, Set.of());
        }

        WorldScopeMode scopeMode = WorldScopeMode.WHITELIST;
        int worldsStartIndex = modeIndex;

        Set<String> worlds = Arrays.stream(Arrays.copyOfRange(args, worldsStartIndex, args.length))
                .flatMap(argument -> splitArgument(argument).stream())
                .map(world -> world.toLowerCase(Locale.ROOT))
                .collect(Collectors.toCollection(LinkedHashSet::new));

        if (worlds.isEmpty()) {
            plugin.getMessageManager().sendMessage(sender, usageKey);
            return null;
        }

        return new ParsedWorldScope(scopeMode, worlds);
    }

    private String resolveTarget(CommandSender sender, String rawTarget, boolean allowDefault) {
        String target = findCommandTargetName(rawTarget);
        if (target == null) {
            plugin.getMessageManager().sendMessage(sender, "preset-not-found", "{preset}", rawTarget);
            return null;
        }

        if (!allowDefault && isDefaultTarget(target)) {
            plugin.getMessageManager().sendMessage(sender, "preset-edit-global");
            return null;
        }

        if (!isDefaultTarget(target) && plugin.getPresetManager().getPreset(target).isEmpty()) {
            plugin.getMessageManager().sendMessage(sender, "preset-not-found", "{preset}", rawTarget);
            return null;
        }

        return target;
    }

    private String resolveOptionalInfoTarget(CommandSender sender, String[] args, int index) {
        if (args.length <= index || args[index].equalsIgnoreCase("all")) {
            return null;
        }

        String target = resolveTarget(sender, args[index], true);
        return target == null ? INVALID_TARGET : target;
    }

    private EnumSet<BlockAction> parseActions(CommandSender sender, Set<String> tokens) {
        if (tokens.isEmpty()) {
            plugin.getMessageManager().sendMessage(sender, "invalid-action", "{action}", "");
            return null;
        }

        if (tokens.contains("all")) {
            return EnumSet.allOf(BlockAction.class);
        }

        EnumSet<BlockAction> actions = EnumSet.noneOf(BlockAction.class);
        for (String token : tokens) {
            BlockAction action = BlockAction.fromKey(token).orElse(null);
            if (action == null) {
                plugin.getMessageManager().sendMessage(sender, "invalid-action", "{action}", token);
                return null;
            }
            actions.add(action);
        }

        return actions.isEmpty() ? null : actions;
    }

    private List<String> getTargetSuggestions() {
        List<String> targets = new ArrayList<>();
        targets.add(DEFAULT_TARGET);
        targets.addAll(getPresetReferenceSuggestions());
        return targets;
    }

    private List<String> getVisibleTopLevelCommands(CommandSender sender) {
        List<String> commands = new ArrayList<>();
        if (canUseAdd(sender)) {
            commands.add("add");
        }
        if (canUseAddHand(sender)) {
            commands.add("addhand");
        }
        if (canUseInfo(sender)) {
            commands.add("info");
        }
        if (canUseItem(sender)) {
            commands.add("item");
        }
        if (canUsePreset(sender)) {
            commands.add("preset");
        }
        if (canUseList(sender)) {
            commands.add("list");
        }
        if (canUseReload(sender)) {
            commands.add("reload");
        }
        if (!commands.isEmpty()) {
            commands.add("help");
        }
        return commands;
    }

    private List<String> getVisibleHelpTopics(CommandSender sender) {
        List<String> topics = new ArrayList<>();
        if (canUseAdd(sender) || canUseAddHand(sender)) {
            topics.add("add");
        }
        if (canUseItem(sender)) {
            topics.add("item");
        }
        if (canUsePreset(sender)) {
            topics.add("preset");
        }
        return topics;
    }

    private boolean canUseAdd(CommandSender sender) {
        return sender.hasPermission("itemblocker.add");
    }

    private boolean canUseAddHand(CommandSender sender) {
        return sender.hasPermission("itemblocker.addhand");
    }

    private boolean canUseInfo(CommandSender sender) {
        return sender.hasPermission("itemblocker.info");
    }

    private boolean canUseItem(CommandSender sender) {
        return sender.hasPermission("itemblocker.add")
                || sender.hasPermission("itemblocker.remove")
                || sender.hasPermission("itemblocker.edit")
                || sender.hasPermission("itemblocker.info")
                || sender.hasPermission("itemblocker.list");
    }

    private boolean canUsePreset(CommandSender sender) {
        return sender.hasPermission("itemblocker.preset");
    }

    private boolean canUseList(CommandSender sender) {
        return sender.hasPermission("itemblocker.list");
    }

    private boolean canUseReload(CommandSender sender) {
        return sender.hasPermission("itemblocker.reload");
    }

    private List<String> getAvailableTargets(String selectedTarget) {
        return getTargetSuggestions().stream()
                .filter(target -> selectedTarget == null || !target.equalsIgnoreCase(selectedTarget))
                .collect(Collectors.toList());
    }

    private List<String> getPresetReferenceSuggestions() {
        return plugin.getPresetManager().getPresetNames();
    }

    private List<String> getItemReferenceSuggestions() {
        return Arrays.stream(Material.values())
                .map(Material::name)
                .sorted()
                .collect(Collectors.toList());
    }

    private List<String> completeItemCommand(String[] args) {
        if (args.length == 2) {
            List<String> values = new ArrayList<>();
            values.add("list");
            values.addAll(getItemReferenceSuggestions());
            return filterByPrefix(values, args[1]);
        }

        if (args[1].equalsIgnoreCase("list")) {
            if (args.length == 3) {
                List<String> values = new ArrayList<>();
                values.add("all");
                values.addAll(getTargetSuggestions());
                return filterByPrefix(values, args[2]);
            }
            return List.of();
        }

        if (args.length == 3) {
            return filterByPrefix(List.of("create", "delete", "info", "list", "edit"), args[2]);
        }

        Material material = parseMaterialOrNull(args[1]);
        String action = args[2].toLowerCase(Locale.ROOT);
        switch (action) {
            case "create":
                if (args.length >= 4 && (args[3].equalsIgnoreCase("worlds") || args[3].equalsIgnoreCase("world"))) {
                    return suggestWorldArguments(args, 4, true);
                }
                return suggestItemMutationArguments(args, 3);
            case "edit":
                if (args.length >= 4 && (args[3].equalsIgnoreCase("worlds") || args[3].equalsIgnoreCase("world"))) {
                    return suggestWorldArguments(args, 4, true);
                }
                return suggestItemEditArguments(args, 3, material);
            case "delete":
                return suggestItemDeleteArguments(args, 3, material);
            case "info":
            case "list":
                if (args.length == 4) {
                    List<String> values = new ArrayList<>();
                    values.add("all");
                    values.addAll(getTargetSuggestions());
                    return filterByPrefix(values, args[3]);
                }
                return List.of();
            default:
                return List.of();
        }
    }

    private List<String> completePresetCommand(String[] args) {
        if (args.length == 2) {
            List<String> values = new ArrayList<>();
            values.add("set");
            values.add("list");
            values.add(DEFAULT_TARGET);
            values.addAll(getPresetReferenceSuggestions());
            return filterByPrefix(values, args[1]);
        }

        if (args[1].equalsIgnoreCase("list")) {
            return List.of();
        }

        if (args[1].equalsIgnoreCase("set")) {
            if (args.length == 3) {
                return filterByPrefix(getPresetReferenceSuggestions(), args[2]);
            }
            return List.of();
        }

        if (args.length == 3) {
            return filterByPrefix(List.of("create", "delete", "info", "list", "edit"), args[2]);
        }

        if (!args[2].equalsIgnoreCase("edit")) {
            return List.of();
        }

        if (args.length == 4) {
            return filterByPrefix(List.of("description", "reason", "worlds"), args[3]);
        }

        if (args[3].equalsIgnoreCase("worlds") || args[3].equalsIgnoreCase("world")) {
            return suggestWorldArguments(args, 4);
        }

        return List.of();
    }

    private String findCommandTargetName(String rawTarget) {
        String normalized = normalizeTargetIdentifier(rawTarget);
        if (normalized == null) {
            return null;
        }

        if (isDefaultTarget(normalized)) {
            return DEFAULT_TARGET;
        }

        if (isPresetReference(rawTarget)) {
            return normalized;
        }

        return plugin.getPresetManager().getPreset(normalized)
                .map(RulePreset::getName)
                .orElse(null);
    }

    private String findExplicitTargetName(String rawTarget) {
        String normalized = normalizeTargetIdentifier(rawTarget);
        if (normalized == null) {
            return null;
        }

        if (isDefaultTarget(normalized)) {
            return DEFAULT_TARGET;
        }

        return plugin.getPresetManager().getPreset(normalized)
                .map(RulePreset::getName)
                .orElse(null);
    }

    private String normalizeTargetIdentifier(String rawTarget) {
        if (rawTarget == null || rawTarget.isBlank()) {
            return null;
        }

        String normalized = rawTarget.trim().toLowerCase(Locale.ROOT);
        if (normalized.startsWith("preset:")) {
            String value = normalized.substring("preset:".length()).trim();
            return value.isBlank() ? null : value;
        }

        return isDefaultAlias(normalized) ? DEFAULT_TARGET : normalized;
    }

    private String stripItemPrefix(String rawItem) {
        if (rawItem == null) {
            return "";
        }

        String normalized = rawItem.trim();
        if (normalized.regionMatches(true, 0, "item:", 0, "item:".length())) {
            return normalized.substring("item:".length());
        }
        return normalized;
    }

    private boolean isDefaultAlias(String value) {
        return value != null && (
                value.equalsIgnoreCase(DEFAULT_TARGET)
                        || value.equalsIgnoreCase(LEGACY_DEFAULT_TARGET)
        );
    }

    private boolean isDefaultTarget(String value) {
        return DEFAULT_TARGET.equalsIgnoreCase(value);
    }

    private boolean isItemReference(String value) {
        return value != null && parseMaterialOrNull(value) != null;
    }

    private boolean isPresetReference(String value) {
        return value != null && (
                value.regionMatches(true, 0, "preset:", 0, "preset:".length())
                        || isDefaultAlias(value)
        );
    }

    private List<String> suggestMaterials(String prefix) {
        String normalizedPrefix = stripItemPrefix(prefix).toUpperCase(Locale.ROOT);
        return Arrays.stream(Material.values())
                .map(Material::name)
                .filter(material -> material.startsWith(normalizedPrefix))
                .sorted()
                .collect(Collectors.toList());
    }

    private List<String> suggestItemMutationArguments(String[] args, int startIndex) {
        KeyedItemMutationSuggestionState state = analyzeKeyedItemMutationSuggestionState(args, startIndex);
        String currentPrefix = args[args.length - 1] == null ? "" : args[args.length - 1].trim();
        String normalizedPrefix = currentPrefix.toLowerCase(Locale.ROOT);

        if (normalizedPrefix.startsWith("actions:")) {
            return suggestKeyedActionArguments(currentPrefix, state);
        }

        if (normalizedPrefix.startsWith("worlds:") || normalizedPrefix.startsWith("world:")) {
            return suggestKeyedWorldArguments(currentPrefix, state);
        }

        if (normalizedPrefix.startsWith("preset:") || normalizedPrefix.startsWith("target:")) {
            return suggestKeyedTargetArguments(currentPrefix, state);
        }

        LinkedHashSet<String> values = new LinkedHashSet<>();
        if (!state.allActionsSelected()) {
            values.addAll(suggestKeyedActionArguments("actions:", state));
        }
        if (!state.worldsDisabled()) {
            values.addAll(suggestKeyedWorldArguments("worlds:", state));
        }
        if (!state.targetSelected()) {
            values.addAll(suggestKeyedTargetArguments("preset:", state));
        }
        return filterByPrefix(new ArrayList<>(values), currentPrefix);
    }

    private List<String> suggestItemEditArguments(String[] args, int startIndex, Material material) {
        KeyedItemMutationSuggestionState state = analyzeKeyedItemMutationSuggestionState(args, startIndex);
        String currentPrefix = args[args.length - 1] == null ? "" : args[args.length - 1].trim();
        String activeTarget = resolveSuggestedTarget(state.selectedTarget());
        Integer selectedScope = extractSelectedScope(args, startIndex);

        if (currentPrefix.toLowerCase(Locale.ROOT).startsWith("scope:")) {
            return suggestScopeArguments(currentPrefix, activeTarget, material);
        }

        LinkedHashSet<String> values = new LinkedHashSet<>(suggestItemMutationArguments(args, startIndex));
        if (selectedScope == null) {
            values.addAll(suggestScopeArguments("scope:", activeTarget, material));
        }
        return filterByPrefix(new ArrayList<>(values), currentPrefix);
    }

    private List<String> suggestItemDeleteArguments(String[] args, int startIndex, Material material) {
        String currentPrefix = args[args.length - 1] == null ? "" : args[args.length - 1].trim();
        String activeTarget = resolveSuggestedTarget(extractSelectedTarget(args, startIndex));
        Integer selectedScope = extractSelectedScope(args, startIndex);

        if (currentPrefix.toLowerCase(Locale.ROOT).startsWith("scope:")) {
            return suggestScopeArguments(currentPrefix, activeTarget, material);
        }

        if (currentPrefix.toLowerCase(Locale.ROOT).startsWith("preset:")
                || currentPrefix.toLowerCase(Locale.ROOT).startsWith("target:")) {
            return suggestKeyedTargetArguments(currentPrefix, new KeyedItemMutationSuggestionState(
                    Set.of(),
                    false,
                    Set.of(),
                    false,
                    extractSelectedTarget(args, startIndex) != null,
                    extractSelectedTarget(args, startIndex)
            ));
        }

        LinkedHashSet<String> values = new LinkedHashSet<>();
        if (selectedScope == null) {
            values.addAll(suggestScopeArguments("scope:", activeTarget, material));
        }
        values.addAll(getTargetSuggestions());
        return filterByPrefix(new ArrayList<>(values), currentPrefix);
    }

    private KeyedItemMutationSuggestionState analyzeKeyedItemMutationSuggestionState(String[] args, int startIndex) {
        LinkedHashSet<String> selectedActions = new LinkedHashSet<>();
        LinkedHashSet<String> selectedWorlds = new LinkedHashSet<>();
        boolean allActionsSelected = false;
        boolean worldsDisabled = false;
        boolean targetSelected = false;
        String selectedTarget = null;

        for (int index = startIndex; index < args.length - 1; index++) {
            String argument = args[index] == null ? "" : args[index].trim();
            if (argument.isBlank()) {
                continue;
            }

            int separatorIndex = argument.indexOf(':');
            if (separatorIndex < 0) {
                continue;
            }

            String key = argument.substring(0, separatorIndex).trim().toLowerCase(Locale.ROOT);
            String rawValue = argument.substring(separatorIndex + 1).trim();
            if (rawValue.isBlank()) {
                continue;
            }

            switch (key) {
                case "actions" -> {
                    List<String> values = splitKeyedValues(rawValue).stream()
                            .map(value -> value.toLowerCase(Locale.ROOT))
                            .toList();
                    if (values.contains("all")) {
                        allActionsSelected = true;
                        selectedActions.clear();
                    } else if (!allActionsSelected) {
                        selectedActions.addAll(values);
                    }
                }
                case "world", "worlds" -> {
                    List<String> values = splitKeyedValues(rawValue).stream()
                            .map(value -> value.toLowerCase(Locale.ROOT))
                            .toList();
                    if (values.contains("all") || values.contains("disabled")) {
                        worldsDisabled = true;
                        selectedWorlds.clear();
                    } else if (!worldsDisabled) {
                        selectedWorlds.addAll(values);
                    }
                }
                case "preset", "target" -> {
                    String resolvedTarget = findCommandTargetName(rawValue);
                    if (resolvedTarget != null) {
                        targetSelected = true;
                        selectedTarget = resolvedTarget;
                    }
                }
                default -> {
                }
            }
        }

        if (!allActionsSelected) {
            Set<String> allActionKeys = Arrays.stream(BlockAction.values())
                    .map(BlockAction::getKey)
                    .collect(Collectors.toSet());
            allActionsSelected = selectedActions.containsAll(allActionKeys);
        }

        return new KeyedItemMutationSuggestionState(
                selectedActions,
                allActionsSelected,
                selectedWorlds,
                worldsDisabled,
                targetSelected,
                selectedTarget
        );
    }

    private String extractSelectedTarget(String[] args, int startIndex) {
        for (int index = startIndex; index < args.length - 1; index++) {
            String argument = args[index] == null ? "" : args[index].trim();
            if (argument.isBlank()) {
                continue;
            }

            int separatorIndex = argument.indexOf(':');
            if (separatorIndex >= 0) {
                String key = argument.substring(0, separatorIndex).trim().toLowerCase(Locale.ROOT);
                if (!key.equals("preset") && !key.equals("target")) {
                    continue;
                }

                String rawValue = argument.substring(separatorIndex + 1).trim();
                String resolvedTarget = findExplicitTargetName(rawValue);
                if (resolvedTarget != null) {
                    return resolvedTarget;
                }
                continue;
            }

            String resolvedTarget = findCommandTargetName(argument);
            if (resolvedTarget != null) {
                return resolvedTarget;
            }
        }

        return null;
    }

    private Integer extractSelectedScope(String[] args, int startIndex) {
        for (int index = startIndex; index < args.length - 1; index++) {
            String argument = args[index] == null ? "" : args[index].trim();
            if (argument.isBlank()) {
                continue;
            }

            int separatorIndex = argument.indexOf(':');
            if (separatorIndex < 0) {
                continue;
            }

            String key = argument.substring(0, separatorIndex).trim().toLowerCase(Locale.ROOT);
            if (!key.equals("scope")) {
                continue;
            }

            Integer parsedScopeIndex = parseScopeIndex(argument.substring(separatorIndex + 1).trim());
            if (parsedScopeIndex != null) {
                return parsedScopeIndex;
            }
        }

        return null;
    }

    private String resolveSuggestedTarget(String target) {
        return target == null || target.isBlank() ? DEFAULT_TARGET : target;
    }

    private List<String> suggestScopeArguments(String prefix, String target, Material material) {
        if (material == null) {
            return List.of();
        }

        String normalizedPrefix = prefix == null ? "" : prefix.trim();
        String valuePrefix = normalizedPrefix.toLowerCase(Locale.ROOT).startsWith("scope:")
                ? normalizedPrefix.substring("scope:".length())
                : normalizedPrefix;
        String activeTarget = resolveSuggestedTarget(target);
        ItemRule itemRule = getItemRule(activeTarget, material);
        if (itemRule == null) {
            return List.of();
        }

        List<ItemRule.ScopedRule> scopes = itemRule.getScopedRules();
        if (scopes.size() <= 1) {
            return List.of();
        }

        List<String> values = new ArrayList<>();
        for (int index = 1; index <= scopes.size(); index++) {
            values.add("scope:" + index);
        }
        return filterByPrefix(values, "scope:" + valuePrefix);
    }

    private List<String> suggestKeyedActionArguments(String currentPrefix, KeyedItemMutationSuggestionState state) {
        String keyPrefix = "actions:";
        String rawValue = currentPrefix.length() >= keyPrefix.length()
                ? currentPrefix.substring(keyPrefix.length()).toLowerCase(Locale.ROOT)
                : "";
        ParsedKeyedListInput input = parseKeyedListInput(rawValue);

        LinkedHashSet<String> selectedActions = new LinkedHashSet<>(state.selectedActions());
        selectedActions.addAll(input.committedValues());
        boolean allSelected = state.allActionsSelected() || selectedActions.contains("all");

        LinkedHashSet<String> values = new LinkedHashSet<>();
        if (!allSelected && input.committedValues().isEmpty() && "all".startsWith(input.partialValue())) {
            values.add(keyPrefix + "all");
        }

        if (allSelected) {
            values.add(keyPrefix + "all");
            return filterByPrefix(new ArrayList<>(values), currentPrefix);
        }

        for (BlockAction action : BlockAction.values()) {
            String actionKey = action.getKey();
            if (selectedActions.contains(actionKey) || !actionKey.startsWith(input.partialValue())) {
                continue;
            }

            List<String> suggestionValues = new ArrayList<>(input.committedValues());
            suggestionValues.add(actionKey);
            values.add(keyPrefix + String.join(",", suggestionValues));
        }

        return filterByPrefix(new ArrayList<>(values), currentPrefix);
    }

    private List<String> suggestKeyedWorldArguments(String currentPrefix, KeyedItemMutationSuggestionState state) {
        String keyPrefix = currentPrefix.toLowerCase(Locale.ROOT).startsWith("world:") ? "world:" : "worlds:";
        String rawValue = currentPrefix.length() >= keyPrefix.length()
                ? currentPrefix.substring(keyPrefix.length()).toLowerCase(Locale.ROOT)
                : "";
        ParsedKeyedListInput input = parseKeyedListInput(rawValue);

        LinkedHashSet<String> selectedWorlds = new LinkedHashSet<>(state.selectedWorlds());
        selectedWorlds.addAll(input.committedValues());
        boolean worldsDisabled = state.worldsDisabled() || selectedWorlds.contains("all") || selectedWorlds.contains("disabled");

        LinkedHashSet<String> values = new LinkedHashSet<>();
        if (input.committedValues().isEmpty()) {
            if ("all".startsWith(input.partialValue())) {
                values.add(keyPrefix + "all");
            }
            if ("disabled".startsWith(input.partialValue())) {
                values.add(keyPrefix + "disabled");
            }
        }

        if (worldsDisabled) {
            return filterByPrefix(new ArrayList<>(values), currentPrefix);
        }

        for (String world : suggestKnownWorlds(input.partialValue(), selectedWorlds)) {
            List<String> suggestionValues = new ArrayList<>(input.committedValues());
            suggestionValues.add(world);
            values.add(keyPrefix + String.join(",", suggestionValues));
        }

        return filterByPrefix(new ArrayList<>(values), currentPrefix);
    }

    private List<String> suggestKeyedTargetArguments(String currentPrefix, KeyedItemMutationSuggestionState state) {
        String keyPrefix = currentPrefix.toLowerCase(Locale.ROOT).startsWith("target:") ? "target:" : "preset:";
        String rawValue = currentPrefix.length() >= keyPrefix.length()
                ? currentPrefix.substring(keyPrefix.length()).toLowerCase(Locale.ROOT)
                : "";

        return getAvailableTargets(state.selectedTarget()).stream()
                .map(target -> keyPrefix + target)
                .filter(value -> value.toLowerCase(Locale.ROOT).startsWith((keyPrefix + rawValue).toLowerCase(Locale.ROOT)))
                .collect(Collectors.toList());
    }

    private ParsedKeyedListInput parseKeyedListInput(String rawValue) {
        String safeValue = rawValue == null ? "" : rawValue.toLowerCase(Locale.ROOT);
        String[] parts = safeValue.split(",", -1);
        List<String> committedValues = new ArrayList<>();
        boolean trailingComma = safeValue.endsWith(",");

        int committedLimit = trailingComma ? parts.length : Math.max(parts.length - 1, 0);
        for (int index = 0; index < committedLimit; index++) {
            String value = parts[index].trim();
            if (!value.isBlank()) {
                committedValues.add(value);
            }
        }

        String partialValue = trailingComma || parts.length == 0
                ? ""
                : parts[parts.length - 1].trim();
        return new ParsedKeyedListInput(committedValues, partialValue);
    }

    private List<String> suggestWorldArguments(String[] args, int startIndex) {
        return suggestWorldArguments(args, startIndex, false);
    }

    private List<String> suggestWorldArguments(String[] args, int startIndex, boolean includeTargets) {
        if (args.length == startIndex + 1) {
            LinkedHashSet<String> values = new LinkedHashSet<>();
            values.add("all");
            values.add("disabled");
            values.addAll(suggestKnownWorlds(args[startIndex]));
            return filterByPrefix(new ArrayList<>(values), args[startIndex]);
        }

        String currentPrefix = args[args.length - 1];
        ExplicitWorldSuggestionState state = analyzeExplicitWorldSuggestionState(args, startIndex);
        LinkedHashSet<String> values = new LinkedHashSet<>();

        if (state.mode() != WorldScopeMode.DISABLED) {
            values.addAll(suggestKnownWorlds(currentPrefix, state.selectedWorlds()));
        }

        if (includeTargets && !state.targetSelected()) {
            values.addAll(filterByPrefix(getAvailableTargets(state.selectedTarget()), currentPrefix));
        }
        return new ArrayList<>(values);
    }

    private List<String> filterByPrefix(List<String> values, String prefix) {
        String normalizedPrefix = prefix.toLowerCase(Locale.ROOT);
        return values.stream()
                .filter(value -> value.toLowerCase(Locale.ROOT).startsWith(normalizedPrefix))
                .collect(Collectors.toList());
    }

    private ItemMutationSuggestionState analyzeItemMutationSuggestionState(String[] args, int startIndex) {
        LinkedHashSet<String> selectedActions = new LinkedHashSet<>();
        LinkedHashSet<String> selectedWorlds = new LinkedHashSet<>();
        boolean worldSectionStarted = false;
        boolean worldModeExpected = false;
        boolean targetSelected = false;
        String selectedTarget = null;
        WorldScopeMode worldMode = null;

        for (int index = startIndex; index < args.length - 1; index++) {
            for (String rawChunk : splitArgument(args[index])) {
                String token = rawChunk.toLowerCase(Locale.ROOT);
                if (token.isBlank()) {
                    continue;
                }

                if (!worldSectionStarted) {
                    String resolvedTarget = findCommandTargetName(rawChunk);
                    if (resolvedTarget != null) {
                        selectedTarget = resolvedTarget;
                        targetSelected = true;
                        continue;
                    }

                    if (token.equals("world") || token.equals("worlds")) {
                        worldSectionStarted = true;
                        worldModeExpected = true;
                        continue;
                    }

                    if (token.equals("all") || BlockAction.fromKey(token).isPresent()) {
                        selectedActions.add(token);
                        continue;
                    }

                    worldSectionStarted = true;
                    if (token.equals("disabled")) {
                        worldMode = WorldScopeMode.DISABLED;
                    } else {
                        worldMode = WorldScopeMode.WHITELIST;
                        selectedWorlds.add(token);
                    }
                    continue;
                }

                if (worldModeExpected) {
                    if (token.equals("all") || token.equals("disabled")) {
                        worldMode = WorldScopeMode.DISABLED;
                    } else {
                        worldMode = WorldScopeMode.WHITELIST;
                        selectedWorlds.add(token);
                    }
                    worldModeExpected = false;
                    continue;
                }

                String resolvedTarget = findExplicitTargetName(rawChunk);
                if (resolvedTarget != null && !isKnownWorld(rawChunk)) {
                    selectedTarget = resolvedTarget;
                    targetSelected = true;
                    continue;
                }

                selectedWorlds.add(token);
            }
        }

        return new ItemMutationSuggestionState(
                selectedActions,
                selectedWorlds,
                worldSectionStarted,
                worldModeExpected,
                worldMode,
                targetSelected,
                selectedTarget
        );
    }

    private ExplicitWorldSuggestionState analyzeExplicitWorldSuggestionState(String[] args, int startIndex) {
        String modeToken = startIndex < args.length ? args[startIndex].toLowerCase(Locale.ROOT) : "";
        WorldScopeMode mode;
        int worldsStartIndex = startIndex + 1;
        LinkedHashSet<String> selectedWorlds = new LinkedHashSet<>();
        if (modeToken.equals("all") || modeToken.equals("disabled")) {
            mode = WorldScopeMode.DISABLED;
        } else {
            mode = WorldScopeMode.WHITELIST;
            if (!modeToken.isBlank()) {
                selectedWorlds.add(modeToken);
            }
        }

        boolean targetSelected = false;
        String selectedTarget = null;

        for (int index = worldsStartIndex; index < args.length - 1; index++) {
            for (String rawChunk : splitArgument(args[index])) {
                String resolvedTarget = findExplicitTargetName(rawChunk);
                if (resolvedTarget != null && !isKnownWorld(rawChunk)) {
                    selectedTarget = resolvedTarget;
                    targetSelected = true;
                    continue;
                }

                selectedWorlds.add(rawChunk.toLowerCase(Locale.ROOT));
            }
        }

        return new ExplicitWorldSuggestionState(mode, selectedWorlds, targetSelected, selectedTarget);
    }

    private List<String> splitArgument(String rawValue) {
        return Arrays.stream(rawValue.split(","))
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .collect(Collectors.toList());
    }

    private String extractTrailingTarget(LinkedHashSet<String> worldTokens) {
        if (worldTokens.isEmpty()) {
            return null;
        }

        List<String> values = new ArrayList<>(worldTokens);
        String candidate = values.get(values.size() - 1);
        String resolvedTarget = findExplicitTargetName(candidate);
        if (resolvedTarget == null || isKnownWorld(candidate)) {
            return null;
        }

        worldTokens.remove(candidate);
        return resolvedTarget;
    }

    private List<String> suggestKnownWorlds(String prefix) {
        return suggestKnownWorlds(prefix, Set.of());
    }

    private List<String> suggestKnownWorlds(String prefix, Set<String> excludedWorlds) {
        LinkedHashSet<String> values = plugin.getServer().getWorlds().stream()
                .map(world -> world.getName().toLowerCase(Locale.ROOT))
                .collect(Collectors.toCollection(LinkedHashSet::new));

        values.add("world");
        values.add("world_nether");
        values.add("world_the_end");

        if (excludedWorlds != null) {
            excludedWorlds.forEach(world -> values.remove(world.toLowerCase(Locale.ROOT)));
        }

        return filterByPrefix(new ArrayList<>(values), prefix);
    }

    private boolean isKnownWorld(String value) {
        if (value == null || value.isBlank()) {
            return false;
        }

        String normalized = value.toLowerCase(Locale.ROOT);
        return plugin.getServer().getWorlds().stream()
                .map(world -> world.getName().toLowerCase(Locale.ROOT))
                .anyMatch(world -> world.equals(normalized));
    }

    private record ParsedItemMutation(String target, EnumSet<BlockAction> actions, ParsedWorldScope worldScope, Integer scopeIndex) {
    }

    private record ParsedItemPatch(String target, EnumSet<BlockAction> actions, ParsedWorldScope worldScope, Integer scopeIndex) {
    }

    private record ParsedItemDelete(String target, Integer scopeIndex) {
    }

    private record ParsedWorldScope(WorldScopeMode mode, Set<String> worlds) {
    }

    private record ParsedTargetedWorldScope(String target, WorldScopeMode mode, Set<String> worlds) {
    }

    private record ItemMutationSuggestionState(
            Set<String> selectedActions,
            Set<String> selectedWorlds,
            boolean worldSectionStarted,
            boolean worldModeExpected,
            WorldScopeMode worldMode,
            boolean targetSelected,
            String selectedTarget
    ) {
    }

    private record KeyedItemMutationSuggestionState(
            Set<String> selectedActions,
            boolean allActionsSelected,
            Set<String> selectedWorlds,
            boolean worldsDisabled,
            boolean targetSelected,
            String selectedTarget
    ) {
    }

    private record ParsedKeyedListInput(
            List<String> committedValues,
            String partialValue
    ) {
    }

    private record ExplicitWorldSuggestionState(
            WorldScopeMode mode,
            Set<String> selectedWorlds,
            boolean targetSelected,
            String selectedTarget
    ) {
    }
}
