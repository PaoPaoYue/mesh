package io.github.paopaoyue.mesh.rpc.core.server;

import io.github.paopaoyue.mesh.rpc.RpcAutoConfiguration;
import io.github.paopaoyue.mesh.rpc.config.Properties;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.channels.ClosedSelectorException;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class SubReactor implements Runnable {

    private static Logger logger = LogManager.getLogger(SubReactor.class);
    private Selector selector;
    private Lock lock;

    public SubReactor() {
        lock = new ReentrantLock();
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
                // prevent blocking register new connection
                lock.lock();
                lock.unlock();
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
        lock.lock();
        try {
            channel.configureBlocking(false);
            selector.wakeup();
            SelectionKey key = channel.register(selector, SelectionKey.OP_READ);
            ConnectionHandler connectionHandler = new ConnectionHandler(this, key);
            logger.debug("Server new connection established: {}", connectionHandler);
            key.attach(connectionHandler);
            selector.wakeup();
            return connectionHandler;
        } catch (IOException e) {
            logger.error("Sub reactor dispatch failure: {}", e.getMessage(), e);
            RpcAutoConfiguration.getRpcServer().shutdown();
        } finally {
            lock.unlock();
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
