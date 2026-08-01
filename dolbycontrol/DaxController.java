package com.mdph.dolbycontrol;

import android.media.audiofx.AudioEffect;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.UUID;

final class DaxController implements AutoCloseable {
    private static final UUID EFFECT_TYPE_NULL = new UUID(0L, 0L);
    private static final UUID DAP_UUID = UUID.fromString(
            "9d4921da-8225-4f29-aefa-39537a04bcaa");

    private final AudioEffect effect;
    private final Method getParameter;
    private final Method setParameterBytes;

    static DaxController open() throws Exception {
        return open(0);
    }

    static DaxController open(int sessionId) throws Exception {
        AudioEffect.Descriptor descriptor = findDescriptor();
        if (descriptor == null) {
            throw new IllegalStateException("DAP implementation UUID is not registered");
        }
        Constructor<AudioEffect> constructor = AudioEffect.class.getConstructor(
                UUID.class, UUID.class, int.class, int.class);
        try {
            return new DaxController(constructor.newInstance(
                    EFFECT_TYPE_NULL, DAP_UUID, 1, sessionId));
        } catch (InvocationTargetException error) {
            Throwable cause = error.getCause();
            if (cause instanceof Exception) {
                throw (Exception) cause;
            }
            throw error;
        }
    }

    private DaxController(AudioEffect effect) throws NoSuchMethodException {
        this.effect = effect;
        getParameter = AudioEffect.class.getDeclaredMethod(
                "getParameter", byte[].class, byte[].class);
        setParameterBytes = AudioEffect.class.getDeclaredMethod(
                "setParameter", byte[].class, byte[].class);
        getParameter.setAccessible(true);
        setParameterBytes.setAccessible(true);
    }

    String getName() {
        return effect.getDescriptor().name;
    }

    boolean hasControl() {
        return effect.hasControl();
    }

    int getEffectId() {
        return effect.getId();
    }

    int getEnable() {
        return getBasicInt(DaxParameterProtocol.EFFECT_PARAM_EFFECT_ENABLE);
    }

    void setEnabled(boolean enabled) {
        setBasicInt(DaxParameterProtocol.EFFECT_PARAM_EFFECT_ENABLE, enabled ? 1 : 0);
        int status = effect.setEnabled(enabled);
        if (status != AudioEffect.SUCCESS && status != AudioEffect.ALREADY_EXISTS) {
            throw new IllegalStateException("setEnabled returned " + status);
        }
    }

    int getProfileCount() {
        return getBasicInt(DaxParameterProtocol.EFFECT_PARAM_PROFILE_NUM);
    }

    int getProfile() {
        return getBasicInt(DaxParameterProtocol.EFFECT_PARAM_PROFILE);
    }

    void setProfile(int profile) {
        setBasicInt(DaxParameterProtocol.EFFECT_PARAM_PROFILE, profile);
    }

    int getProfileInt(int profile, int parameter) {
        return getProfileInts(profile, parameter, 1)[0];
    }

    int[] getProfileInts(int profile, int parameter, int valueCount) {
        byte[] result = new byte[(valueCount + 2) * 4];
        int key = DaxParameterProtocol.profileGetKey(profile, parameter);
        checkStatus(
                "get profile parameter 0x" + Integer.toHexString(parameter),
                invoke(getParameter, DaxParameterProtocol.encodeParameterKey(key), result));
        return DaxParameterProtocol.decodeInts(result, 0, valueCount);
    }

    void setProfileInt(int profile, int parameter, int value) {
        setProfileInts(profile, parameter, new int[] {value});
    }

    void setProfileInts(int profile, int parameter, int[] values) {
        byte[] payload = DaxParameterProtocol.encodeProfileSet(profile, parameter, values);
        checkStatus(
                "set profile parameter 0x" + Integer.toHexString(parameter),
                invoke(
                        setParameterBytes,
                        DaxParameterProtocol.encodeParameterKey(
                                DaxParameterProtocol.EFFECT_PARAM_CPDP_VALUES),
                        payload));
    }

    void setProfilePortInt(int profile, int port, int parameter, int value) {
        setProfilePortInts(profile, port, parameter, new int[] {value});
    }

    void setProfilePortInts(int profile, int port, int parameter, int[] values) {
        byte[] payload = DaxParameterProtocol.encodeProfilePortSet(
                profile, port, parameter, values);
        checkStatus(
                "set profile port parameter 0x" + Integer.toHexString(parameter)
                        + " port " + port,
                invoke(
                        setParameterBytes,
                        DaxParameterProtocol.encodeParameterKey(
                                DaxParameterProtocol.EFFECT_PARAM_CPDP_VALUES),
                        payload));
    }

