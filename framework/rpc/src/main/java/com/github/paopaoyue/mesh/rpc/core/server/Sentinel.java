package com.github.paopaoyue.mesh.rpc.core.server;

import com.github.paopaoyue.mesh.rpc.RpcAutoConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Set;
import java.util.TimerTask;

public class Sentinel extends TimerTask {

    private static final Logger logger = LoggerFactory.getLogger(Sentinel.class);

    private final Set<ConnectionHandler> connectionHandlers;

    public Sentinel() {
        this.connectionHandlers = new HashSet<>();
    }

    public void watch(ConnectionHandler connectionHandler) {
        connectionHandlers.add(connectionHandler);
    }

    @Override
    public void run() {
        if (RpcAutoConfiguration.getRpcServer().getStatus() != RpcServer.Status.RUNNING) {
            cancel();
            return;
        }
        connectionHandlers.removeIf(connectionHandler -> connectionHandler.getStatus() == ConnectionHandler.Status.TERMINATING ||
                connectionHandler.getStatus() == ConnectionHandler.Status.TERMINATED ||
                connectionHandler.checkKeepAliveTimeout());
    }
}
