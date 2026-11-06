package com.breakinblocks.bblink.core.api;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class NameSanitizerTest {
    private static String name(String input) {
        return NameSanitizer.cleanName(input, "Fallback_1");
    }

    @Test
    void plainNamesAreUnchanged() {
        assertEquals("Steve", name("Steve"));
        assertEquals("Cool_Guy_99", name("Cool_Guy_99"));
        assertEquals("Sir Steve", name("Sir Steve"));
        assertEquals("José", name("José"));
        assertEquals("Ünïcödé", name("Ünïcödé"));
        assertEquals("名前", name("名前"));
        assertEquals("สมชาย", name("สมชาย")); // Thai relies on combining marks
    }

    @Test
    void legacyColourCodesAreRemoved() {
        assertEquals("Steve", name("§cSte§lve§r"));
        assertEquals("Steve", name("§x§f§f§0§0§a§aSteve"));
        assertEquals("Steve", name("Steve§"));
    }

    @Test
    void ampersandAndHexCodesAreRemoved() {
        assertEquals("Steve", name("&c&lSteve"));
        assertEquals("Steve", name("&#ff00aaSteve"));
        assertEquals("Steve", name("{#ff00aa}Steve"));
        assertEquals("Steve", name("<#ff00aa>Steve</#ff00aa>"));
        assertEquals("Tom&Jerry", name("Tom&Jerry"));
    }

    @Test
    void gradientAndStyleTagsAreRemoved() {
        assertEquals("Steve", name("<gradient:#ff0000:#0000ff>Steve</gradient>"));
        assertEquals("Steve", name("<rainbow>Steve</rainbow>"));
        assertEquals("Steve", name("<bold><red>Steve</red></bold>"));
        assertEquals("Steve <3", name("Steve <3"));
    }

    @Test
    void decorativeAndInvisibleCharactersAreRemoved() {
        assertEquals("Steve", name("★ Steve ★"));
        assertEquals("Steve", name("🔥Steve🔥")); // fire emoji
        assertEquals("Steve", name("Steve")); // private use: resource pack badge
        assertEquals("Steve", name("St​eve‍")); // zero-width
        assertEquals("Steve", name("‮Steve")); // bidi override
        assertEquals("Sir Steve", name("  Sir \t  Steve\n"));
    }

    @Test
    void zalgoIsLimited() {
        String zalgo = "S̀́̂̃̄̅teve";
        assertEquals("S̀́teve", name(zalgo));
    }

    @Test
    void fallsBackWhenNothingIsLeft() {
        assertEquals("Fallback_1", name("★★★"));
        assertEquals("Fallback_1", name("§c"));
        assertEquals("Fallback_1", name(""));
        assertEquals("Fallback_1", name(null));
    }

    @Test
    void stripFormattingKeepsChatContent() {
        assertEquals("hi ★ :) <3 &c", NameSanitizer.stripFormatting("§chi ★ :) <3 &c"));
        assertEquals("line1\nline2", NameSanitizer.stripFormatting("line1\nline2"));
        assertEquals("ab", NameSanitizer.stripFormatting("a​b"));
    }
}
