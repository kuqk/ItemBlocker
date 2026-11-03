package pl.variant.itemblocker.message;

import pl.variant.itemblocker.rule.Action;

/**
 * Every message the plugin can send, with its path in the language files.
 */
public enum Message {
    PREFIX("prefix"),

    RESTRICTION_CRAFTING("restriction.crafting"),
    RESTRICTION_PICKUP("restriction.pickup"),
    RESTRICTION_DROP("restriction.drop"),
    RESTRICTION_USE("restriction.use"),
    RESTRICTION_PLACE("restriction.place"),
    RESTRICTION_ARMOR("restriction.armor"),
    RESTRICTION_INVENTORY("restriction.inventory"),
    RESTRICTION_HOPPER("restriction.hopper"),
    RESTRICTION_SMITHING("restriction.smithing"),
    RESTRICTION_ATTACK("restriction.attack"),
    RESTRICTION_BREAK("restriction.break"),
    RESTRICTION_BREAK_WITH("restriction.break-with"),
    RESTRICTION_CRAFTER("restriction.crafter"),
    RESTRICTION_DISPENSE("restriction.dispense"),
    RESTRICTION_BREW("restriction.brew"),
    RESTRICTION_TRADE("restriction.trade"),
    RESTRICTION_LOOT("restriction.loot"),
    RESTRICTION_ENTITY_DROP("restriction.entity_drop"),
    RESTRICTION_ENCHANTED_ITEM("restriction.enchanted-item"),
    RESTRICTION_ENCHANTING("restriction.enchanting"),
    RESTRICTION_ENCHANTMENT_DOWNGRADED("restriction.enchantment-downgraded"),
    RESTRICTION_ENCHANTMENT_REMOVED("restriction.enchantment-removed"),
    RESTRICTION_ENCHANTMENT_CONFISCATED("restriction.enchantment-confiscated"),
    RESTRICTION_STACK_LIMIT("restriction.stack-limit"),
    RESTRICTION_INVENTORY_LIMIT("restriction.inventory-limit"),
    RESTRICTION_POTION("restriction.potion"),
    RESTRICTION_REASON("restriction.reason"),

    WORD_ALL_ACTIONS("word.all-actions"),
    WORD_NO_ACTIONS("word.no-actions"),
    WORD_ALL_WORLDS("word.all-worlds"),
    WORD_ALL_REGIONS("word.all-regions"),
    WORD_NONE("word.none"),
    WORD_NO_LIMIT("word.no-limit"),
    WORD_MODE_BLOCK("word.mode-block"),
    WORD_MODE_DOWNGRADE("word.mode-downgrade"),
    WORD_MODE_REMOVE("word.mode-remove"),
    WORD_MODE_CONFISCATE("word.mode-confiscate"),

    ERROR_NO_PERMISSION("error.no-permission"),
    ERROR_PLAYERS_ONLY("error.players-only"),
    ERROR_UNKNOWN_COMMAND("error.unknown-command"),
    ERROR_USAGE("error.usage"),
    ERROR_UNKNOWN_ITEM("error.unknown-item"),
    ERROR_UNKNOWN_WORLD("error.unknown-world"),
    ERROR_UNKNOWN_ENCHANTMENT("error.unknown-enchantment"),
    ERROR_UNKNOWN_MODE("error.unknown-mode"),
    ERROR_UNKNOWN_POTION("error.unknown-potion"),
    ERROR_UNKNOWN_ACTION("error.unknown-action"),
    ERROR_UNKNOWN_OPTION("error.unknown-option"),
    ERROR_INVALID_LEVEL("error.invalid-level"),
    ERROR_INVALID_LIMIT("error.invalid-limit"),
    ERROR_INVALID_MODEL("error.invalid-model"),
    ERROR_EMPTY_HAND("error.empty-hand"),
    ERROR_UNKNOWN_PRESET("error.unknown-preset"),
    ERROR_SAVE_FAILED("error.save-failed"),
    ERROR_RELOAD_FAILED("error.reload-failed"),

