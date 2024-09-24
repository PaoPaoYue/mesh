package io.github.paopaoyue.mesh.rpc.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.hibernate.validator.constraints.Range;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;
import org.springframework.validation.annotation.Validated;

import java.util.Arrays;
import java.util.Set;

@Validated
@ConfigurationProperties(prefix = "mesh.rpc")
public class Properties {
    @NotBlank
    private String env = "default";
    @NotNull
    private boolean serverEnabled = false;
    @NestedConfigurationProperty
    private ServiceProperties serverService = null;
    @Range(min = 1, max = 16)
    private int serverNetworkThreads = 1;
    @Range(min = 0, max = 32)
    private int serverWorkerThreads = 0;
    @Range(min = 0, max = 600)
    private int serverWorkerKeepAliveTimeout = 60;
    @Range(min = 1, max = 60)
    private int serverShutDownTimeout = 10;
    @Range(min = 0, max = 4)
    private int ServerShutDownDelay = 0;
    private boolean ServerHealthCheckEnabled = false;
    @Range(min = 1000, max = 65535)
    private int ServerHealthCheckPort = 50051;

    @NotNull
    private boolean clientEnabled = false;
    @NestedConfigurationProperty
    private ServiceProperties defaultClientService = null;
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
    @Range(min = 10, max = 60)
    private int keepAliveIdleTimeout = 10;
    @Range(min = 1, max = 3)
    private int keepAliveHeartbeatTimeout = 1;

    public String getEnv() {
        return env;
    }

    public void setEnv(String env) {
        this.env = env;
    }

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

    public int getServerWorkerKeepAliveTimeout() {
        return serverWorkerKeepAliveTimeout;
    }

    public void setServerWorkerKeepAliveTimeout(int serverWorkerKeepAliveTimeout) {
        this.serverWorkerKeepAliveTimeout = serverWorkerKeepAliveTimeout;
    }

    public int getServerShutDownTimeout() {
        return serverShutDownTimeout;
    }

    public void setServerShutDownTimeout(int serverShutDownTimeout) {
        this.serverShutDownTimeout = serverShutDownTimeout;
    }

    public int getServerShutDownDelay() {
        return ServerShutDownDelay;
    }

    public void setServerShutDownDelay(int ServerShutDownDelay) {
        this.ServerShutDownDelay = ServerShutDownDelay;
    }

    public boolean isServerHealthCheckEnabled() {
        return ServerHealthCheckEnabled;
    }

    public void setServerHealthCheckEnabled(boolean ServerHealthCheckEnabled) {
        this.ServerHealthCheckEnabled = ServerHealthCheckEnabled;
    }

    public int getServerHealthCheckPort() {
        return ServerHealthCheckPort;
    }

    public void setServerHealthCheckPort(int ServerHealthCheckPort) {
        this.ServerHealthCheckPort = ServerHealthCheckPort;
    }

    public boolean isClientEnabled() {
        return clientEnabled;
    }

    public void setClientEnabled(boolean clientEnabled) {
        this.clientEnabled = clientEnabled;
    }

    public ServiceProperties getDefaultClientService() {
        return defaultClientService;
    }

    public void setDefaultClientService(ServiceProperties defaultClientService) {
        this.defaultClientService = defaultClientService;
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

    public int getKeepAliveIdleTimeout() {
        return keepAliveIdleTimeout;
    }

    public void setKeepAliveIdleTimeout(int keepAliveIdleTimeout) {
        this.keepAliveIdleTimeout = keepAliveIdleTimeout;
    }

    public int getKeepAliveHeartbeatTimeout() {
        return keepAliveHeartbeatTimeout;
    }

    public void setKeepAliveHeartbeatTimeout(int keepAliveHeartbeatTimeout) {
        this.keepAliveHeartbeatTimeout = keepAliveHeartbeatTimeout;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("env=").append(env);
        if (serverEnabled) {
            sb.append(", serverService=").append(serverService);
            sb.append(", serverNetworkThreads=").append(serverNetworkThreads);
            sb.append(", serverWorkerThreads=").append(serverWorkerThreads);
            sb.append(", serverWorkerKeepAliveTimeout=").append(serverWorkerKeepAliveTimeout);
            sb.append(", serverShutDownTimeout=").append(serverShutDownTimeout);
            sb.append(", ServerShutDownDelay=").append(ServerShutDownDelay);
            sb.append(", ServerHealthCheckEnabled=").append(ServerHealthCheckEnabled);
            sb.append(", ServerHealthCheckPort=").append(ServerHealthCheckPort);
        } else {
            sb.append(", serverEnabled=false");
        }
        if (clientEnabled) {
            sb.append(", defaultClientService=").append(defaultClientService);
            sb.append(", clientServices=").append(Arrays.toString(clientServices.stream().map(ServiceProperties::toString).toArray()));
            sb.append(", clientShutDownTimeout=").append(clientShutDownTimeout);
        } else {
            sb.append(", clientEnabled=false");
        }
        if (clientEnabled || serverEnabled) {
            sb.append(", packetMaxSize=").append(packetMaxSize);
            sb.append(", keepAliveTimeout=").append(keepAliveTimeout);
            sb.append(", keepAliveInterval=").append(keepAliveInterval);
            sb.append(", keepAliveIdleTimeout=").append(keepAliveIdleTimeout);
            sb.append(", keepAliveHeartbeatTimeout=").append(keepAliveHeartbeatTimeout);
        } else {
            return "Properties{serverEnabled=false, clientEnabled=false}";
        }
        return "Properties{" + sb.toString() + '}';
    }
}
