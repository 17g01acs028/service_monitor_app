package org.example;

import java.io.*;
import java.nio.file.*;
import java.text.*;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.zip.*;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream;
import org.apache.commons.compress.utils.IOUtils;

public class LogArchiver implements Runnable {
    private final String logDirectoryPath;
    private final SimpleDateFormat dateFormat;
    private final long archiveIntervalMillis;
    private final String filename;

   ServiceInfo service;


    public LogArchiver(String logDirectoryPath, String filename, String timeInterval, ServiceInfo service) {
        this.logDirectoryPath = logDirectoryPath;
        this.filename = filename;
        this.dateFormat = new SimpleDateFormat("yyyyMMdd_HHmmss");
        this.service=service;
        switch (timeInterval.toLowerCase()) {
            case "hourly":
                this.archiveIntervalMillis = TimeUnit.HOURS.toMillis(1);
                break;
            case "daily":
                this.archiveIntervalMillis = TimeUnit.DAYS.toMillis(1);
                break;
            case "weekly":
                this.archiveIntervalMillis = TimeUnit.DAYS.toMillis(7);
                break;
            case "monthly":
                LocalDate today = LocalDate.now();
                LocalDate oneMonthLater = today.plusMonths(1);
                long daysBetween = ChronoUnit.DAYS.between(today, oneMonthLater);
                this.archiveIntervalMillis = TimeUnit.DAYS.toMillis(daysBetween);
                break;
            default:
                throw new IllegalArgumentException("Invalid time interval");
        }
    }

    @Override
    public void run() {
        try {
            File logDir = new File(logDirectoryPath);
            if (!logDir.exists() || !logDir.isDirectory()) return;

            File[] logFiles = logDir.listFiles((dir, name) -> name.endsWith(".log"));
            if (logFiles == null || logFiles.length == 0) return;
            String zipFileName = "";

            if(shoulCreateNewdArchive(service.getLastArchiveTime()) || service.getLastArchiveTime().isEmpty()){
                SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmmss");
                String timestamp = sdf.format(new Date());
                service.setLastArchiveTime(timestamp);

                Main.updateCsvLastTime(service.getId(),timestamp,filename,"archive");

            }
                zipFileName = logDirectoryPath + "/"+service.getLastArchiveTime()+"_archive.zip";


            File zipFile = new File(zipFileName);
            File tempZipFile = new File(zipFileName + ".tmp");

            // Create a temporary ZIP file
            try (ZipArchiveOutputStream tempOut = new ZipArchiveOutputStream(tempZipFile)) {
                // If the original ZIP exists, copy its contents to the temporary ZIP
                if (zipFile.exists()) {
                    try (ZipFile existingZip = new ZipFile(zipFile)) {
                        Enumeration<? extends ZipEntry> entries = existingZip.entries();
                        while (entries.hasMoreElements()) {
                            ZipEntry entry = entries.nextElement();
                            tempOut.putArchiveEntry(new ZipArchiveEntry(entry.getName()));
                            IOUtils.copy(existingZip.getInputStream(entry), tempOut);
                            tempOut.closeArchiveEntry();
                        }
                    }
                }

                // Add new log files to the temporary ZIP
                for (File logFile : logFiles) {
                    String applicationLogName = service.getLastLogTime() + "_application.log";
                    String serverLogName = service.getLastLogTime() + "_server.log";

                    if (!logFile.getName().equals(applicationLogName) && !logFile.getName().equals(serverLogName) && shouldArchive(logFile.getName())) {
                        ZipArchiveEntry zipEntry = new ZipArchiveEntry(logFile, logFile.getName());
                        tempOut.putArchiveEntry(zipEntry);
                        try (FileInputStream fis = new FileInputStream(logFile)) {
                            IOUtils.copy(fis, tempOut);
                        }
                        tempOut.closeArchiveEntry();
                        Files.delete(logFile.toPath());
                    }
                }
            }

            // Replace the original ZIP with the temporary one
            if (tempZipFile.exists()) {
                if (zipFile.exists()) {
                    Files.delete(zipFile.toPath());
                }
                Files.move(tempZipFile.toPath(), zipFile.toPath());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private boolean shouldArchive(String fileName) {
        try {
            // Extracting the timestamp from the file name
            String timestampStr = fileName.substring(0, 15);
            Date fileDate = dateFormat.parse(timestampStr);
            long fileTime = fileDate.getTime();
            long currentTime = System.currentTimeMillis();

            return (currentTime - fileTime) > archiveIntervalMillis;
        } catch (ParseException e) {
            e.printStackTrace();
            return false;
        }
    }

    private boolean shoulCreateNewdArchive(String timestring) {
        try {
            // Extracting the timestamp from the file name
            Date fileDate = dateFormat.parse(timestring);
            long fileTime = fileDate.getTime();
            long currentTime = System.currentTimeMillis();

            // Calculate the future time by adding the archive interval
            long futureTime = fileTime + archiveIntervalMillis;

            // Check if the future time is after the current time
            return futureTime <= currentTime;
        } catch (ParseException e) {
            e.printStackTrace();
            return false;
        }
    }

    private void zipFile(File file, ZipOutputStream zos) throws IOException {
        try (FileInputStream fis = new FileInputStream(file)) {
            ZipEntry zipEntry = new ZipEntry(file.getName());
            zos.putNextEntry(zipEntry);

            byte[] bytes = new byte[1024];
            int length;
            while ((length = fis.read(bytes)) >= 0) {
                zos.write(bytes, 0, length);
            }

            zos.closeEntry();
        }
    }
}
