package com.github.paopaoyue.mesh.rpc.core.client;

import com.github.paopaoyue.mesh.rpc.config.Properties;
import com.github.paopaoyue.mesh.rpc.config.RpcAutoConfiguration;
import com.github.paopaoyue.mesh.rpc.config.ServiceProperties;
import com.github.paopaoyue.mesh.rpc.stub.SystemClientStub;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Timer;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class RpcClient {

    private static Logger logger = LoggerFactory.getLogger(RpcClient.class);

    Map<String, ServiceProperties> servicePropMap;
    private SystemClientStub systemStub;
    private volatile Status status;
    private CountDownLatch latch;
    private Reactor reactor;
    private Sender sender;
    private Timer timer;

    public RpcClient() {
        Properties prop = RpcAutoConfiguration.getProp();

        this.status = Status.INIT;
        this.latch = new CountDownLatch(1);

        this.reactor = new Reactor(latch);
        this.sender = new Sender(reactor);

        this.timer = new Timer();

        this.servicePropMap = prop.getClientServices().stream().collect(Collectors.toMap(ServiceProperties::getName, s -> s));
        this.systemStub = new SystemClientStub();
    }

    @PostConstruct
    public void start() {
        logger.info("Starting rpc client...");
        Properties prop = RpcAutoConfiguration.getProp();

        status = Status.RUNNING;
        new Thread(reactor).start();
        timer.scheduleAtFixedRate(new Sentinel(), 0, prop.getKeepAliveInterval() * 1000L);
        logger.info("Rpc client up!!!");
    }

    @PreDestroy
    public void shutdown() {
        logger.info("Shutting down rpc client...");
        Properties prop = RpcAutoConfiguration.getProp();

        status = Status.TERMINATING;
        try {
            timer.cancel();
            timer.purge();
            Thread.sleep(1000); // ensure all the upcoming requests are processed
            reactor.shutdown();
            boolean allDone = latch.await(prop.getClientShutDownTimeout(), TimeUnit.SECONDS);
            if (!allDone) {
                logger.error("Client connection not clear while shutdown timeout, force shutdown!!!");
            }
        } catch (Exception e) {
            logger.error("Rpc client shutdown failure: {}", e.getMessage(), e);
            return;
        }
        status = Status.TERMINATED;
        logger.info("rpc client shutdown complete");
    }

    public Status getStatus() {
        return status;
    }

    public Reactor getReactor() {
        return reactor;
    }

    public Sender getSender() {
        return sender;
    }

    public SystemClientStub getSystemStub() {
        return systemStub;
    }

    public enum Status {
        INIT,
        RUNNING,
        TERMINATING,
        TERMINATED
    }
}
