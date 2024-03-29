package com.github.paopaoyue.mesh.rpc.stub;

import com.github.paopaoyue.mesh.rpc.core.server.RpcServer;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.AliasFor;
import org.springframework.stereotype.Component;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@ConditionalOnProperty(prefix = "mesh.rpc.server-service", name = "name")
@ConditionalOnBean(RpcServer.class)
@Component
public @interface ServiceServerStub {

    @AliasFor(annotation = Component.class)
    String value() default "";

    @AliasFor(annotation = ConditionalOnProperty.class, attribute = "havingValue")
    String serviceName() default "";
}
