package pl.variant.model;

import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;

public enum BlockAction {

    CRAFTING("crafting", "crafting", "cannot-craft"),
    PICKUP("pickup", "pickup", "cannot-pickup"),
    DROP("drop", "drop", "cannot-drop"),
    USE("use", "use", "cannot-use"),
    PLACE("place", "place", "cannot-place"),
    ARMOR("armor", "armor", "cannot-equip"),
    INVENTORY("inventory", "inventory", "cannot-inventory"),
    HOPPER("hopper", "hopper", "cannot-hopper");

    private final String key;
    private final String permissionSuffix;
    private final String messageKey;

    BlockAction(String key, String permissionSuffix, String messageKey) {
        this.key = key;
        this.permissionSuffix = permissionSuffix;
        this.messageKey = messageKey;
    }

    public String getKey() {
        return key;
    }

    public String getPermissionSuffix() {
        return permissionSuffix;
    }

    public String getMessageKey() {
        return messageKey;
    }

    public static Optional<BlockAction> fromKey(String value) {
        if (value == null || value.isBlank()) {
            return Optional.empty();
        }

        String normalized = value.trim().toLowerCase(Locale.ROOT);
        return Arrays.stream(values())
                .filter(action -> action.key.equals(normalized))
                .findFirst();
    }
}
