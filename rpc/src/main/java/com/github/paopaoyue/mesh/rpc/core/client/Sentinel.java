package com.github.paopaoyue.mesh.rpc.core.client;

import com.github.paopaoyue.mesh.rpc.RpcAutoConfiguration;
import com.github.paopaoyue.mesh.rpc.api.CallOption;
import com.github.paopaoyue.mesh.rpc.proto.System;
import com.github.paopaoyue.mesh.rpc.util.RespBaseUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.Map;
import java.util.TimerTask;

public class Sentinel extends TimerTask {

    private static final Logger logger = LoggerFactory.getLogger(Sentinel.class);

    private static final String PING_MESSAGE = "HEARTBEAT";
    private static final System.PingRequest HEARTBEAT_REQUEST = System.PingRequest.newBuilder().setMessage(PING_MESSAGE).build();
    private static final CallOption HEARTBEAT_CALLOPTION = new CallOption().setTimeout(Duration.ofSeconds(RpcAutoConfiguration.getProp().getKeepAliveHeartbeatTimeout()));

    @Override
    public void run() {
        if (RpcAutoConfiguration.getRpcClient().getStatus() != RpcClient.Status.RUNNING) {
            cancel();
            return;
        }
        Map<String, ConnectionHandler> connectionPool = RpcAutoConfiguration.getRpcClient().getReactor().getConnectionPool();
        connectionPool.entrySet().removeIf(entry -> {
            ConnectionHandler connectionHandler = entry.getValue();
            return connectionHandler.getStatus() == ConnectionHandler.Status.TERMINATING ||
                    connectionHandler.getStatus() == ConnectionHandler.Status.TERMINATED ||
                    connectionHandler.checkIdleTimeout();
        });
        connectionPool.forEach((key, connectionHandler) -> {
            System.PingResponse response = RpcAutoConfiguration.getRpcClient().getSystemStub().process(System.PingResponse.class, HEARTBEAT_REQUEST, connectionHandler.getServiceName(), HEARTBEAT_CALLOPTION);
            if (!RespBaseUtil.isOK(response.getBase())) {
                logger.error("{} keep alive heart beat ping failed: code={}, {}", connectionHandler, response.getBase().getCode(), response.getBase().getMessage());
            }
        });
    }
}
