package com.mdph.dolbycontrol;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

final class PlaybackSessionCollector {
    interface Access<T> {
        int getSessionId(T playback) throws Exception;

        boolean isActive(T playback) throws Exception;
    }

    private PlaybackSessionCollector() {
    }

    static <T> Set<Integer> collect(List<T> playbacks, Access<T> access) throws Exception {
        Set<Integer> sessions = new LinkedHashSet<Integer>();
        if (playbacks == null) {
            return sessions;
        }
        for (T playback : playbacks) {
            int sessionId = access.getSessionId(playback);
            boolean active = access.isActive(playback);
            if (PlaybackSessionPolicy.shouldProcess(sessionId, active)) {
                sessions.add(sessionId);
            }
        }
        return sessions;
    }
}
