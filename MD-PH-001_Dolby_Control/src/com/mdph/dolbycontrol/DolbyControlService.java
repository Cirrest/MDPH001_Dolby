package com.codex.dolbycontrol;

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
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;

import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

public final class DolbyControlService extends Service {
    interface Listener {
        void onSnapshotChanged(DolbySnapshot snapshot);
    }

    private interface ControllerOperation {
        void run(DaxController controller) throws Exception;
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
    private DaxController controller;
    private int lastVolume = -1;
    private int lastMaxVolume = -1;
    private String lastRoute = "";

    private final Runnable healthCheck = new Runnable() {
        @Override
        public void run() {
            try {
                ensureController();
                enforceDesiredState();
                refreshSnapshot();
            } catch (Throwable error) {
                handleControllerFailure(error);
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

    @Override
    public void onCreate() {
        super.onCreate();
        preferences = getSharedPreferences(PREFS, MODE_PRIVATE);
        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        createNotificationChannel();
        startForeground(NOTIFICATION_ID, buildNotification());

        workerThread = new HandlerThread("DolbyControlWorker");
        workerThread.start();
        worker = new Handler(workerThread.getLooper());

        IntentFilter volumeFilter = new IntentFilter("android.media.VOLUME_CHANGED_ACTION");
        registerReceiver(volumeReceiver, volumeFilter);
        audioManager.registerAudioDeviceCallback(deviceCallback, mainHandler);
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
        if (worker != null) {
            worker.removeCallbacksAndMessages(null);
        }
        closeController();
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
                    ensureController();
                    if (volumeMayHaveChanged && ModePolicy.usesCustomGeq(getDesiredMode())) {
                        applyCustomGeq(controller);
                    }
                    refreshSnapshot();
                } catch (Throwable error) {
                    handleControllerFailure(error);
                }
            }
        });
    }

    private void postOperation(final ControllerOperation operation) {
        worker.post(new Runnable() {
            @Override
            public void run() {
                try {
                    ensureController();
                    operation.run(controller);
                    refreshSnapshot();
                } catch (Throwable error) {
                    handleControllerFailure(error);
                }
            }
        });
    }

    private void ensureController() throws Exception {
        if (controller != null && controller.isAlive()) {
            return;
        }
        closeController();
        controller = DaxController.open();

        int actualProfile = controller.getProfile();
        SharedPreferences.Editor editor = preferences.edit();
        if (!preferences.contains(KEY_ENABLED)) {
            editor.putBoolean(KEY_ENABLED, true);
        }
        if (!preferences.contains(KEY_MODE)) {
            int initialMode = actualProfile >= 0 && actualProfile <= 2
                    ? actualProfile
                    : ModePolicy.MODE_DYNAMIC;
            editor.putInt(KEY_MODE, initialMode);
        }
        if (!preferences.contains(KEY_GEQ_ENABLED)) {
            editor.putBoolean(KEY_GEQ_ENABLED, true);
        }
        editor.apply();
        applyDesiredState();
    }

    private void enforceDesiredState() throws Exception {
        boolean enabled = preferences.getBoolean(KEY_ENABLED, true);
        int actualEnabled = controller.getEnable();
        if ((actualEnabled != 0) != enabled) {
            applyDesiredState();
            return;
        }
        if (enabled) {
            int expectedProfile = ModePolicy.profileForMode(getDesiredMode());
            if (controller.getProfile() != expectedProfile) {
                applyDesiredState();
                return;
            }
        }

        int volume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
        int maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        String route = getOutputRoute();
        if (ModePolicy.usesCustomGeq(getDesiredMode())
                && (volume != lastVolume || maxVolume != lastMaxVolume)) {
            applyCustomGeq(controller);
        }
        lastRoute = route;
    }

    private void applyDesiredState() throws Exception {
        boolean enabled = preferences.getBoolean(KEY_ENABLED, true);
        controller.setEnabled(enabled);
        if (!enabled) {
            return;
        }
        int mode = getDesiredMode();
        int profile = ModePolicy.profileForMode(mode);
        controller.setProfile(profile);
        applyStoredOverrides(profile);
        if (ModePolicy.usesCustomGeq(mode)) {
            applyCustomGeq(controller);
        } else if (mode == ModePolicy.MODE_MUSIC) {
            controller.setProfileInt(profile, DaxParameterProtocol.PARAM_GEQ_ENABLE, 0);
        }
    }

