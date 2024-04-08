package com.github.paopaoyue.mesh.dictionary_application;

import com.github.paopaoyue.mesh.rpc.config.Properties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;

@SpringBootApplication
public class DictionaryApplication {

    public static void main(String[] args) {
        ConfigurableApplicationContext context = SpringApplication.run(DictionaryApplication.class, args);
        if (context.getBean(Properties.class).isClientEnabled()) {
            JavaFxLauncher.springContext = context;
            JavaFxLauncher.launch(JavaFxLauncher.class, args);
        }
    }

}
