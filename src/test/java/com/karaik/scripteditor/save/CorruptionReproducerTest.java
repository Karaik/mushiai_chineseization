package com.karaik.scripteditor.save;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;

/**
 * 针对喵喵在保存时正好电脑死机，而导致文件被写坏，进行了一些修改，这是当时的测试类
 * 若要回溯当时的情况，请在 4c8687ece9d2d4a0d02d5c1edbc6d0cb6cf75a8e 复现
 */
class CorruptionReproducerTest {

    @TempDir
    Path tempDir;

    @Test
    void simulateCrashDuringOverwriteLeavesZeroFilledFile() throws IOException {
        Path source = Paths.get("src/main/resources/spt/01A.spt.txt");
        Assertions.assertTrue(Files.exists(source), "sample missing under src/main/resources/spt");

        Path workingCopy = tempDir.resolve(source.getFileName());
        Files.copy(source, workingCopy, StandardCopyOption.REPLACE_EXISTING);

        byte[] originalBytes = Files.readAllBytes(source);
        int crashOffset = 8192; // 8 KiB, typical buffer size
        int chunkSize = Math.min(65536, originalBytes.length - crashOffset);
        Assertions.assertTrue(chunkSize > 0, "sample too small to reproduce zero fill");

        // Simulate FileOutputStream truncate + partial write + sudden crash.
        try (RandomAccessFile raf = new RandomAccessFile(workingCopy.toFile(), "rw")) {
            raf.setLength(0); // truncate()
            raf.setLength(originalBytes.length); // pre-extend so unwritten ranges are zeroed
            raf.seek(crashOffset);
            raf.write(originalBytes, crashOffset, chunkSize); // write just a slice
            raf.getFD().sync(); // force current buffers to disk, then "crash"
            // Crash simulation: stop writing here and let method exit.
        }

        byte[] corruptedBytes = Files.readAllBytes(workingCopy);
        Assertions.assertEquals(originalBytes.length, corruptedBytes.length, "size deviated from original");
        Assertions.assertFalse(Arrays.equals(originalBytes, corruptedBytes), "content unexpectedly intact");

        long leadingZeros = countLeadingZeros(corruptedBytes);
        long trailingZeros = countTrailingZeros(corruptedBytes);
        Assertions.assertTrue(leadingZeros >= crashOffset, "leading zero run shorter than expected");
        Assertions.assertTrue(trailingZeros >= (originalBytes.length - crashOffset - chunkSize), "trailing zero run shorter than expected");

        System.out.println("Corrupted file located at: " + workingCopy.toAbsolutePath());
        System.out.printf(
                "leading zero fill: %d B, retained data: %d B, trailing zero fill: %d B%n",
                leadingZeros,
                chunkSize,
                trailingZeros
        );
    }

    private static long countLeadingZeros(byte[] data) {
        long count = 0;
        for (byte b : data) {
            if (b != 0) {
                break;
            }
            count++;
        }
        return count;
    }

    private static long countTrailingZeros(byte[] data) {
        long count = 0;
        for (int i = data.length - 1; i >= 0; i--) {
            if (data[i] != 0) {
                break;
            }
            count++;
        }
        return count;
    }
}