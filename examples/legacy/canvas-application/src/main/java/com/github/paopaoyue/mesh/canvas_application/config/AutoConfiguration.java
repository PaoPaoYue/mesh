package com.github.paopaoyue.mesh.canvas_application.config;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@Configuration("canvas-application")
@EnableConfigurationProperties(Properties.class)
@ComponentScan(basePackages = "com.github.paopaoyue.mesh.canvas_application")
public class AutoConfiguration {

    private static final Logger logger = LoggerFactory.getLogger(AutoConfiguration.class);

    private static Properties prop;

    public static Properties getProp() {
        return prop;
    }

    @Autowired
    public void setProp(Properties prop) {
        AutoConfiguration.prop = prop;
    }

    @PostConstruct
    public void init() {
        logger.info("Canvas application auto configuration initialized with properties: {}", prop);
    }
}