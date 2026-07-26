package com.codex.dolbycontrol;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

final class DaxParameterProtocol {
    static final int EFFECT_PARAM_EFFECT_ENABLE = 0x00000000;
    static final int EFFECT_PARAM_SELECTED_TUNING = 0x00000004;
    static final int EFFECT_PARAM_CPDP_VALUES = 0x00000005;
    static final int EFFECT_PARAM_PROFILE = 0x0A000000;
    static final int EFFECT_PARAM_PROFILE_NUM = 0x03000000;
    static final int EFFECT_PARAM_PROFILE_PARAMETER = 0x01000000;
    static final int EFFECT_PARAM_PROFILE_PORT_PARAMETER = 0x02000000;

    static final int PARAM_HEADPHONE_VIRTUALIZER = 0x65;
    static final int PARAM_SPEAKER_VIRTUALIZER = 0x66;
    static final int PARAM_VOLUME_LEVELER = 0x67;
    static final int PARAM_IEQ_PRESET = 0x68;
    static final int PARAM_DIALOG_ENHANCEMENT_ENABLE = 0x69;
    static final int PARAM_GEQ_ENABLE = 0x6A;
    static final int PARAM_IEQ_AMOUNT = 0x6B;
    static final int PARAM_DIALOG_ENHANCEMENT_AMOUNT = 0x6C;
    static final int PARAM_DIALOG_ENHANCEMENT_DUCKING = 0x6D;
    static final int PARAM_GEQ_BAND_GAINS = 0x6E;

    private DaxParameterProtocol() {
    }

    static int basicGetKey(int parameter) {
        return parameter + EFFECT_PARAM_CPDP_VALUES;
    }

    static int profileGetKey(int profile, int parameter) {
        return EFFECT_PARAM_PROFILE_PARAMETER
                + EFFECT_PARAM_CPDP_VALUES
                + (profile << 8)
                + (parameter << 16);
    }

    static int profilePortGetKey(int profile, int port, int parameter) {
        return EFFECT_PARAM_PROFILE_PORT_PARAMETER
                + EFFECT_PARAM_CPDP_VALUES
                + (profile << 12)
                + (port << 8)
                + (parameter << 16);
    }

    static int valueCountForParameter(int parameter) {
        return parameter == PARAM_GEQ_BAND_GAINS ? 20 : 1;
    }

    static byte[] encodeParameterKey(int key) {
        return encodeInts(key);
    }

    static byte[] encodeBasicGetBuffer(int parameter) {
        return encodeInts(parameter, 0, 0);
    }

    static byte[] encodeBasicSet(int parameter, int value) {
        return encodeInts(parameter, 1, value);
    }

    static byte[] encodeProfileSet(int profile, int parameter, int[] values) {
        return encodeSet(
                EFFECT_PARAM_PROFILE_PARAMETER,
                new int[] {profile, parameter},
                values);
    }

    static byte[] encodeProfilePortSet(int profile, int port, int parameter, int[] values) {
        return encodeSet(
                EFFECT_PARAM_PROFILE_PORT_PARAMETER,
                new int[] {profile, port, parameter},
                values);
    }

    static int decodeInt(byte[] bytes, int offset) {
        return ByteBuffer.wrap(bytes, offset, 4)
                .order(ByteOrder.LITTLE_ENDIAN)
                .getInt();
    }

    static int[] decodeInts(byte[] bytes, int offset, int count) {
        ByteBuffer buffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN);
        buffer.position(offset);
        int[] values = new int[count];
        for (int i = 0; i < count; i++) {
            values[i] = buffer.getInt();
        }
        return values;
    }

    private static byte[] encodeSet(int command, int[] selectors, int[] values) {
        int[] fields = new int[2 + selectors.length + values.length];
        fields[0] = command;
        fields[1] = values.length + 1;
        System.arraycopy(selectors, 0, fields, 2, selectors.length);
        System.arraycopy(values, 0, fields, 2 + selectors.length, values.length);
        return encodeInts(fields);
    }

    private static byte[] encodeInts(int... values) {
        ByteBuffer buffer = ByteBuffer.allocate(values.length * 4)
                .order(ByteOrder.LITTLE_ENDIAN);
        for (int value : values) {
            buffer.putInt(value);
        }
        return buffer.array();
    }
}
