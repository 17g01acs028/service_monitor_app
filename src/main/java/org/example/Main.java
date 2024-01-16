package org.example;

import java.io.*;
import java.net.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.ZoneId;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;

import javax.net.ssl.*;
import javax.xml.parsers.DocumentBuilderFactory;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.io.*;
import java.net.*;

import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.nodes.Tag;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import com.google.gson.*;
import com.google.gson.reflect.TypeToken;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.ZoneId;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;

import javax.net.ssl.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;

import org.ini4j.Ini;
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
    public static  String fileName;
    private static String lasttimeLog="";

    public static void main(String[] args) throws IOException {
        String configDirPath = "config";
        File configDir = new File(configDirPath);
        File[] files = configDir.listFiles();

        if (files == null || files.length == 0) {
            System.out.println("We don't have a config file.");
            return;
        }

        boolean configFileProcessed = false;

        try {
            for (File file : files) {
                 String filename = file.getName();
                 fileName = file.getAbsolutePath();
                 if (filename.endsWith(".csv")) {
                    services = readServicesFromCSV(fileName);
                    configFileProcessed = true;
                 }else if (filename.endsWith(".xml")) {
                    services = readServicesFromXML(fileName);
                    configFileProcessed = true;
                 }else if (filename.endsWith(".ini")) {
                     services = readServicesFromINI(fileName);
                     configFileProcessed = true;
                 }else if (filename.endsWith(".yaml")) {
                     services = readServicesFromYAML(fileName);
                     configFileProcessed = true;
                 }else if (filename.endsWith(".json")) {
                     services = readServicesFromJSON(fileName);
                     configFileProcessed = true;
                 }


                if (configFileProcessed) {
                    taskArchive();
                    System.out.println(services);
                    break;
                }
            }

            if (!configFileProcessed) {
                System.out.println("No valid config file found.");
                return;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        startCommandListener();
    }

    private static void processServices(List<ServiceInfo> services) throws Exception {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmmss");
        String timestamp = sdf.format(new Date());

        ExecutorService executor = Executors.newSingleThreadExecutor();
        for (ServiceInfo service : services) {
            if ("yes".equalsIgnoreCase(service.getEnableFileArchiving())) {
                String logDirectoryPath = "logs/"+service.getServiceName() ;
                File logDir = new File(logDirectoryPath);

                if("yes".equalsIgnoreCase(service.getEnableFileArchiving())){
                    if (service.getLastArchiveTime().isEmpty()) {
                        if(fileName.endsWith(".xml")){
                            updateXmlLastLogTime(service.getId(), timestamp,fileName, "archive");
                        }else if(fileName.endsWith(".csv")){
                            updateCsvLastTime(service.getId(), timestamp,fileName, "archive");
                        }else  if(fileName.endsWith(".yaml")){
                            updateYamlLastLogTime(service.getId(), timestamp,fileName, "archive");
                        }else  if(fileName.endsWith(".ini")){
                            updateIniLastLogTime(service.getId(), timestamp,fileName, "archive");
                        }else  if(fileName.endsWith(".json")){
                            updateJsonLastLogTime(service.getId(), timestamp,fileName, "archive");
                        }

                        service.setLastArchiveTime(timestamp);
                    }

                    LocalDateTime currentTime = LocalDateTime.now();

                    LocalDateTime nextArchiveTime = getNextArchiveTime(service, service.getLastArchiveTime());
                    if (currentTime.isAfter(nextArchiveTime)) {
                        if(fileName.endsWith(".xml")){
                            updateXmlLastLogTime(service.getId(), timestamp,fileName, "archive");
                        }else if(fileName.endsWith(".csv")){
                            updateCsvLastTime(service.getId(), timestamp,fileName, "archive");
                        }else  if(fileName.endsWith(".yaml")){
                            updateYamlLastLogTime(service.getId(), timestamp,fileName, "archive");
                        }else  if(fileName.endsWith(".ini")){
                            updateIniLastLogTime(service.getId(), timestamp,fileName, "archive");
                        }else  if(fileName.endsWith(".json")){
                            updateJsonLastLogTime(service.getId(), timestamp,fileName, "archive");
                        }
                        service.setLastArchiveTime(timestamp);
                    }
                }

                if (logDir.exists() && logDir.isDirectory()) {
                    LogArchiver archiver = new LogArchiver(logDirectoryPath, fileName, service.getFileArchivingInterval().toLowerCase(),service);
                    executor.submit(archiver);
                }
            }
        }
    }

    private static void taskLog() throws Exception {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmmss");
        String timestamp = sdf.format(new Date());
        if (isMonitoringActive) {
            for (ServiceInfo service : services) {
                String filename = fileName;
                if("yes".equalsIgnoreCase(service.getEnableFileArchiving())){
                    if (service.getLastArchiveTime().isEmpty()) {
                        if(fileName.endsWith(".xml")){
                            updateXmlLastLogTime(service.getId(), timestamp,fileName, "archive");
                        }else if(fileName.endsWith(".csv")){
                            updateCsvLastTime(service.getId(), timestamp,fileName, "archive");
                        }else  if(fileName.endsWith(".yaml")){
                            updateYamlLastLogTime(service.getId(), timestamp,fileName, "archive");
                        }else  if(fileName.endsWith(".ini")){
                            updateIniLastLogTime(service.getId(), timestamp,fileName, "archive");
                        }else  if(fileName.endsWith(".json")){
                            updateJsonLastLogTime(service.getId(), timestamp,fileName, "archive");
                        }
                        service.setLastArchiveTime(timestamp);
                    }

                    LocalDateTime nextLogTime = getNextLogTime(service, service.getLastLogTime());
                    LocalDateTime currentTime = LocalDateTime.now();

                    if (currentTime.isAfter(nextLogTime)) {
                        if(fileName.endsWith(".xml")){
                            updateXmlLastLogTime(service.getId(), timestamp,fileName, "log");
                        }else if(fileName.endsWith(".csv")){
                            updateCsvLastTime(service.getId(), timestamp,fileName, "log");
                        }else  if(fileName.endsWith(".yaml")){
                            updateYamlLastLogTime(service.getId(), timestamp,fileName, "log");
                        }else  if(fileName.endsWith(".ini")){
                            updateIniLastLogTime(service.getId(), timestamp,fileName, "log");
                        }else  if(fileName.endsWith(".json")){
                            updateJsonLastLogTime(service.getId(), timestamp,fileName, "log");
                        }
                        service.setLastLogTime(timestamp);
                    }

                    LocalDateTime nextArchiveTime = getNextArchiveTime(service, service.getLastArchiveTime());
                    if (currentTime.isAfter(nextArchiveTime)) {
                        if(fileName.endsWith(".xml")){
                            updateXmlLastLogTime(service.getId(), timestamp,fileName, "archive");
                        }else if(fileName.endsWith(".csv")){
                            updateCsvLastTime(service.getId(), timestamp,fileName, "archive");
                        }else  if(fileName.endsWith(".yaml")){
                            updateYamlLastLogTime(service.getId(), timestamp,fileName, "archive");
                        }else  if(fileName.endsWith(".ini")){
                            updateIniLastLogTime(service.getId(), timestamp,fileName, "archive");
                        }else  if(fileName.endsWith(".json")){
                            updateJsonLastLogTime(service.getId(), timestamp,fileName, "archive");
                        }
                        service.setLastArchiveTime(timestamp);
                    }
                }

                if("yes".equalsIgnoreCase(service.getEnableFileLogging())){
                    if (!doesFileExist(filename)) {
                        if(fileName.endsWith(".xml")){
                            updateXmlLastLogTime(service.getId(), timestamp,fileName, "log");
                        }else if(fileName.endsWith(".csv")){
                            updateCsvLastTime(service.getId(), timestamp,fileName, "log");
                        }else  if(fileName.endsWith(".yaml")){
                            updateYamlLastLogTime(service.getId(), timestamp,fileName, "log");
                        }else  if(fileName.endsWith(".ini")){
                            updateIniLastLogTime(service.getId(), timestamp,fileName, "log");
                        }else  if(fileName.endsWith(".json")){
                            updateJsonLastLogTime(service.getId(), timestamp,fileName, "log");
                        }
                        service.setLastLogTime(timestamp);
                    }

                    LocalDateTime nextLogTime = getNextLogTime(service, service.getLastLogTime());
                    LocalDateTime currentTime = LocalDateTime.now();

                    if (currentTime.isAfter(nextLogTime)) {
                        if(fileName.endsWith(".xml")){
                            updateXmlLastLogTime(service.getId(), timestamp,fileName, "log");
                        }else if(fileName.endsWith(".csv")){
                            updateCsvLastTime(service.getId(), timestamp,fileName, "log");
                        }else  if(fileName.endsWith(".yaml")){
                            updateYamlLastLogTime(service.getId(), timestamp,fileName, "log");
                        }else  if(fileName.endsWith(".ini")){
                            updateIniLastLogTime(service.getId(), timestamp,fileName, "log");
                        }else  if(fileName.endsWith(".json")){
                            updateJsonLastLogTime(service.getId(), timestamp,fileName, "log");
                        }
                        service.setLastLogTime(timestamp);
                    }

                }

                Runnable task = createMonitoringTask(service);
                long interval = service.getMonitoringInterval();
                TimeUnit unit = service.getTimeUnit();
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

    private static void taskArchive() throws Exception {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmmss");
        String timestamp = sdf.format(new Date());
            for (ServiceInfo service : services) {
                String filename = fileName;
                if("yes".equalsIgnoreCase(service.getEnableFileArchiving())){
                    if (service.getLastArchiveTime().isEmpty()) {
                        if(fileName.endsWith(".xml")){
                            updateXmlLastLogTime(service.getId(), timestamp,fileName, "archive");
                        }else if(fileName.endsWith(".csv")){
                            updateCsvLastTime(service.getId(), timestamp,fileName, "archive");
                        }else  if(fileName.endsWith(".yaml")){
                            updateYamlLastLogTime(service.getId(), timestamp,fileName, "archive");
                        }else  if(fileName.endsWith(".ini")){
                            updateIniLastLogTime(service.getId(), timestamp,fileName, "archive");
                        }else  if(fileName.endsWith(".json")){
                            updateJsonLastLogTime(service.getId(), timestamp,fileName, "archive");
                        }
                        service.setLastArchiveTime(timestamp);
                    }

                    LocalDateTime currentTime = LocalDateTime.now();
                    LocalDateTime nextArchiveTime = getNextArchiveTime(service, service.getLastArchiveTime());

                    if (currentTime.isAfter(nextArchiveTime)) {
                        if(fileName.endsWith(".xml")){
                            updateXmlLastLogTime(service.getId(), timestamp,fileName, "archive");
                        }else if(fileName.endsWith(".csv")){
                            updateCsvLastTime(service.getId(), timestamp,fileName, "archive");
                        }else  if(fileName.endsWith(".yaml")){
                            updateYamlLastLogTime(service.getId(), timestamp,fileName, "archive");
                        }else  if(fileName.endsWith(".ini")){
                            updateIniLastLogTime(service.getId(), timestamp,fileName, "archive");
                        }else  if(fileName.endsWith(".json")){
                            updateJsonLastLogTime(service.getId(), timestamp,fileName, "archive");
                        }
                        service.setLastArchiveTime(timestamp);
                    }
                }

                Runnable task = createArchivingTask(service);
                long interval = 5;
                TimeUnit unit = TimeUnit.SECONDS;
                executor.scheduleAtFixedRate(task, 0, interval, unit);
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
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }).start();
    }


    /*This Method check and processes commands*/
    private static void processCommand(String command) {
        if ("sky-monitor start".equalsIgnoreCase(command)) {
            isMonitoringActive = true;
            System.out.println("🚀 Monitoring started.");
        } else if ("sky-monitor stop".equalsIgnoreCase(command)) {
            isMonitoringActive = false;
            System.out.println("🛑 Monitoring stopped.");
        } else if (command.startsWith("sky-monitor application status")) {
            String serviceId = extractServiceId(command);
            checkApplicationStatus(serviceId);
        } else if (command.startsWith("sky-monitor server status")) {
            String serviceId = extractServiceId(command);
            checkServerStatus(serviceId);
        } else if ("sky-monitor service list".equalsIgnoreCase(command)) {
            listAllServices();
        } else if ("sky-monitor help".equalsIgnoreCase(command)) {
            showHelp();
        } else if ("exit".equalsIgnoreCase(command)) {
            System.out.println("🚪 Exiting...");
            System.exit(0);
        }
        else {
            System.out.println("❓ Please Enter a valid Command, use sky-monitor help to get list of commands.");
        }
    }

    private static void showHelp() {
        System.out.println("🆘 Sky-Monitor Command Help:");
        System.out.println("  🚀 sky-monitor start - Starts the monitoring process.");
        System.out.println("  🛑 sky-monitor stop - Stops the monitoring process.");
        System.out.println("  📊 sky-monitor application status <ID> - Checks the status of a specific application by its ID.");
        System.out.println("  🖥️ sky-monitor server status <ID> - Checks the status of a specific server by its ID.");
        System.out.println("  📋 sky-monitor service list - Lists all services currently being monitored.");
        System.out.println("  🆘 sky-monitor help - Displays help information with a list of commands.");
        System.out.println("  🚪 exit - Exits the program.");
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
        try {
            InetAddress addr = InetAddress.getByName(host);
            SocketAddress sockaddr = new InetSocketAddress(addr, port);
            Socket sock = new Socket();
            int timeoutMs = 2000;
            sock.connect(sockaddr, timeoutMs);
            sock.close();
            return new Object[]{true, message};
        } catch (IOException e) {
            return new Object[]{false, "Server is not reachable on host: " + host + " and port: " + port};
        }
    }

    //Check if application is up or down and send response
    private static Object[] isApplicationUp(String protocol, String host, String path , String method) {

        try {
            TrustManager[] trustAllCerts = new TrustManager[]{
                    new X509TrustManager() {
                        public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                            return null;
                        }
                        public void checkClientTrusted(java.security.cert.X509Certificate[] certs, String authType) {
                        }
                        public void checkServerTrusted(java.security.cert.X509Certificate[] certs, String authType) {
                        }
                    }
            };

            SSLContext sc = SSLContext.getInstance("SSL");
            sc.init(null, trustAllCerts, new java.security.SecureRandom());
            HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
            HostnameVerifier allHostsValid = (hostname, session) -> true;
            HttpsURLConnection.setDefaultHostnameVerifier(allHostsValid);

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
                        record.get("Enable Logs Archiving"),
                        record.get("Log Archiving Intervals"),
                        record.get("Last Log Time"),
                        record.get("Last Archive Time")
                );
                services.add(service);
            }
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
        return services;
    }

    private static List<ServiceInfo> readServicesFromXML(String fileName) {
        List<ServiceInfo> services = new ArrayList<>();
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();

        try {
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(new File(fileName));
            doc.getDocumentElement().normalize();

            NodeList nodeList = doc.getElementsByTagName("service");
            String time = "";

            for (int i = 0; i < nodeList.getLength(); i++) {
                Element element = (Element) nodeList.item(i);

                ServiceInfo service = new ServiceInfo(
                        element.getElementsByTagName("ID").item(0).getTextContent(),
                        element.getElementsByTagName("ServiceName").item(0).getTextContent(),
                        element.getElementsByTagName("ServiceHost").item(0).getTextContent(),
                        element.getElementsByTagName("Protocol").item(0).getTextContent(),
                        Integer.parseInt(element.getElementsByTagName("ServicePort").item(0).getTextContent()),
                        element.getElementsByTagName("ServiceResourceURI").item(0).getTextContent(),
                        element.getElementsByTagName("ServiceMethod").item(0).getTextContent(),
                        element.getElementsByTagName("ExpectedTelnetResponse").item(0).getTextContent(),
                        element.getElementsByTagName("ExpectedRequestResponse").item(0).getTextContent(),
                        Long.parseLong(element.getElementsByTagName("MonitoringIntervals").item(0).getTextContent()),
                        TimeUnit.valueOf(element.getElementsByTagName("MonitoringIntervalsTimeUnit").item(0).getTextContent().toUpperCase()),
                        element.getElementsByTagName("EnableFileLogging").item(0).getTextContent(),
                        element.getElementsByTagName("FileLoggingInterval").item(0).getTextContent(),
                        element.getElementsByTagName("EnableLogsArchiving").item(0).getTextContent(),
                        element.getElementsByTagName("LogArchivingIntervals").item(0).getTextContent(),
                        element.getElementsByTagName("LastLogTime").item(0).getTextContent(),
                        element.getElementsByTagName("LastArchiveTime").item(0).getTextContent()

                );
                services.add(service);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return services;
    }

    private static List<ServiceInfo> readServicesFromYAML(String fileName) {
        List<ServiceInfo> services = new ArrayList<>();
        Yaml yaml = new Yaml(new Constructor(Map.class));

        try (FileReader reader = new FileReader(fileName)) {
            Map<String, Object> yamlData = yaml.load(reader);
            List<Map<String, Object>> serviceList = (List<Map<String, Object>>) yamlData.get("services");
            String time = "";
            for (Map<String, Object> map : serviceList) {
                ServiceInfo service = new ServiceInfo(
                        map.get("ID").toString(),
                        map.get("ServiceName").toString(),
                        map.get("ServiceHost").toString(),
                        map.get("Protocol").toString(),
                        Integer.parseInt(map.get("ServicePort").toString()),
                        map.get("ServiceResourceURI").toString(),
                        map.get("ServiceMethod").toString(),
                        map.get("ExpectedTelnetResponse").toString(),
                        map.get("ExpectedRequestResponse").toString(),
                        Long.parseLong(map.get("MonitoringIntervals").toString()),
                        TimeUnit.valueOf(((String) map.get("MonitoringIntervalsTimeUnit")).toUpperCase()),
                        map.get("EnableFileLogging").toString(),
                        map.get("FileLoggingInterval").toString(),
                        map.get("EnableLogsArchiving").toString(),
                        map.get("LogArchivingIntervals").toString(),
                        map.get("LastLogTime").toString(),
                        map.get("LastArchiveTime").toString()
                );
                services.add(service);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return services;
    }

    private static List<ServiceInfo> readServicesFromJSON(String fileName)  {
        try{
            JSONParser parser = new JSONParser();
            JSONArray jsonArray = (JSONArray) parser.parse(new FileReader(fileName));
            List<ServiceInfo> services = new ArrayList<>();
            String time = "";
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
                        jsonObject.get("EnableLogsArchiving").toString(),
                        jsonObject.get("LogArchivingIntervals").toString(),
                        jsonObject.get("LastLogTime").toString(),
                        jsonObject.get("LastArchiveTime").toString()
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
        String time = "";

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
                    section.get("EnableLogsArchiving"),
                    section.get("LogArchivingIntervals"),
                    section.get("LastLogTime"),
                    section.get("LastArchiveTime")
            );
            services.add(service);
        }
        return services;
    }

    public static boolean doesFileExist(String filename) {
        File file = new File(filename);
        return file.exists();
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

            String dirPath = "logs/"+service.getServiceName();
            File directory = new File(dirPath);

            SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmmss");
            String timestamp = sdf.format(new Date());

            if(service.getEnableFileLogging().equals("yes")){
                if (!directory.exists()) {
                    boolean isCreated = directory.mkdirs();
                   // LogTimeMonitor.updateLastLogTime(dirPath + File.separator +"last_time_log.txt");
                   service.setLastLogTime(timestamp);
                    try {
                        if(fileName.endsWith(".xml")){
                            updateXmlLastLogTime(service.getId(), timestamp,fileName, "log");
                        }else if(fileName.endsWith(".csv")){
                            updateCsvLastTime(service.getId(), timestamp,fileName, "log");
                        }else  if(fileName.endsWith(".yaml")){
                            updateYamlLastLogTime(service.getId(), timestamp,fileName, "log");
                        }else  if(fileName.endsWith(".ini")){
                            updateIniLastLogTime(service.getId(), timestamp,fileName, "log");
                        }else  if(fileName.endsWith(".json")){
                            updateJsonLastLogTime(service.getId(), timestamp,fileName, "log");
                        }
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                    if (!isCreated) {
                        System.out.println("Failed to create directory.");
                    }
                }

                if(directory.exists()){
                    lasttimeLog = service.getLastLogTime();
                    if(lasttimeLog.isEmpty()){

                        try {
                            service.setLastLogTime(timestamp);
                            if(fileName.endsWith(".xml")){
                                updateXmlLastLogTime(service.getId(), timestamp,fileName, "log");
                            }else if(fileName.endsWith(".csv")){
                                updateCsvLastTime(service.getId(), timestamp,fileName, "log");
                            }else  if(fileName.endsWith(".yaml")){
                                updateYamlLastLogTime(service.getId(), timestamp,fileName, "log");
                            }else  if(fileName.endsWith(".ini")){
                                updateIniLastLogTime(service.getId(), timestamp,fileName, "log");
                            }else  if(fileName.endsWith(".json")){
                                updateJsonLastLogTime(service.getId(), timestamp,fileName, "log");
                            }
                            String logServerFileName = generateLogFileName(dirPath, "server");
                            writeLogEntry(logServerFileName, dayOfWeek + " " + formattedDateTime + " " + "Server status for " + service.getServiceName() + ": " + (isServerUp ? "Up" : "Down") + ", Response: " + server_result[1]);
                            String logApplicationFileName = generateLogFileName(dirPath, "application");
                            writeLogEntry(logApplicationFileName, dayOfWeek + " " + formattedDateTime + " " + "Application status for " + service.getServiceName() + ": " + (isAppUp ? "Up" : "Down") + ", Response: " + app_result[1]);
                        }catch(Exception e){

                        }
                    }else {

                        LocalDateTime nextLogTime = getNextLogTime(service, service.getLastLogTime());
                        LocalDateTime currentTime = LocalDateTime.now();

                        if (currentTime.isAfter(nextLogTime)) {
                            try {
                                service.setLastLogTime(timestamp);
                                if(fileName.endsWith(".xml")){
                                    updateXmlLastLogTime(service.getId(), timestamp,fileName, "log");
                                }else if(fileName.endsWith(".csv")){
                                    updateCsvLastTime(service.getId(), timestamp,fileName, "log");
                                }else  if(fileName.endsWith(".yaml")){
                                    updateYamlLastLogTime(service.getId(), timestamp,fileName, "log");
                                }else  if(fileName.endsWith(".ini")){
                                    updateIniLastLogTime(service.getId(), timestamp,fileName, "log");
                                }else  if(fileName.endsWith(".json")){
                                    updateJsonLastLogTime(service.getId(), timestamp,fileName, "log");
                                }
                                String logServerFileName = generateLogFileName(dirPath, "server");
                                writeLogEntry(logServerFileName, dayOfWeek + " " + formattedDateTime + " " + "Server status for " + service.getServiceName() + ": " + (isServerUp ? "Up" : "Down") + ", Response: " + server_result[1]);
                                String logApplicationFileName = generateLogFileName(dirPath, "application");
                                writeLogEntry(logApplicationFileName, dayOfWeek + " " + formattedDateTime + " " + "Application status for " + service.getServiceName() + ": " + (isAppUp ? "Up" : "Down") + ", Response: " + app_result[1]);

                            } catch (Exception e) {
                                System.out.println(e.getMessage());
                            }

                        } else {
                            String logServerFileName = dirPath + File.separator + service.getLastLogTime() + "_server.log";
                            String logApplicationFileName = dirPath + File.separator + service.getLastLogTime() + "_application.log";
                            writeLogEntry(logServerFileName, dayOfWeek + " " + formattedDateTime + " " + "Server status for " + service.getServiceName() + ": " + (isServerUp ? "Up" : "Down") + ", Response: " + server_result[1]);
                            writeLogEntry(logApplicationFileName, dayOfWeek + " " + formattedDateTime + " " + "Application status for " + service.getServiceName() + ": " + (isAppUp ? "Up" : "Down") + ", Response: " + app_result[1]);

                        }
                    }
                }
            }


        };
    }

    private static Runnable createArchivingTask(ServiceInfo service) {
        return () -> {
            String logDirectoryPath = "logs/"+service.getServiceName() ;
            File logDir = new File(logDirectoryPath);
     if("yes".equalsIgnoreCase(service.getEnableFileArchiving())){
         if (logDir.exists() && logDir.isDirectory()) {
             LogArchiver archiver = new LogArchiver(logDirectoryPath, fileName, service.getFileArchivingInterval().toLowerCase(),service);
             executor.submit(archiver);
         }
     }

       };
    }
    private static LocalDateTime getNextLogTime(ServiceInfo service, String lastLogTimeStr) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmmss");
        LocalDateTime lastLogTime;
        String timestamp = sdf.format(new Date());

        try {
            Date lastLogDate = sdf.parse(lastLogTimeStr);
            lastLogTime = lastLogDate.toInstant()
                    .atZone(ZoneId.systemDefault())
                    .toLocalDateTime();
        } catch (ParseException e) {
            //System.out.println("Error parsing the timestamp string: " + e.getMessage());
            lastLogTime = LocalDateTime.now();
        }

        switch (service.getFileLoggingInterval().toLowerCase()) {
            case "hourly":
                return lastLogTime.plusHours(1);
            case "daily":
                return lastLogTime.plusDays(1);
            case "weekly":
                return lastLogTime.plusWeeks(1);
            case "monthly":
                return lastLogTime.plusMonths(1);
            default:
                throw new IllegalArgumentException("Invalid logging interval");
        }
    }

    private static LocalDateTime getNextArchiveTime(ServiceInfo service, String lastArchiveTimeStr) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmmss");
        LocalDateTime lastLogTime;
        String timestamp = sdf.format(new Date());

        try {
            Date lastLogDate = sdf.parse(lastArchiveTimeStr);
            lastLogTime = lastLogDate.toInstant()
                    .atZone(ZoneId.systemDefault())
                    .toLocalDateTime();
        } catch (ParseException e) {
            //System.out.println("Error parsing the timestamp string: " + e.getMessage());
            lastLogTime = LocalDateTime.now();
        }

        switch (service.getFileArchivingInterval().toLowerCase()) {
            case "hourly":
                return lastLogTime.plusHours(1);
            case "daily":
                return lastLogTime.plusDays(1);
            case "weekly":
                return lastLogTime.plusWeeks(1);
            case "monthly":
                return lastLogTime.plusMonths(1);
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

    //Write to files

    //write to csv
    public static void updateCsvLastTime(String serviceId, String newLogTime,String CSV_FILE_PATH,String action_type ) throws IOException {
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
                int lastLogTimeIndex;
                if(action_type.equalsIgnoreCase("log")){
                    lastLogTimeIndex = record.getParser().getHeaderMap().get("Last Log Time");
                }else{
                    lastLogTimeIndex = record.getParser().getHeaderMap().get("Last Archive Time");
                }
                recordData[lastLogTimeIndex] = newLogTime;
            }

            updatedRecords.add(recordData);
        }

        writeCsvFile(updatedRecords, CSV_FILE_PATH);
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


    //Write to .Xml

    public static void updateXmlLastLogTime(String serviceId, String newLogTime, String xmlFilePath, String action_type) throws Exception {
        File xmlFile = new File(xmlFilePath);
        DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
        Document document = documentBuilder.parse(xmlFile);
        document.getDocumentElement().normalize();

        NodeList serviceList = document.getElementsByTagName("service");
        boolean serviceFound = false;

        for (int i = 0; i < serviceList.getLength(); i++) {
            Element service = (Element) serviceList.item(i);
            if("log".equalsIgnoreCase(action_type)){
                if (service.getElementsByTagName("ID").item(0).getTextContent().equals(serviceId)) {
                    service.getElementsByTagName("LastLogTime").item(0).setTextContent(newLogTime);
                    serviceFound = true;
                    break;
                }
            }else {
                if (service.getElementsByTagName("ID").item(0).getTextContent().equals(serviceId)) {
                    service.getElementsByTagName("LastArchiveTime").item(0).setTextContent(newLogTime);
                    serviceFound = true;
                    break;
                }
            }
        }

        if (!serviceFound) {
            throw new IllegalArgumentException("Service ID not found in XML file");
        }

        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        Transformer transformer = transformerFactory.newTransformer();
       // transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        DOMSource source = new DOMSource(document);
        StreamResult result = new StreamResult(xmlFile);
        transformer.transform(source, result);
    }

    //Write to .yaml
    public static void updateYamlLastLogTime(String serviceId, String newLogTime, String yamlFilePath, String action_type) throws IOException {
        Yaml yaml = new Yaml();
        Map<String, Object> yamlData;

        try (FileReader reader = new FileReader(yamlFilePath)) {
            yamlData = yaml.load(reader);
        }

        List<Map<String, Object>> services = (List<Map<String, Object>>) yamlData.get("services");
        boolean serviceFound = false;
        for (Map<String, Object> service : services) {
            if (service.get("ID").toString().equals(serviceId)) {
                if("log".equalsIgnoreCase(action_type)){
                    service.put("LastLogTime", newLogTime);
                }else{
                    service.put("LastArchiveTime", newLogTime);
                }
                serviceFound = true;
                break;

            }
        }

        if (!serviceFound) {
            throw new IllegalArgumentException("Service ID not found in yaml file");
        }

        DumperOptions options = new DumperOptions();
        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        yaml = new Yaml(options);

        try (FileWriter writer = new FileWriter(yamlFilePath)) {
            yaml.dump(yamlData, writer);
        }
    }

    //write to .ini
    private static void updateIniLastLogTime(String serviceId, String newLogTime, String iniFilePath, String action_type) throws IOException {
        Path path = Paths.get(iniFilePath);
        List<String> fileContent = new ArrayList<>(Files.readAllLines(path, StandardCharsets.UTF_8));

        boolean serviceFound = false;
        for (int i = 0; i < fileContent.size(); i++) {
            String line = fileContent.get(i);

            if (line.trim().startsWith("[Service") && line.contains(serviceId)) {
                serviceFound = true;
            }
                if("log".equalsIgnoreCase(action_type)){
                    if (serviceFound && line.startsWith("LastLogTime")) {
                        fileContent.set(i, "LastLogTime = " + newLogTime);
                        break;
                    }
                }else{
                    if (serviceFound && line.startsWith("LastArchiveTime")) {
                        fileContent.set(i, "LastArchiveTime = " + newLogTime);
                        break;
                    }
                }


        }

        if (!serviceFound) {
            throw new IllegalArgumentException("Service ID not found in ini file");
        }

        Files.write(path, fileContent, StandardCharsets.UTF_8);
    }

    //write to json file
    private static Gson createGsonWithCustomDeserializer() {
        return new GsonBuilder()
                .setPrettyPrinting()
                .registerTypeAdapter(ServiceInfo.class, new JsonDeserializer<ServiceInfo>() {

                    @Override
                    public ServiceInfo deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
                        JsonObject jsonObject = json.getAsJsonObject();

                        String ID = jsonObject.get("ID").getAsString();
                        String ServiceName = jsonObject.get("ServiceName").getAsString();
                        String ServiceHost = jsonObject.get("ServiceHost").getAsString();
                        int ServicePort = jsonObject.get("ServicePort").getAsInt();
                        String Protocol = jsonObject.get("Protocol").getAsString();
                        String ServiceResourceURI = jsonObject.get("ServiceResourceURI").getAsString();
                        String ServiceMethod = jsonObject.get("ServiceMethod").getAsString();
                        String ExpectedTelnetResponse = jsonObject.get("ExpectedTelnetResponse").getAsString();
                        String ExpectedRequestResponse = jsonObject.get("ExpectedRequestResponse").getAsString();
                        long MonitoringIntervals = jsonObject.get("MonitoringIntervals").getAsLong();
                        TimeUnit MonitoringIntervalsTimeUnit = TimeUnit.valueOf(jsonObject.get("MonitoringIntervalsTimeUnit").getAsString().toUpperCase());
                        String EnableFileLogging = jsonObject.get("EnableFileLogging").getAsString();
                        String FileLoggingInterval = jsonObject.get("FileLoggingInterval").getAsString();
                        String EnableLogsArchiving = jsonObject.get("EnableLogsArchiving").getAsString();
                        String LogArchivingIntervals = jsonObject.get("LogArchivingIntervals").getAsString();
                        String LastLogTime = jsonObject.get("LastLogTime").getAsString();
                        String LastArchiveTime = jsonObject.get("LastArchiveTime").getAsString();

                        return new ServiceInfo(ID, ServiceName, ServiceHost, Protocol, ServicePort, ServiceResourceURI,
                                ServiceMethod, ExpectedTelnetResponse, ExpectedRequestResponse,
                                MonitoringIntervals, MonitoringIntervalsTimeUnit, EnableFileLogging, FileLoggingInterval,EnableLogsArchiving, LogArchivingIntervals, LastLogTime,LastArchiveTime);
                    }
                })
                .create();
    }

    private static void updateJsonLastLogTime(String serviceId, String newLogTime, String jsonFilePath, String action_type ) throws IOException {
        Path path = Path.of(jsonFilePath);
        Gson gson = createGsonWithCustomDeserializer();

        // Read the JSON file
        String json = Files.readString(path);
        Type listType = new TypeToken<List<ServiceInfo>>(){}.getType();
        List<ServiceInfo> services = gson.fromJson(json, listType);

        // Update the LastLogTime for the specified service ID
        boolean serviceFound = false;
        for (ServiceInfo service : services) {
            if (service.getId().equals(serviceId)) {

                ServiceInfo updatedService;
                if("log".equalsIgnoreCase(action_type)) {
                    updatedService = new ServiceInfo(service.getId(), service.getServiceName(), service.getServiceHost(),
                            service.getServiceProtocol(), service.getServicePort(), service.getServiceResourceURI(), service.getServiceMethod(),
                            service.getExpectedTelnetResponse(), service.getExpectedRequestResponse(), service.getMonitoringInterval(),
                            service.getTimeUnit(), service.getEnableFileLogging(), service.getFileLoggingInterval(), service.getEnableFileArchiving(), service.getFileArchivingInterval(), newLogTime, service.getLastArchiveTime());
                  
                }else{
                    updatedService = new ServiceInfo(service.getId(), service.getServiceName(), service.getServiceHost(),
                            service.getServiceProtocol(), service.getServicePort(), service.getServiceResourceURI(), service.getServiceMethod(),
                            service.getExpectedTelnetResponse(), service.getExpectedRequestResponse(), service.getMonitoringInterval(),
                            service.getTimeUnit(), service.getEnableFileLogging(), service.getFileLoggingInterval(), service.getEnableFileArchiving(), service.getFileArchivingInterval(), service.getLastLogTime(), newLogTime);
                    
                }
                services.set(services.indexOf(service), updatedService);
                serviceFound = true;
                break;


            }
        }

        if (!serviceFound) {
            throw new IllegalArgumentException("Service ID not found in JSON file");
        }

        // Write the updated JSON back to the file
        String updatedJson = gson.toJson(services);
        Files.writeString(path, updatedJson, StandardOpenOption.TRUNCATE_EXISTING);
    }


}

