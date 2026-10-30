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
    RESTRICTION_POTION("restriction.potion"),
    RESTRICTION_REASON("restriction.reason"),

    WORD_ALL_ACTIONS("word.all-actions"),
    WORD_NO_ACTIONS("word.no-actions"),
    WORD_ALL_WORLDS("word.all-worlds"),
    WORD_NONE("word.none"),

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
    ERROR_EMPTY_HAND("error.empty-hand"),
    ERROR_UNKNOWN_PRESET("error.unknown-preset"),
    ERROR_SAVE_FAILED("error.save-failed"),
    ERROR_RELOAD_FAILED("error.reload-failed"),

    ITEM_BLOCKED("item.blocked"),
    ITEM_UPDATED("item.updated"),
    ITEM_UNBLOCKED("item.unblocked"),
    ITEM_NOT_BLOCKED("item.not-blocked"),

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

    SHOW_ITEM_HEADER("show.item-header"),
    SHOW_ITEM_ENTRY("show.item-entry"),
    SHOW_ITEM_EMPTY("show.item-empty"),
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
    LIST_EMPTY("list.empty"),

    PRESETS_HEADER("presets.header"),
    PRESETS_ENTRY("presets.entry"),
    PRESETS_NO_DESCRIPTION("presets.no-description"),
    PRESETS_DETAILS_HEADER("presets.details-header"),
    PRESETS_DETAILS_DESCRIPTION("presets.details-description"),
    PRESETS_DETAILS_REASON("presets.details-reason"),
    PRESETS_DETAILS_WORLDS("presets.details-worlds"),
    PRESETS_DETAILS_COUNTS("presets.details-counts"),

    RELOADED("reloaded"),

    HELP_HEADER("help.header"),
    HELP_BLOCK("help.block"),
    HELP_BLOCK_ENCHANTMENT("help.block-enchantment"),
    HELP_BLOCK_POTION("help.block-potion"),
    HELP_EDIT("help.edit"),
    HELP_UNBLOCK("help.unblock"),
    HELP_SHOW("help.show"),
    HELP_LIST("help.list"),
    HELP_PRESET("help.preset"),
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
