package com.github.paopaoyue.mesh.canvas_application;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class CanvasApplication {

    public static void main(String[] args) {
        JavaFxLauncher.springContext = SpringApplication.run(CanvasApplication.class, args);
        JavaFxLauncher.launch(JavaFxLauncher.class, args);

    }

}