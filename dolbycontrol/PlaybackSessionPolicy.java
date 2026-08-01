package com.mdph.dolbycontrol;

final class PlaybackSessionPolicy {
    private PlaybackSessionPolicy() {
    }

    static boolean shouldProcess(int sessionId, boolean active) {
        return active && sessionId > 0;
    }
}
