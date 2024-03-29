package com.github.paopaoyue.mesh.rpc.call;

import com.github.paopaoyue.mesh.rpc.config.ClientConfigCondition;
import com.github.paopaoyue.mesh.rpc.core.client.RpcClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Conditional;
import org.springframework.core.annotation.AliasFor;
import org.springframework.stereotype.Component;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@ConditionalOnBean(RpcClient.class)
@Conditional(ClientConfigCondition.class)
@Component
public @interface ServiceCaller {

    @AliasFor(annotation = Component.class)
    String value() default "";

    String serviceName() default "";
}
