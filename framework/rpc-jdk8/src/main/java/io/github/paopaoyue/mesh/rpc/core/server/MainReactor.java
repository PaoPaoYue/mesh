package io.github.paopaoyue.mesh.rpc.core.server;


import io.github.paopaoyue.mesh.rpc.RpcAutoConfiguration;
import io.github.paopaoyue.mesh.rpc.config.Properties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.ClosedSelectorException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.util.Set;

public class MainReactor implements Runnable {

    private static Logger logger = LoggerFactory.getLogger(MainReactor.class);
    private Selector selector;
    private SelectionKey key;
    private ServerSocketChannel socketChannel;


    public MainReactor() {
    }

    @Override
    public void run() {
        Properties prop = RpcAutoConfiguration.getProp();
        RpcServer rpcServer = RpcAutoConfiguration.getRpcServer();

        try {
            this.selector = Selector.open();
            this.socketChannel = ServerSocketChannel.open();
            this.socketChannel.socket().bind(new InetSocketAddress(prop.getServerService().getHost(), prop.getServerService().getPort()));
            this.socketChannel.configureBlocking(false);
            this.key = this.socketChannel.register(selector, SelectionKey.OP_ACCEPT);
        } catch (IOException e) {
            logger.error("Main reactor initialize failure, check configuration: {}", e.getMessage(), e);
            RpcAutoConfiguration.getRpcServer().shutdown();
        }

        while (true) {
            try {
                selector.select();
            } catch (IOException e) {
                logger.error("Main reactor select() failed: {}", e.getMessage(), e);
                RpcAutoConfiguration.getRpcServer().shutdown();
                break;
            } catch (ClosedSelectorException e) {
                logger.error("Main reactor selector closed: {}", e.getMessage(), e);
                RpcAutoConfiguration.getRpcServer().shutdown();
                break;
            }
            if (Thread.interrupted() || rpcServer.getStatus() != RpcServer.Status.RUNNING) {
                if (rpcServer.getStatus() == RpcServer.Status.TERMINATED) {
                    break;
                }
                if (rpcServer.getStatus() == RpcServer.Status.TERMINATING && selector.keys().isEmpty()) {
                    logger.debug("All connection done, Main reactor shutdown complete");
                    break;
                }
                ;
            }
            Acceptor acceptor = RpcAutoConfiguration.getRpcServer().getAcceptor();
            Set<SelectionKey> selected = selector.selectedKeys();
            for (SelectionKey key : selected) {
                if (key.isAcceptable()) {
                    try {
                        acceptor.accept(socketChannel.accept());
                    } catch (IOException e) {
                        logger.error("Main reactor server socket accept() failed: {}", e.getMessage(), e);
                        RpcAutoConfiguration.getRpcServer().shutdown();
                        break;
                    } catch (ClosedSelectorException e) {
                        logger.error("Main reactor server socket closed: {}", e.getMessage(), e);
                        RpcAutoConfiguration.getRpcServer().shutdown();
                        break;
                    }
                }
            }
            selected.clear();
        }

        try {
            selector.close();
        } catch (IOException e) {
            logger.error("Main reactor selector close failure: {}", e.getMessage(), e);
        }
    }

    public void shutdown() {
        try {
            socketChannel.socket().close();
            socketChannel.close();
            key.cancel();

            selector.wakeup();
        } catch (IOException e) {
            logger.error("Main reactor shutdown failure: {}", e.getMessage(), e);
        }
    }

}
