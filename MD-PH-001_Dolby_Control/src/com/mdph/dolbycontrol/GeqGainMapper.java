package com.codex.dolbycontrol;

final class GeqGainMapper {
    static final int BAND_COUNT = 20;
    static final int MIN_DB = -10;
    static final int MAX_DB = 10;

    private static final float[] VOLUME_MULTIPLIERS = {
            16.0f, 16.0f, 16.0f, 16.0f,
            16.0f, 16.0f, 16.0f, 16.0f,
            16.0f, 16.0f, 16.0f, 16.0f,
            16.0f, 13.34f, 10.67f, 8.0f, 4.0f
    };

    private GeqGainMapper() {
    }

    static int scaleDbToDapGain(int db, int volume, int maxVolume) {
        int safeDb = clamp(db, MIN_DB, MAX_DB);
        int bucket = volumeBucket(volume, maxVolume);
        return (int) (safeDb * VOLUME_MULTIPLIERS[bucket]);
    }

    static int[] mapDbToDapGains(int[] dbValues, int volume, int maxVolume) {
        if (dbValues == null || dbValues.length != BAND_COUNT) {
            throw new IllegalArgumentException("GEQ requires exactly 20 bands");
        }
        int[] gains = new int[BAND_COUNT];
        for (int i = 0; i < BAND_COUNT; i++) {
            gains[i] = scaleDbToDapGain(dbValues[i], volume, maxVolume);
        }
        return gains;
    }

    static int sanitizeDb(int db) {
        return clamp(db, MIN_DB, MAX_DB);
    }

    private static int volumeBucket(int volume, int maxVolume) {
        if (maxVolume <= 0) {
            return 0;
        }
        int safeVolume = clamp(volume, 0, maxVolume);
        return safeVolume * (VOLUME_MULTIPLIERS.length - 1) / maxVolume;
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
