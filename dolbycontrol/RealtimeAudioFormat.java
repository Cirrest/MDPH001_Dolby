package com.mdph.dolbycontrol;

import java.math.BigDecimal;

final class RealtimeAudioFormat {
    private RealtimeAudioFormat() {
    }

    static String describe(int playerType, int sampleRate, String noOutputText) {
        if (sampleRate <= 0) {
            return noOutputText;
        }
        return describePlayerType(playerType) + " - " + describeSampleRate(sampleRate);
    }

    private static String describePlayerType(int playerType) {
        switch (playerType) {
            case 1:
                return "AudioTrack";
            case 2:
                return "MediaPlayer";
            case 3:
                return "SoundPool";
            case 11:
            case 12:
                return "OpenSL ES";
            case 13:
                return "AAudio";
            default:
                return "Unknown API";
        }
    }

    private static String describeSampleRate(int sampleRate) {
        if (sampleRate <= 0) {
            return "--";
        }
        BigDecimal khz = BigDecimal.valueOf(sampleRate)
                .divide(BigDecimal.valueOf(1000))
                .stripTrailingZeros();
        return khz.toPlainString() + "kHz";
    }
}
