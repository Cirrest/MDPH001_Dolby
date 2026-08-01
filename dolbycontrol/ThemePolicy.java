package com.mdph.dolbycontrol;

final class ThemePolicy {
    static final int THEME_SYSTEM = 0;
    static final int THEME_LIGHT = 1;
    static final int THEME_DARK = 2;

    private ThemePolicy() {
    }

    static int sanitize(int value) {
        return value < THEME_SYSTEM || value > THEME_DARK ? THEME_SYSTEM : value;
    }

    static boolean isDark(int theme, boolean systemDark) {
        int safeTheme = sanitize(theme);
        return safeTheme == THEME_DARK || (safeTheme == THEME_SYSTEM && systemDark);
    }
}
