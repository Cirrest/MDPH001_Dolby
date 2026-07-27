package com.mdph.dolbycontrol;

final class ModePolicy {
    static final int MODE_DYNAMIC = 0;
    static final int MODE_MOVIE = 1;
    static final int MODE_MUSIC = 2;
    static final int MODE_CUSTOM = 3;

    private ModePolicy() {
    }

    static int sanitizeMode(int mode) {
        if (mode < MODE_DYNAMIC || mode > MODE_CUSTOM) {
            return MODE_DYNAMIC;
        }
        return mode;
    }

    static int profileForMode(int mode) {
        int safeMode = sanitizeMode(mode);
        return safeMode == MODE_CUSTOM ? MODE_MUSIC : safeMode;
    }

    static boolean usesCustomGeq(int mode) {
        return sanitizeMode(mode) == MODE_CUSTOM;
    }
}
