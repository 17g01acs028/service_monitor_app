package org.example;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.ZoneId;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.net.Socket;
import org.ini4j.Ini;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

public class Main {
    private static final int NUMBER_OF_THREADS = 10;
    private static boolean isMonitoringActive = false;
    private  static final ScheduledExecutorService executor = Executors.newScheduledThreadPool(NUMBER_OF_THREADS);
    private static List<ServiceInfo> services;

    private static  final int lock =0;
    private static final String activeServiceName = null;
   private static final String CSV_FILE_PATH = "services.csv";

    public static void main(String[] args) {
        try {
            String fileName = "services.xml";
            services = readServicesFromXML(fileName);
            System.out.println(services);

        }catch(Exception e){

        }

        startCommandListener();

    }

    private static void taskLog(){
        if (isMonitoringActive) {
            for (ServiceInfo service : services) {
                Runnable task = createMonitoringTask(service);
                long interval = service.getMonitoringInterval(); // Get interval from the ServiceInfo object
                TimeUnit unit = service.getTimeUnit(); // Get time unit from the ServiceInfo object
                executor.scheduleAtFixedRate(task, 0, interval, unit);
            }
        }else{
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
    }

    /*This Method listens for commands and runs them Start Here*/
    private static void startCommandListener() {
        new Thread(() -> {
            BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
            String line;
            try {
                System.out.println("Enter Command: ");
                while (true) {
                    if(isMonitoringActive){
                        System.out.print("sky-monitor active $:- ");
                        taskLog();
                    }else{
                        System.out.print("$:- ");
                    }

                    line = reader.readLine();
                    processCommand(line);
                }
            } catch (IOException e) {
                System.out.println(e.getMessage());
            }
        }).start();
    }


    /*This Method check and processes commands*/
    private static void processCommand(String command) {
        if ("sky-monitor start".equalsIgnoreCase(command)) {
            isMonitoringActive = true;
            System.out.println("Monitoring started.");
        } else if ("sky-monitor stop".equalsIgnoreCase(command)) {
            isMonitoringActive = false;
            System.out.println("Monitoring stopped.");
        } else if (command.startsWith("sky-monitor application status")) {
            String serviceId = extractServiceId(command);
            checkApplicationStatus(serviceId);
        } else if (command.startsWith("sky-monitor server status")) {
            String serviceId = extractServiceId(command);
            checkServerStatus(serviceId);
        } else if ("sky-monitor service list".equalsIgnoreCase(command)) {
            listAllServices();
        }else{
            System.out.println("Please Enter a valid Command");
        }
    }


    private static void listAllServices() {
        if (!isMonitoringActive) {
            System.out.println("Monitoring is not active.");
            return;
        }
        String leftAlignFormat = "| %-4s | %-35s | %-10s | %-20s | %-12s | %-20s |%n";

        System.out.format("+------+-------------------------------------+------------+-------------------------+--------------+-------------------------+%n");
        System.out.format("| ID   | Name                                | App Status | App_status_time         | Server Status|Server_status_time       |%n");
        System.out.format("+------+-------------------------------------+------------+-------------------------+--------------+-------------------------+%n");

        for (ServiceInfo service : services) {
            checklist(service,leftAlignFormat);
            System.out.format("+------+-------------------------------------+------------+-------------------------+--------------+-------------------------+%n");

        }

    }


    /* Application check process starts Here*/
    private static void checkApplicationStatus(String command) {
        if (!isMonitoringActive) {
            System.out.println("Monitoring is not active.");
            return;
        }

        ServiceInfo service = findServiceById(command);
        if (service != null) {
            checkApplication(service);
        }

    }
    private static void checkApplication(ServiceInfo service){
        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        String formattedDateTime = now.format(dateTimeFormatter);
        String dayOfWeek = now.getDayOfWeek().getDisplayName(TextStyle.SHORT, Locale.ENGLISH);

        String host  = service.getServiceHost();
        String path = service.getServiceResourceURI();
        String protocol = service.getServiceProtocol();
        String method = service.getServiceMethod();

        Object[] app_result = isApplicationUp(protocol,host,path,method);
        boolean isAppUp = (Boolean) app_result[0];

        System.out.println(dayOfWeek + " " + formattedDateTime + " " + service.getServiceName() +" "+(isAppUp ? "Up" : "Down")+", Response: "+app_result[1] );


    }

    private static void checkServer(ServiceInfo service){
        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        String formattedDateTime = now.format(dateTimeFormatter);
        String dayOfWeek = now.getDayOfWeek().getDisplayName(TextStyle.SHORT, Locale.ENGLISH);

        String host  = service.getServiceHost();
        int port = service.getServicePort();

        Object[] result = isServerUp(host, port, service.getExpectedTelnetResponse());
        boolean isServerUp = (Boolean) result[0];
        System.out.println(dayOfWeek + " " + formattedDateTime + " " +"Server status for " + service.getServiceName() + ": " + (isServerUp ? "Up" : "Down")+", Response: "+result[1]);

    }
private static void checklist(ServiceInfo service,String leftAlignFormat){

    LocalDateTime now = LocalDateTime.now();
    DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    String formattedDateTime = now.format(dateTimeFormatter);
    String dayOfWeek = now.getDayOfWeek().getDisplayName(TextStyle.SHORT, Locale.ENGLISH);

    String host  = service.getServiceHost();
    int port = service.getServicePort();
    String path = service.getServiceResourceURI();
    String protocol = service.getServiceProtocol();
    String method = service.getServiceMethod();

    Object[] server_result = isServerUp(host, port, service.getExpectedTelnetResponse());
    boolean isServerUp = (Boolean) server_result[0];

    Object[] app_result = isApplicationUp(protocol,host,path,method);
    boolean isAppUp = (Boolean) app_result[0];

    System.out.format(leftAlignFormat,service.getId(),service.getServiceName(),(isServerUp ? "Up" : "Down") ,dayOfWeek+ " " + formattedDateTime,(isAppUp ? "Up" : "Down") ,dayOfWeek + " " + formattedDateTime );

}
    private static void checkServerStatus(String command) {
        if (!isMonitoringActive) {
            System.out.println("Monitoring is not active.");
            return;
        }
        ServiceInfo service = findServiceById(command);
        if (service != null) {
            checkServer(service);
        } else {
            System.out.println("Service with ID " + command + " not found.");
        }
    }


    private static String extractServiceId(String command) {
        String[] parts = command.split(" ");
        return parts.length > 3 ? parts[3] : "";
    }

    private static ServiceInfo findServiceById(String serviceId) {
        for (ServiceInfo service : services) {
            if (service.getId().equals(serviceId)) {
                return service;
            }
        }
        return null;
    }

//Check if server is Up or down and send Response
    public static Object[] isServerUp(String host, int port, String message) {
        try (Socket pingSocket = new Socket(host, port)) {
            return new Object[]{true, message};
        } catch (IOException e) {
            return new Object[]{false, "Server is not reachable on host: " + host + " and port: " + port};
        }
    }

    //Check if application is up or down and send response
    private static Object[] isApplicationUp(String protocol, String host, String path , String method) {

        try {

            URL url = new URL(protocol + "://" + host + path);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod(method);
            connection.setConnectTimeout(1000);
            connection.setReadTimeout(1000);

            int responseCode = connection.getResponseCode();
            return new Object[]{true, responseCode};

        } catch (Exception e) {
            return new Object[]{false, e.getMessage()};
        }
    }



    private static void shutdownExecutor() {
        executor.shutdown();
        try {
            if (!executor.awaitTermination(60, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
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
                        record.get("Protocol"),
                        Integer.parseInt(record.get("Service Port")),
                        record.get("Service Resource URI"),
                        record.get("Service Method"),
                        record.get("Expected Telnet Response"),
                        record.get("Expected Request Response"),
                        Long.parseLong(record.get("Monitoring Intervals")),
                        TimeUnit.valueOf(record.get("Monitoring Intervals Time Unit").toUpperCase()),
                        record.get("Enable File Logging"),
                        record.get("File Logging Interval"),
                        record.get("Last Log Time")
                );
                services.add(service);
            }
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
        return services;
    }

    private static List<ServiceInfo> readServicesFromXML(String fileName) throws Exception {
        List<ServiceInfo> services = new ArrayList<>();
        JAXBContext jaxbContext = JAXBContext.newInstance(ServiceInfoListWrapper.class);
        Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
        ServiceInfoListWrapper service = (ServiceInfoListWrapper) unmarshaller.unmarshal(new File(fileName));

        //ServiceInfoListWrapper wrapper = (ServiceInfoListWrapper) jaxbUnmarshaller.unmarshal(new File(fileName));
        return service.getServices();
    }

    // You'll need a wrapper class for JAXB to work with lists
    @XmlRootElement(name = "services")
    public class ServiceInfoListWrapper {
        private List<ServiceInfo> services;

        @XmlElement(name = "service")
        public List<ServiceInfo> getServices() {
            return services;
        }

        public void setServices(List<ServiceInfo> services) {
            this.services = services;
        }
    }

    private static List<ServiceInfo> readServicesFromYAML(String fileName) throws IOException {
        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        return mapper.readValue(new File(fileName), new TypeReference<List<ServiceInfo>>() {});
    }


    private static List<ServiceInfo> readServicesFromJSON(String fileName)  {
try{
        JSONParser parser = new JSONParser();
        JSONArray jsonArray = (JSONArray) parser.parse(new FileReader(fileName));
        List<ServiceInfo> services = new ArrayList<>();

        for (Object o : jsonArray) {
            JSONObject jsonObject = (JSONObject) o;
            ServiceInfo service = new ServiceInfo(
                     jsonObject.get("ID").toString(),
                     jsonObject.get("ServiceName").toString(),
                     jsonObject.get("ServiceHost").toString(),
                     jsonObject.get("Protocol").toString(),
                     Integer.parseInt(jsonObject.get("ServicePort").toString()),
                     jsonObject.get("ServiceResourceURI").toString(),
                     jsonObject.get("ServiceMethod").toString(),
                     jsonObject.get("ExpectedTelnetResponse").toString(),
                     jsonObject.get("ExpectedRequestResponse").toString(),
                     Long.parseLong(jsonObject.get("MonitoringIntervals").toString()),
                     TimeUnit.valueOf(((String) jsonObject.get("MonitoringIntervalsTimeUnit")).toUpperCase()),
                     jsonObject.get("EnableFileLogging").toString(),
                     jsonObject.get("FileLoggingInterval").toString(),
                     jsonObject.get("LastLogTime").toString()
                    // ... other fields
            );
            services.add(service);
        }
    return services;
}catch(Exception e){
   e.printStackTrace();
}

        return services;
    }
    private static List<ServiceInfo> readServicesFromINI(String fileName) throws IOException {
        Ini ini = new Ini(new File(fileName));
        List<ServiceInfo> services = new ArrayList<>();

        for (String sectionName : ini.keySet()) {
            Ini.Section section = ini.get(sectionName);
            ServiceInfo service = new ServiceInfo(
                    section.get("ID"),
                    section.get("ServiceName"),
                    section.get("ServiceHost"),
                    section.get("Protocol"),
                    Integer.parseInt(section.get("ServicePort")),
                    section.get("ServiceResourceURI"),
                    section.get("ServiceMethod"),
                    section.get("ExpectedTelnetResponse"),
                    section.get("ExpectedRequestResponse"),
                    Long.parseLong(section.get("MonitoringIntervals")),
                    TimeUnit.valueOf(section.get("MonitoringIntervalsTimeUnit").toUpperCase()),
                    section.get("EnableFileLogging"),
                    section.get("FileLoggingInterval"),
                    section.get("LastLogTime")
                    // ... other fields
            );
            services.add(service);
        }
        return services;
    }

    private static Runnable createMonitoringTask(ServiceInfo service) {
        return () -> {
            LocalDateTime now = LocalDateTime.now();
            DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            String formattedDateTime = now.format(dateTimeFormatter);
            String dayOfWeek = now.getDayOfWeek().getDisplayName(TextStyle.SHORT, Locale.ENGLISH);

            String host  = service.getServiceHost();
            int port = service.getServicePort();
            String path = service.getServiceResourceURI();
            String protocol = service.getServiceProtocol();
            String method = service.getServiceMethod();

            Object[] server_result = isServerUp(host, port, service.getExpectedTelnetResponse());
            boolean isServerUp = (Boolean) server_result[0];

            Object[] app_result = isApplicationUp(protocol,host,path,method);
            boolean isAppUp = (Boolean) app_result[0];

            String dirPath = "logs/service"+service.getId();
            File directory = new File(dirPath);

if(service.getEnableFileLogging().equals("yes")){
    if (!directory.exists()) {
        boolean isCreated = directory.mkdirs();

        if (!isCreated) {
         System.out.println("Failed to create directory.");
        }
    }

    if(directory.exists()){
        LocalDateTime nextLogTime = getNextLogTime(service, service.getLastLogTime());
        LocalDateTime currentTime = LocalDateTime.now();

        if (currentTime.isAfter(nextLogTime)) {
            try{
                SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmmss");
                String timestamp = sdf.format(new Date());
                updateCsvLastLogTime(service.getId(), timestamp);

                String logServerFileName = generateLogFileName(dirPath,"server");
                writeLogEntry(logServerFileName, dayOfWeek + " " + formattedDateTime + " " +"Server status for " + service.getServiceName() + ": " + (isServerUp ? "Up" : "Down")+", Response: "+server_result[1]);
                String logApplicationFileName = generateLogFileName(dirPath,"application");
                writeLogEntry(logApplicationFileName, dayOfWeek + " " + formattedDateTime + " " +"Application status for " + service.getServiceName() + ": " + (isAppUp ? "Up" : "Down")+", Response: "+app_result[1]);

            }catch(Exception e){
                 System.out.println(e.getMessage());
            }

        } else {
            String logServerFileName=dirPath+ File.separator + service.getLastLogTime()+"_server.log";
            String logApplicationFileName=dirPath+ File.separator + service.getLastLogTime()+"_application.log";
            writeLogEntry(logServerFileName, dayOfWeek + " " + formattedDateTime + " " +"Server status for " + service.getServiceName() + ": " + (isServerUp ? "Up" : "Down")+", Response: "+server_result[1]);
            writeLogEntry(logApplicationFileName, dayOfWeek + " " + formattedDateTime + " " +"Application status for " + service.getServiceName() + ": " + (isAppUp ? "Up" : "Down")+", Response: "+app_result[1]);

        }

    }
}


        };
    }



        private static List<CSVRecord> readCsvFile(String filePath) throws IOException {
            try (Reader reader = Files.newBufferedReader(Paths.get(filePath))) {
                CSVParser parser = CSVFormat.DEFAULT.withFirstRecordAsHeader().parse(reader);
                return parser.getRecords();
            }
        }

    private static void writeCsvFile(List<String[]> updatedRecords, String filePath) throws IOException {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath));
             CSVPrinter csvPrinter = new CSVPrinter(writer, CSVFormat.DEFAULT.withHeader(updatedRecords.get(0)))) {

            // Skip the header for writing data
            for (int i = 1; i < updatedRecords.size(); i++) {
                csvPrinter.printRecord((Object[]) updatedRecords.get(i));
            }
        }
    }
    private static void updateCsvLastLogTime(String serviceId, String newLogTime) throws IOException {
        List<CSVRecord> records = readCsvFile(CSV_FILE_PATH);
        List<String[]> updatedRecords = new ArrayList<>();

        // Add the header to updatedRecords
        String[] header = records.get(0).getParser().getHeaderMap().keySet().toArray(new String[0]);
        updatedRecords.add(header);

        for (CSVRecord record : records) {
            String[] recordData = new String[record.size()];
            for (int i = 0; i < record.size(); i++) {
                recordData[i] = record.get(i);
            }

            if (record.get("ID").equals(serviceId)) {
                // Assuming the 'Last Log Time' column is named as such
                int lastLogTimeIndex = record.getParser().getHeaderMap().get("Last Log Time");
                recordData[lastLogTimeIndex] = newLogTime;
            }

            updatedRecords.add(recordData);
        }

        writeCsvFile(updatedRecords, CSV_FILE_PATH);
    }

    private static LocalDateTime getNextLogTime(ServiceInfo service, String lastLogTimeStr) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmmss");
        LocalDateTime lastLogTime;
        String timestamp = sdf.format(new Date());

        if (lastLogTimeStr.isEmpty()) {
            try{
            updateCsvLastLogTime(service.getId(), timestamp);}
            catch(Exception e){
                 System.out.println(e.getMessage());
            }
        }
        try {
            // Parse the timestamp string to Date
            Date lastLogDate = sdf.parse(lastLogTimeStr);

            // Convert Date to LocalDateTime
            lastLogTime = lastLogDate.toInstant()
                    .atZone(ZoneId.systemDefault())
                    .toLocalDateTime();
        } catch (ParseException e) {
            //System.out.println("Error parsing the timestamp string: " + e.getMessage());
            lastLogTime = LocalDateTime.now();
        }

        switch (service.getFileLoggingInterval()) {
            case "Hourly":
                return lastLogTime.plusHours(1);
            case "Daily":
                return lastLogTime.plusDays(1);
            case "Weekly":
                return lastLogTime.plusWeeks(1);
            default:
                throw new IllegalArgumentException("Invalid logging interval");
        }
    }
    private static String generateLogFileName(String dirPath, String name) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmmss");
        String timestamp = sdf.format(new Date());
        return dirPath + File.separator + timestamp + "_"+name+".log";
    }

    private static void writeLogEntry(String logFileName, String logEntry) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(logFileName, true))) {
            writer.write(logEntry);
            writer.newLine();
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
    }

}
