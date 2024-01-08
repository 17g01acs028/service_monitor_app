package org.example;

import java.util.concurrent.TimeUnit;

class ServiceInfo {
    private final String id;
    private final String serviceName;
    private final String serviceHost;
    private final int servicePort;
    private final String serviceProtocol;
    private final String serviceResourceURI;
    private final String serviceMethod;
    private final String expectedTelnetResponse;
    private final String expectedRequestResponse;
    private final long monitoringInterval;
    private final TimeUnit timeUnit;

    private final String enableFileLogging;

    private final String fileLoggingInterval;

    private final String lastLogTime;

    public ServiceInfo(String id, String serviceName, String serviceHost, String serviceProtocol, int servicePort, String serviceResourceURI,
                       String serviceMethod, String expectedTelnetResponse, String expectedRequestResponse,
                       long monitoringInterval, TimeUnit timeUnit, String enableFileLogging, String fileLoggingInterval, String lastLogTime) {
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
        this.enableFileLogging = enableFileLogging;
        this.fileLoggingInterval = fileLoggingInterval;
        this.lastLogTime = lastLogTime;
    }
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

    public String getEnableFileLogging(){return enableFileLogging;}

    public String getFileLoggingInterval(){return fileLoggingInterval;}

    public String getLastLogTime(){return lastLogTime;}
}