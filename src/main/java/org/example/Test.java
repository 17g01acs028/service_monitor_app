//package org.example;
//import java.io.*;
//import java.net.*;
//
//import org.yaml.snakeyaml.Yaml;
//import org.yaml.snakeyaml.DumperOptions;
//import org.yaml.snakeyaml.nodes.Tag;
//
//import javax.xml.transform.OutputKeys;
//import javax.xml.transform.Transformer;
//import javax.xml.transform.TransformerFactory;
//import javax.xml.transform.dom.DOMSource;
//import javax.xml.transform.stream.StreamResult;
//
//import javax.xml.parsers.DocumentBuilder;
//import javax.xml.parsers.DocumentBuilderFactory;
//import org.w3c.dom.Document;
//import org.w3c.dom.Element;
//import org.w3c.dom.NodeList;
//import com.google.gson.*;
//import com.google.gson.reflect.TypeToken;
//import java.io.IOException;
//import java.lang.reflect.Type;
//import java.nio.charset.StandardCharsets;
//import java.nio.file.Files;
//import java.nio.file.Path;
//import java.nio.file.Paths;
//import java.nio.file.StandardOpenOption;
//import java.text.ParseException;
//import java.text.SimpleDateFormat;
//import java.time.ZoneId;
//import java.util.*;
//import java.util.concurrent.ExecutorService;
//import java.util.concurrent.Executors;
//import java.util.concurrent.ScheduledExecutorService;
//import java.util.concurrent.TimeUnit;
//
//import org.apache.commons.csv.CSVFormat;
//import org.apache.commons.csv.CSVParser;
//import org.apache.commons.csv.CSVPrinter;
//import org.apache.commons.csv.CSVRecord;
//
//import javax.net.ssl.*;
//import java.time.LocalDateTime;
//import java.time.format.DateTimeFormatter;
//import java.time.format.TextStyle;
//
//import org.ini4j.Ini;
//import org.yaml.snakeyaml.Yaml;
//import org.yaml.snakeyaml.constructor.Constructor;
//import org.json.simple.JSONArray;
//import org.json.simple.JSONObject;
//import org.json.simple.parser.JSONParser;
//public class Test {
//
//    private static final int NUMBER_OF_THREADS = 10;
//    private static boolean isMonitoringActive = false;
//    private  static final ScheduledExecutorService executor = Executors.newScheduledThreadPool(NUMBER_OF_THREADS);
//    private static List<ServiceInfo> services;
//
//    private static  final int lock =0;
//    private static final String activeServiceName = null;
//    public static final String fileName = "services.csv";
//    private static String lasttimeLog="";
//
//    public static void main(String[] args) {
//        try {
//
//            if(fileName.endsWith(".xml")){
//                services = readServicesFromXML(fileName);
//            }else if(fileName.endsWith(".csv")){
//                services = readServicesFromCSV(fileName);
//            }else  if(fileName.endsWith(".yaml")){
//                services = readServicesFromYAML(fileName);
//            }else  if(fileName.endsWith(".ini")){
//                services = readServicesFromINI(fileName);
//            }else  if(fileName.endsWith(".json")){
//                services = readServicesFromJSON(fileName);
//            }
//            ExecutorService executor = Executors.newSingleThreadExecutor();
//            for (ServiceInfo service : services) {
//                if ("yes".equalsIgnoreCase(service.getEnableFileArchiving())) {
//                    String logDirectoryPath = "logs/service" + service.getId();
//                    File logDir = new File(logDirectoryPath);
//                    if (logDir.exists() && logDir.isDirectory()) {
//                        LogArchiver archiver = new LogArchiver(logDirectoryPath, service.getFileArchivingInterval().toLowerCase());
//                        executor.submit(archiver);
//                    }
//                }
//            }
//
//            System.out.println(services);
//
//        }catch(Exception e){
//
//        }
//
//        startCommandListener();
//
//    }
//
//    private static void taskLog(){
//        if (isMonitoringActive) {
//            for (ServiceInfo service : services) {
//                Runnable task = createMonitoringTask(service);
//                long interval = service.getMonitoringInterval(); // Get interval from the ServiceInfo object
//                TimeUnit unit = service.getTimeUnit(); // Get time unit from the ServiceInfo object
//                executor.scheduleAtFixedRate(task, 0, interval, unit);
//            }
//        }else{
//            // Add a shutdown hook for proper executor shutdown
//            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
//                System.out.println("Shutting down executor");
//                executor.shutdown();
//                try {
//                    if (!executor.awaitTermination(60, TimeUnit.SECONDS)) {
//                        executor.shutdownNow();
//                    }
//                } catch (InterruptedException e) {
//                    executor.shutdownNow();
//                    Thread.currentThread().interrupt();
//                }
//            }));
//        }
//    }
//
//    /*This Method listens for commands and runs them Start Here*/
//    private static void startCommandListener() {
//        new Thread(() -> {
//            BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
//            String line;
//            try {
//                System.out.println("Enter Command: ");
//                while (true) {
//                    if(isMonitoringActive){
//                        System.out.print("sky-monitor active $:- ");
//                        taskLog();
//                    }else{
//                        System.out.print("$:- ");
//                    }
//
//                    line = reader.readLine();
//                    processCommand(line);
//                }
//            } catch (IOException e) {
//                System.out.println(e.getMessage());
//            }
//        }).start();
//    }
//
//
//    /*This Method check and processes commands*/
//    private static void processCommand(String command) {
//        if ("sky-monitor start".equalsIgnoreCase(command)) {
//            isMonitoringActive = true;
//            System.out.println("🚀 Monitoring started.");
//        } else if ("sky-monitor stop".equalsIgnoreCase(command)) {
//            isMonitoringActive = false;
//            System.out.println("🛑 Monitoring stopped.");
//        } else if (command.startsWith("sky-monitor application status")) {
//            String serviceId = extractServiceId(command);
//            checkApplicationStatus(serviceId);
//        } else if (command.startsWith("sky-monitor server status")) {
//            String serviceId = extractServiceId(command);
//            checkServerStatus(serviceId);
//        } else if ("sky-monitor service list".equalsIgnoreCase(command)) {
//            listAllServices();
//        } else if ("sky-monitor help".equalsIgnoreCase(command)) {
//            showHelp();
//        } else if ("exit".equalsIgnoreCase(command)) {
//            System.out.println("🚪 Exiting...");
//            System.exit(0);
//        }
//        else {
//            System.out.println("❓ Please Enter a valid Command, use sky-monitor help to get list of commands.");
//        }
//    }
//
//    private static void showHelp() {
//        System.out.println("🆘 Sky-Monitor Command Help:");
//        System.out.println("  🚀 sky-monitor start - Starts the monitoring process.");
//        System.out.println("  🛑 sky-monitor stop - Stops the monitoring process.");
//        System.out.println("  📊 sky-monitor application status <ID> - Checks the status of a specific application by its ID.");
//        System.out.println("  🖥️ sky-monitor server status <ID> - Checks the status of a specific server by its ID.");
//        System.out.println("  📋 sky-monitor service list - Lists all services currently being monitored.");
//        System.out.println("  🆘 sky-monitor help - Displays help information with a list of commands.");
//        System.out.println("  🚪 exit - Exits the program.");
//    }
//
//    private static void listAllServices() {
//        if (!isMonitoringActive) {
//            System.out.println("Monitoring is not active.");
//            return;
//        }
//        String leftAlignFormat = "| %-4s | %-35s | %-10s | %-20s | %-12s | %-20s |%n";
//
//        System.out.format("+------+-------------------------------------+------------+-------------------------+--------------+-------------------------+%n");
//        System.out.format("| ID   | Name                                | App Status | App_status_time         | Server Status|Server_status_time       |%n");
//        System.out.format("+------+-------------------------------------+------------+-------------------------+--------------+-------------------------+%n");
//
//        for (ServiceInfo service : services) {
//            checklist(service,leftAlignFormat);
//            System.out.format("+------+-------------------------------------+------------+-------------------------+--------------+-------------------------+%n");
//
//        }
//
//    }
//
//
//    /* Application check process starts Here*/
//    private static void checkApplicationStatus(String command) {
//        if (!isMonitoringActive) {
//            System.out.println("Monitoring is not active.");
//            return;
//        }
//
//        ServiceInfo service = findServiceById(command);
//        if (service != null) {
//            checkApplication(service);
//        }
//
//    }
//    private static void checkApplication(ServiceInfo service){
//        LocalDateTime now = LocalDateTime.now();
//        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
//        String formattedDateTime = now.format(dateTimeFormatter);
//        String dayOfWeek = now.getDayOfWeek().getDisplayName(TextStyle.SHORT, Locale.ENGLISH);
//
//        String host  = service.getServiceHost();
//        String path = service.getServiceResourceURI();
//        String protocol = service.getServiceProtocol();
//        String method = service.getServiceMethod();
//
//        Object[] app_result = isApplicationUp(protocol,host,path,method);
//        boolean isAppUp = (Boolean) app_result[0];
//
//        System.out.println(dayOfWeek + " " + formattedDateTime + " " + service.getServiceName() +" "+(isAppUp ? "Up" : "Down")+", Response: "+app_result[1] );
//
//
//    }
//
//    private static void checkServer(ServiceInfo service){
//        LocalDateTime now = LocalDateTime.now();
//        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
//        String formattedDateTime = now.format(dateTimeFormatter);
//        String dayOfWeek = now.getDayOfWeek().getDisplayName(TextStyle.SHORT, Locale.ENGLISH);
//
//        String host  = service.getServiceHost();
//        int port = service.getServicePort();
//
//        Object[] result = isServerUp(host, port, service.getExpectedTelnetResponse());
//        boolean isServerUp = (Boolean) result[0];
//        System.out.println(dayOfWeek + " " + formattedDateTime + " " +"Server status for " + service.getServiceName() + ": " + (isServerUp ? "Up" : "Down")+", Response: "+result[1]);
//
//    }
//    private static void checklist(ServiceInfo service,String leftAlignFormat){
//
//        LocalDateTime now = LocalDateTime.now();
//        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
//        String formattedDateTime = now.format(dateTimeFormatter);
//        String dayOfWeek = now.getDayOfWeek().getDisplayName(TextStyle.SHORT, Locale.ENGLISH);
//
//        String host  = service.getServiceHost();
//        int port = service.getServicePort();
//        String path = service.getServiceResourceURI();
//        String protocol = service.getServiceProtocol();
//        String method = service.getServiceMethod();
//
//        Object[] server_result = isServerUp(host, port, service.getExpectedTelnetResponse());
//        boolean isServerUp = (Boolean) server_result[0];
//
//        Object[] app_result = isApplicationUp(protocol,host,path,method);
//        boolean isAppUp = (Boolean) app_result[0];
//
//        System.out.format(leftAlignFormat,service.getId(),service.getServiceName(),(isServerUp ? "Up" : "Down") ,dayOfWeek+ " " + formattedDateTime,(isAppUp ? "Up" : "Down") ,dayOfWeek + " " + formattedDateTime );
//
//    }
//    private static void checkServerStatus(String command) {
//        if (!isMonitoringActive) {
//            System.out.println("Monitoring is not active.");
//            return;
//        }
//        ServiceInfo service = findServiceById(command);
//        if (service != null) {
//            checkServer(service);
//        } else {
//            System.out.println("Service with ID " + command + " not found.");
//        }
//    }
//
//
//    private static String extractServiceId(String command) {
//        String[] parts = command.split(" ");
//        return parts.length > 3 ? parts[3] : "";
//    }
//
//    private static ServiceInfo findServiceById(String serviceId) {
//        for (ServiceInfo service : services) {
//            if (service.getId().equals(serviceId)) {
//                return service;
//            }
//        }
//        return null;
//    }
//
//    //Check if server is Up or down and send Response
//    public static Object[] isServerUp(String host, int port, String message) {
//        try {
//            InetAddress addr = InetAddress.getByName(host);
//            SocketAddress sockaddr = new InetSocketAddress(addr, port);
//            Socket sock = new Socket();
//            int timeoutMs = 2000;
//            sock.connect(sockaddr, timeoutMs);
//            sock.close();
//            return new Object[]{true, message};
//        } catch (IOException e) {
//            return new Object[]{false, "Server is not reachable on host: " + host + " and port: " + port};
//        }
//    }
//
//    //Check if application is up or down and send response
//    private static Object[] isApplicationUp(String protocol, String host, String path , String method) {
//
//        try {
//            TrustManager[] trustAllCerts = new TrustManager[]{
//                    new X509TrustManager() {
//                        public java.security.cert.X509Certificate[] getAcceptedIssuers() {
//                            return null;
//                        }
//                        public void checkClientTrusted(java.security.cert.X509Certificate[] certs, String authType) {
//                        }
//                        public void checkServerTrusted(java.security.cert.X509Certificate[] certs, String authType) {
//                        }
//                    }
//            };
//
//            SSLContext sc = SSLContext.getInstance("SSL");
//            sc.init(null, trustAllCerts, new java.security.SecureRandom());
//            HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
//            HostnameVerifier allHostsValid = (hostname, session) -> true;
//            HttpsURLConnection.setDefaultHostnameVerifier(allHostsValid);
//
//            URL url = new URL(protocol + "://" + host + path);
//            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
//            connection.setRequestMethod(method);
//            connection.setConnectTimeout(1000);
//            connection.setReadTimeout(1000);
//
//            int responseCode = connection.getResponseCode();
//            return new Object[]{true, responseCode};
//
//        } catch (Exception e) {
//            return new Object[]{false, e.getMessage()};
//        }
//    }
//
//
//
//    private static void shutdownExecutor() {
//        executor.shutdown();
//        try {
//            if (!executor.awaitTermination(60, TimeUnit.SECONDS)) {
//                executor.shutdownNow();
//            }
//        } catch (InterruptedException e) {
//            executor.shutdownNow();
//            Thread.currentThread().interrupt();
//        }
//    }
//
//    private static List<ServiceInfo> readServicesFromCSV(String fileName) {
//        List<ServiceInfo> services = new ArrayList<>();
//        try (Reader reader = new FileReader(fileName);
//             CSVParser csvParser = new CSVParser(reader, CSVFormat.DEFAULT.withFirstRecordAsHeader())) {
//            for (CSVRecord record : csvParser) {
//                ServiceInfo service = new ServiceInfo(
//                        record.get("ID"),
//                        record.get("Service Name"),
//                        record.get("Service Host"),
//                        record.get("Protocol"),
//                        Integer.parseInt(record.get("Service Port")),
//                        record.get("Service Resource URI"),
//                        record.get("Service Method"),
//                        record.get("Expected Telnet Response"),
//                        record.get("Expected Request Response"),
//                        Long.parseLong(record.get("Monitoring Intervals")),
//                        TimeUnit.valueOf(record.get("Monitoring Intervals Time Unit").toUpperCase()),
//                        record.get("Enable File Logging"),
//                        record.get("File Logging Interval"),
//                        record.get("Enable File Archiving"),
//                        record.get("File Archiving Interval"),
//                        record.get("Last Log Time")
//                );
//                services.add(service);
//            }
//        } catch (IOException e) {
//            System.out.println(e.getMessage());
//        }
//        return services;
//    }
//
//    private static List<ServiceInfo> readServicesFromXML(String fileName) {
//        List<ServiceInfo> services = new ArrayList<>();
//        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
//
//        try {
//            DocumentBuilder builder = factory.newDocumentBuilder();
//            Document doc = builder.parse(new File(fileName));
//            doc.getDocumentElement().normalize();
//
//            NodeList nodeList = doc.getElementsByTagName("service"); // Use lowercase 'service' to match your XML
//
//            for (int i = 0; i < nodeList.getLength(); i++) {
//                Element element = (Element) nodeList.item(i);
//
//                ServiceInfo service = new ServiceInfo(
//                        element.getElementsByTagName("ID").item(0).getTextContent(),
//                        element.getElementsByTagName("ServiceName").item(0).getTextContent(),
//                        element.getElementsByTagName("ServiceHost").item(0).getTextContent(),
//                        element.getElementsByTagName("Protocol").item(0).getTextContent(),
//                        Integer.parseInt(element.getElementsByTagName("ServicePort").item(0).getTextContent()),
//                        element.getElementsByTagName("ServiceResourceURI").item(0).getTextContent(),
//                        element.getElementsByTagName("ServiceMethod").item(0).getTextContent(),
//                        element.getElementsByTagName("ExpectedTelnetResponse").item(0).getTextContent(),
//                        element.getElementsByTagName("ExpectedRequestResponse").item(0).getTextContent(),
//                        Long.parseLong(element.getElementsByTagName("MonitoringIntervals").item(0).getTextContent()),
//                        TimeUnit.valueOf(element.getElementsByTagName("MonitoringIntervalsTimeUnit").item(0).getTextContent().toUpperCase()),
//                        element.getElementsByTagName("EnableFileLogging").item(0).getTextContent(),
//                        element.getElementsByTagName("FileLoggingInterval").item(0).getTextContent(),
//                        element.getElementsByTagName("EnableFileArchiving").item(0).getTextContent(),
//                        element.getElementsByTagName("FileArchivingInterval").item(0).getTextContent(),
//                        element.getElementsByTagName("LastLogTime").item(0).getTextContent()
//                        // ... other fields
//                );
//                services.add(service);
//            }
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//
//        return services;
//    }
//
//    private static List<ServiceInfo> readServicesFromYAML(String fileName) {
//        List<ServiceInfo> services = new ArrayList<>();
//        Yaml yaml = new Yaml(new Constructor(Map.class));
//
//        try (FileReader reader = new FileReader(fileName)) {
//            Map<String, Object> yamlData = yaml.load(reader);
//            List<Map<String, Object>> serviceList = (List<Map<String, Object>>) yamlData.get("services");
//
//            for (Map<String, Object> map : serviceList) {
//                ServiceInfo service = new ServiceInfo(
//                        map.get("ID").toString(),
//                        map.get("ServiceName").toString(),
//                        map.get("ServiceHost").toString(),
//                        map.get("Protocol").toString(),
//                        Integer.parseInt(map.get("ServicePort").toString()),
//                        map.get("ServiceResourceURI").toString(),
//                        map.get("ServiceMethod").toString(),
//                        map.get("ExpectedTelnetResponse").toString(),
//                        map.get("ExpectedRequestResponse").toString(),
//                        Long.parseLong(map.get("MonitoringIntervals").toString()),
//                        TimeUnit.valueOf(((String) map.get("MonitoringIntervalsTimeUnit")).toUpperCase()),
//                        map.get("EnableFileLogging").toString(),
//                        map.get("FileLoggingInterval").toString(),
//                        map.get("EnableFileArchiving").toString(),
//                        map.get("FileArchivingInterval").toString(),
//                        (String) map.get("LastLogTime") // Assuming LastLogTime can be null or empty
//                        // ... other fields
//                );
//                services.add(service);
//            }
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//
//        return services;
//    }
//
//    private static List<ServiceInfo> readServicesFromJSON(String fileName)  {
//        try{
//            JSONParser parser = new JSONParser();
//            JSONArray jsonArray = (JSONArray) parser.parse(new FileReader(fileName));
//            List<ServiceInfo> services = new ArrayList<>();
//
//            for (Object o : jsonArray) {
//                JSONObject jsonObject = (JSONObject) o;
//                ServiceInfo service = new ServiceInfo(
//                        jsonObject.get("ID").toString(),
//                        jsonObject.get("ServiceName").toString(),
//                        jsonObject.get("ServiceHost").toString(),
//                        jsonObject.get("Protocol").toString(),
//                        Integer.parseInt(jsonObject.get("ServicePort").toString()),
//                        jsonObject.get("ServiceResourceURI").toString(),
//                        jsonObject.get("ServiceMethod").toString(),
//                        jsonObject.get("ExpectedTelnetResponse").toString(),
//                        jsonObject.get("ExpectedRequestResponse").toString(),
//                        Long.parseLong(jsonObject.get("MonitoringIntervals").toString()),
//                        TimeUnit.valueOf(((String) jsonObject.get("MonitoringIntervalsTimeUnit")).toUpperCase()),
//                        jsonObject.get("EnableFileLogging").toString(),
//                        jsonObject.get("FileLoggingInterval").toString(),
//                        jsonObject.get("EnableFileArchiving").toString(),
//                        jsonObject.get("FileArchivingInterval").toString(),
//                        jsonObject.get("LastLogTime").toString()
//                        // ... other fields
//                );
//                services.add(service);
//            }
//            return services;
//        }catch(Exception e){
//            e.printStackTrace();
//        }
//
//        return services;
//    }
//    private static List<ServiceInfo> readServicesFromINI(String fileName) throws IOException {
//        Ini ini = new Ini(new File(fileName));
//        List<ServiceInfo> services = new ArrayList<>();
//
//        for (String sectionName : ini.keySet()) {
//            Ini.Section section = ini.get(sectionName);
//            ServiceInfo service = new ServiceInfo(
//                    section.get("ID"),
//                    section.get("ServiceName"),
//                    section.get("ServiceHost"),
//                    section.get("Protocol"),
//                    Integer.parseInt(section.get("ServicePort")),
//                    section.get("ServiceResourceURI"),
//                    section.get("ServiceMethod"),
//                    section.get("ExpectedTelnetResponse"),
//                    section.get("ExpectedRequestResponse"),
//                    Long.parseLong(section.get("MonitoringIntervals")),
//                    TimeUnit.valueOf(section.get("MonitoringIntervalsTimeUnit").toUpperCase()),
//                    section.get("EnableFileLogging"),
//                    section.get("FileLoggingInterval"),
//                    section.get("EnableFileArchiving"),
//                    section.get("FileArchivingInterval"),
//                    section.get("LastLogTime")
//                    // ... other fields
//            );
//            services.add(service);
//        }
//        return services;
//    }
//
//    private static Runnable createMonitoringTask(ServiceInfo service) {
//        return () -> {
//            LocalDateTime now = LocalDateTime.now();
//            DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
//            String formattedDateTime = now.format(dateTimeFormatter);
//            String dayOfWeek = now.getDayOfWeek().getDisplayName(TextStyle.SHORT, Locale.ENGLISH);
//
//            String host  = service.getServiceHost();
//            int port = service.getServicePort();
//            String path = service.getServiceResourceURI();
//            String protocol = service.getServiceProtocol();
//            String method = service.getServiceMethod();
//
//            Object[] server_result = isServerUp(host, port, service.getExpectedTelnetResponse());
//            boolean isServerUp = (Boolean) server_result[0];
//
//            Object[] app_result = isApplicationUp(protocol,host,path,method);
//            boolean isAppUp = (Boolean) app_result[0];
//
//            String dirPath = "logs/service"+service.getId();
//            File directory = new File(dirPath);
//
//            if(service.getEnableFileLogging().equals("yes")){
//                if (!directory.exists()) {
//                    boolean isCreated = directory.mkdirs();
//
//                    if (!isCreated) {
//                        System.out.println("Failed to create directory.");
//                    }
//                }
//
//                if(directory.exists()){
//                    lasttimeLog = service.getLastLogTime();
//                    if(lasttimeLog.isEmpty()){
//
//                        try {
//                            SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmmss");
//                            String timestamp = sdf.format(new Date());
//                            service.setLastLogTime(timestamp);
//
//                            if(fileName.endsWith(".xml")){
//                                updateXmlLastLogTime(service.getId(),timestamp,fileName);
//                            }else if(fileName.endsWith(".csv")){
//                                updateCsvLastLogTime(service.getId(), timestamp,fileName);
//                            }else  if(fileName.endsWith(".yaml")){
//
//                            }else  if(fileName.endsWith(".ini")){
//                                updateIniLastLogTime(service.getId(),timestamp,fileName);
//                            }else  if(fileName.endsWith(".json")){
//                                updateJsonLastLogTime(service.getId(),timestamp,fileName);
//                            }
//
//                            String logServerFileName = generateLogFileName(dirPath, "server");
//                            writeLogEntry(logServerFileName, dayOfWeek + " " + formattedDateTime + " " + "Server status for " + service.getServiceName() + ": " + (isServerUp ? "Up" : "Down") + ", Response: " + server_result[1]);
//                            String logApplicationFileName = generateLogFileName(dirPath, "application");
//                            writeLogEntry(logApplicationFileName, dayOfWeek + " " + formattedDateTime + " " + "Application status for " + service.getServiceName() + ": " + (isAppUp ? "Up" : "Down") + ", Response: " + app_result[1]);
//                        }catch(Exception e){
//
//                        }
//                    }else {
//
//                        LocalDateTime nextLogTime = getNextLogTime(service, service.getLastLogTime());
//                        LocalDateTime currentTime = LocalDateTime.now();
//
//                        if (currentTime.isAfter(nextLogTime)) {
//                            try {
//                                SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmmss");
//                                String timestamp = sdf.format(new Date());
//                                service.setLastLogTime(timestamp);
//
//                                if(fileName.endsWith(".xml")){
//                                    updateXmlLastLogTime(service.getId(),timestamp,fileName);
//                                }else if(fileName.endsWith(".csv")){
//                                    updateCsvLastLogTime(service.getId(), timestamp,fileName);
//                                }else  if(fileName.endsWith(".yaml")){
//
//                                }else  if(fileName.endsWith(".ini")){
//                                    updateIniLastLogTime(service.getId(),timestamp,fileName);
//                                }else  if(fileName.endsWith(".json")){
//                                    updateJsonLastLogTime(service.getId(),timestamp,fileName);
//                                }
//
//                                String logServerFileName = generateLogFileName(dirPath, "server");
//                                writeLogEntry(logServerFileName, dayOfWeek + " " + formattedDateTime + " " + "Server status for " + service.getServiceName() + ": " + (isServerUp ? "Up" : "Down") + ", Response: " + server_result[1]);
//                                String logApplicationFileName = generateLogFileName(dirPath, "application");
//                                writeLogEntry(logApplicationFileName, dayOfWeek + " " + formattedDateTime + " " + "Application status for " + service.getServiceName() + ": " + (isAppUp ? "Up" : "Down") + ", Response: " + app_result[1]);
//
//                            } catch (Exception e) {
//                                System.out.println(e.getMessage());
//                            }
//
//                        } else {
//                            String logServerFileName = dirPath + File.separator + service.getLastLogTime() + "_server.log";
//                            String logApplicationFileName = dirPath + File.separator + service.getLastLogTime() + "_application.log";
//                            writeLogEntry(logServerFileName, dayOfWeek + " " + formattedDateTime + " " + "Server status for " + service.getServiceName() + ": " + (isServerUp ? "Up" : "Down") + ", Response: " + server_result[1]);
//                            writeLogEntry(logApplicationFileName, dayOfWeek + " " + formattedDateTime + " " + "Application status for " + service.getServiceName() + ": " + (isAppUp ? "Up" : "Down") + ", Response: " + app_result[1]);
//
//                        }
//                    }
//                }
//            }
//
//
//        };
//    }
//
//
//
//    private static List<CSVRecord> readCsvFile(String filePath) throws IOException {
//        try (Reader reader = Files.newBufferedReader(Paths.get(filePath))) {
//            CSVParser parser = CSVFormat.DEFAULT.withFirstRecordAsHeader().parse(reader);
//            return parser.getRecords();
//        }
//    }
//
//    private static void writeCsvFile(List<String[]> updatedRecords, String filePath) throws IOException {
//        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath));
//             CSVPrinter csvPrinter = new CSVPrinter(writer, CSVFormat.DEFAULT.withHeader(updatedRecords.get(0)))) {
//
//            // Skip the header for writing data
//            for (int i = 1; i < updatedRecords.size(); i++) {
//                csvPrinter.printRecord((Object[]) updatedRecords.get(i));
//            }
//        }
//    }
//    private static void updateCsvLastLogTime(String serviceId, String newLogTime,String CSV_FILE_PATH ) throws IOException {
//        List<CSVRecord> records = readCsvFile(CSV_FILE_PATH);
//        List<String[]> updatedRecords = new ArrayList<>();
//
//        // Add the header to updatedRecords
//        String[] header = records.get(0).getParser().getHeaderMap().keySet().toArray(new String[0]);
//        updatedRecords.add(header);
//
//        for (CSVRecord record : records) {
//            String[] recordData = new String[record.size()];
//            for (int i = 0; i < record.size(); i++) {
//                recordData[i] = record.get(i);
//            }
//
//            if (record.get("ID").equals(serviceId)) {
//                // Assuming the 'Last Log Time' column is named as such
//                int lastLogTimeIndex = record.getParser().getHeaderMap().get("Last Log Time");
//                recordData[lastLogTimeIndex] = newLogTime;
//            }
//
//            updatedRecords.add(recordData);
//        }
//
//        writeCsvFile(updatedRecords, CSV_FILE_PATH);
//    }
//    public static void updateYamlLastLogTime(String serviceId, String newLogTime, String yamlFilePath) throws IOException {
//        Yaml yaml = new Yaml();
//        Map<String, Object> yamlData;
//
//        // Read YAML file
//        try (FileReader reader = new FileReader(yamlFilePath)) {
//            yamlData = yaml.load(reader);
//        }
//
//        // Assume services are listed under a "services" key and each service is a map
//        List<Map<String, Object>> services = (List<Map<String, Object>>) yamlData.get("services");
//        boolean serviceFound = false;
//
//        for (Map<String, Object> service : services) {
//            if (service.get("ID").equals(serviceId)) {
//                service.put("LastLogTime", newLogTime);
//                serviceFound = true;
//                break;
//            }
//        }
//
//        if (!serviceFound) {
//            throw new IllegalArgumentException("Service ID not found in YAML file");
//        }
//
//        // Write updated YAML back to file
//        DumperOptions options = new DumperOptions();
//        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
//        yaml = new Yaml(options);
//
//        try (FileWriter writer = new FileWriter(yamlFilePath)) {
//            yaml.dump(yamlData, writer);
//        }
//    }
//    private static void updateIniLastLogTime(String serviceId, String newLogTime, String iniFilePath) throws IOException {
//        Path path = Paths.get(iniFilePath);
//        List<String> fileContent = new ArrayList<>(Files.readAllLines(path, StandardCharsets.UTF_8));
//
//        boolean serviceFound = false;
//        for (int i = 0; i < fileContent.size(); i++) {
//            String line = fileContent.get(i);
//
//            if (line.trim().startsWith("[Service") && line.contains(serviceId)) {
//                serviceFound = true;
//            }
//
//            if (serviceFound && line.startsWith("LastLogTime")) {
//                fileContent.set(i, "LastLogTime = " + newLogTime);
//                break;
//            }
//        }
//
//        if (!serviceFound) {
//            throw new IllegalArgumentException("Service ID not found in ini file");
//        }
//
//        Files.write(path, fileContent, StandardCharsets.UTF_8);
//    }
//    private static Gson createGsonWithCustomDeserializer() {
//        return new GsonBuilder()
//                .registerTypeAdapter(ServiceInfo.class, new JsonDeserializer<ServiceInfo>() {
//                    @Override
//                    public ServiceInfo deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
//                        JsonObject jsonObject = json.getAsJsonObject();
//
//                        String ID = jsonObject.get("ID").getAsString();
//                        String ServiceName = jsonObject.get("ServiceName").getAsString();
//                        String ServiceHost = jsonObject.get("ServiceHost").getAsString();
//                        int ServicePort = jsonObject.get("ServicePort").getAsInt();
//                        String Protocol = jsonObject.get("Protocol").getAsString();
//                        String ServiceResourceURI = jsonObject.get("ServiceResourceURI").getAsString();
//                        String ServiceMethod = jsonObject.get("ServiceMethod").getAsString();
//                        String ExpectedTelnetResponse = jsonObject.get("ExpectedTelnetResponse").getAsString();
//                        String ExpectedRequestResponse = jsonObject.get("ExpectedRequestResponse").getAsString();
//                        long MonitoringIntervals = jsonObject.get("MonitoringIntervals").getAsLong();
//                        TimeUnit MonitoringIntervalsTimeUnit = TimeUnit.valueOf(jsonObject.get("MonitoringIntervalsTimeUnit").getAsString().toUpperCase());
//                        String EnableFileLogging = jsonObject.get("EnableFileLogging").getAsString();
//                        String FileLoggingInterval = jsonObject.get("FileLoggingInterval").getAsString();
//                        String EnableFileArchiving = jsonObject.get("EnableFileArchiving").getAsString();
//                        String FileArchivingInterval = jsonObject.get("FileArchivingInterval").getAsString();
//                        String LastLogTime = jsonObject.get("LastLogTime").getAsString();
//
//                        return new ServiceInfo(ID, ServiceName, ServiceHost, Protocol, ServicePort, ServiceResourceURI,
//                                ServiceMethod, ExpectedTelnetResponse, ExpectedRequestResponse,
//                                MonitoringIntervals, MonitoringIntervalsTimeUnit, EnableFileLogging, FileLoggingInterval,EnableFileArchiving, FileArchivingInterval, LastLogTime);
//                    }
//                })
//                .create();
//    }
//
//    private static void updateJsonLastLogTime(String serviceId, String newLogTime, String jsonFilePath) throws IOException {
//        Path path = Path.of(jsonFilePath);
//        Gson gson = createGsonWithCustomDeserializer();
//
//        // Read the JSON file
//        String json = Files.readString(path);
//        Type listType = new TypeToken<List<ServiceInfo>>(){}.getType();
//        List<ServiceInfo> services = gson.fromJson(json, listType);
//
//        // Update the LastLogTime for the specified service ID
//        boolean serviceFound = false;
//        for (ServiceInfo service : services) {
//            if (service.getId().equals(serviceId)) {
//                // Create a new ServiceInfo object with updated lastLogTime
//                ServiceInfo updatedService = new ServiceInfo(service.getId(), service.getServiceName(), service.getServiceHost(),
//                        service.getServiceProtocol(), service.getServicePort(), service.getServiceResourceURI(), service.getServiceMethod(),
//                        service.getExpectedTelnetResponse(), service.getExpectedRequestResponse(), service.getMonitoringInterval(),
//                        service.getTimeUnit(), service.getEnableFileLogging(), service.getFileLoggingInterval(),service.getEnableFileArchiving(), service.getFileArchivingInterval(), newLogTime);
//
//                // Replace the old service object with the updated one
//                services.set(services.indexOf(service), updatedService);
//                serviceFound = true;
//                break;
//            }
//        }
//
//        if (!serviceFound) {
//            throw new IllegalArgumentException("Service ID not found in JSON file");
//        }
//
//        // Write the updated JSON back to the file
//        String updatedJson = gson.toJson(services);
//        Files.writeString(path, updatedJson, StandardOpenOption.TRUNCATE_EXISTING);
//    }
//
//    public static void updateXmlLastLogTime(String serviceId, String newLogTime, String xmlFilePath) throws Exception {
//        File xmlFile = new File(xmlFilePath);
//        DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
//        DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
//        Document document = documentBuilder.parse(xmlFile);
//        document.getDocumentElement().normalize();
//
//        NodeList serviceList = document.getElementsByTagName("service");
//        boolean serviceFound = false;
//
//        for (int i = 0; i < serviceList.getLength(); i++) {
//            Element service = (Element) serviceList.item(i);
//            if (service.getElementsByTagName("ID").item(0).getTextContent().equals(serviceId)) {
//                service.getElementsByTagName("LastLogTime").item(0).setTextContent(newLogTime);
//                serviceFound = true;
//                break;
//            }
//        }
//
//        if (!serviceFound) {
//            throw new IllegalArgumentException("Service ID not found in XML file");
//        }
//
//        TransformerFactory transformerFactory = TransformerFactory.newInstance();
//        Transformer transformer = transformerFactory.newTransformer();
//        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
//        DOMSource source = new DOMSource(document);
//        StreamResult result = new StreamResult(xmlFile);
//        transformer.transform(source, result);
//    }
//
//    private static LocalDateTime getNextLogTime(ServiceInfo service, String lastLogTimeStr) {
//        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmmss");
//        LocalDateTime lastLogTime;
//        String timestamp = sdf.format(new Date());
//
//        try {
//            // Parse the timestamp string to Date
//            Date lastLogDate = sdf.parse(lastLogTimeStr);
//
//            // Convert Date to LocalDateTime
//            lastLogTime = lastLogDate.toInstant()
//                    .atZone(ZoneId.systemDefault())
//                    .toLocalDateTime();
//        } catch (ParseException e) {
//            //System.out.println("Error parsing the timestamp string: " + e.getMessage());
//            lastLogTime = LocalDateTime.now();
//        }
//
//        switch (service.getFileLoggingInterval()) {
//            case "Hourly":
//                return lastLogTime.plusHours(1);
//            case "Daily":
//                return lastLogTime.plusDays(1);
//            case "Weekly":
//                return lastLogTime.plusWeeks(1);
//            default:
//                throw new IllegalArgumentException("Invalid logging interval");
//        }
//    }
//    private static String generateLogFileName(String dirPath, String name) {
//        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmmss");
//        String timestamp = sdf.format(new Date());
//        return dirPath + File.separator + timestamp + "_"+name+".log";
//    }
//
//    private static void writeLogEntry(String logFileName, String logEntry) {
//        try (BufferedWriter writer = new BufferedWriter(new FileWriter(logFileName, true))) {
//            writer.write(logEntry);
//            writer.newLine();
//        } catch (IOException e) {
//            System.out.println(e.getMessage());
//        }
//    }
//
//}