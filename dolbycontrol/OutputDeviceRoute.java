package com.mdph.dolbycontrol;

final class OutputDeviceRoute {
    private OutputDeviceRoute() {
    }

    static int nativeMaskForType(int deviceType) {
        switch (deviceType) {
            case 1: return 0x00000001;  // Built-in earpiece
            case 2: return 0x00000002;  // Built-in speaker
            case 3: return 0x00000004;  // Wired headset
            case 4: return 0x00000008;  // Wired headphones
            case 7: return 0x00000020;  // Bluetooth SCO headset
            case 8: return 0x00000080;  // Bluetooth A2DP
            case 9: return 0x00000400;  // HDMI
            case 10: return 0x00040000; // HDMI ARC
            case 11: return 0x00004000; // USB device
            case 22: return 0x04000000; // USB headset
            case 26: return 0x20000000; // Bluetooth LE headset
            case 27: return 0x20000001; // Bluetooth LE speaker
            case 30: return 0x20000002; // Bluetooth LE broadcast
            default: return 0;
        }
    }

    static int tuningPortForMask(int deviceMask) {
        switch (deviceMask) {
            case 0x00000001:
            case 0x00000002:
                return 0; // Internal speaker
            case 0x00000400:
            case 0x00040000:
                return 1; // HDMI
            case 0x00000004:
            case 0x00000008:
                return 3; // Headphone
            case 0x00000010:
            case 0x00000020:
            case 0x00000040:
            case 0x00000080:
            case 0x00000100:
            case 0x00000200:
            case 0x20000000:
            case 0x20000001:
            case 0x20000002:
                return 4; // Bluetooth
            case 0x00002000:
            case 0x00004000:
            case 0x04000000:
                return 5; // USB
            default:
                return -1;
        }
    }

    static String defaultTuningDeviceForMask(int deviceMask) {
        switch (tuningPortForMask(deviceMask)) {
            case 0: return "default_internal_speaker";
            case 1: return "default_hdmi";
            case 3: return "default_headphone";
            case 4: return "default_bluetooth";
            case 5: return "default_headphone";
            default: return null;
        }
    }
}
