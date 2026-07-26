package com.codex.dolbycontrol;

public final class ControlValuePolicyTest {
    public static void main(String[] args) {
        assertEquals(0, ControlValuePolicy.sanitizeIeq(-1));
        assertEquals(3, ControlValuePolicy.sanitizeIeq(9));
        assertEquals(2, ControlValuePolicy.sanitizeIeq(2));
        assertEquals(0, ControlValuePolicy.sanitizeDialogAmount(-4));
        assertEquals(16, ControlValuePolicy.sanitizeDialogAmount(99));
        assertEquals(7, ControlValuePolicy.sanitizeDialogAmount(7));
        assertTrue(ControlValuePolicy.isValidBandIndex(0));
        assertTrue(ControlValuePolicy.isValidBandIndex(19));
        assertFalse(ControlValuePolicy.isValidBandIndex(-1));
        assertFalse(ControlValuePolicy.isValidBandIndex(20));

        System.out.println("ControlValuePolicyTest PASS");
    }

    private static void assertEquals(int expected, int actual) {
        if (expected != actual) {
            throw new AssertionError("expected=" + expected + " actual=" + actual);
        }
    }

    private static void assertTrue(boolean value) {
        if (!value) {
            throw new AssertionError("Expected true");
        }
    }

    private static void assertFalse(boolean value) {
        if (value) {
            throw new AssertionError("Expected false");
        }
    }
}