    ITEM_BLOCKED("item.blocked"),
    ITEM_UPDATED("item.updated"),
    ITEM_UNBLOCKED("item.unblocked"),
    ITEM_NOT_BLOCKED("item.not-blocked"),

    LIMIT_SET("limit.set"),
    LIMIT_REMOVED("limit.removed"),

    FILTER_SAVED("filter.saved"),
    FILTER_REMOVED("filter.removed"),
    FILTER_UNKNOWN("filter.unknown"),
    FILTER_INVALID_ID("filter.invalid-id"),
    FILTER_EMPTY("filter.empty"),

    GROUP_SAVED("group.saved"),
    GROUP_REMOVED("group.removed"),
    GROUP_UNKNOWN("group.unknown"),
    GROUP_INVALID("group.invalid"),

    ALLOW_ADDED("allow.added"),
    ALLOW_REMOVED("allow.removed"),
    ALLOW_UNKNOWN("allow.unknown"),

    SCAN_STARTED("scan.started"),
    SCAN_HEADER("scan.header"),
    SCAN_ENTRY("scan.entry"),
    SCAN_MORE("scan.more"),
    SCAN_NONE("scan.none"),
    SCAN_HINT("scan.hint"),
    SCAN_REMOVED("scan.removed"),
    SCAN_CONFISCATED("scan.confiscated"),
    SCAN_UNSUPPORTED("scan.unsupported"),

    CONFISCATED_HEADER("confiscated.header"),
    CONFISCATED_ENTRY("confiscated.entry"),
    CONFISCATED_EMPTY("confiscated.empty"),
    CONFISCATED_GIVEN("confiscated.given"),
    CONFISCATED_UNKNOWN("confiscated.unknown"),

    LOG_HEADER("log.header"),
    LOG_ENTRY("log.entry"),
    LOG_EMPTY("log.empty"),

    ENCHANTMENT_BLOCKED("enchantment.blocked"),
    ENCHANTMENT_UNBLOCKED("enchantment.unblocked"),
    ENCHANTMENT_NOT_BLOCKED("enchantment.not-blocked"),

    POTION_BLOCKED("potion.blocked"),
    POTION_UNBLOCKED("potion.unblocked"),
    POTION_NOT_BLOCKED("potion.not-blocked"),

    PRESET_CREATED("preset.created"),
    PRESET_DELETED("preset.deleted"),
    PRESET_EXISTS("preset.exists"),
    PRESET_INVALID_NAME("preset.invalid-name"),
    PRESET_DEFAULT_PROTECTED("preset.default-protected"),
    PRESET_DESCRIPTION_SET("preset.description-set"),
    PRESET_REASON_SET("preset.reason-set"),
    PRESET_WORLDS_SET("preset.worlds-set"),
    PRESET_REGIONS_SET("preset.regions-set"),
    PRESET_REGIONS_UNAVAILABLE("preset.regions-unavailable"),

    SHOW_ITEM_HEADER("show.item-header"),
    SHOW_ITEM_ENTRY("show.item-entry"),
    SHOW_ITEM_EMPTY("show.item-empty"),
    SHOW_LIMIT_ENTRY("show.limit-entry"),
    SHOW_FILTER_ENTRY("show.filter-entry"),
    SHOW_ENCHANTMENT_HEADER("show.enchantment-header"),
    SHOW_POTION_HEADER("show.potion-header"),
    SHOW_LEVEL_ENTRY("show.level-entry"),
    SHOW_LEVEL_EMPTY("show.level-empty"),

