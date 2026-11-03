package pl.variant.itemblocker.testing;

import pl.variant.itemblocker.message.Names;

import java.util.regex.Pattern;

/**
 * Text helpers for asserting on rendered messages.
 */
public final class Text {

    private static final Pattern COLOR_CODE = Pattern.compile("§[0-9a-fk-orx]", Pattern.CASE_INSENSITIVE);

    // The test server does not translate names, so players get the translation keys themselves.
    private static final Pattern TRANSLATION_KEY = Pattern.compile("(?:item|block|enchantment|effect)\\.minecraft\\.([a-z0-9_]+)");

    private Text() {
    }

    /**
     * Removes legacy color codes and shows translated names in English, as an English game does.
     */
    public static String plain(String text) {
        String plain = COLOR_CODE.matcher(text).replaceAll("");
        return TRANSLATION_KEY.matcher(plain).replaceAll(match -> Names.ofKey(match.group(1)));
    }
}
