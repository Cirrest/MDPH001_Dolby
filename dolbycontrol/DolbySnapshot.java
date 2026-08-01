package com.mdph.dolbycontrol;

final class DolbySnapshot {
    boolean connected;
    boolean released;
    boolean enabled;
    boolean hasControl;
    int mode;
    int profile;
    int profileCount;
    int ieq;
    boolean dialogEnabled;
    int dialogAmount;
    boolean volumeLeveler;
    boolean headphoneVirtualizer;
    boolean speakerVirtualizer;
    boolean geqEnabled;
    int volume;
    int maxVolume;
    int[] geqDb = new int[GeqGainMapper.BAND_COUNT];
    String outputRoute = "Unknown";
    String tuningStatus = "Not connected";
    String lastError = "";

    DolbySnapshot copy() {
        DolbySnapshot copy = new DolbySnapshot();
        copy.connected = connected;
        copy.released = released;
        copy.enabled = enabled;
        copy.hasControl = hasControl;
        copy.mode = mode;
        copy.profile = profile;
        copy.profileCount = profileCount;
        copy.ieq = ieq;
        copy.dialogEnabled = dialogEnabled;
        copy.dialogAmount = dialogAmount;
        copy.volumeLeveler = volumeLeveler;
        copy.headphoneVirtualizer = headphoneVirtualizer;
        copy.speakerVirtualizer = speakerVirtualizer;
        copy.geqEnabled = geqEnabled;
        copy.volume = volume;
        copy.maxVolume = maxVolume;
        copy.geqDb = geqDb.clone();
        copy.outputRoute = outputRoute;
        copy.tuningStatus = tuningStatus;
        copy.lastError = lastError;
        return copy;
    }
}
