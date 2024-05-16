package com.github.paopaoyue.mesh.canvas_application;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.springframework.context.ConfigurableApplicationContext;

import java.io.IOException;

public class JavaFxLauncher extends Application {

    public static ConfigurableApplicationContext springContext;

    public static Stage primaryStage;

    public static void shutDownApplication() {
        Platform.exit();
        springContext.getBean("clientCanvasService",
                com.github.paopaoyue.mesh.canvas_application.service.ClientCanvasService.class).stopSync();
        springContext.close();
        System.exit(0);
    }

    @Override
    public void start(Stage primaryStage) throws IOException {
        JavaFxLauncher.primaryStage = primaryStage;
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/view/CanvasView.fxml"));
        fxmlLoader.setControllerFactory(springContext::getBean);
        Parent root = fxmlLoader.load();
        primaryStage.setScene(new Scene(root));
        primaryStage.setTitle("Canvas Application");
        primaryStage.show();
    }

    @Override
    public void stop() {
        shutDownApplication();
    }

}
