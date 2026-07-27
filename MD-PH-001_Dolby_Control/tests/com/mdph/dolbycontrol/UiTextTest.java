package com.mdph.dolbycontrol;

public final class UiTextTest {
    public static void main(String[] args) {
        UiText chinese = UiText.forLanguageTag("zh-CN");
        UiText english = UiText.forLanguageTag("en-US");
        UiText fallback = UiText.forLanguageTag("fr-FR");

        assertEquals("MIAD01 Dolby Atoms", chinese.get(UiText.Key.APP_TITLE));
        assertEquals("MIAD01 Dolby Atoms", english.get(UiText.Key.APP_TITLE));
        assertEquals("\u6b63\u5728\u8fde\u63a5", chinese.get(UiText.Key.CONNECTING));
        assertEquals("Connecting", english.get(UiText.Key.CONNECTING));
        assertEquals("Connecting", fallback.get(UiText.Key.CONNECTING));
        assertEquals("主题", chinese.get(UiText.Key.THEME));
        assertEquals("Theme", english.get(UiText.Key.THEME));

        for (UiText.Key key : UiText.Key.values()) {
            assertNotEmpty(chinese.get(key), "Chinese " + key);
            assertNotEmpty(english.get(key), "English " + key);
        }
        System.out.println("UiTextTest PASS");
    }

    private static void assertEquals(String expected, String actual) {
        if (!expected.equals(actual)) {
            throw new AssertionError("expected " + expected + " but was " + actual);
        }
    }

    private static void assertNotEmpty(String value, String label) {
        if (value == null || value.length() == 0) {
            throw new AssertionError(label + " must not be empty");
        }
    }
}
