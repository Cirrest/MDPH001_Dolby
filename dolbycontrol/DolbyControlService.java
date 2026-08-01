package com.mdph.dolbycontrol;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.media.AudioDeviceCallback;
import android.media.AudioDeviceInfo;
import android.media.AudioManager;
import android.media.AudioPlaybackConfiguration;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;

import java.io.IOException;
import java.util.Locale;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

public final class DolbyControlService extends Service {
    interface Listener {
        void onSnapshotChanged(DolbySnapshot snapshot);
    }

    private interface BackendOperation {
        void run(DolbyDsBackend backend) throws Exception;
    }

    private interface SettingsMutation {
        void apply(DolbyDsBackend.ProfileSettings settings);
    }

    private static final String TAG = "DolbyControlService";
    private static final String PREFS = "dolby_control";
    private static final String KEY_ENABLED = "enabled";
    private static final String KEY_MODE = "mode";
    private static final String KEY_GEQ_ENABLED = "custom_geq_enabled";
    private static final String CHANNEL_ID = "dolby_control_service";
    private static final int NOTIFICATION_ID = 7001;
    private static final long HEALTH_INTERVAL_MS = 5000L;

    private final LocalBinder binder = new LocalBinder();
    private final Object snapshotLock = new Object();
    private final Set<Listener> listeners = new CopyOnWriteArraySet<Listener>();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final DolbySnapshot snapshot = new DolbySnapshot();

    private SharedPreferences preferences;
    private AudioManager audioManager;
    private HandlerThread workerThread;
    private Handler worker;
    private DolbyDsBackend backend;
    private RealtimePlaybackFormatReader playbackFormatReader;
    private UiText uiText;

