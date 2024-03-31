package com.github.paopaoyue.mesh.rpc.util;

import com.github.paopaoyue.mesh.rpc.config.RpcAutoConfiguration;
import com.github.paopaoyue.mesh.rpc.config.ServiceProperties;
import com.github.paopaoyue.mesh.rpc.proto.Protocol;

import java.util.UUID;

public class TraceInfoUtil {

    private static final String DEFAULT_DEVICE_NAME = "unknown";

    public static Protocol.TraceInfo createTraceInfo(Context context) {
        ServiceProperties prop = RpcAutoConfiguration.getProp().getServerService();
        return Protocol.TraceInfo.newBuilder()
                .setTraceId(context.getTraceId() == 0 ? UUID.randomUUID().getMostSignificantBits() & Long.MAX_VALUE : context.getTraceId())
                .setStartTime(context.getStartTime() == 0 ? System.currentTimeMillis() : context.getStartTime())
                .setUpperService(context.getService().isEmpty() ? prop.getName() : context.getService())
                .setUpperHandler(context.getHandler())
                .setUpperDevice(getDeviceName())
                .setUpperHost(prop.getHost())
                .setUpperPort(prop.getPort())
                .build();
    }

    private static String getDeviceName() {
        try {
            return java.net.InetAddress.getLocalHost().getHostName();
        } catch (Exception e) {
            return DEFAULT_DEVICE_NAME;
        }
    }
}
