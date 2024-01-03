package org.example;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.List;
import java.util.ArrayList;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import java.io.Reader;
import java.io.FileReader;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.Locale;
import java.net.Socket;

public class Main {
    private static final int NUMBER_OF_THREADS = 10; // Adjust based on your needs

    public static void main(String[] args) {
        ScheduledExecutorService executor = Executors.newScheduledThreadPool(NUMBER_OF_THREADS);
        String fileName = "services.csv"; // Replace this with dynamic file name if needed

        List<ServiceInfo> services = null;

        if (fileName.endsWith(".csv")) {
            services = readServicesFromCSV(fileName);
        }
//        else if (fileName.endsWith(".json")) {
//            services = readServicesFromJSON(fileName);
//        } else if (fileName.endsWith(".xml")) {
//            services = readServicesFromXML(fileName);
//        } else if (fileName.endsWith(".ini")) {
//            services = readServicesFromINI(fileName);
//        } else if (fileName.endsWith(".xlsx") || fileName.endsWith(".xls")) {
//            services = readServicesFromExcel(fileName);
//        } else {
//            System.out.println("Unsupported file format");
//            return;
//        }

        for (ServiceInfo service : services) {
            Runnable task = createMonitoringTask(service);
            long interval = service.getMonitoringInterval(); // Get interval from the ServiceInfo object
            TimeUnit unit = service.getTimeUnit(); // Get time unit from the ServiceInfo object
            executor.scheduleAtFixedRate(task, 0, interval, unit);
        }

        // Add a shutdown hook for proper executor shutdown
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("Shutting down executor");
            executor.shutdown();
            try {
                if (!executor.awaitTermination(60, TimeUnit.SECONDS)) {
                    executor.shutdownNow();
                }
            } catch (InterruptedException e) {
                executor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }));
    }

    private static List<ServiceInfo> readServicesFromCSV(String fileName) {
        List<ServiceInfo> services = new ArrayList<>();
        try (Reader reader = new FileReader(fileName);
             CSVParser csvParser = new CSVParser(reader, CSVFormat.DEFAULT.withFirstRecordAsHeader())) {
            for (CSVRecord record : csvParser) {
             ServiceInfo service = new ServiceInfo(
                    record.get("ID"),
                    record.get("Service Name"),
                    record.get("Service Host"),
                    record.get("Protocol"), // Assuming "Protocol" is the column name in your CSV
                    Integer.parseInt(record.get("Service Port")),
                    record.get("Service Resource URI"),
                    record.get("Service Method"),
                    record.get("Expected Telnet Response"),
                    record.get("Expected Request Response"),
                    Long.parseLong(record.get("Monitoring Intervals")),
                    TimeUnit.valueOf(record.get("Monitoring Intervals Time Unit").toUpperCase())
            );
            services.add(service);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return services;
    }

    private static Runnable createMonitoringTask(ServiceInfo service) {
        return () -> {
            LocalDateTime now = LocalDateTime.now();

            // Format for the date and time (timestamp)
            DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            String formattedDateTime = now.format(dateTimeFormatter);

            // Get the day of the week (like "Tue" for Tuesday)
            String dayOfWeek = now.getDayOfWeek().getDisplayName(TextStyle.SHORT, Locale.ENGLISH);
            if(isServerUp(service.getServiceHost(), service.getServicePort())){
            try {



                    URL url = new URL(service.getServiceProtocol()+"://" + service.getServiceHost()+ service.getServiceResourceURI());
                    HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                    connection.setRequestMethod(service.getServiceMethod());
                    connection.setConnectTimeout(5000);
                    connection.setReadTimeout(5000);

                    int responseCode = connection.getResponseCode();
                    if (responseCode >= 200 && responseCode < 300) {
                        System.out.println(dayOfWeek+" "+formattedDateTime+" "+service.getServiceName() + " both server and application are online");
                    } else {
                        System.out.println(dayOfWeek+" "+formattedDateTime+" "+service.getServiceName() + " server is up but application is down - Response code: " + responseCode);
                    }


            } catch (Exception e) {
                System.out.println(dayOfWeek+" "+formattedDateTime+" "+service.getServiceName() + " server is up but application is down - Error: " + e.getMessage());
            }
            }else{
                System.out.println(dayOfWeek+" "+formattedDateTime+" "+service.getServiceName() + " server is down");
            }
        };
    }
    public static boolean isServerUp(String host, int port) {
        try (Socket socket = new Socket(host, port)) {
            // If the code reaches this point without an exception, the connection was successful
            return true;
        } catch (IOException e) {
            // An IOException was thrown: the server is down or not reachable on that port
            return false;
        }
    }
    // ServiceInfo class definition...
    static class ServiceInfo {
        private String id;
        private String serviceName;
        private String serviceHost;
        private int servicePort;

        private String serviceProtocol;
        private String serviceResourceURI;
        private String serviceMethod;
        private String expectedTelnetResponse;
        private String expectedRequestResponse;
        private long monitoringInterval;
        private TimeUnit timeUnit;

        public ServiceInfo(String id, String serviceName, String serviceHost, String serviceProtocol, int servicePort, String serviceResourceURI,
                           String serviceMethod, String expectedTelnetResponse, String expectedRequestResponse,
                           long monitoringInterval, TimeUnit timeUnit) {
            this.id = id;
            this.serviceName = serviceName;
            this.serviceHost = serviceHost;
            this.servicePort = servicePort;
            this.serviceResourceURI = serviceResourceURI;
            this.serviceMethod = serviceMethod;
            this.expectedTelnetResponse = expectedTelnetResponse;
            this.expectedRequestResponse = expectedRequestResponse;
            this.monitoringInterval = monitoringInterval;
            this.timeUnit = timeUnit;
            this.serviceProtocol = serviceProtocol;
        }

        // Getters...
        public String getId() { return id; }
        public String getServiceName() { return serviceName; }
        public String getServiceHost() { return serviceHost; }

        public String getServiceProtocol(){return serviceProtocol;}
        public int getServicePort() { return servicePort; }
        public String getServiceResourceURI() { return serviceResourceURI; }
        public String getServiceMethod() { return serviceMethod; }
        public String getExpectedTelnetResponse() { return expectedTelnetResponse; }
        public String getExpectedRequestResponse() { return expectedRequestResponse; }
        public long getMonitoringInterval() { return monitoringInterval; }
        public TimeUnit getTimeUnit() { return timeUnit; }
    }
    // Getter methods

}
