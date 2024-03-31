package com.github.paopaoyue.mesh.rpc.config;

import jakarta.validation.constraints.NotNull;
import org.hibernate.validator.constraints.Range;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;

import java.util.Arrays;
import java.util.Set;

@ConfigurationProperties(prefix = "mesh.rpc")
public class Properties {
    @NotNull
    private boolean serverEnabled = false;
    @NestedConfigurationProperty
    private ServiceProperties serverService = null;
    @Range(min = 1, max = 16)
    private int serverNetworkThreads = 1;
    @Range(min = 1, max = 32)
    private int serverWorkerThreads = 1;
    @Range(min = 1, max = 60)
    private int serverShutDownTimeout = 10;

    @NotNull
    private boolean clientEnabled = false;
    @NestedConfigurationProperty
    private Set<ServiceProperties> clientServices = Set.of();
    @Range(min = 1, max = 60)
    private int clientShutDownTimeout = 10;

    @Range(min = 1, max = 2 * 1024 * 1024)
    private int packetMaxSize = 1024 * 1024;
    @Range(min = 3, max = 10)
    private int keepAliveTimeout = 6;
    @Range(min = 1, max = 3)
    private int keepAliveInterval = 2;

    public boolean isServerEnabled() {
        return serverEnabled;
    }

    public void setServerEnabled(boolean serverEnabled) {
        this.serverEnabled = serverEnabled;
    }

    public ServiceProperties getServerService() {
        return serverService;
    }

    public void setServerService(ServiceProperties serverService) {
        this.serverService = serverService;
    }

    public int getServerNetworkThreads() {
        return serverNetworkThreads;
    }

    public void setServerNetworkThreads(int serverNetworkThreads) {
        this.serverNetworkThreads = serverNetworkThreads;
    }

    public int getServerWorkerThreads() {
        return serverWorkerThreads;
    }

    public void setServerWorkerThreads(int serverWorkerThreads) {
        this.serverWorkerThreads = serverWorkerThreads;
    }

    public int getServerShutDownTimeout() {
        return serverShutDownTimeout;
    }

    public void setServerShutDownTimeout(int serverShutDownTimeout) {
        this.serverShutDownTimeout = serverShutDownTimeout;
    }

    public boolean isClientEnabled() {
        return clientEnabled;
    }

    public void setClientEnabled(boolean clientEnabled) {
        this.clientEnabled = clientEnabled;
    }

    public Set<ServiceProperties> getClientServices() {
        return clientServices;
    }

    public void setClientServices(Set<ServiceProperties> clientServices) {
        this.clientServices = clientServices;
    }

    public int getClientShutDownTimeout() {
        return clientShutDownTimeout;
    }

    public void setClientShutDownTimeout(int clientShutDownTimeout) {
        this.clientShutDownTimeout = clientShutDownTimeout;
    }

    public int getPacketMaxSize() {
        return packetMaxSize;
    }

    public void setPacketMaxSize(int packetMaxSize) {
        this.packetMaxSize = packetMaxSize;
    }

    public int getKeepAliveTimeout() {
        return keepAliveTimeout;
    }

    public void setKeepAliveTimeout(int keepAliveTimeout) {
        this.keepAliveTimeout = keepAliveTimeout;
    }

    public int getKeepAliveInterval() {
        return keepAliveInterval;
    }

    public void setKeepAliveInterval(int keepAliveInterval) {
        this.keepAliveInterval = keepAliveInterval;
    }

    @Override
    public String toString() {
        return "Properties{" +
                "serverEnabled=" + serverEnabled +
                ", serverService=" + serverService +
                ", serverNetworkThreads=" + serverNetworkThreads +
                ", serverWorkerThreads=" + serverWorkerThreads +
                ", serverShutDownTimeout=" + serverShutDownTimeout +
                ", clientEnabled=" + clientEnabled +
                ", clientServices=" + Arrays.toString(clientServices.stream().map(ServiceProperties::toString).toArray()) +
                ", clientShutDownTimeout=" + clientShutDownTimeout +
                ", packetMaxSize=" + packetMaxSize +
                ", keepAliveTimeout=" + keepAliveTimeout +
                ", keepAliveInterval=" + keepAliveInterval +
                '}';
    }
}
