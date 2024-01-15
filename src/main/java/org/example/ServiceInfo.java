package org.example;

import com.google.gson.annotations.SerializedName;

import java.util.concurrent.TimeUnit;

class ServiceInfo {
    @SerializedName("ID")
    private final String id;
    @SerializedName("ServiceName")
    private final String serviceName;
    @SerializedName("ServiceHost")
    private final String serviceHost;
    @SerializedName("ServicePort")
    private final int servicePort;
    @SerializedName("Protocol")
    private final String serviceProtocol;
    @SerializedName("ServiceResourceURI")
    private final String serviceResourceURI;
    @SerializedName("ServiceMethod")
    private final String serviceMethod;
    @SerializedName("ExpectedTelnetResponse")
    private final String expectedTelnetResponse;
    @SerializedName("ExpectedRequestResponse")
    private final String expectedRequestResponse;
    @SerializedName("MonitoringIntervals")
    private final long monitoringInterval;
    @SerializedName("MonitoringIntervalsTimeUnit")
    private final TimeUnit timeUnit;

    @SerializedName("EnableFileLogging")
    private final String enableFileLogging;
    @SerializedName("FileLoggingInterval")
    private final String fileLoggingInterval;

    @SerializedName("EnableLogsArchiving")
    private final String enableFileArchiving;
    @SerializedName("LogArchivingIntervals")
    private final String fileArchivingInterval;

    @SerializedName("LastLogTime")
    private String lastLogTime;
    @SerializedName("LastArchiveTime")
    private String lastArchiveTime;

    public ServiceInfo(String id, String serviceName, String serviceHost, String serviceProtocol, int servicePort, String serviceResourceURI,
                       String serviceMethod, String expectedTelnetResponse, String expectedRequestResponse,
                       long monitoringInterval, TimeUnit timeUnit, String enableFileLogging, String fileLoggingInterval,  String enableFileArchiving, String fileArchivingInterval, String lastLogTime, String lastArchiveTime) {
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
        this.enableFileArchiving = enableFileArchiving;
        this.fileArchivingInterval = fileArchivingInterval;
        this.lastLogTime = lastLogTime;
        this.lastArchiveTime = lastArchiveTime;
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

    public String getEnableFileArchiving(){return enableFileArchiving;}

    public String getFileArchivingInterval(){return fileArchivingInterval;}

    public String getLastLogTime(){return lastLogTime;}

    public void setLastLogTime(String value){
        this.lastLogTime = value;
    }
    public String getLastArchiveTime(){return lastArchiveTime;}
    public void setLastArchiveTime(String value){
        this.lastArchiveTime = value;
    }
}