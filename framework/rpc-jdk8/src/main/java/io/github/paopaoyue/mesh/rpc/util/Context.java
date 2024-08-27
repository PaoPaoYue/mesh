package io.github.paopaoyue.mesh.rpc.util;

import io.github.paopaoyue.mesh.rpc.RpcAutoConfiguration;
import io.github.paopaoyue.mesh.rpc.proto.Protocol;

import java.util.Date;

public class Context {
    private static final ThreadLocal<Context> CONTEXT = ThreadLocal.withInitial(Context::new);

    private String service;
    private String handler;
    private String env;
    private long requestId;
    private long traceId;
    private long startTime;
    private String upperService;
    private String upperHandler;
    private String upperEnv;
    private String upperDevice;
    private String upperHost;
    private int upperPort;

    public Context() {
        env = RpcAutoConfiguration.getEnv();
        service = RpcAutoConfiguration.getProp().isServerEnabled() ?
                RpcAutoConfiguration.getProp().getServerService().getName() : "";
        handler = "";
        requestId = 0;
        traceId = 0;
        startTime = 0;
        upperService = "";
        upperHandler = "";
        upperEnv = "";
        upperDevice = "";
        upperHost = "";
        upperPort = 0;
    }

    public Context(Protocol.Packet packet) {
        this();
        if (packet != null) {
            Protocol.PacketHeader header = packet.getHeader();
            Protocol.TraceInfo traceInfo = packet.getTraceInfo();
            handler = header.getHandler();
            requestId = header.getRequestId();
            traceId = traceInfo.getTraceId();
            startTime = traceInfo.getStartTime();
            upperService = traceInfo.getUpperService();
            upperEnv = traceInfo.getUpperEnv();
            upperHandler = traceInfo.getUpperHandler();
            upperDevice = traceInfo.getUpperDevice();
            upperHost = traceInfo.getUpperHost();
            upperPort = traceInfo.getUpperPort();
        }
    }

    public static Context getContext() {
        return CONTEXT.get();
    }

    public static void setContext(Context context) {
        CONTEXT.set(context);
    }

    public static void removeContext() {
        CONTEXT.remove();
    }

    public String getService() {
        return service;
    }

    public void setService(String service) {
        this.service = service;
    }

    public String getHandler() {
        return handler;
    }

    public void setHandler(String handler) {
        this.handler = handler;
    }

    public String getEnv() {
        return env;
    }

    public void setEnv(String env) {
        this.env = env;
    }

    public long getRequestId() {
        return requestId;
    }

    public void setRequestId(long requestId) {
        this.requestId = requestId;
    }

    public long getTraceId() {
        return traceId;
    }

    public void setTraceId(long traceId) {
        this.traceId = traceId;
    }

    public long getStartTime() {
        return startTime;
    }

    public void setStartTime(long startTime) {
        this.startTime = startTime;
    }

    public Date getReadableStartTime() {
        return new Date(startTime);
    }

    public void setReadableStartTime(Date date) {
        this.startTime = date.getTime();
    }

    public String getUpperService() {
        return upperService;
    }

    public void setUpperService(String upperService) {
        this.upperService = upperService;
    }

    public String getUpperHandler() {
        return upperHandler;
    }

    public void setUpperHandler(String upperHandler) {
        this.upperHandler = upperHandler;
    }

    public String getUpperEnv() {
        return upperEnv;
    }

    public void setUpperEnv(String upperEnv) {
        this.upperEnv = upperEnv;
    }

    public String getUpperDevice() {
        return upperDevice;
    }

    public void setUpperDevice(String upperDevice) {
        this.upperDevice = upperDevice;
    }

    public String getUpperHost() {
        return upperHost;
    }

    public void setUpperHost(String upperHost) {
        this.upperHost = upperHost;
    }

    public int getUpperPort() {
        return upperPort;
    }

    public void setUpperPort(int upperPort) {
        this.upperPort = upperPort;
    }

    @Override
    public String toString() {
        return "Context{" +
                "service='" + service + '\'' +
                ", handler='" + handler + '\'' +
                ", env='" + env + '\'' +
                ", requestId=" + requestId +
                ", traceId=" + traceId +
                ", startTime=" + getReadableStartTime() +
                ", upperService='" + upperService + '\'' +
                ", upperHandler='" + upperHandler + '\'' +
                ", upperEnv='" + upperEnv + '\'' +
                ", upperDevice='" + upperDevice + '\'' +
                ", upperHost='" + upperHost + '\'' +
                ", upperPort=" + upperPort +
                '}';
    }

}
