package pl.variant.itemblocker.restriction;

import pl.variant.itemblocker.message.Message;
import pl.variant.itemblocker.message.Placeholder;
import pl.variant.itemblocker.rule.Preset;

import java.util.List;

/**
 * A blocked action: the message explaining it and the preset whose rule blocked it.
 */
public record Denial(Message message, Preset source, List<Placeholder> placeholders) {

    public Denial {
        placeholders = List.copyOf(placeholders);
    }
}
