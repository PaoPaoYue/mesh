package com.github.paopaoyue.mesh.rpc.config;

import com.github.paopaoyue.mesh.rpc.call.ServiceCaller;
import com.github.paopaoyue.mesh.rpc.stub.ServiceClientStub;
import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;

import java.util.Map;
import java.util.Objects;

public class ClientConfigCondition implements Condition {
    @Override
    public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
        Map<String, Object> attributes = metadata.getAnnotationAttributes(ServiceCaller.class.getName());
        if (attributes == null) {
            attributes = metadata.getAnnotationAttributes(ServiceClientStub.class.getName());
        }
        if (attributes == null) {
            return false;
        }
        String serviceName = (String) attributes.get("serviceName");
        Properties prop = Objects.requireNonNull(context.getBeanFactory()).getBean(Properties.class);
        return prop.getClientServices().stream().anyMatch(serviceProp -> serviceProp.getName().equals(serviceName));
    }
}