    LIST_HEADER("list.header"),
    LIST_PRESET_ENTRY("list.preset-entry"),
    LIST_ITEMS_HEADER("list.items-header"),
    LIST_ITEM_ENTRY("list.item-entry"),
    LIST_ENCHANTMENTS_HEADER("list.enchantments-header"),
    LIST_POTIONS_HEADER("list.potions-header"),
    LIST_LEVEL_ENTRY("list.level-entry"),
    LIST_LIMITS_HEADER("list.limits-header"),
    LIST_LIMIT_ENTRY("list.limit-entry"),
    LIST_FILTERS_HEADER("list.filters-header"),
    LIST_FILTER_ENTRY("list.filter-entry"),
    LIST_GROUPS_HEADER("list.groups-header"),
    LIST_GROUP_ENTRY("list.group-entry"),
    LIST_ALLOWED_HEADER("list.allowed-header"),
    LIST_ALLOWED_ENTRY("list.allowed-entry"),
    LIST_EMPTY("list.empty"),

    PRESETS_HEADER("presets.header"),
    PRESETS_ENTRY("presets.entry"),
    PRESETS_NO_DESCRIPTION("presets.no-description"),
    PRESETS_DETAILS_HEADER("presets.details-header"),
    PRESETS_DETAILS_DESCRIPTION("presets.details-description"),
    PRESETS_DETAILS_REASON("presets.details-reason"),
    PRESETS_DETAILS_WORLDS("presets.details-worlds"),
    PRESETS_DETAILS_REGIONS("presets.details-regions"),
    PRESETS_DETAILS_COUNTS("presets.details-counts"),

    RELOADED("reloaded"),

    GUI_PRESETS_TITLE("gui.presets-title"),
    GUI_PRESET_TITLE("gui.preset-title"),
    GUI_ITEM_TITLE("gui.item-title"),
    GUI_PRESET_NAME("gui.preset-name"),
    GUI_PRESET_LORE("gui.preset-lore"),
    GUI_ENTRY_LORE("gui.entry-lore"),
    GUI_ITEM_LORE("gui.item-lore"),
    GUI_ACTION_NAME("gui.action-name"),
    GUI_ACTION_BLOCKED("gui.action-blocked"),
    GUI_ACTION_ALLOWED("gui.action-allowed"),
    GUI_STACK_NAME("gui.stack-name"),
    GUI_INVENTORY_NAME("gui.inventory-name"),
    GUI_LIMIT_LORE("gui.limit-lore"),
    GUI_WORLDS_NAME("gui.worlds-name"),
    GUI_REMOVE_NAME("gui.remove-name"),
    GUI_REMOVE_LORE("gui.remove-lore"),
    GUI_ADD_NAME("gui.add-name"),
    GUI_ADD_LORE("gui.add-lore"),
    GUI_BACK("gui.back"),
    GUI_CLOSE("gui.close"),
    GUI_PREVIOUS("gui.previous"),
    GUI_NEXT("gui.next"),
    GUI_CREATE_NAME("gui.create-name"),
    GUI_CREATE_LORE("gui.create-lore"),
    GUI_PROMPT("gui.prompt"),
    GUI_PROMPT_CANCELLED("gui.prompt-cancelled"),
    GUI_WHAT_PRESET_NAME("gui.what-preset-name"),
    GUI_WHAT_DESCRIPTION("gui.what-description"),
    GUI_WHAT_REASON("gui.what-reason"),
    GUI_WHAT_WORLDS("gui.what-worlds"),
    GUI_WHAT_REGIONS("gui.what-regions"),
    GUI_WHAT_PATTERN("gui.what-pattern"),
    GUI_WHAT_FILTER_ID("gui.what-filter-id"),
    GUI_WHAT_PERMISSION("gui.what-permission"),
    GUI_WHAT_VALUE("gui.what-value"),
    GUI_SECTION_ITEMS("gui.section-items"),
    GUI_SECTION_GROUPS("gui.section-groups"),
    GUI_SECTION_FILTERS("gui.section-filters"),
    GUI_SECTION_ALLOWED("gui.section-allowed"),
    GUI_SECTION_ENCHANTMENTS("gui.section-enchantments"),
    GUI_SECTION_EFFECTS("gui.section-effects"),
    GUI_SECTION_SETTINGS("gui.section-settings"),
    GUI_SECTION_LORE("gui.section-lore"),
    GUI_DELETE_NAME("gui.delete-name"),
    GUI_DELETE_LORE("gui.delete-lore"),
    GUI_ITEMS_TITLE("gui.items-title"),
    GUI_GROUPS_TITLE("gui.groups-title"),
    GUI_FILTERS_TITLE("gui.filters-title"),
    GUI_ALLOWED_TITLE("gui.allowed-title"),
    GUI_ENCHANTMENTS_TITLE("gui.enchantments-title"),
    GUI_EFFECTS_TITLE("gui.effects-title"),
    GUI_SETTINGS_TITLE("gui.settings-title"),
    GUI_GROUP_TITLE("gui.group-title"),
    GUI_FILTER_TITLE("gui.filter-title"),
    GUI_ADD_GROUP_NAME("gui.add-group-name"),
    GUI_ADD_GROUP_LORE("gui.add-group-lore"),
    GUI_ADD_FILTER_LORE("gui.add-filter-lore"),
    GUI_ADD_ALLOWED_LORE("gui.add-allowed-lore"),
    GUI_GROUP_LORE("gui.group-lore"),
    GUI_FILTER_LORE("gui.filter-lore"),
    GUI_ALLOWED_LORE("gui.allowed-lore"),
    GUI_LEVEL_LORE("gui.level-lore"),
    GUI_ENCHANTMENT_LORE("gui.enchantment-lore"),
    GUI_NOT_BLOCKED("gui.not-blocked"),
    GUI_BYPASS_NAME("gui.bypass-name"),
    GUI_CHANGE_LORE("gui.change-lore"),
    GUI_CLEAR_LORE("gui.clear-lore"),
    GUI_CRITERION_NAME("gui.criterion-name"),
    GUI_DESCRIPTION_NAME("gui.description-name"),
    GUI_REASON_NAME("gui.reason-name"),
    GUI_REGIONS_NAME("gui.regions-name"),

