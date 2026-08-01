package com.mdph.dolbycontrol;

final class ControlValuePolicy {
    private ControlValuePolicy() {
    }

    static int sanitizeIeq(int value) {
        return clamp(value, 0, 3);
    }

    static int sanitizeDialogAmount(int value) {
        return clamp(value, 0, 16);
    }

    static boolean isValidBandIndex(int band) {
        return band >= 0 && band < GeqGainMapper.BAND_COUNT;
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
