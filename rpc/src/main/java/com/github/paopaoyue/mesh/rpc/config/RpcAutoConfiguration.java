package com.github.paopaoyue.mesh.rpc.config;

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
import org.springframework.boot.autoconfigure.AutoConfigureOrder;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

import java.util.Map;
import java.util.stream.Collectors;

@AutoConfigureOrder(Ordered.LOWEST_PRECEDENCE)
@Configuration
@ComponentScan(basePackages = "com.github.paopaoyue.mesh")
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
        logger.info("rpc framework starts with properties:" + prop.toString());
    }

    @Autowired
    public void setContext(ApplicationContext context) {
        RpcAutoConfiguration.context = context;
    }

    @Bean
    @ConditionalOnProperty(prefix = "mesh.rpc", name = "server-enabled", havingValue = "true")
    public RpcServer rpcServer() {
        try {
            Map<String, Object> stubs = context.getBeansWithAnnotation(ServiceServerStub.class);
            Map<String, IServerStub> serviceServerStubs = stubs.values().stream().collect(Collectors.toMap(o -> o.getClass().getAnnotation(ServiceServerStub.class).serviceName(), IServerStub.class::cast));
            ServiceProperties serviceProperties = prop.getServerService();
            if (serviceServerStubs.isEmpty()) {
                throw new RuntimeException("No service stub found, please add @ServiceServerStub to your service stub implementation");
            }
            if (serviceProperties == null) {
                throw new RuntimeException("No service properties found, please add server service properties to your configuration");
            }
            if (!serviceServerStubs.containsKey(serviceProperties.getName())) {
                throw new RuntimeException("Service stub not found for service properties, please check the service name in @ServiceClientStub");
            }
            rpcServer = new RpcServer(serviceServerStubs.get(serviceProperties.getName()));
        } catch (ClassCastException e) {
            throw new RuntimeException("Please add @ServiceServerStub to your service stub implementing IServerStub", e);
        } catch (Exception e) {
            throw new RuntimeException("Rpc server initialization failure", e);
        }
        return rpcServer;
    }

    @Bean
    @ConditionalOnProperty(prefix = "mesh.rpc", name = "client-enabled", havingValue = "true")
    public RpcClient rpcClient() {
        try {
            Map<String, Object> stubs = context.getBeansWithAnnotation(ServiceClientStub.class);
            Map<String, IClientStub> serviceClientStubs = stubs.values().stream().collect(Collectors.toMap(o -> o.getClass().getAnnotation(ServiceClientStub.class).serviceName(), IClientStub.class::cast));
            Map<String, ServiceProperties> serviceProperties = prop.getClientServices().stream().collect(Collectors.toMap(ServiceProperties::getName, s -> s));
            if (serviceClientStubs.isEmpty()) {
                throw new RuntimeException("No service stub found, please add @ServiceClientStub to your service stub implementation");
            }
            if (serviceProperties.isEmpty()) {
                throw new RuntimeException("No service properties found, please add client service properties to your configuration");
            }
            if (serviceProperties.keySet().stream().anyMatch(s -> !serviceClientStubs.containsKey(s))) {
                throw new RuntimeException("Service stub not found for service properties, please check the service name in @ServiceClientStub");
            }
            rpcClient = new RpcClient();
        } catch (ClassCastException e) {
            throw new RuntimeException("Please add @ServiceClientStub to your service stub implementing IClientStub", e);
        } catch (Exception e) {
            throw new RuntimeException("Rpc client initialization failure", e);
        }
        return rpcClient;
    }
}
