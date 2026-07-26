package com.codex.dolbycontrol;

public final class ModePolicyTest {
    public static void main(String[] args) {
        assertEquals(0, ModePolicy.profileForMode(ModePolicy.MODE_DYNAMIC));
        assertEquals(1, ModePolicy.profileForMode(ModePolicy.MODE_MOVIE));
        assertEquals(2, ModePolicy.profileForMode(ModePolicy.MODE_MUSIC));
        assertEquals(2, ModePolicy.profileForMode(ModePolicy.MODE_CUSTOM));
        assertTrue(ModePolicy.usesCustomGeq(ModePolicy.MODE_CUSTOM));
        assertFalse(ModePolicy.usesCustomGeq(ModePolicy.MODE_MUSIC));
        assertEquals(ModePolicy.MODE_DYNAMIC, ModePolicy.sanitizeMode(-1));
        assertEquals(ModePolicy.MODE_DYNAMIC, ModePolicy.sanitizeMode(99));

        System.out.println("ModePolicyTest PASS");
    }

    private static void assertEquals(int expected, int actual) {
        if (expected != actual) {
            throw new AssertionError("expected=" + expected + " actual=" + actual);
        }
    }

    private static void assertTrue(boolean actual) {
        if (!actual) {
            throw new AssertionError("Expected true");
        }
    }

    private static void assertFalse(boolean actual) {
        if (actual) {
            throw new AssertionError("Expected false");
        }
    }
}
