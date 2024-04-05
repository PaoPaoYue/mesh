package com.github.paopaoyue.mesh.rpc.api;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.AliasFor;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@ConditionalOnProperty(prefix = "mesh.rpc", name = "client-enabled", havingValue = "true")
@Order(2)
@Component
public @interface RpcCaller {

    @AliasFor(annotation = Component.class)
    String value() default "";

    String serviceName() default "";
}
