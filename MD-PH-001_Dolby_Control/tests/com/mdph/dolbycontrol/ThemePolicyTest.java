package com.mdph.dolbycontrol;

public final class ThemePolicyTest {
    public static void main(String[] args) {
        assertEquals(ThemePolicy.THEME_SYSTEM, ThemePolicy.sanitize(99));
        assertEquals(ThemePolicy.THEME_LIGHT, ThemePolicy.sanitize(ThemePolicy.THEME_LIGHT));
        assertEquals(ThemePolicy.THEME_DARK, ThemePolicy.sanitize(ThemePolicy.THEME_DARK));
        assertTrue(ThemePolicy.isDark(ThemePolicy.THEME_DARK, false), "dark override");
        assertFalse(ThemePolicy.isDark(ThemePolicy.THEME_LIGHT, true), "light override");
        assertTrue(ThemePolicy.isDark(ThemePolicy.THEME_SYSTEM, true), "dark system");
        assertFalse(ThemePolicy.isDark(ThemePolicy.THEME_SYSTEM, false), "light system");
        System.out.println("ThemePolicyTest PASS");
    }

    private static void assertEquals(int expected, int actual) {
        if (expected != actual) {
            throw new AssertionError("expected " + expected + " but was " + actual);
        }
    }

    private static void assertTrue(boolean value, String label) {
        if (!value) throw new AssertionError(label);
    }

    private static void assertFalse(boolean value, String label) {
        if (value) throw new AssertionError(label);
    }
}
