package com.github.paopaoyue.mesh.rpc.service;

import com.github.paopaoyue.mesh.rpc.config.ClientConfigCondition;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Conditional;
import org.springframework.core.annotation.AliasFor;
import org.springframework.stereotype.Component;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@ConditionalOnProperty(prefix = "mesh.rpc.server-service", name = "name")
@Conditional(ClientConfigCondition.class)
@Component
public @interface ServiceHandler {

    @AliasFor(annotation = Component.class)
    String value() default "";

    @AliasFor(annotation = ConditionalOnProperty.class, attribute = "havingValue")
    String serviceName() default "";
}
