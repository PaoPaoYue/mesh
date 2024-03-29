package com.github.paopaoyue.mesh.rpc.core;

import com.github.paopaoyue.mesh.rpc.proto.Protocol;

import java.util.Date;
import java.util.UUID;

public class Context {
    private static final ThreadLocal<Context> CONTEXT = ThreadLocal.withInitial(Context::new);

    private String service;
    private String handler;
    private Flag flag;
    private long requestId;
    private long traceId;
    private long startTime;
    private String upperService;
    private String upperHandler;
    private String upperDevice;
    private String upperHost;
    private int upperPort;

    public Context() {
        requestId = UUID.randomUUID().getMostSignificantBits() & Long.MAX_VALUE;
        traceId = UUID.randomUUID().getMostSignificantBits() & Long.MAX_VALUE;
        startTime = System.currentTimeMillis();
    }

    public Context(Protocol.Packet packet) {
        if (packet != null) {
            Protocol.PacketHeader header = packet.getHeader();
            Protocol.TraceInfo traceInfo = packet.getTraceInfo();
            service = header.getService();
            handler = header.getHandler();
            flag = new Flag(header.getFlag());
            requestId = header.getRequestId();
            traceId = traceInfo.getTraceId();
            startTime = traceInfo.getStartTime();
            upperService = traceInfo.getUpperService();
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

    public Flag getFlag() {
        return flag;
    }

    public void setFlag(Flag flag) {
        this.flag = flag;
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
                ", flag='" + Integer.toBinaryString(flag.getValue()) + '\'' +
                ", requestId=" + requestId +
                ", traceId=" + traceId +
                ", startTime=" + getReadableStartTime() +
                ", upperService='" + upperService + '\'' +
                ", upperHandler='" + upperHandler + '\'' +
                ", upperDevice='" + upperDevice + '\'' +
                ", upperHost='" + upperHost + '\'' +
                ", upperPort=" + upperPort +
                '}';
    }
}
