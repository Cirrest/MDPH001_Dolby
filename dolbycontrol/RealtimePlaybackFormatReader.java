package com.mdph.dolbycontrol;

import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.AudioPlaybackConfiguration;

import java.lang.reflect.Method;
import java.util.List;

final class RealtimePlaybackFormatReader {
    private final AudioManager audioManager;
    private final Method isActiveMethod;
    private final Method getSampleRateMethod;
    private final Method getPlayerTypeMethod;

    RealtimePlaybackFormatReader(AudioManager audioManager) {
        this.audioManager = audioManager;
        isActiveMethod = findMethod(AudioPlaybackConfiguration.class, "isActive");
        getSampleRateMethod = findMethod(
                AudioPlaybackConfiguration.class, "getSampleRate");
        getPlayerTypeMethod = findMethod(
                AudioPlaybackConfiguration.class, "getPlayerType");
    }

    String read(String noOutputText) {
        PlaybackFormat format = readActivePlaybackFormat();
        return RealtimeAudioFormat.describe(
                format.playerType, format.sampleRate, noOutputText);
    }

    private PlaybackFormat readActivePlaybackFormat() {
        if (isActiveMethod == null || getSampleRateMethod == null
                || getPlayerTypeMethod == null) {
            return PlaybackFormat.NONE;
        }
        try {
            List<AudioPlaybackConfiguration> configurations =
                    audioManager.getActivePlaybackConfigurations();
            PlaybackFormat fallback = PlaybackFormat.NONE;
            for (AudioPlaybackConfiguration configuration : configurations) {
                if (!((Boolean) isActiveMethod.invoke(configuration))) {
                    continue;
                }
                int sampleRate = (Integer) getSampleRateMethod.invoke(configuration);
                if (sampleRate <= 0) {
                    continue;
                }
                int playerType = (Integer) getPlayerTypeMethod.invoke(configuration);
                PlaybackFormat current = new PlaybackFormat(playerType, sampleRate);
                int usage = configuration.getAudioAttributes().getUsage();
                if (usage == AudioAttributes.USAGE_MEDIA
                        || usage == AudioAttributes.USAGE_GAME
                        || usage == AudioAttributes.USAGE_ASSISTANT) {
                    return current;
                }
                if (fallback == PlaybackFormat.NONE) {
                    fallback = current;
                }
            }
            return fallback;
        } catch (ReflectiveOperationException | RuntimeException ignored) {
            return PlaybackFormat.NONE;
        }
    }

    private static Method findMethod(Class<?> type, String name) {
        try {
            Method method = type.getDeclaredMethod(name);
            method.setAccessible(true);
            return method;
        } catch (ReflectiveOperationException | RuntimeException ignored) {
            return null;
        }
    }

    private static final class PlaybackFormat {
        static final PlaybackFormat NONE = new PlaybackFormat(0, 0);

        final int playerType;
        final int sampleRate;

        PlaybackFormat(int playerType, int sampleRate) {
            this.playerType = playerType;
            this.sampleRate = sampleRate;
        }
    }
}
