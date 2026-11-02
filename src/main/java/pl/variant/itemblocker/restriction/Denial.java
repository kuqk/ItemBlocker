package pl.variant.itemblocker.restriction;

import pl.variant.itemblocker.message.Message;
import pl.variant.itemblocker.message.Placeholder;
import pl.variant.itemblocker.rule.Preset;

import java.util.List;
import java.util.Optional;

/**
 * A blocked action: the message explaining it and the preset whose rule blocked it.
 */
public record Denial(Message message, Preset source, List<Placeholder> placeholders) {

    private static final String RESTRICTION_PREFIX = "restriction.";

    public Denial {
        placeholders = List.copyOf(placeholders);
    }

    /**
     * What was blocked, such as {@code place}, {@code enchanting} or {@code stack-limit}.
     */
    public String action() {
        String path = message.path();
        return path.startsWith(RESTRICTION_PREFIX) ? path.substring(RESTRICTION_PREFIX.length()) : path;
    }

    /**
     * The item, or the enchantment or effect with its level, such as {@code TNT} or {@code Sharpness 5}.
     */
    public String subject() {
        Optional<String> item = value("item");
        if (item.isPresent()) {
            return item.get();
        }
        String level = value("level").map(value -> " " + value).orElse("");
        return value("enchantment").or(() -> value("effect")).map(name -> name + level).orElse("?");
    }

    private Optional<String> value(String name) {
        return placeholders.stream()
                .filter(placeholder -> placeholder.name().equals(name))
                .map(Placeholder::value)
                .findFirst();
    }
}
