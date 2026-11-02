package pl.variant.itemblocker.scan;

/**
 * What a scan does with the forbidden items it finds.
 */
public enum ScanMode {

    /** Only lists them. */
    REPORT,

    /** Deletes them. */
    REMOVE,

    /** Moves them to {@link ConfiscatedItems}, from where an admin can take them. */
    CONFISCATE
}
