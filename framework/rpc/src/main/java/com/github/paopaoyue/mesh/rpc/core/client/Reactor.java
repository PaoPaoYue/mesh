package com.github.paopaoyue.mesh.rpc.core.client;

import com.github.paopaoyue.mesh.rpc.RpcAutoConfiguration;
import com.github.paopaoyue.mesh.rpc.config.ServiceProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.ClosedSelectorException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

public class Reactor implements Runnable {

    private static Logger logger = LoggerFactory.getLogger(Reactor.class);

    private Map<String, ServiceProperties> serviceLookupTable;
    private CountDownLatch latch;
    private Selector selector;
    private Map<String, ConnectionHandler> connectionPool;
    private Lock lock;

    public Reactor(CountDownLatch latch) {
        this.serviceLookupTable = RpcAutoConfiguration.getProp().getClientServices().stream().collect(Collectors.toMap(ServiceProperties::getName, prop -> prop));
        this.latch = latch;
        this.connectionPool = new ConcurrentHashMap<>();
        this.lock = new ReentrantLock();
    }

    private static String getKeyForConnection(String serviceName, String tag) {
        return serviceName + "|" + tag;
    }

    @Override
    public void run() {
        RpcClient rpcClient = RpcAutoConfiguration.getRpcClient();

        try {
            this.selector = Selector.open();
        } catch (IOException e) {
            logger.error("Client reactor initialize failure, check configuration: {}", e.getMessage(), e);
            rpcClient.shutdown();
        }

        while (true) {
            try {
                selector.select();
            } catch (IOException e) {
                logger.error("Client reactor select() failed: {}", e.getMessage(), e);
                RpcAutoConfiguration.getRpcClient().shutdown();
                break;
            } catch (ClosedSelectorException e) {
                logger.error("Client reactor selector closed: {}", e.getMessage(), e);
                RpcAutoConfiguration.getRpcClient().shutdown();
                break;
            }
            if (Thread.interrupted() || rpcClient.getStatus() != RpcClient.Status.RUNNING) {
                if (rpcClient.getStatus() == RpcClient.Status.TERMINATED) {
                    break;
                }
                if (rpcClient.getStatus() == RpcClient.Status.TERMINATING && selector.keys().isEmpty()) {
                    logger.debug("All connection done, client reactor shutdown complete");
                    break;
                }
            }
            Set<SelectionKey> selected = selector.selectedKeys();
            for (SelectionKey key : selected) {
                if (key.isValid() && key.attachment() != null) {
                    try {
                        ((ConnectionHandler) key.attachment()).process();
                    } catch (Exception e) {
                        logger.error("Client connection handler process failure with unknown exception: {}", e.getMessage(), e);
                        ((ConnectionHandler) key.attachment()).stopNow(e);
                    }
                }
            }
            selected.clear();
        }

        try {
            this.selector.close();
        } catch (IOException e) {
            logger.error("Client reactor selector close failure: {}", e.getMessage(), e);
        }

        latch.countDown();

    }

    public ConnectionHandler createConnection(boolean keepAlive, String serviceName, String tag) {
        try {
            ServiceProperties prop = serviceLookupTable.get(serviceName);
            if (prop == null) {
                throw new IllegalArgumentException("Service not found: " + serviceName);
            }
            InetSocketAddress address = new InetSocketAddress(prop.getHost(), prop.getPort());
            SocketChannel channel = SocketChannel.open(address);
            channel.configureBlocking(false);
            SelectionKey key = channel.register(selector, SelectionKey.OP_READ);
            ConnectionHandler connectionHandler = new ConnectionHandler(this, keepAlive, serviceName, tag, key);
            logger.debug("Client new connection established: {}", connectionHandler);
            key.attach(connectionHandler);
            selector.wakeup();
            return connectionHandler;
        } catch (IOException e) {
            logger.error("Client reactor dispatch failure: {}", e.getMessage(), e);
        }
        return null;
    }

    public ConnectionHandler getOrCreateConnection(String serviceName, String tag, boolean isSystem) {
        String key = getKeyForConnection(serviceName, tag);
        ConnectionHandler connectionHandler = connectionPool.get(key);
        if (connectionHandler == null ||
                connectionHandler.getStatus() == ConnectionHandler.Status.TERMINATED ||
                connectionHandler.getStatus() == ConnectionHandler.Status.TERMINATING ||
                (!isSystem && connectionHandler.updateAndCheckIdleTimeout())) {
            lock.lock();
            try {
                connectionHandler = connectionPool.get(key);
                if (connectionHandler == null) {
                    connectionHandler = createConnection(true, serviceName, tag);
                    connectionPool.put(key, connectionHandler);
                } else if (connectionHandler.getStatus() == ConnectionHandler.Status.TERMINATING ||
                        connectionHandler.getStatus() == ConnectionHandler.Status.TERMINATED ||
                        (!isSystem && connectionHandler.updateAndCheckIdleTimeout())) {
                    // lazy remove and recreate
                    connectionPool.remove(key);
                    connectionHandler = createConnection(true, serviceName, tag);
                    connectionPool.put(key, connectionHandler);
                }
            } finally {
                lock.unlock();
            }
        }
        return connectionHandler;
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

    public Map<String, ConnectionHandler> getConnectionPool() {
        return connectionPool;
    }

}
