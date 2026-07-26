package com.codex.dolbycontrol;

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
        AudioEffect.Descriptor descriptor = findDescriptor();
        if (descriptor == null) {
            throw new IllegalStateException("DAP implementation UUID is not registered");
        }
        Constructor<AudioEffect> constructor = AudioEffect.class.getConstructor(
                UUID.class, UUID.class, int.class, int.class);
        try {
            return new DaxController(constructor.newInstance(EFFECT_TYPE_NULL, DAP_UUID, 1, 0));
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
        getParameter = AudioEffect.class.getMethod(
                "getParameter", byte[].class, byte[].class);
        setParameterBytes = AudioEffect.class.getMethod(
                "setParameter", byte[].class, byte[].class);
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
