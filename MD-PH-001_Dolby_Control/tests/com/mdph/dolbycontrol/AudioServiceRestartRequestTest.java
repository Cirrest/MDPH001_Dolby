package com.mdph.dolbycontrol;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

public final class AudioServiceRestartRequestTest {
    public static void main(String[] args) throws Exception {
        File directory = Files.createTempDirectory("dolby-restart-request-").toFile();
        try {
            File request = AudioServiceRestartRequest.create(directory, 123456789L);
            assertEquals("restart_audio_service.request", request.getName());
            assertEquals("123456789\n", new String(
                    Files.readAllBytes(request.toPath()), StandardCharsets.US_ASCII));
        } finally {
            deleteRecursively(directory);
        }
        System.out.println("AudioServiceRestartRequestTest PASS");
    }

    private static void deleteRecursively(File file) {
        File[] children = file.listFiles();
        if (children != null) {
            for (File child : children) {
                deleteRecursively(child);
            }
        }
        file.delete();
    }

    private static void assertEquals(String expected, String actual) {
        if (!expected.equals(actual)) {
            throw new AssertionError("expected=" + expected + " actual=" + actual);
        }
    }
}
