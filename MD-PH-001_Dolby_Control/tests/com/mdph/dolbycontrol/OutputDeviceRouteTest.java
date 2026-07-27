package com.mdph.dolbycontrol;

public final class OutputDeviceRouteTest {
    public static void main(String[] args) {
        assertEquals(0x00000001, OutputDeviceRoute.nativeMaskForType(1));
        assertEquals(0x00000002, OutputDeviceRoute.nativeMaskForType(2));
        assertEquals(0x00000004, OutputDeviceRoute.nativeMaskForType(3));
        assertEquals(0x00000008, OutputDeviceRoute.nativeMaskForType(4));
        assertEquals(0x00000020, OutputDeviceRoute.nativeMaskForType(7));
        assertEquals(0x00000080, OutputDeviceRoute.nativeMaskForType(8));
        assertEquals(0x00000400, OutputDeviceRoute.nativeMaskForType(9));
        assertEquals(0x00040000, OutputDeviceRoute.nativeMaskForType(10));
        assertEquals(0x00004000, OutputDeviceRoute.nativeMaskForType(11));
        assertEquals(0x04000000, OutputDeviceRoute.nativeMaskForType(22));
        assertEquals(0x20000000, OutputDeviceRoute.nativeMaskForType(26));
        assertEquals(0x20000001, OutputDeviceRoute.nativeMaskForType(27));
        assertEquals(0x20000002, OutputDeviceRoute.nativeMaskForType(30));
        assertEquals(0, OutputDeviceRoute.nativeMaskForType(999));

        assertTuning(0, "default_internal_speaker", 0x00000002);
        assertTuning(3, "default_headphone", 0x00000008);
        assertTuning(4, "default_bluetooth", 0x00000080);
        assertTuning(5, "default_headphone", 0x04000000);
        assertTuning(1, "default_hdmi", 0x00000400);
        assertTuning(-1, null, 0);
        System.out.println("OutputDeviceRouteTest PASS");
    }

    private static void assertTuning(int expectedPort, String expectedDevice, int mask) {
        assertEquals(expectedPort, OutputDeviceRoute.tuningPortForMask(mask));
        String actualDevice = OutputDeviceRoute.defaultTuningDeviceForMask(mask);
        if (expectedDevice == null ? actualDevice != null : !expectedDevice.equals(actualDevice)) {
            throw new AssertionError(
                    "expected=" + expectedDevice + " actual=" + actualDevice);
        }
    }

    private static void assertEquals(int expected, int actual) {
        if (expected != actual) {
            throw new AssertionError(
                    "expected=0x" + Integer.toHexString(expected)
                            + " actual=0x" + Integer.toHexString(actual));
        }
    }
}