    void markProfileSettingsModified(int profile) {
        try {
            byte[] payload = DaxParameterProtocol.encodeBasicSet(
                    DaxParameterProtocol.EFFECT_PARAM_PROFILE_SETTINGS_MODIFIED,
                    profile);
            checkStatus(
                    "mark profile settings modified",
                    invoke(
                            setParameterBytes,
                            DaxParameterProtocol.encodeParameterKey(
                                    DaxParameterProtocol.EFFECT_PARAM_CPDP_VALUES),
                            payload));
        } catch (RuntimeException error) {
            // This key is read-only on some DAX3 vendor builds.
            // Profile/port writes are still retained and applied independently.
        }
    }

    void syncOutputDevice(int deviceMask) {
        checkStatus(
                "sync output device 0x" + Integer.toHexString(deviceMask),
                invoke(
                        setParameterBytes,
                        DaxParameterProtocol.encodeParameterKey(
                                DaxParameterProtocol.EFFECT_PARAM_ROUTE_SYNC),
                        DaxParameterProtocol.encodeRouteDevice(deviceMask)));
    }

    String getSelectedTuningDevice(int port) {
        byte[] lengthResult = new byte[4];
        checkStatus(
                "get tuning device name length for port " + port,
                invoke(
                        getParameter,
                        DaxParameterProtocol.encodeParameterKey(
                                DaxParameterProtocol.tuningDeviceNameLengthKey(port)),
                        lengthResult));
        int length = DaxParameterProtocol.decodeInt(lengthResult, 0);
        if (length <= 0 || length > 4096) {
            throw new IllegalStateException("invalid tuning device name length " + length);
        }
        byte[] result = new byte[((length + 4) >> 2) * 4];
        checkStatus(
                "get selected tuning device for port " + port,
                invoke(
                        getParameter,
                        DaxParameterProtocol.encodeParameterKey(
                                DaxParameterProtocol.selectedTuningDeviceKey(port)),
                        result));
        return DaxParameterProtocol.decodeUtf8String(result, 0);
    }

    void setSelectedTuningDevice(int port, String deviceId) {
        checkStatus(
                "set selected tuning device for port " + port,
                invoke(
                        setParameterBytes,
                        DaxParameterProtocol.encodeParameterKey(
                                DaxParameterProtocol.EFFECT_PARAM_SELECTED_TUNING),
                        DaxParameterProtocol.encodeSelectedTuningDevice(port, deviceId)));
    }

    boolean isAlive() {
        try {
            return getProfileCount() > 0;
        } catch (RuntimeException error) {
            return false;
        }
    }

    @Override
    public void close() {
        effect.release();
    }

    private int getBasicInt(int parameter) {
        byte[] result = DaxParameterProtocol.encodeBasicGetBuffer(parameter);
        checkStatus(
                "get parameter 0x" + Integer.toHexString(parameter),
                invoke(
                        getParameter,
                        DaxParameterProtocol.encodeParameterKey(
                                DaxParameterProtocol.basicGetKey(parameter)),
                        result));
        return DaxParameterProtocol.decodeInt(result, 0);
    }

    private void setBasicInt(int parameter, int value) {
        byte[] payload = DaxParameterProtocol.encodeBasicSet(parameter, value);
        checkStatus(
                "set parameter 0x" + Integer.toHexString(parameter),
                invoke(
                        setParameterBytes,
                        DaxParameterProtocol.encodeParameterKey(
                                DaxParameterProtocol.EFFECT_PARAM_CPDP_VALUES),
                        payload));
    }

    private int invoke(Method method, byte[] parameter, byte[] value) {
        try {
            return (Integer) method.invoke(effect, parameter, value);
        } catch (InvocationTargetException error) {
            Throwable cause = error.getCause();
            if (cause instanceof RuntimeException) {
                throw (RuntimeException) cause;
            }
            throw new IllegalStateException(cause);
        } catch (IllegalAccessException error) {
            throw new IllegalStateException(error);
        }
    }

    private static AudioEffect.Descriptor findDescriptor() {
        AudioEffect.Descriptor[] descriptors = AudioEffect.queryEffects();
        if (descriptors == null) {
            return null;
        }
        for (AudioEffect.Descriptor descriptor : descriptors) {
            if (DAP_UUID.equals(descriptor.uuid)) {
                return descriptor;
            }
        }
        return null;
    }

    private static void checkStatus(String operation, int status) {
        if (status < 0) {
            throw new IllegalStateException(operation + " returned " + status);
        }
    }
}
