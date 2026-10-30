package pl.variant.itemblocker.testing;

import java.util.regex.Pattern;

/**
 * Text helpers for asserting on rendered messages.
 */
public final class Text {

    private static final Pattern COLOR_CODE = Pattern.compile("§[0-9a-fk-orx]", Pattern.CASE_INSENSITIVE);

    private Text() {
    }

    /**
     * Removes legacy color codes.
     */
    public static String plain(String text) {
        return COLOR_CODE.matcher(text).replaceAll("");
    }
}
