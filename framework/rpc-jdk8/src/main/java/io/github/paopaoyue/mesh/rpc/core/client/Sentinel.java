package io.github.paopaoyue.mesh.rpc.core.client;

import io.github.paopaoyue.mesh.rpc.RpcAutoConfiguration;
import io.github.paopaoyue.mesh.rpc.api.CallOption;
import io.github.paopaoyue.mesh.rpc.proto.System;
import io.github.paopaoyue.mesh.rpc.util.RespBaseUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.time.Duration;
import java.util.Map;
import java.util.TimerTask;

public class Sentinel extends TimerTask {

    private static final Logger logger = LogManager.getLogger(Sentinel.class);

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
                logger.error("{} keep alive heartbeat ping failed: code={}, {}", connectionHandler, response.getBase().getCode(), response.getBase().getMessage());
            }
        });
    }
}
