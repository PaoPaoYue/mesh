package io.github.paopaoyue.mesh.rpc.core.server;

import io.github.paopaoyue.mesh.rpc.RpcAutoConfiguration;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.HashSet;
import java.util.Set;
import java.util.TimerTask;

public class Sentinel extends TimerTask {

    private static final Logger logger = LogManager.getLogger(Sentinel.class);

    private final Set<ConnectionHandler> connectionHandlers;

    public Sentinel() {
        this.connectionHandlers = new HashSet<>();
    }

    public synchronized void watch(ConnectionHandler connectionHandler) {
        connectionHandlers.add(connectionHandler);
    }

    @Override
    public void run() {
        if (RpcAutoConfiguration.getRpcServer().getStatus() != RpcServer.Status.RUNNING) {
            cancel();
            return;
        }
        synchronized (this) {
            connectionHandlers.removeIf(connectionHandler -> connectionHandler.getStatus() == ConnectionHandler.Status.TERMINATING ||
                    connectionHandler.getStatus() == ConnectionHandler.Status.TERMINATED ||
                    connectionHandler.checkKeepAliveTimeout());
        }
    }
}
