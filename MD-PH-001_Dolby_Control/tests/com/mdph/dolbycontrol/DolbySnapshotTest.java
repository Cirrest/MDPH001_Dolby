package com.mdph.dolbycontrol;

public final class DolbySnapshotTest {
    public static void main(String[] args) {
        DolbySnapshot source = new DolbySnapshot();
        source.connected = true;
        source.mode = ModePolicy.MODE_CUSTOM;
        source.profile = 2;
        source.geqDb[0] = -4;
        source.outputRoute = "USB";

        DolbySnapshot copy = source.copy();
        source.geqDb[0] = 9;
        source.outputRoute = "Speaker";

        assertTrue(copy.connected);
        assertEquals(ModePolicy.MODE_CUSTOM, copy.mode);
        assertEquals(2, copy.profile);
        assertEquals(-4, copy.geqDb[0]);
        assertEquals("USB", copy.outputRoute);
        assertFalse(source.geqDb == copy.geqDb);

        System.out.println("DolbySnapshotTest PASS");
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
