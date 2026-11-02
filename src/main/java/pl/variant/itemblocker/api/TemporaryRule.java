package pl.variant.itemblocker.api;

/**
 * A rule added through {@link ItemBlockerApi}. It lasts until it is removed or the server stops.
 */
public interface TemporaryRule {

    void remove();

    boolean isActive();
}
