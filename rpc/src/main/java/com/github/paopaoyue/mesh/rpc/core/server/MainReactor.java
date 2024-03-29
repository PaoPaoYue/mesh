package com.github.paopaoyue.mesh.rpc.core.server;


import com.github.paopaoyue.mesh.rpc.RpcAutoConfiguration;
import com.github.paopaoyue.mesh.rpc.config.Properties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.util.Set;

public class MainReactor implements Runnable {

    private static Logger logger = LoggerFactory.getLogger(MainReactor.class);
    private Selector selector;
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
            this.socketChannel.register(selector, SelectionKey.OP_ACCEPT);
        } catch (IOException e) {
            logger.error("Main reactor initialize failure, check configuration: {}", e.getMessage(), e);
            RpcAutoConfiguration.getRpcServer().shutdown();
        }

        try {
            while (true) {
                selector.select();
                if (Thread.interrupted() || rpcServer.getStatus() != RpcServer.Status.RUNNING) {
                    break;
                }
                Set<SelectionKey> selected = selector.selectedKeys();
                for (SelectionKey key : selected) {
                    if (key.isAcceptable()) {
                        Acceptor acceptor = RpcAutoConfiguration.getRpcServer().getAcceptor();
                        acceptor.accept(key);
                    }
                }
                selected.clear();
            }
        } catch (IOException e) {
            logger.error("Main reactor running failure: {}", e.getMessage(), e);
            RpcAutoConfiguration.getRpcServer().shutdown();
        }
    }

    public void shutdown() {
        try {
            socketChannel.socket().close();
            socketChannel.close();
            selector.close();
        } catch (IOException e) {
            logger.error("Main reactor shutdown failure: {}", e.getMessage(), e);
        }
    }

    public Selector getSelector() {
        return selector;
    }

    public ServerSocketChannel getSocketChannel() {
        return socketChannel;
    }
}
