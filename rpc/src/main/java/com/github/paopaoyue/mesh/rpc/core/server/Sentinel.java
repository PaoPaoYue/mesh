package com.github.paopaoyue.mesh.rpc.core.server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.TimerTask;

public class Sentinel extends TimerTask {

    private static final Logger logger = LoggerFactory.getLogger(Sentinel.class);

    private final List<ConnectionHandler> connectionHandlers;

    public Sentinel() {
        this.connectionHandlers = new ArrayList<>();
    }

    public void watch(ConnectionHandler connectionHandler) {
        connectionHandlers.add(connectionHandler);
    }

    @Override
    public void run() {
        for (ConnectionHandler connectionHandler : connectionHandlers) {
            if (connectionHandler.getStatus() == ConnectionHandler.Status.TERMINATING || connectionHandler.getStatus() == ConnectionHandler.Status.TERMINATED) {
                this.connectionHandlers.remove(connectionHandler);
            } else if (!connectionHandler.checkAlive()) {
                this.connectionHandlers.remove(connectionHandler);
            }
        }
    }

}
