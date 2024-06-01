package com.github.paopaoyue.mesh.dictionary_application.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@Configuration("dictionary-application")
@EnableConfigurationProperties(Properties.class)
@ComponentScan(basePackages = "com.github.paopaoyue.mesh.dictionary_application")
public class AutoConfiguration {
}
