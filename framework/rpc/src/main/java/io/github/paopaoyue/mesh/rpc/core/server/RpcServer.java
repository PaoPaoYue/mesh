package io.github.paopaoyue.mesh.rpc.core.server;

import io.github.paopaoyue.mesh.rpc.RpcAutoConfiguration;
import io.github.paopaoyue.mesh.rpc.config.Properties;
import io.github.paopaoyue.mesh.rpc.stub.IServerStub;
import io.github.paopaoyue.mesh.rpc.stub.SystemServerStub;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Timer;
import java.util.concurrent.*;

public class RpcServer {

    private static Logger logger = LogManager.getLogger(RpcServer.class);
    private SystemServerStub systemStub;
    private IServerStub serviceStub;
    private volatile Status status;
    private MainReactor mainReactor;
    private SubReactor[] subReactors;
    private Acceptor acceptor;
    private ExecutorService threadPool;
    private Sentinel sentinel;

    public RpcServer(IServerStub serviceStub) {
        Properties prop = RpcAutoConfiguration.getProp();

        this.status = Status.INIT;

        this.mainReactor = new MainReactor();
        this.subReactors = new SubReactor[prop.getServerNetworkThreads()];
        for (int i = 0; i < subReactors.length; i++) {
            this.subReactors[i] = new SubReactor();
        }
        this.sentinel = new Sentinel();
        this.acceptor = new Acceptor(this.subReactors, this.sentinel);

        this.threadPool = new ThreadPoolExecutor(1 + prop.getServerNetworkThreads() + prop.getServerWorkerThreads(),
                prop.getServerWorkerThreads() != 0 ? 1 + prop.getServerNetworkThreads() + prop.getServerWorkerThreads() : Integer.MAX_VALUE, // main reactor + sub reactors + worker threads
                prop.getServerWorkerKeepAliveTimeout(), TimeUnit.SECONDS,
                prop.getServerWorkerThreads() != 0 ? new LinkedBlockingQueue<>() : new SynchronousQueue<>());

        systemStub = new SystemServerStub();
        this.serviceStub = serviceStub;
    }

    public void registerHandler() {

    }

    @PostConstruct
    public void start() {
        logger.info("Starting rpc server...");
        Properties prop = RpcAutoConfiguration.getProp();

        status = Status.RUNNING;
        threadPool.execute(mainReactor);
        for (SubReactor subReactor : subReactors) {
            threadPool.execute(subReactor);
        }
        new Timer().scheduleAtFixedRate(sentinel, prop.getKeepAliveInterval() * 1000L, prop.getKeepAliveInterval() * 1000L);
        logger.info("Rpc server up!!!");
    }

    @PreDestroy
    public void shutdown() {
        if (status != Status.RUNNING) {
            return;
        }
        logger.info("Shutting down rpc server...");
        Properties prop = RpcAutoConfiguration.getProp();

        status = Status.TERMINATING;
        try {
            mainReactor.shutdown();
            for (SubReactor subReactor : subReactors) {
                subReactor.shutdown();
            }
            Thread.sleep(1000); // ensure all the upcoming requests are submitted to the thread pool
            threadPool.shutdown();
            boolean allDone = threadPool.awaitTermination(prop.getServerShutDownTimeout() - 1, TimeUnit.SECONDS); // wait for all the worker to finish
            if (!allDone) {
                logger.error("Server connection not clear while shutdown timeout, force shutdown!!!");
            }
        } catch (Exception e) {
            logger.error("Rpc server shutdown failure: {}", e.getMessage(), e);
            return;
        }
        status = Status.TERMINATED;
        logger.info("rpc server shutdown complete");
    }

    public Status getStatus() {
        return status;
    }

    public MainReactor getMainReactor() {
        return mainReactor;
    }

    public SubReactor[] getSubReactors() {
        return subReactors;
    }

    public Acceptor getAcceptor() {
        return acceptor;
    }

    public ExecutorService getThreadPool() {
        return threadPool;
    }

    public SystemServerStub getSystemStub() {
        return systemStub;
    }

    public IServerStub getServiceStub() {
        return serviceStub;
    }

    public enum Status {
        INIT,
        RUNNING,
        TERMINATING,
        TERMINATED
    }
}
