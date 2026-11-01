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
    RESTRICTION_ENCHANTED_ITEM("restriction.enchanted-item"),
    RESTRICTION_ENCHANTING("restriction.enchanting"),
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

    ERROR_NO_PERMISSION("error.no-permission"),
    ERROR_PLAYERS_ONLY("error.players-only"),
    ERROR_UNKNOWN_COMMAND("error.unknown-command"),
    ERROR_USAGE("error.usage"),
    ERROR_UNKNOWN_ITEM("error.unknown-item"),
    ERROR_UNKNOWN_ENCHANTMENT("error.unknown-enchantment"),
    ERROR_UNKNOWN_POTION("error.unknown-potion"),
    ERROR_UNKNOWN_ACTION("error.unknown-action"),
    ERROR_UNKNOWN_OPTION("error.unknown-option"),
    ERROR_INVALID_LEVEL("error.invalid-level"),
    ERROR_INVALID_LIMIT("error.invalid-limit"),
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
    GUI_WORLDS_LORE("gui.worlds-lore"),
    GUI_REMOVE_NAME("gui.remove-name"),
    GUI_REMOVE_LORE("gui.remove-lore"),
    GUI_ADD_NAME("gui.add-name"),
    GUI_ADD_LORE("gui.add-lore"),
    GUI_BACK("gui.back"),
    GUI_CLOSE("gui.close"),
    GUI_PREVIOUS("gui.previous"),
    GUI_NEXT("gui.next"),

    HELP_HEADER("help.header"),
    HELP_BLOCK("help.block"),
    HELP_BLOCK_ENCHANTMENT("help.block-enchantment"),
    HELP_BLOCK_POTION("help.block-potion"),
    HELP_EDIT("help.edit"),
    HELP_LIMIT("help.limit"),
    HELP_UNBLOCK("help.unblock"),
    HELP_SHOW("help.show"),
    HELP_LIST("help.list"),
    HELP_PRESET("help.preset"),
    HELP_GUI("help.gui"),
    HELP_RELOAD("help.reload"),
    HELP_OPTIONS("help.options");

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
        };
    }
}