    private final DolbyDsBackend.Listener backendListener =
            new DolbyDsBackend.Listener() {
                @Override
                public void onConnected() {
                    if (worker != null) {
                        worker.post(new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    if (!isGlobalProcessingEnabled()) {
                                        releaseGlobalProcessing();
                                        return;
                                    }
                                    initializeBackendState();
                                    applyDesiredState(backend);
                                    refreshSnapshot();
                                } catch (Throwable error) {
                                    handleBackendFailure(error);
                                }
                            }
                        });
                    }
                }

                @Override
                public void onDisconnected() {
                    if (isGlobalProcessingEnabled()) {
                        publishDisconnected("Dolby DsService disconnected");
                    } else if (GlobalProcessingState.isDisabled(getFilesDir())) {
                        publishReleased();
                    } else {
                        publishDisconnected("Dolby processing release is pending");
                    }
                }
            };

    private final Runnable healthCheck = new Runnable() {
        @Override
        public void run() {
            try {
                if (!isGlobalProcessingEnabled()) {
                    releaseGlobalProcessing();
                    return;
                }
                GlobalProcessingState.setDisabled(getFilesDir(), false);
                ensureBackend();
                if (backend.isConnected()) {
                    enforceDesiredState();
                    refreshSnapshot();
                }
            } catch (Throwable error) {
                handleBackendFailure(error);
            } finally {
                if (worker != null) {
                    worker.postDelayed(this, HEALTH_INTERVAL_MS);
                }
            }
        }
    };

    private final BroadcastReceiver volumeReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            scheduleRefresh(true);
        }
    };

    private final AudioDeviceCallback deviceCallback = new AudioDeviceCallback() {
        @Override
        public void onAudioDevicesAdded(AudioDeviceInfo[] addedDevices) {
            scheduleRefresh(false);
        }

        @Override
        public void onAudioDevicesRemoved(AudioDeviceInfo[] removedDevices) {
            scheduleRefresh(false);
        }
    };

    private final AudioManager.AudioPlaybackCallback playbackCallback =
            new AudioManager.AudioPlaybackCallback() {
                @Override
                public void onPlaybackConfigChanged(
                        List<AudioPlaybackConfiguration> configurations) {
                    scheduleRefresh(false);
                }
            };

    @Override
    public void onCreate() {
        super.onCreate();
        preferences = getSharedPreferences(PREFS, MODE_PRIVATE);
        uiText = UiText.forLanguageTag(Locale.getDefault().toLanguageTag());
        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        playbackFormatReader = new RealtimePlaybackFormatReader(audioManager);
        createNotificationChannel();
        startForeground(NOTIFICATION_ID, buildNotification());

        workerThread = new HandlerThread("DolbyControlWorker");
        workerThread.start();
        worker = new Handler(workerThread.getLooper());

        IntentFilter volumeFilter = new IntentFilter("android.media.VOLUME_CHANGED_ACTION");
        if (Build.VERSION.SDK_INT >= 33) {
            registerReceiver(volumeReceiver, volumeFilter, Context.RECEIVER_NOT_EXPORTED);
        } else {
            registerReceiver(volumeReceiver, volumeFilter);
        }
        audioManager.registerAudioDeviceCallback(deviceCallback, mainHandler);
        audioManager.registerAudioPlaybackCallback(playbackCallback, mainHandler);

        worker.post(healthCheck);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        scheduleRefresh(false);
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    @Override
    public void onDestroy() {
        unregisterReceiver(volumeReceiver);
        audioManager.unregisterAudioDeviceCallback(deviceCallback);
        audioManager.unregisterAudioPlaybackCallback(playbackCallback);
        if (worker != null) {
            worker.removeCallbacksAndMessages(null);
        }
        closeBackend();
        if (workerThread != null) {
            workerThread.quitSafely();
        }
        super.onDestroy();
    }

    private void scheduleRefresh(final boolean volumeMayHaveChanged) {
        if (worker == null) {
            return;
        }
        worker.post(new Runnable() {
            @Override
            public void run() {
                try {
                    if (!isGlobalProcessingEnabled()) {
                        releaseGlobalProcessing();
                        return;
                    }
                    GlobalProcessingState.setDisabled(getFilesDir(), false);
                    ensureBackend();
                    if (volumeMayHaveChanged && ModePolicy.usesCustomGeq(getDesiredMode())) {
                        applyCustomGeq(backend);
                    }
                    refreshSnapshot();
                } catch (Throwable error) {
                    handleBackendFailure(error);
                }
            }
        });
    }

    private void postOperation(final BackendOperation operation) {
        if (worker == null) {
            return;
        }
        worker.post(new Runnable() {
            @Override
            public void run() {
                try {
                    if (!isGlobalProcessingEnabled()) {
                        releaseGlobalProcessing();
                        return;
                    }
                    GlobalProcessingState.setDisabled(getFilesDir(), false);
                    ensureBackend();
                    ensureDesiredProfile();
                    operation.run(backend);
                    refreshSnapshot();
                } catch (Throwable error) {
                    handleBackendFailure(error);
                }
            }
        });
    }

    private void restartAudioService() {
        if (worker == null) {
            return;
        }
        worker.post(new Runnable() {
            @Override
            public void run() {
                try {
                    closeBackend();
                    AudioServiceRestartRequest.create(
                            getFilesDir(), System.currentTimeMillis());
                    worker.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                if (isGlobalProcessingEnabled()) {
                                    GlobalProcessingState.setDisabled(getFilesDir(), false);
                                    ensureBackend();
                                } else {
                                    releaseGlobalProcessing();
                                }
                            } catch (Throwable error) {
                                handleBackendFailure(error);
                            }
                        }
                    }, 5000L);
                } catch (Throwable error) {
                    handleBackendFailure(error);
                }
            }
        });
    }

    private void ensureBackend() {
        if (!isGlobalProcessingEnabled()) {
            throw new IllegalStateException("Global Dolby processing is released");
        }
        if (backend == null) {
            backend = new DolbyDsBackend(this, backendListener);
        }
        if (!backend.isConnected()) {
            if (!backend.bind()) {
                throw new IllegalStateException("Unable to bind DAX2 DsService");
            }
            throw new IllegalStateException("Waiting for DAX2 DsService");
        }
    }

    private void initializeBackendState() {
        if (backend == null || !backend.isConnected()) {
            return;
        }
        SharedPreferences.Editor editor = preferences.edit();
        if (!preferences.contains(KEY_ENABLED)) {
            editor.putBoolean(KEY_ENABLED, backend.getEnabled());
        }
        if (!preferences.contains(KEY_MODE)) {
            editor.putInt(KEY_MODE, ModePolicy.modeForProfile(backend.getProfile()));
        }
        if (!preferences.contains(KEY_GEQ_ENABLED)) {
            int profile = ModePolicy.profileForMode(ModePolicy.MODE_CUSTOM);
            editor.putBoolean(
                    KEY_GEQ_ENABLED,
                    backend.getProfileSettings(profile).geqEnabled);
        }
        editor.apply();
    }

    private void ensureDesiredProfile() {
        if (!preferences.getBoolean(KEY_ENABLED, true)) {
            return;
        }
        int profile = ModePolicy.profileForMode(getDesiredMode());
        if (backend.getProfile() != profile) {
            backend.setProfile(profile);
            applyStoredOverrides(backend, profile);
        }
    }

    private void enforceDesiredState() {
        boolean desiredEnabled = preferences.getBoolean(KEY_ENABLED, true);
        if (backend.getEnabled() != desiredEnabled) {
            applyDesiredState(backend);
            return;
        }
        if (desiredEnabled) {
            int expectedProfile = ModePolicy.profileForMode(getDesiredMode());
            if (backend.getProfile() != expectedProfile) {
                applyDesiredState(backend);
            }
        }
    }

    private void applyDesiredState(DolbyDsBackend target) {
        boolean enabled = preferences.getBoolean(KEY_ENABLED, true);
        target.setEnabled(enabled);
        if (!enabled) {
            return;
        }
        int profile = ModePolicy.profileForMode(getDesiredMode());
        target.setProfile(profile);
        applyStoredOverrides(target, profile);
        if (ModePolicy.usesCustomGeq(getDesiredMode())) {
            applyCustomGeq(target);
        }
    }

    private void applyStoredOverrides(DolbyDsBackend target, int profile) {
        String ieqKey = profileKey(profile, "ieq");
        if (preferences.contains(ieqKey)) {
            target.setIeqPreset(
                    profile,
                    ControlValuePolicy.sanitizeIeq(preferences.getInt(ieqKey, 0)));
        }

        DolbyDsBackend.ProfileSettings settings = target.getProfileSettings(profile);
        applyStoredBoolean(settings, profile, "dialog_enabled");
        applyStoredBoolean(settings, profile, "leveler");
        applyStoredBoolean(settings, profile, "headphone_virtualizer");
        applyStoredBoolean(settings, profile, "speaker_virtualizer");
        if (profile == ModePolicy.profileForMode(ModePolicy.MODE_CUSTOM)) {
            settings.geqEnabled = preferences.getBoolean(KEY_GEQ_ENABLED, true);
        }
        target.setProfileSettings(profile, settings);

        String dialogAmountKey = profileKey(profile, "dialog_amount");
        if (preferences.contains(dialogAmountKey)) {
            target.setDsApParam(
                    DolbyDsProtocol.PARAM_DIALOG_AMOUNT,
                    ControlValuePolicy.sanitizeDialogAmount(
                            preferences.getInt(dialogAmountKey, 0)));
        }
    }

    private void applyStoredBoolean(
            DolbyDsBackend.ProfileSettings settings, int profile, String name) {
        String key = profileKey(profile, name);
        if (!preferences.contains(key)) {
            return;
        }
        boolean value = preferences.getInt(key, 0) != 0;
        if ("dialog_enabled".equals(name)) {
            settings.dialogEnabled = value;
        } else if ("leveler".equals(name)) {
            settings.volumeLeveler = value;
        } else if ("headphone_virtualizer".equals(name)) {
            settings.headphoneVirtualizer = value;
        } else if ("speaker_virtualizer".equals(name)) {
            settings.speakerVirtualizer = value;
        }
    }

    private void updateProfileSettings(
            DolbyDsBackend target, int profile, SettingsMutation mutation) {
        DolbyDsBackend.ProfileSettings settings = target.getProfileSettings(profile);
        mutation.apply(settings);
        target.setProfileSettings(profile, settings);
    }

    private void applyCustomGeq(DolbyDsBackend target) {
        int profile = ModePolicy.profileForMode(ModePolicy.MODE_CUSTOM);
        boolean enabled = preferences.getBoolean(KEY_GEQ_ENABLED, true);
        DolbyDsBackend.ProfileSettings settings = target.getProfileSettings(profile);
        settings.geqEnabled = enabled;
        target.setProfileSettings(profile, settings);
        if (!enabled || target.getProfile() != profile) {
            return;
        }
        String ieqKey = profileKey(profile, "ieq");
        int preset = preferences.contains(ieqKey)
                ? ControlValuePolicy.sanitizeIeq(preferences.getInt(ieqKey, 0))
                : target.getIeqPreset(profile);
        target.setGeq(
                profile,
                preset,
                DolbyDsProtocol.toServiceGeq(loadGeqDb()));
    }

    private void refreshSnapshot() {
        ensureBackend();
        DolbySnapshot next = new DolbySnapshot();
        next.connected = true;
        next.released = false;
        next.hasControl = true;
        next.enabled = backend.getEnabled();
        next.mode = getDesiredMode();
        next.profileCount = backend.getProfileCount();
        next.profile = backend.getProfile();
        next.volume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
        next.maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        next.outputRoute = getOutputRoute();
        next.tuningStatus = "DAX2 / " + next.profileCount + " "
                + uiText.get(UiText.Key.PROFILES) + " / "
                + playbackFormatReader.read(uiText.get(UiText.Key.NO_OUTPUT));
        next.geqDb = loadGeqDb();

        DolbyDsBackend.ProfileSettings settings =
                backend.getProfileSettings(next.profile);
        next.geqEnabled = settings.geqEnabled;
        next.ieq = backend.getIeqPreset(next.profile);
        next.dialogEnabled = settings.dialogEnabled;
        next.dialogAmount = preferences.getInt(
                profileKey(next.profile, "dialog_amount"),
                ModePolicy.defaultDialogAmountForProfile(next.profile));
        next.volumeLeveler = settings.volumeLeveler;
        next.headphoneVirtualizer = settings.headphoneVirtualizer;
        next.speakerVirtualizer = settings.speakerVirtualizer;

        synchronized (snapshotLock) {
            copyInto(next, snapshot);
        }
        notifyListeners(next.copy());
    }

    private void handleBackendFailure(Throwable error) {
        Log.e(TAG, "Dolby DAX2 backend failure", error);
        publishDisconnected(
                error.getClass().getSimpleName() + ": " + String.valueOf(error.getMessage()));
    }

    private void publishDisconnected(String message) {
        DolbySnapshot failed;
        synchronized (snapshotLock) {
            snapshot.connected = false;
            snapshot.released = false;
            snapshot.hasControl = false;
            snapshot.lastError = message;
            snapshot.tuningStatus = uiText == null
                    ? "Not connected"
                    : uiText.get(UiText.Key.NOT_CONNECTED);
            failed = snapshot.copy();
        }
        notifyListeners(failed);
    }

    private void publishReleased() {
        DolbySnapshot released;
        synchronized (snapshotLock) {
            snapshot.connected = false;
            snapshot.released = true;
            snapshot.enabled = false;
            snapshot.hasControl = false;
            snapshot.lastError = "";
            snapshot.tuningStatus = uiText == null
                    ? "Original audio path"
                    : uiText.get(UiText.Key.ORIGINAL_AUDIO_PATH);
            if (audioManager != null) {
                snapshot.outputRoute = getOutputRoute();
                snapshot.volume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
                snapshot.maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
            }
            released = snapshot.copy();
        }
        notifyListeners(released);
    }

    private boolean isGlobalProcessingEnabled() {
        return preferences == null || preferences.getBoolean(KEY_ENABLED, true);
    }

    private void releaseGlobalProcessing() throws IOException {
        if (backend != null && backend.isConnected()) {
            try {
                backend.setEnabled(false);
            } catch (RuntimeException error) {
                Log.w(TAG, "Unable to apply the DAX2 off profile before release", error);
            }
        }
        GlobalProcessingState.setDisabled(getFilesDir(), true);
        closeBackend();
        publishReleased();
    }

    private void notifyListeners(final DolbySnapshot value) {
        mainHandler.post(new Runnable() {
            @Override
            public void run() {
                for (Listener listener : listeners) {
                    listener.onSnapshotChanged(value.copy());
                }
            }
        });
    }

    private int getDesiredMode() {
        return ModePolicy.sanitizeMode(
                preferences.getInt(KEY_MODE, ModePolicy.MODE_DYNAMIC));
    }

    private int[] loadGeqDb() {
        int[] values = new int[GeqGainMapper.BAND_COUNT];
        for (int i = 0; i < values.length; i++) {
            values[i] = GeqGainMapper.sanitizeDb(
                    preferences.getInt("geq_" + i, 0));
        }
        return values;
    }

    private String getOutputRoute() {
        AudioDeviceInfo[] devices = audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS);
        AudioDeviceInfo fallback = null;
        for (AudioDeviceInfo device : devices) {
            int type = device.getType();
            if (type == AudioDeviceInfo.TYPE_BUILTIN_SPEAKER
                    || type == AudioDeviceInfo.TYPE_BUILTIN_EARPIECE) {
                fallback = device;
                continue;
            }
            String route = routeName(device);
            if (route != null) {
                return route;
            }
        }
        return fallback == null
                ? uiText.get(UiText.Key.ROUTE_UNKNOWN)
                : routeName(fallback);
    }

    private String routeName(AudioDeviceInfo device) {
        String type;
        switch (device.getType()) {
            case AudioDeviceInfo.TYPE_WIRED_HEADSET:
            case AudioDeviceInfo.TYPE_WIRED_HEADPHONES:
                type = uiText.get(UiText.Key.ROUTE_HEADPHONE);
                break;
            case AudioDeviceInfo.TYPE_BLUETOOTH_A2DP:
            case AudioDeviceInfo.TYPE_BLUETOOTH_SCO:
                type = uiText.get(UiText.Key.ROUTE_BLUETOOTH);
                break;
            case AudioDeviceInfo.TYPE_USB_DEVICE:
            case AudioDeviceInfo.TYPE_USB_HEADSET:
                type = uiText.get(UiText.Key.ROUTE_USB);
                break;
            case AudioDeviceInfo.TYPE_HDMI:
            case AudioDeviceInfo.TYPE_HDMI_ARC:
                type = uiText.get(UiText.Key.ROUTE_HDMI);
                break;
            case AudioDeviceInfo.TYPE_BUILTIN_SPEAKER:
                type = uiText.get(UiText.Key.ROUTE_SPEAKER);
                break;
            case AudioDeviceInfo.TYPE_BUILTIN_EARPIECE:
                type = uiText.get(UiText.Key.ROUTE_EARPIECE);
                break;
            default:
                return null;
        }
        CharSequence product = device.getProductName();
        return product == null || product.length() == 0
                ? type
                : type + " / " + product;
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT < 26) {
            return;
        }
        NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                uiText.get(UiText.Key.CHANNEL_NAME),
                NotificationManager.IMPORTANCE_LOW);
        channel.setDescription(uiText.get(UiText.Key.CHANNEL_DESCRIPTION));
        NotificationManager manager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        manager.createNotificationChannel(channel);
    }

    private Notification buildNotification() {
        Intent activityIntent = new Intent(this, MainActivity.class);
        int pendingFlags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= 23) {
            pendingFlags |= PendingIntent.FLAG_IMMUTABLE;
        }
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this, 0, activityIntent, pendingFlags);
        Notification.Builder builder = Build.VERSION.SDK_INT >= 26
                ? new Notification.Builder(this, CHANNEL_ID)
                : new Notification.Builder(this);
        return builder
                .setSmallIcon(android.R.drawable.ic_media_play)
                .setContentTitle(uiText.get(UiText.Key.APP_TITLE))
                .setContentText(uiText.get(UiText.Key.NOTIFICATION_ACTIVE))
                .setContentIntent(pendingIntent)
                .setOngoing(true)
                .setCategory(Notification.CATEGORY_SERVICE)
                .build();
    }

    private void closeBackend() {
        if (backend != null) {
            try {
                backend.close();
            } catch (RuntimeException ignored) {
            }
            backend = null;
        }
    }

    private static String profileKey(int profile, String name) {
        return "profile_" + profile + "_" + name;
    }

    private static void copyInto(DolbySnapshot from, DolbySnapshot to) {
        DolbySnapshot copy = from.copy();
        to.connected = copy.connected;
        to.released = copy.released;
        to.enabled = copy.enabled;
        to.hasControl = copy.hasControl;
        to.mode = copy.mode;
        to.profile = copy.profile;
        to.profileCount = copy.profileCount;
        to.ieq = copy.ieq;
        to.dialogEnabled = copy.dialogEnabled;
        to.dialogAmount = copy.dialogAmount;
        to.volumeLeveler = copy.volumeLeveler;
        to.headphoneVirtualizer = copy.headphoneVirtualizer;
        to.speakerVirtualizer = copy.speakerVirtualizer;
        to.geqEnabled = copy.geqEnabled;
        to.volume = copy.volume;
        to.maxVolume = copy.maxVolume;
        to.geqDb = copy.geqDb;
        to.outputRoute = copy.outputRoute;
        to.tuningStatus = copy.tuningStatus;
        to.lastError = copy.lastError;
    }

    public final class LocalBinder extends Binder {
        DolbySnapshot getSnapshot() {
            synchronized (snapshotLock) {
                return snapshot.copy();
            }
        }

        void registerListener(Listener listener) {
            listeners.add(listener);
            listener.onSnapshotChanged(getSnapshot());
        }

        void unregisterListener(Listener listener) {
            listeners.remove(listener);
        }

        void setEnabled(final boolean enabled) {
            preferences.edit().putBoolean(KEY_ENABLED, enabled).apply();
            if (worker == null) {
                return;
            }
            worker.post(new Runnable() {
                @Override
                public void run() {
                    try {
                        if (!enabled) {
                            releaseGlobalProcessing();
                            return;
                        }
                        GlobalProcessingState.setDisabled(getFilesDir(), false);
                        ensureBackend();
                        applyDesiredState(backend);
                        refreshSnapshot();
                    } catch (Throwable error) {
                        handleBackendFailure(error);
                    }
                }
            });
        }

        void setMode(final int mode) {
            preferences.edit().putInt(KEY_MODE, ModePolicy.sanitizeMode(mode)).apply();
            postOperation(new BackendOperation() {
                @Override
                public void run(DolbyDsBackend target) {
                    applyDesiredState(target);
                }
            });
        }

        void setIeq(final int value) {
            final int profile = ModePolicy.profileForMode(getDesiredMode());
            final int preset = ControlValuePolicy.sanitizeIeq(value);
            preferences.edit().putInt(profileKey(profile, "ieq"), preset).apply();
            postOperation(new BackendOperation() {
                @Override
                public void run(DolbyDsBackend target) {
                    target.setIeqPreset(profile, preset);
                    if (profile == ModePolicy.profileForMode(ModePolicy.MODE_CUSTOM)) {
                        applyCustomGeq(target);
                    }
                }
            });
        }

        void setDialogEnabled(final boolean enabled) {
            setProfileBoolean("dialog_enabled", enabled, new SettingsMutation() {
                @Override
                public void apply(DolbyDsBackend.ProfileSettings settings) {
                    settings.dialogEnabled = enabled;
                }
            });
        }

        void setDialogAmount(final int amount) {
            final int profile = ModePolicy.profileForMode(getDesiredMode());
            final int safeAmount = ControlValuePolicy.sanitizeDialogAmount(amount);
            preferences.edit()
                    .putInt(profileKey(profile, "dialog_amount"), safeAmount)
                    .apply();
            postOperation(new BackendOperation() {
                @Override
                public void run(DolbyDsBackend target) {
                    target.setDsApParam(DolbyDsProtocol.PARAM_DIALOG_AMOUNT, safeAmount);
                }
            });
        }

        void setVolumeLeveler(final boolean enabled) {
            setProfileBoolean("leveler", enabled, new SettingsMutation() {
                @Override
                public void apply(DolbyDsBackend.ProfileSettings settings) {
                    settings.volumeLeveler = enabled;
                }
            });
        }

        void setHeadphoneVirtualizer(final boolean enabled) {
            setProfileBoolean("headphone_virtualizer", enabled, new SettingsMutation() {
                @Override
                public void apply(DolbyDsBackend.ProfileSettings settings) {
                    settings.headphoneVirtualizer = enabled;
                }
            });
        }

        void setSpeakerVirtualizer(final boolean enabled) {
            setProfileBoolean("speaker_virtualizer", enabled, new SettingsMutation() {
                @Override
                public void apply(DolbyDsBackend.ProfileSettings settings) {
                    settings.speakerVirtualizer = enabled;
                }
            });
        }

        void setGeqEnabled(final boolean enabled) {
            preferences.edit().putBoolean(KEY_GEQ_ENABLED, enabled).apply();
            postOperation(new BackendOperation() {
                @Override
                public void run(DolbyDsBackend target) {
                    if (ModePolicy.usesCustomGeq(getDesiredMode())) {
                        applyCustomGeq(target);
                    }
                }
            });
        }

        void setGeqBand(final int band, int db) {
            if (!ControlValuePolicy.isValidBandIndex(band)) {
                return;
            }
            preferences.edit()
                    .putInt("geq_" + band, GeqGainMapper.sanitizeDb(db))
                    .apply();
            postOperation(new BackendOperation() {
                @Override
                public void run(DolbyDsBackend target) {
                    if (ModePolicy.usesCustomGeq(getDesiredMode())) {
                        applyCustomGeq(target);
                    }
                }
            });
        }

        void resetGeq() {
            SharedPreferences.Editor editor = preferences.edit();
            for (int i = 0; i < GeqGainMapper.BAND_COUNT; i++) {
                editor.putInt("geq_" + i, 0);
            }
            editor.apply();
            postOperation(new BackendOperation() {
                @Override
                public void run(DolbyDsBackend target) {
                    if (ModePolicy.usesCustomGeq(getDesiredMode())) {
                        applyCustomGeq(target);
                    }
                }
            });
        }

        void refresh() {
            scheduleRefresh(false);
        }

        void restartAudioService() {
            DolbyControlService.this.restartAudioService();
        }

        private void setProfileBoolean(
                String name, boolean enabled, final SettingsMutation mutation) {
            final int profile = ModePolicy.profileForMode(getDesiredMode());
            preferences.edit()
                    .putInt(profileKey(profile, name), enabled ? 1 : 0)
                    .apply();
            postOperation(new BackendOperation() {
                @Override
                public void run(DolbyDsBackend target) {
                    updateProfileSettings(target, profile, mutation);
                }
            });
        }
    }
}