    private void applyStoredOverrides(int profile) {
        applyStoredInt(profile, "ieq", DaxParameterProtocol.PARAM_IEQ_PRESET);
        applyStoredInt(
                profile,
                "dialog_enabled",
                DaxParameterProtocol.PARAM_DIALOG_ENHANCEMENT_ENABLE);
        applyStoredInt(
                profile,
                "dialog_amount",
                DaxParameterProtocol.PARAM_DIALOG_ENHANCEMENT_AMOUNT);
        applyStoredInt(profile, "leveler", DaxParameterProtocol.PARAM_VOLUME_LEVELER);
        applyStoredInt(
                profile,
                "headphone_virtualizer",
                DaxParameterProtocol.PARAM_HEADPHONE_VIRTUALIZER);
        applyStoredInt(
                profile,
                "speaker_virtualizer",
                DaxParameterProtocol.PARAM_SPEAKER_VIRTUALIZER);
    }

    private void applyStoredInt(int profile, String name, int parameter) {
        String key = profileKey(profile, name);
        if (!preferences.contains(key)) {
            return;
        }
        try {
            controller.setProfileInt(profile, parameter, preferences.getInt(key, 0));
        } catch (RuntimeException error) {
            Log.w(TAG, "Unable to restore " + key, error);
        }
    }

    private void applyCustomGeq(DaxController target) {
        int profile = ModePolicy.profileForMode(ModePolicy.MODE_CUSTOM);
        boolean enabled = preferences.getBoolean(KEY_GEQ_ENABLED, true);
        target.setProfileInt(
                profile,
                DaxParameterProtocol.PARAM_GEQ_ENABLE,
                enabled ? 1 : 0);
        if (!enabled) {
            return;
        }
        int volume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
        int maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        int[] gains = GeqGainMapper.mapDbToDapGains(loadGeqDb(), volume, maxVolume);
        target.setProfileInts(profile, DaxParameterProtocol.PARAM_GEQ_BAND_GAINS, gains);
        lastVolume = volume;
        lastMaxVolume = maxVolume;
    }

    private void refreshSnapshot() {
        DolbySnapshot next = new DolbySnapshot();
        next.connected = true;
        next.hasControl = controller.hasControl();
        next.enabled = controller.getEnable() != 0;
        next.mode = getDesiredMode();
        next.profileCount = controller.getProfileCount();
        next.profile = controller.getProfile();
        next.volume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
        next.maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        next.outputRoute = getOutputRoute();
        next.tuningStatus = controller.getName()
                + " / " + next.profileCount + " profiles"
                + " / effect " + controller.getEffectId();
        next.geqDb = loadGeqDb();
        next.geqEnabled = preferences.getBoolean(KEY_GEQ_ENABLED, true);

        int profile = next.profile;
        next.ieq = safeGetProfileInt(
                profile,
                DaxParameterProtocol.PARAM_IEQ_PRESET,
                0);
        next.dialogEnabled = safeGetProfileInt(
                profile,
                DaxParameterProtocol.PARAM_DIALOG_ENHANCEMENT_ENABLE,
                0) != 0;
        next.dialogAmount = safeGetProfileInt(
                profile,
                DaxParameterProtocol.PARAM_DIALOG_ENHANCEMENT_AMOUNT,
                0);
        next.volumeLeveler = safeGetProfileInt(
                profile,
                DaxParameterProtocol.PARAM_VOLUME_LEVELER,
                0) != 0;
        next.headphoneVirtualizer = safeGetProfileInt(
                profile,
                DaxParameterProtocol.PARAM_HEADPHONE_VIRTUALIZER,
                0) != 0;
        next.speakerVirtualizer = safeGetProfileInt(
                profile,
                DaxParameterProtocol.PARAM_SPEAKER_VIRTUALIZER,
                0) != 0;

        synchronized (snapshotLock) {
            copyInto(next, snapshot);
        }
        notifyListeners(next.copy());
    }

    private int safeGetProfileInt(int profile, int parameter, int fallback) {
        try {
            return controller.getProfileInt(profile, parameter);
        } catch (RuntimeException error) {
            Log.w(TAG, "Unable to read parameter 0x" + Integer.toHexString(parameter), error);
            return fallback;
        }
    }

    private void handleControllerFailure(Throwable error) {
        Log.e(TAG, "Dolby controller failure", error);
        closeController();
        DolbySnapshot failed;
        synchronized (snapshotLock) {
            snapshot.connected = false;
            snapshot.hasControl = false;
            snapshot.lastError = error.getClass().getSimpleName()
                    + ": " + String.valueOf(error.getMessage());
            snapshot.tuningStatus = "Not connected";
            failed = snapshot.copy();
        }
        notifyListeners(failed);
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
        return fallback == null ? "Unknown" : routeName(fallback);
    }

