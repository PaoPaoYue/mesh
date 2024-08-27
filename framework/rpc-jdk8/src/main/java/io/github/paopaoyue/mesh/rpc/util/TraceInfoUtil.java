package io.github.paopaoyue.mesh.rpc.util;

import io.github.paopaoyue.mesh.rpc.proto.Protocol;

import java.net.Socket;

public class TraceInfoUtil {

    private static final String DEFAULT_DEVICE_NAME = "unknown";

    public static Protocol.TraceInfo createTraceInfo(Context context, Socket socket) {
        return Protocol.TraceInfo.newBuilder()
                .setTraceId(context.getTraceId() == 0 ? IDGenerator.generateTraceId() : context.getTraceId())
                .setStartTime(context.getStartTime() == 0 ? System.currentTimeMillis() : context.getStartTime())
                .setUpperService(context.getService())
                .setUpperHandler(context.getHandler())
                .setUpperEnv(context.getEnv())
                .setUpperDevice(getDeviceName())
                .setUpperHost(socket.getLocalAddress().getHostAddress())
                .setUpperPort(socket.getLocalPort())
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
