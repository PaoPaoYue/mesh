package io.github.paopaoyue.mesh.rpc.core.client;

import io.github.paopaoyue.mesh.rpc.RpcAutoConfiguration;
import io.github.paopaoyue.mesh.rpc.config.Properties;
import io.github.paopaoyue.mesh.rpc.config.ServiceProperties;
import io.github.paopaoyue.mesh.rpc.stub.SystemClientStub;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Map;
import java.util.Timer;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class RpcClient {

    private static Logger logger = LogManager.getLogger(RpcClient.class);

    Map<String, ServiceProperties> servicePropMap;
    private SystemClientStub systemStub;
    private volatile Status status;
    private CountDownLatch latch;
    private Reactor reactor;
    private Sender sender;
    private Sentinel sentinel;

    public RpcClient() {
        Properties prop = RpcAutoConfiguration.getProp();

        this.status = Status.INIT;
        this.latch = new CountDownLatch(1);

        this.reactor = new Reactor(latch);
        this.sender = new Sender(reactor);
        this.sentinel = new Sentinel();

        this.servicePropMap = prop.getClientServices().stream().collect(Collectors.toMap(ServiceProperties::getName, s -> s));
        this.systemStub = new SystemClientStub();
    }

    @PostConstruct
    public void start() {
        logger.info("Starting rpc client...");
        Properties prop = RpcAutoConfiguration.getProp();

        status = Status.RUNNING;
        new Thread(reactor).start();
        new Timer().scheduleAtFixedRate(sentinel, prop.getKeepAliveInterval() * 1000L, prop.getKeepAliveInterval() * 1000L);
        logger.info("Rpc client up!!!");
    }

    @PreDestroy
    public void shutdown() {
        if (status != Status.RUNNING) {
            return;
        }
        logger.info("Shutting down rpc client...");
        Properties prop = RpcAutoConfiguration.getProp();

        status = Status.TERMINATING;
        try {
            reactor.shutdown();
            Thread.sleep(1000); // ensure all the upcoming requests are processed
            boolean allDone = latch.await(prop.getClientShutDownTimeout() - 1, TimeUnit.SECONDS);
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