    private static String routeName(AudioDeviceInfo device) {
        String type;
        switch (device.getType()) {
            case AudioDeviceInfo.TYPE_WIRED_HEADSET:
            case AudioDeviceInfo.TYPE_WIRED_HEADPHONES:
                type = "Headphone";
                break;
            case AudioDeviceInfo.TYPE_BLUETOOTH_A2DP:
            case AudioDeviceInfo.TYPE_BLUETOOTH_SCO:
                type = "Bluetooth";
                break;
            case AudioDeviceInfo.TYPE_USB_DEVICE:
            case AudioDeviceInfo.TYPE_USB_HEADSET:
                type = "USB";
                break;
            case AudioDeviceInfo.TYPE_HDMI:
            case AudioDeviceInfo.TYPE_HDMI_ARC:
                type = "HDMI";
                break;
            case AudioDeviceInfo.TYPE_BUILTIN_SPEAKER:
                type = "Speaker";
                break;
            case AudioDeviceInfo.TYPE_BUILTIN_EARPIECE:
                type = "Earpiece";
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
                "Dolby control",
                NotificationManager.IMPORTANCE_LOW);
        channel.setDescription("Keeps the global Dolby DAP effect active");
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
                .setContentTitle("MD-PH-001 Dolby Atmos")
                .setContentText("Global DAP controller is active")
                .setContentIntent(pendingIntent)
                .setOngoing(true)
                .setCategory(Notification.CATEGORY_SERVICE)
                .build();
    }

    private void closeController() {
        if (controller != null) {
            try {
                controller.close();
            } catch (RuntimeException ignored) {
                // The audio server may already be gone.
            }
            controller = null;
        }
    }

    private static String profileKey(int profile, String name) {
        return "profile_" + profile + "_" + name;
    }

    private static void copyInto(DolbySnapshot from, DolbySnapshot to) {
        DolbySnapshot copy = from.copy();
        to.connected = copy.connected;
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
            postOperation(new ControllerOperation() {
                @Override
                public void run(DaxController target) throws Exception {
                    applyDesiredState();
                }
            });
        }

        void setMode(final int mode) {
            preferences.edit().putInt(KEY_MODE, ModePolicy.sanitizeMode(mode)).apply();
            postOperation(new ControllerOperation() {
                @Override
                public void run(DaxController target) throws Exception {
                    applyDesiredState();
                }
            });
        }

        void setIeq(int value) {
            setProfileParameter(
                    "ieq",
                    DaxParameterProtocol.PARAM_IEQ_PRESET,
                    ControlValuePolicy.sanitizeIeq(value));
        }

        void setDialogEnabled(boolean enabled) {
            setProfileParameter(
                    "dialog_enabled",
                    DaxParameterProtocol.PARAM_DIALOG_ENHANCEMENT_ENABLE,
                    enabled ? 1 : 0);
        }

        void setDialogAmount(int amount) {
            setProfileParameter(
                    "dialog_amount",
                    DaxParameterProtocol.PARAM_DIALOG_ENHANCEMENT_AMOUNT,
                    ControlValuePolicy.sanitizeDialogAmount(amount));
        }

        void setVolumeLeveler(boolean enabled) {
            setProfileParameter(
                    "leveler",
                    DaxParameterProtocol.PARAM_VOLUME_LEVELER,
                    enabled ? 1 : 0);
        }

        void setHeadphoneVirtualizer(boolean enabled) {
            setProfileParameter(
                    "headphone_virtualizer",
                    DaxParameterProtocol.PARAM_HEADPHONE_VIRTUALIZER,
                    enabled ? 1 : 0);
        }

        void setSpeakerVirtualizer(boolean enabled) {
            setProfileParameter(
                    "speaker_virtualizer",
                    DaxParameterProtocol.PARAM_SPEAKER_VIRTUALIZER,
                    enabled ? 1 : 0);
        }

        void setGeqEnabled(final boolean enabled) {
            preferences.edit().putBoolean(KEY_GEQ_ENABLED, enabled).apply();
            postOperation(new ControllerOperation() {
                @Override
                public void run(DaxController target) {
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
            postOperation(new ControllerOperation() {
                @Override
                public void run(DaxController target) {
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
            postOperation(new ControllerOperation() {
                @Override
                public void run(DaxController target) {
                    if (ModePolicy.usesCustomGeq(getDesiredMode())) {
                        applyCustomGeq(target);
                    }
                }
            });
        }

        void refresh() {
            scheduleRefresh(false);
        }

        private void setProfileParameter(
                final String name,
                final int parameter,
                final int value) {
            final int profile = ModePolicy.profileForMode(getDesiredMode());
            preferences.edit().putInt(profileKey(profile, name), value).apply();
            postOperation(new ControllerOperation() {
                @Override
                public void run(DaxController target) {
                    target.setProfileInt(profile, parameter, value);
                }
            });
        }
    }
}
