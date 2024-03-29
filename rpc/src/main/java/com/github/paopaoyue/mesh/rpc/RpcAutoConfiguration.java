package com.github.paopaoyue.mesh.rpc;

import com.github.paopaoyue.mesh.rpc.config.Properties;
import com.github.paopaoyue.mesh.rpc.core.client.RpcClient;
import com.github.paopaoyue.mesh.rpc.core.server.RpcServer;
import com.github.paopaoyue.mesh.rpc.stub.IClientStub;
import com.github.paopaoyue.mesh.rpc.stub.IServerStub;
import com.github.paopaoyue.mesh.rpc.stub.ServiceClientStub;
import com.github.paopaoyue.mesh.rpc.stub.ServiceServerStub;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Configuration
@EnableConfigurationProperties(Properties.class)
public class RpcAutoConfiguration {

    private static final Logger logger = LoggerFactory.getLogger(RpcAutoConfiguration.class);
    private static ApplicationContext context;
    private static Properties prop;
    private static RpcServer rpcServer;
    private static RpcClient rpcClient;

    public static Properties getProp() {
        return prop;
    }

    @Autowired
    public void setProp(Properties prop) {
        RpcAutoConfiguration.prop = prop;
    }

    public static RpcServer getRpcServer() {
        return rpcServer;
    }

    public static RpcClient getRpcClient() {
        return rpcClient;
    }

    @PostConstruct
    public void afterConstruct() {
        logger.info("rpc framework starts with properties:" + prop);
        if (rpcServer != null) {
            rpcServer.start();
        }
        if (rpcClient != null) {
            rpcClient.start();
        }
    }

    @Autowired
    public void setContext(ApplicationContext context) {
        RpcAutoConfiguration.context = context;
    }

    @Bean
    @ConditionalOnBean
    @ConditionalOnProperty(prefix = "mesh.rpc", name = "serverEnabled", havingValue = "true")
    public RpcServer rpcServer() {
        try {
            Map<String, Object> stubs = context.getBeansWithAnnotation(ServiceServerStub.class);
            IServerStub serviceServerStub = stubs.values().stream().findAny().map(IServerStub.class::cast).orElse(null);
            if (serviceServerStub == null) {
                throw new RuntimeException("No service stub found, please add @ServiceServerStub to your service stub implementation");
            }
            rpcServer = new RpcServer(serviceServerStub);
        } catch (ClassCastException e) {
            throw new RuntimeException("Please add @ServiceServerStub to your service stub implementing IServerStub", e);
        } catch (Exception e) {
            throw new RuntimeException("Rpc server initialization failure", e);
        }
        return rpcServer;
    }

    @Bean
    @ConditionalOnProperty(prefix = "mesh.rpc", name = "clientEnabled", havingValue = "true")
    public RpcClient rpcClient() {
        try {
            Map<String, Object> stubs = context.getBeansWithAnnotation(ServiceClientStub.class);
            Set<IClientStub> serviceClientStubs = stubs.values().stream().map(IClientStub.class::cast).collect(Collectors.toSet());
            if (serviceClientStubs.isEmpty()) {
                throw new RuntimeException("No service stub found, please add @ServiceClientStub to your service stub implementation");
            }
            rpcClient = new RpcClient(serviceClientStubs);
        } catch (ClassCastException e) {
            throw new RuntimeException("Please add @ServiceClientStub to your service stub implementing IClientStub", e);
        } catch (Exception e) {
            throw new RuntimeException("Rpc client initialization failure", e);
        }
        return rpcClient;
    }
}
