package com.github.paopaoyue.mesh.rpc;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ComponentScan;

@Configuration
@ComponentScan(basePackages = "com.github.paopaoyue.mesh.rpc")
@EnableConfigurationProperties(Properties.class)
public class RpcAutoConfiguration {

    static {
        System.out.println("AutoConfiguration");
    }
}
