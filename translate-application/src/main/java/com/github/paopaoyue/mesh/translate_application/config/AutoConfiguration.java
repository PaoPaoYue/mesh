package com.github.paopaoyue.mesh.translate_application.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@Configuration("translate-application")
@EnableConfigurationProperties(Properties.class)
@ComponentScan(basePackages = "com.github.paopaoyue.mesh.translate_application")
public class AutoConfiguration {
}
