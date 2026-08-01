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
        switch (safeMode) {
            case MODE_MOVIE:
                return 0;
            case MODE_MUSIC:
                return 1;
            case MODE_CUSTOM:
                return 4;
            case MODE_DYNAMIC:
            default:
                return 2;
        }
    }

    static boolean usesCustomGeq(int mode) {
        return sanitizeMode(mode) == MODE_CUSTOM;
    }

    static int modeForProfile(int profile) {
        switch (profile) {
            case 0:
                return MODE_MOVIE;
            case 1:
                return MODE_MUSIC;
            case 4:
                return MODE_CUSTOM;
            case 2:
            default:
                return MODE_DYNAMIC;
        }
    }

    static int defaultDialogAmountForProfile(int profile) {
        switch (profile) {
            case 0:
                return 5;
            case 3:
                return 10;
            case 1:
            case 2:
            case 4:
            case 5:
            default:
                return 7;
        }
    }
}
