package com.mdph.dolbycontrol;

import java.util.Arrays;

public final class DaxParameterProtocolTest {
    public static void main(String[] args) {
        assertEquals(
                0x016E0205,
                DaxParameterProtocol.profileGetKey(
                        2,
                        DaxParameterProtocol.PARAM_GEQ_BAND_GAINS));
        assertEquals(20, DaxParameterProtocol.valueCountForParameter(
                DaxParameterProtocol.PARAM_GEQ_BAND_GAINS));
        assertEquals(1, DaxParameterProtocol.valueCountForParameter(
                DaxParameterProtocol.PARAM_IEQ_PRESET));

        int[] gains = new int[20];
        gains[0] = -160;
        gains[19] = 160;
        byte[] encoded = DaxParameterProtocol.encodeProfileSet(
                2,
                DaxParameterProtocol.PARAM_GEQ_BAND_GAINS,
                gains);
        assertEquals((20 + 4) * 4, encoded.length);
        assertArrayEquals(
                new int[] {
                        DaxParameterProtocol.EFFECT_PARAM_PROFILE_PARAMETER,
                        21,
                        2,
                        DaxParameterProtocol.PARAM_GEQ_BAND_GAINS,
                        -160
                },
                DaxParameterProtocol.decodeInts(encoded, 0, 5));
        assertEquals(160, DaxParameterProtocol.decodeInt(encoded, encoded.length - 4));

        assertArrayEquals(
                intsToLittleEndian(0x016E0205),
                DaxParameterProtocol.encodeParameterKey(0x016E0205));

        assertEquals(0x4D445254, DaxParameterProtocol.EFFECT_PARAM_ROUTE_SYNC);
        assertArrayEquals(
                intsToLittleEndian(0x00000008),
                DaxParameterProtocol.encodeRouteDevice(0x00000008));

        assertEquals(
                0x00030002,
                DaxParameterProtocol.tuningDeviceNameLengthKey(3));
        assertEquals(
                0x00030004,
                DaxParameterProtocol.selectedTuningDeviceKey(3));
        byte[] selectedTuning = DaxParameterProtocol.encodeSelectedTuningDevice(
                3,
                "default_headphone");
        assertEquals(21, selectedTuning.length);
        assertEquals(3, DaxParameterProtocol.decodeInt(selectedTuning, 0));
        assertEquals(
                "default_headphone",
                DaxParameterProtocol.decodeUtf8String(selectedTuning, 4));

        System.out.println("DaxParameterProtocolTest PASS");
    }

    private static byte[] intsToLittleEndian(int... values) {
        byte[] bytes = new byte[values.length * 4];
        for (int i = 0; i < values.length; i++) {
            int value = values[i];
            int offset = i * 4;
            bytes[offset] = (byte) value;
            bytes[offset + 1] = (byte) (value >>> 8);
            bytes[offset + 2] = (byte) (value >>> 16);
            bytes[offset + 3] = (byte) (value >>> 24);
        }
        return bytes;
    }

    private static void assertEquals(int expected, int actual) {
        if (expected != actual) {
            throw new AssertionError("expected=" + expected + " actual=" + actual);
        }
    }

    private static void assertEquals(String expected, String actual) {
        if (!expected.equals(actual)) {
            throw new AssertionError("expected=" + expected + " actual=" + actual);
        }
    }

    private static void assertArrayEquals(byte[] expected, byte[] actual) {
        if (!Arrays.equals(expected, actual)) {
            throw new AssertionError(
                    "expected=" + Arrays.toString(expected)
                            + " actual=" + Arrays.toString(actual));
        }
    }

    private static void assertArrayEquals(int[] expected, int[] actual) {
        if (!Arrays.equals(expected, actual)) {
            throw new AssertionError(
                    "expected=" + Arrays.toString(expected)
                            + " actual=" + Arrays.toString(actual));
        }
    }
}
