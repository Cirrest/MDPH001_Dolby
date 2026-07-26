package com.codex.dolbycontrol;

import java.util.Arrays;

public final class GeqGainMapperTest {
    public static void main(String[] args) {
        assertEquals(160, GeqGainMapper.scaleDbToDapGain(10, 8, 16));
        assertEquals(-160, GeqGainMapper.scaleDbToDapGain(-10, 8, 16));
        assertEquals(40, GeqGainMapper.scaleDbToDapGain(10, 16, 16));
        assertEquals(-40, GeqGainMapper.scaleDbToDapGain(-10, 16, 16));
        assertEquals(133, GeqGainMapper.scaleDbToDapGain(10, 13, 16));
        assertEquals(160, GeqGainMapper.scaleDbToDapGain(12, 0, 16));
        assertEquals(-160, GeqGainMapper.scaleDbToDapGain(-12, 0, 16));

        int[] db = new int[20];
        db[0] = -10;
        db[1] = 5;
        db[19] = 10;
        int[] expected = new int[20];
        expected[0] = -160;
        expected[1] = 80;
        expected[19] = 160;
        assertArrayEquals(expected, GeqGainMapper.mapDbToDapGains(db, 6, 16));

        assertThrows(new Runnable() {
            @Override
            public void run() {
                GeqGainMapper.mapDbToDapGains(new int[10], 6, 16);
            }
        });

        System.out.println("GeqGainMapperTest PASS");
    }

    private static void assertEquals(int expected, int actual) {
        if (expected != actual) {
            throw new AssertionError("expected=" + expected + " actual=" + actual);
        }
    }

    private static void assertArrayEquals(int[] expected, int[] actual) {
        if (!Arrays.equals(expected, actual)) {
            throw new AssertionError(
                    "expected=" + Arrays.toString(expected)
                            + " actual=" + Arrays.toString(actual));
        }
    }

    private static void assertThrows(Runnable runnable) {
        try {
            runnable.run();
        } catch (IllegalArgumentException expected) {
            return;
        }
        throw new AssertionError("Expected IllegalArgumentException");
    }
}
