package com.breakinblocks.bblink.core.api;

import javax.annotation.Nullable;
import java.text.Normalizer;
import java.util.regex.Pattern;

/**
 * Turns player names / nicknames into plain text suitable for relaying.
 * <p>
 * Removes colour and style codes (legacy section-sign codes, ampersand codes, hex colours, MiniMessage-style
 * tags such as gradients and rainbows) and decorative or invisible characters (symbols, emoji, resource-pack
 * glyphs, zero-width and control characters). What remains is the readable name.
 */
public final class NameSanitizer {
    /**
     * Legacy formatting: {@code §a}, {@code §l}, and each pair of the {@code §x§r§r§g§g§b§b} hex form.
     */
    private static final Pattern SECTION_CODES = Pattern.compile("\u00a7[0-9a-fk-orx]?", Pattern.CASE_INSENSITIVE);
    /**
     * Ampersand codes left unparsed by nickname plugins: {@code &a}, {@code &#ff00aa}, {@code &x&f&f...}.
     */
    private static final Pattern AMPERSAND_CODES = Pattern.compile("&(?:#[0-9a-f]{6}|[0-9a-fk-orx])", Pattern.CASE_INSENSITIVE);
    /**
     * Bare hex colours some formats use: {@code {#ff00aa}} and {@code <#ff00aa>}.
     */
    private static final Pattern BRACE_HEX = Pattern.compile("\\{#[0-9a-f]{6}}|</?#[0-9a-f]{6}>", Pattern.CASE_INSENSITIVE);
    /**
     * MiniMessage / Placeholder API text-format style tags, e.g. {@code <gradient:#f00:#00f>}, {@code </rainbow>}, {@code <bold>}.
     */
    private static final Pattern STYLE_TAGS = Pattern.compile(
            "</?(?:gradient|rainbow|transition|color|colour|c|shadow|font|hover|click|insert|reset|"
                    + "bold|b|italic|em|i|underlined|u|strikethrough|st|obfuscated|obf|"
                    + "black|dark_blue|dark_green|dark_aqua|dark_red|dark_purple|gold|gray|grey|dark_gray|dark_grey|"
                    + "blue|green|aqua|red|light_purple|yellow|white)(?::[^<>]*)?>",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern WHITESPACE = Pattern.compile("\\s+");
    private static final int MAX_COMBINING_MARKS = 2;

    private NameSanitizer() {
    }

    /**
     * Clean a name for display.
     *
     * @param preferred The nickname / display name, possibly containing formatting.
     * @param fallback  Used if {@code preferred} is null or nothing readable is left after cleaning
     *                  (normally the account name, which is always plain).
     * @return Plain text name.
     */
    public static String cleanName(@Nullable String preferred, String fallback) {
        String cleaned = preferred == null ? "" : clean(preferred, true);
        if (!cleaned.isEmpty()) return cleaned;
        String cleanedFallback = clean(fallback, true);
        return cleanedFallback.isEmpty() ? fallback : cleanedFallback;
    }

    /**
     * Remove formatting codes and invisible characters from free text (chat, death messages).
     * Unlike {@link #cleanName}, symbols and emoji are kept since players type those deliberately.
     */
    public static String stripFormatting(String text) {
        return clean(text, false);
    }

    private static String clean(String input, boolean isName) {
        String s = Normalizer.normalize(input, Normalizer.Form.NFC);
        s = SECTION_CODES.matcher(s).replaceAll("");
        if (isName) {
            // Only for names: in chat, "&a" etc. is far more likely to be something a player typed.
            s = AMPERSAND_CODES.matcher(s).replaceAll("");
            s = BRACE_HEX.matcher(s).replaceAll("");
            s = STYLE_TAGS.matcher(s).replaceAll("");
        }

        StringBuilder out = new StringBuilder(s.length());
        int marksInARow = 0;
        for (int i = 0; i < s.length(); ) {
            int cp = s.codePointAt(i);
            i += Character.charCount(cp);
            if (Character.getType(cp) == Character.NON_SPACING_MARK) {
                // Real scripts need one or two marks per letter; long runs are "zalgo" decoration.
                if (++marksInARow > MAX_COMBINING_MARKS) continue;
            } else {
                marksInARow = 0;
            }
            if (keep(cp, isName)) {
                out.appendCodePoint(cp);
            } else if (Character.isWhitespace(cp) || Character.isSpaceChar(cp)) {
                out.append(' ');
            }
        }

        if (isName) {
            return WHITESPACE.matcher(out).replaceAll(" ").trim();
        }
        return out.toString();
    }

    private static boolean keep(int cp, boolean isName) {
        if (cp == '\n' && !isName) return true;
        switch (Character.getType(cp)) {
            case Character.CONTROL:
            case Character.FORMAT: // zero-width joiners/spaces, bidi overrides
            case Character.PRIVATE_USE: // resource-pack icons / rank badges
            case Character.SURROGATE: // unpaired halves
            case Character.UNASSIGNED:
            case Character.LINE_SEPARATOR:
            case Character.PARAGRAPH_SEPARATOR:
                return false;
            case Character.SPACE_SEPARATOR:
                return cp == ' ';
            case Character.OTHER_SYMBOL: // emoji, stars, decorative dingbats
            case Character.MODIFIER_SYMBOL: // emoji skin tones and similar
            case Character.ENCLOSING_MARK: // keycap / circle overlays
                return !isName;
            default:
                return true;
        }
    }
}
