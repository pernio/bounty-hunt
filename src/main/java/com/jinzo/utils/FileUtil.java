package com.jinzo.utils;

import com.jinzo.BountyHunt;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Date;

public class FileUtil {
    public static void logToFile(File file, String message) {
        String entry = "[" + new Date() + "] " + message;
        try (FileWriter writer = new FileWriter(file, true)) {
            writer.write(entry + System.lineSeparator());
        } catch (IOException e) {
            BountyHunt.getInstance().getLogger().warning("Failed to write to log file.");
        }
    }
}
