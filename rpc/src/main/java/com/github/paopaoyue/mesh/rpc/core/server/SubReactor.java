package com.github.paopaoyue.mesh.rpc.core.server;

import com.github.paopaoyue.mesh.rpc.RpcAutoConfiguration;
import com.github.paopaoyue.mesh.rpc.config.Properties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
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

        try {
            while (true) {
                selector.select();
                if (Thread.interrupted() || rpcServer.getStatus() != RpcServer.Status.RUNNING) {
                    break;
                }
                Set<SelectionKey> selected = selector.selectedKeys();
                for (SelectionKey key : selected) {
                    ((ConnectionHandler) key.attachment()).process();
                }
                selected.clear();
            }
        } catch (IOException e) {
            logger.error("Sub reactor running failure: {}", e.getMessage(), e);
            RpcAutoConfiguration.getRpcServer().shutdown();
        }
    }


    public void dispatch(SelectableChannel channel) {
        try {
            channel.configureBlocking(false);
            SelectionKey key = channel.register(selector, SelectionKey.OP_READ);
            key.attach(new ConnectionHandler(key));
            selector.wakeup();
        } catch (IOException e) {
            logger.error("Sub reactor dispatch failure: {}", e.getMessage(), e);
            RpcAutoConfiguration.getRpcServer().shutdown();
        }
    }

    public void closeConnection(SelectionKey key) {
        logger.info("Closing client connection: {}", key.attachment());
        try {
            ((SocketChannel) key.channel()).socket().close();
            key.channel().close();
        } catch (IOException e) {
            logger.error("Client connection close failure: {}", e.getMessage(), e);
        }
        key.cancel();

    }

    public void shutdown() {
        try {
            for (SelectionKey key : selector.keys()) {
                ConnectionHandler connectionHandler = (ConnectionHandler) key.attachment();
                connectionHandler.stop();
            }
            selector.close();
        } catch (IOException e) {
            logger.error("Sub reactor shutdown failure: {}", e.getMessage(), e);
        }
    }

    public Selector getSelector() {
        return selector;
    }


}
