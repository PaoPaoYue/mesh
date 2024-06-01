package com.github.paopaoyue.mesh.rpc.api;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@ConditionalOnProperty(prefix = "mesh.rpc", name = "client-enabled", havingValue = "true")
@Bean
public @interface MockRpcCaller {

    String value() default "";

    String serviceName() default "";
}
