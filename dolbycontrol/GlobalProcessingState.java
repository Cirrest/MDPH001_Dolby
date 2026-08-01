package com.mdph.dolbycontrol;

import java.io.File;
import java.io.IOException;

final class GlobalProcessingState {
    static final String FILE_NAME = "global_processing.disabled";

    private GlobalProcessingState() {
    }

    static boolean isDisabled(File filesDir) {
        return new File(filesDir, FILE_NAME).isFile();
    }

    static void setDisabled(File filesDir, boolean disabled) throws IOException {
        File marker = new File(filesDir, FILE_NAME);
        if (disabled) {
            if (!filesDir.isDirectory() && !filesDir.mkdirs()) {
                throw new IOException("Unable to create controller files directory");
            }
            if (!marker.isFile() && !marker.createNewFile()) {
                throw new IOException("Unable to create global processing marker");
            }
        } else if (marker.exists() && !marker.delete()) {
            throw new IOException("Unable to remove global processing marker");
        }
    }
}
