package com.breakinblocks.bbchat.api;

import javax.annotation.Nullable;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class TextUtils {
    private static final Pattern MC_FORMATTING_CODES_ALL = Pattern.compile("\u00a7([0-9a-fklmnor])", Pattern.CASE_INSENSITIVE);
    private static final Pattern MC_FORMATTING_CODES_COLOURS = Pattern.compile("\u00a7([0-9a-f])", Pattern.CASE_INSENSITIVE);
    private static final Pattern MC_FORMATTING_CODES_INCOMPATIBLE = Pattern.compile("\u00a7([0-9a-fk])", Pattern.CASE_INSENSITIVE);
    private static final Pattern MC_FORMATTING_CODES_COMPATIBLE = Pattern.compile("\u00a7([lmnor])", Pattern.CASE_INSENSITIVE);
    private static final Pattern MC_FORMATTING_CODES_RESET = Pattern.compile("\u00a7(r)", Pattern.CASE_INSENSITIVE);

    /**
     * Remove all Minecraft formatting codes
     */
    public static String removeAllMC(String input) {
        return MC_FORMATTING_CODES_ALL.matcher(input).replaceAll("");
    }

    /**
     * Remove all Discord incompatible Minecraft formatting codes
     * Colour codes are replaced with a reset (because Java edition BE doesn't reset on colour)
     */
    public static String removeNonCompMC(String input) {
        Matcher matcher = MC_FORMATTING_CODES_INCOMPATIBLE.matcher(input);
        StringBuffer buff = new StringBuffer();
        while (matcher.find()) {
            String formattingCode = matcher.group();
            if (MC_FORMATTING_CODES_COLOURS.matcher(formattingCode).matches()) {
                matcher.appendReplacement(buff, Formatting.RESET.mcfc);
            } else {
                matcher.appendReplacement(buff, "");
            }
        }
        matcher.appendTail(buff);
        return buff.toString();
    }

    /**
     * Case sensitive word replacement, expects boundaries on left and right.
     */
    public static String replaceWord(String message, String word, String replacement) {
        final Pattern pattern = Pattern.compile("(?<=^|\\W)" + Pattern.quote(word) + "(?=$|\\W)");
        return pattern.matcher(message).replaceAll(replacement);
    }

    /**
     * Convert Minecraft formatting to Discord formatting
     */
    public static String convertToDiscord(String input) {
        return Arrays.stream(removeNonCompMC(input).split("\n"))
                .map(line -> MC_FORMATTING_CODES_RESET.splitAsStream(line)
                        .map(TextUtils::convertToDiscordNoReset)
                        .collect(Collectors.joining()))
                .collect(Collectors.joining("\n"));
    }

    /**
     * Do not pass any reset codes in
     */
    private static String convertToDiscordNoReset(String input) {
        Matcher matcher = MC_FORMATTING_CODES_COMPATIBLE.matcher(input);
        StringBuffer buff = new StringBuffer();
        Set<Formatting> formatting = new HashSet<>();
        while (matcher.find()) {
            String code = matcher.group(1);
            Formatting fm = Objects.requireNonNull(Formatting.fromMc(code)); // Should not be null
            String replacement;
            if (formatting.contains(fm)) {
                replacement = "";
            } else {
                replacement = fm.dc;
            }
            formatting.add(fm);
            matcher.appendReplacement(buff, replacement);
        }
        matcher.appendTail(buff);
        for (Formatting fm : Formatting.values()) {
            if (formatting.contains(fm)) {
                buff.append(fm.dc);
            }
        }
        return buff.toString();
    }

    /**
     * Compatible formatting codes for Minecraft and Discord
     */
    public enum Formatting {
        BOLD("l", "**"),
        ITALIC("o", "*"),
        STRIKETHROUGH("m", "~~"),
        UNDERLINE("n", "__"),
        RESET("r", "");

        private static final Map<String, Formatting> FROM_MINECRAFT = Arrays.stream(Formatting.values())
                .collect(Collectors.toMap((f) -> f.mc, (f) -> f));
        /**
         * Just the key portion of the Minecraft formatting code
         */
        public final String mc;
        /**
         * Discord formatting code
         */
        public final String dc;
        /**
         * Minecraft formatting code
         */
        public final String mcfc;

        Formatting(String mc, String dc) {
            this.mc = mc;
            this.dc = dc;
            this.mcfc = "\u00a7" + mc;
        }

        @Nullable
        public static Formatting fromMc(String c) {
            return FROM_MINECRAFT.get(c.toLowerCase(Locale.ROOT));
        }

        /**
         * Returns the Minecraft Formatting code
         */
        @Override
        public String toString() {
            return mcfc;
        }
    }
}
