package com.github.paopaoyue.mesh.rpc.core.server;

import com.github.paopaoyue.mesh.rpc.RpcAutoConfiguration;
import com.github.paopaoyue.mesh.rpc.config.Properties;
import com.github.paopaoyue.mesh.rpc.stub.IServerStub;
import com.github.paopaoyue.mesh.rpc.stub.SystemServerStub;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Timer;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class RpcServer {

    private static Logger logger = LoggerFactory.getLogger(RpcServer.class);
    private SystemServerStub systemStub;
    private IServerStub serviceStub;
    private volatile Status status;
    private MainReactor mainReactor;
    private SubReactor[] subReactors;
    private Acceptor acceptor;
    private ExecutorService threadPool;
    private Timer timer;

    public RpcServer(IServerStub serviceStub) {
        Properties prop = RpcAutoConfiguration.getProp();

        this.status = Status.INIT;

        this.mainReactor = new MainReactor();
        this.subReactors = new SubReactor[prop.getServerNetworkThreads()];
        for (int i = 0; i < subReactors.length; i++) {
            this.subReactors[i] = new SubReactor();
        }
        this.acceptor = new Acceptor();
        this.threadPool = Executors.newFixedThreadPool(1 + prop.getServerNetworkThreads() + prop.getServerWorkerThreads()); // main reactor + sub reactors + worker threads

        this.timer = new Timer();

        systemStub = new SystemServerStub();
        this.serviceStub = serviceStub;
    }

    public void registerHandler() {

    }

    public void start() {
        logger.info("Starting rpc server...");
        Properties prop = RpcAutoConfiguration.getProp();

        status = Status.RUNNING;
        threadPool.submit(mainReactor);
        for (SubReactor subReactor : subReactors) {
            threadPool.submit(subReactor);
        }
        timer.scheduleAtFixedRate(new Sentinel(), 0, prop.getKeepAliveInterval());
        logger.info("Rpc server up!!!");
    }

    @PreDestroy
    public void shutdown() {
        logger.info("Shutting down rpc server...");
        Properties prop = RpcAutoConfiguration.getProp();

        status = Status.TERMINATING;
        try {
            timer.cancel();
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
        logger.info("rpc server shutdown gracefully");
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
