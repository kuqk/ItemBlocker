package pl.variant.itemblocker.message;

import java.util.Objects;

/**
 * A value that replaces {@code {name}} in a message.
 */
public record Placeholder(String name, String value) {

    public Placeholder {
        Objects.requireNonNull(name, "name");
        value = Objects.requireNonNullElse(value, "");
    }

    public static Placeholder of(String name, Object value) {
        return new Placeholder(name, value == null ? "" : value.toString());
    }
}
