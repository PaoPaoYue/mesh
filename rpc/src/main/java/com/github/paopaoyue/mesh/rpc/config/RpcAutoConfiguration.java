package com.github.paopaoyue.mesh.rpc.config;

import com.github.paopaoyue.mesh.rpc.api.RpcCaller;
import com.github.paopaoyue.mesh.rpc.core.client.RpcClient;
import com.github.paopaoyue.mesh.rpc.core.server.RpcServer;
import com.github.paopaoyue.mesh.rpc.service.RpcService;
import com.github.paopaoyue.mesh.rpc.stub.IClientStub;
import com.github.paopaoyue.mesh.rpc.stub.IServerStub;
import com.github.paopaoyue.mesh.rpc.stub.ServiceClientStub;
import com.github.paopaoyue.mesh.rpc.stub.ServiceServerStub;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.autoconfigure.AutoConfigureOrder;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Field;
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

    @Bean
    @ConditionalOnProperty(prefix = "mesh.rpc", name = "server-enabled", havingValue = "true")
    public RpcServer rpcServer() {
        Map<String, Object> stubs = context.getBeansWithAnnotation(ServiceServerStub.class);
        ServiceProperties serviceProperties = prop.getServerService();
        if (serviceProperties == null) {
            throw new BeanCreationException("No service properties found, please add server service properties to your configuration");
        }
        Map<String, IServerStub> serviceServerStubs;
        try {
            serviceServerStubs = stubs.values().stream().collect(Collectors.toMap(o -> o.getClass().getAnnotation(ServiceServerStub.class).serviceName(), IServerStub.class::cast));
            rpcServer = new RpcServer(serviceServerStubs.get(serviceProperties.getName()));
        } catch (ClassCastException e) {
            throw new BeanCreationException("Please add @ServiceServerStub to your service stub implementing IServerStub", e);
        } catch (Exception e) {
            throw new BeanCreationException("Error creating rpc server", e);
        }
        if (serviceServerStubs.isEmpty()) {
            throw new BeanCreationException("No service stub found, please add @ServiceServerStub to your service stub implementation");
        }
        if (!serviceServerStubs.containsKey(serviceProperties.getName())) {
            throw new BeanCreationException("Service stub not found for service properties, please check the service name in @ServiceClientStub");
        }
        return rpcServer;
    }

    @Bean
    @ConditionalOnProperty(prefix = "mesh.rpc", name = "client-enabled", havingValue = "true")
    public RpcClient rpcClient() {
        Map<String, Object> stubs = context.getBeansWithAnnotation(ServiceClientStub.class);
        Map<String, ServiceProperties> serviceProperties = prop.getClientServices().stream().collect(Collectors.toMap(ServiceProperties::getName, s -> s));
        if (serviceProperties.isEmpty()) {
            throw new BeanCreationException("No service properties found, please add client service properties to your configuration");
        }
        Map<String, IClientStub> serviceClientStubs;
        try {
            serviceClientStubs = stubs.values().stream().collect(Collectors.toMap(o -> o.getClass().getAnnotation(ServiceClientStub.class).serviceName(), IClientStub.class::cast));
            rpcClient = new RpcClient();
        } catch (ClassCastException e) {
            throw new BeanCreationException("Please add @ServiceClientStub to your service stub implementing IClientStub", e);
        } catch (Exception e) {
            throw new BeanCreationException("Error creating rpc client", e);
        }
        if (serviceClientStubs.isEmpty()) {
            throw new BeanCreationException("No service stub found, please add @ServiceClientStub to your service stub implementation");
        }
        if (serviceProperties.keySet().stream().anyMatch(s -> !serviceClientStubs.containsKey(s))) {
            throw new BeanCreationException("Service stub not found for service properties, please check the service name in @ServiceClientStub");
        }
        return rpcClient;
    }

    @Component
    public static class PostProcessor implements BeanPostProcessor, ApplicationContextAware {

        @Override
        public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
            context = applicationContext;
        }

        @Override
        public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
            Object injected;
            injected = injectRpcStub(bean);
            injected = injectRpcService(injected);
            return injected;
        }

        private Object injectRpcStub(Object bean) throws BeansException {
            RpcCaller annotation = bean.getClass().getAnnotation(RpcCaller.class);
            if (annotation == null) {
                return bean;
            }
            if (annotation.serviceName().isEmpty()) {
                throw new BeanInitializationException("Please specify service name in @RpcCaller");
            }
            Map<String, Object> stubs = context.getBeansWithAnnotation(ServiceClientStub.class);
            try {
                IClientStub matched = (IClientStub) stubs.values().stream()
                        .filter(o -> o.getClass().getAnnotation(ServiceClientStub.class).serviceName().equals(annotation.serviceName()))
                        .findFirst()
                        .orElseThrow(() -> new BeanInitializationException("Caller stub not found for service name: " + annotation.serviceName()));
                Field field = ReflectionUtils.findField(bean.getClass(), "clientStub", IClientStub.class);
                if (field == null) {
                    throw new BeanInitializationException("Please add 'clientStub' field to your rpc client interface implementation");
                }
                field.setAccessible(true);
                ReflectionUtils.setField(field, bean, matched);
                return bean;
            } catch (ClassCastException e) {
                throw new BeanInitializationException("Please add @ServiceClientStub to your service stub implementing IClientStub", e);
            } catch (BeanInitializationException e) {
                throw e;
            } catch (Exception e) {
                throw new BeanInitializationException("Error injecting rpc stub", e);
            }
        }

        private Object injectRpcService(Object bean) throws BeansException {
            ServiceServerStub annotation = bean.getClass().getAnnotation(ServiceServerStub.class);
            if (annotation == null) {
                return bean;
            }
            if (annotation.serviceName().isEmpty()) {
                throw new BeanInitializationException("Please specify service name in @ServiceServerStub");
            }
            Map<String, Object> stubs = context.getBeansWithAnnotation(RpcService.class);
            try {
                Object matched = stubs.values().stream()
                        .filter(o -> o.getClass().getAnnotation(RpcService.class).serviceName().equals(annotation.serviceName()))
                        .findFirst()
                        .orElseThrow(() -> new BeanInitializationException("Service not found for service name: " + annotation.serviceName()));
                Field field = ReflectionUtils.findField(bean.getClass(), "service");
                if (field == null) {
                    throw new BeanInitializationException("Please add 'service' field to your rpc server stub implementation");
                }
                field.setAccessible(true);
                ReflectionUtils.setField(field, bean, matched);
                return bean;
            } catch (BeanInitializationException e) {
                throw e;
            } catch (Exception e) {
                throw new BeanInitializationException("Error injecting rpc service", e);
            }
        }
    }

}
