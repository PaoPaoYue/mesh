package com.github.paopaoyue.mesh.rpc.core.client;

import com.github.paopaoyue.mesh.rpc.RpcAutoConfiguration;
import com.github.paopaoyue.mesh.rpc.api.CallOption;
import com.github.paopaoyue.mesh.rpc.exception.TimeoutException;
import com.github.paopaoyue.mesh.rpc.proto.Protocol;
import com.github.paopaoyue.mesh.rpc.util.Context;
import com.github.paopaoyue.mesh.rpc.util.Flag;
import com.github.paopaoyue.mesh.rpc.util.IDGenerator;
import com.github.paopaoyue.mesh.rpc.util.TraceInfoUtil;
import com.google.protobuf.Any;

import java.time.Duration;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import static org.awaitility.Awaitility.await;

public class Sender {

    private final Reactor reactor;
    private Lock lock;

    public Sender(Reactor reactor) {
        this.reactor = reactor;
        this.lock = new ReentrantLock();
    }

    public Protocol.Packet send(String serviceName, String handlerName, Any request, boolean isSystem, CallOption option) throws Exception {
        if (serviceName.isEmpty()) {
            throw new IllegalArgumentException("Service name is empty");
        }
        if (handlerName.isEmpty()) {
            throw new IllegalArgumentException("Handler name is empty");
        }
        Context context = Context.getContext();
        Duration overallTimeout = option.getOverallTimeout();
        for (int i = 0; i <= option.getRetryTimes(); i++) {
            if (RpcAutoConfiguration.getRpcClient().getStatus() != RpcClient.Status.RUNNING) {
                throw new IllegalStateException("Client is shutting down or not started");
            }

            ConnectionHandler connectionHandler = null;
            if (option.isKeepAlive()) {
                connectionHandler = reactor.getOrCreateConnection(serviceName, option.getConnectionTag(), isSystem);
            } else {
                connectionHandler = reactor.createConnection(option.isKeepAlive(), serviceName, option.getConnectionTag());
            }

            if (connectionHandler == null) {
                throw new IllegalStateException("Unable to create connection to service: " + serviceName);
            }

            Protocol.Packet packet = Protocol.Packet.newBuilder()
                    .setHeader(Protocol.PacketHeader.newBuilder()
                            .setLength(1)
                            .setService(serviceName)
                            .setHandler(handlerName)
                            .setEnv(option.getEnv().isEmpty() ? context.getEnv() : option.getEnv())
                            .setRequestId(IDGenerator.generateRequestId())
                            .setFlag((isSystem ? Flag.SYSTEM_CALL : Flag.SERVICE_CALL) |
                                    (option.isKeepAlive() ? Flag.KEEP_ALIVE : 0) |
                                    (option.isFin() ? Flag.FIN : 0))
                            .build())
                    .setTraceInfo(TraceInfoUtil.createTraceInfo(context, connectionHandler.getSocket()))
                    .setBody(request)
                    .build();

            ConnectionHandler.Waiter waiter = connectionHandler.sendPacket(packet);
            try {
                return waiter.getResponse(overallTimeout.compareTo(option.getTimeout()) >= 0 ? option.getTimeout() : overallTimeout);
            } catch (Exception e) {
                if (i == option.getRetryTimes()) {
                    throw e;
                }
            }
            overallTimeout = overallTimeout.minus(option.getTimeout());
            if (option.getRetryInterval().isPositive()) {
                await().atMost(overallTimeout.compareTo(option.getRetryInterval()) >= 0 ? option.getRetryInterval() : overallTimeout);
            }
            overallTimeout = overallTimeout.minus(option.getRetryInterval());
        }
        if (!overallTimeout.isPositive()) {
            throw new TimeoutException("Overall timeout");
        }

        return null;
    }
}
