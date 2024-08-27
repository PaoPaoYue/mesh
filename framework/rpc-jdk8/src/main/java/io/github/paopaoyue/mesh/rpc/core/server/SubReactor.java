package io.github.paopaoyue.mesh.rpc.core.server;

import io.github.paopaoyue.mesh.rpc.RpcAutoConfiguration;
import io.github.paopaoyue.mesh.rpc.config.Properties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.channels.ClosedSelectorException;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.Set;

public class SubReactor implements Runnable {

    private static Logger logger = LoggerFactory.getLogger(SubReactor.class);
    private Selector selector;


    public SubReactor() {
    }

    @Override
    public void run() {
        Properties prop = RpcAutoConfiguration.getProp();
        RpcServer rpcServer = RpcAutoConfiguration.getRpcServer();

        try {
            this.selector = Selector.open();
        } catch (IOException e) {
            logger.error("Sub reactor initialize failure, check configuration: {}", e.getMessage(), e);
            RpcAutoConfiguration.getRpcServer().shutdown();
        }

        while (true) {
            try {
                selector.select();
            } catch (IOException e) {
                logger.error("Sub reactor select() failure: {}", e.getMessage(), e);
                RpcAutoConfiguration.getRpcServer().shutdown();
            } catch (ClosedSelectorException e) {
                logger.error("Sub reactor selector closed: {}", e.getMessage(), e);
                RpcAutoConfiguration.getRpcServer().shutdown();
                break;
            }
            if (Thread.interrupted() || rpcServer.getStatus() != RpcServer.Status.RUNNING) {
                if (rpcServer.getStatus() == RpcServer.Status.TERMINATED) {
                    break;
                }
                if (rpcServer.getStatus() == RpcServer.Status.TERMINATING && selector.keys().isEmpty()) {
                    logger.debug("All connection done, Sub reactor shutdown complete");
                    break;
                }
            }
            Set<SelectionKey> selected = selector.selectedKeys();
            for (SelectionKey key : selected) {
                if (key.isValid() && key.attachment() != null) {
                    try {
                        ((ConnectionHandler) key.attachment()).process();
                    } catch (Exception e) {
                        logger.error("Server connection handler process failure with unknown exception: {}", e.getMessage(), e);
                        ((ConnectionHandler) key.attachment()).stopNow();
                    }
                }
            }
            selected.clear();
        }

        try {
            selector.close();
        } catch (IOException e) {
            logger.error("Sub reactor selector close failure: {}", e.getMessage(), e);
        }
    }


    public ConnectionHandler dispatch(SelectableChannel channel) {
        try {
            channel.configureBlocking(false);
            SelectionKey key = channel.register(selector, SelectionKey.OP_READ);
            ConnectionHandler connectionHandler = new ConnectionHandler(this, key);
            logger.debug("Server new connection established: {}", connectionHandler);
            key.attach(connectionHandler);
            selector.wakeup();
            return connectionHandler;
        } catch (IOException e) {
            logger.error("Sub reactor dispatch failure: {}", e.getMessage(), e);
            RpcAutoConfiguration.getRpcServer().shutdown();
        }
        return null;
    }

    public void shutdown() {
        for (SelectionKey key : selector.keys()) {
            ConnectionHandler connectionHandler = (ConnectionHandler) key.attachment();
            connectionHandler.stop();
        }
        selector.wakeup();
    }

    public Selector getSelector() {
        return selector;
    }
}
