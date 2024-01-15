package org.example;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Date;

public class LogTimeMonitor {

    public static String fetchLastLogTime(String filename) {
        Path path = Paths.get(filename);
        try {
            return Files.readString(path);
        } catch (IOException e) {
            System.err.println("Error reading the last log time file: " + e.getMessage());
            return "Unable to fetch last log time.";
        }
    }

    public static boolean updateLastLogTime(String filename) {

        try {
            // Create a File object from the filename
            File file = new File(filename);

            // Ensure parent directories exist
            File parentDir = file.getParentFile();
            if (parentDir != null && !parentDir.exists()) {
                parentDir.mkdirs();
            }

            SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmmss");
        String timestamp = sdf.format(new Date());

        try (PrintWriter out = new PrintWriter(new FileWriter(filename))) {
            out.println(timestamp);
            return  true;
        } catch (IOException e) {
            System.err.println("Error writing to last log time file: " + e.getMessage());
        }
        return false;
        } catch (Exception e) {
            System.err.println("Error writing to last log time file: " + e.getMessage());
            return false;
        }
    }

}
