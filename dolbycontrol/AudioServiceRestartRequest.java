package com.mdph.dolbycontrol;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

final class AudioServiceRestartRequest {
    static final String FILE_NAME = "restart_audio_service.request";

    private AudioServiceRestartRequest() {
    }

    static File create(File filesDirectory, long timestamp) throws IOException {
        if (!filesDirectory.isDirectory() && !filesDirectory.mkdirs()) {
            throw new IOException("Unable to create app files directory");
        }
        File temporary = new File(filesDirectory, FILE_NAME + ".tmp");
        File request = new File(filesDirectory, FILE_NAME);
        FileOutputStream output = new FileOutputStream(temporary, false);
        try {
            output.write((Long.toString(timestamp) + "\n")
                    .getBytes(StandardCharsets.US_ASCII));
            output.getFD().sync();
        } finally {
            output.close();
        }
        if (!temporary.renameTo(request)) {
            if (request.exists() && !request.delete()) {
                throw new IOException("Unable to replace audio restart request");
            }
            if (!temporary.renameTo(request)) {
                throw new IOException("Unable to publish audio restart request");
            }
        }
        return request;
    }
}
