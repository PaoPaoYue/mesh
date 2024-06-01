package com.github.paopaoyue.mesh.dictionary_application;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;

import java.util.Arrays;

@SpringBootApplication
public class DictionaryApplication {

    public static void main(String[] args) {
        ConfigurableApplicationContext context = SpringApplication.run(DictionaryApplication.class, args);
        if (Arrays.asList(args).contains("--spring.profiles.active=client")) {
            JavaFxLauncher.springContext = context;
            JavaFxLauncher.launch(JavaFxLauncher.class, args);
        }
    }

}