    HELP_HEADER("help.header"),
    HELP_BLOCK("help.block"),
    HELP_BLOCK_ENCHANTMENT("help.block-enchantment"),
    HELP_BLOCK_POTION("help.block-potion"),
    HELP_EDIT("help.edit"),
    HELP_LIMIT("help.limit"),
    HELP_FILTER("help.filter"),
    HELP_GROUP("help.group"),
    HELP_ALLOW("help.allow"),
    HELP_SCAN("help.scan"),
    HELP_CONFISCATED("help.confiscated"),
    HELP_LOG("help.log"),
    HELP_UNBLOCK("help.unblock"),
    HELP_SHOW("help.show"),
    HELP_LIST("help.list"),
    HELP_PRESET("help.preset"),
    HELP_GUI("help.gui"),
    HELP_RELOAD("help.reload"),
    HELP_OPTIONS("help.options"),
    HELP_OPTIONS_HINT("help.options-hint");

    private final String path;

    Message(String path) {
        this.path = path;
    }

    public String path() {
        return path;
    }

    /**
     * Returns the message sent to a player when an item rule blocks the action.
     */
    public static Message restriction(Action action) {
        return switch (action) {
            case CRAFTING -> RESTRICTION_CRAFTING;
            case PICKUP -> RESTRICTION_PICKUP;
            case DROP -> RESTRICTION_DROP;
            case USE -> RESTRICTION_USE;
            case PLACE -> RESTRICTION_PLACE;
            case ARMOR -> RESTRICTION_ARMOR;
            case INVENTORY -> RESTRICTION_INVENTORY;
            case HOPPER -> RESTRICTION_HOPPER;
            case SMITHING -> RESTRICTION_SMITHING;
            case ATTACK -> RESTRICTION_ATTACK;
            case BREAK -> RESTRICTION_BREAK;
            case CRAFTER -> RESTRICTION_CRAFTER;
            case DISPENSE -> RESTRICTION_DISPENSE;
            case BREW -> RESTRICTION_BREW;
            case TRADE -> RESTRICTION_TRADE;
            case LOOT -> RESTRICTION_LOOT;
            case ENTITY_DROP -> RESTRICTION_ENTITY_DROP;
        };
    }
}
