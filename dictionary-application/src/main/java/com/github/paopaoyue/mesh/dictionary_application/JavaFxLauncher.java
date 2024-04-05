package com.github.paopaoyue.mesh.dictionary_application;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.springframework.context.ConfigurableApplicationContext;

import java.io.IOException;

public class JavaFxLauncher extends Application {

    static ConfigurableApplicationContext springContext;

    @Override
    public void start(Stage primaryStage) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/view/DictionaryView.fxml"));
        fxmlLoader.setControllerFactory(springContext::getBean);
        Parent root = fxmlLoader.load();
        primaryStage.setScene(new Scene(root));
        primaryStage.setTitle("Dictionary Application");
        primaryStage.show();
    }

    @Override
    public void stop() {
        springContext.close();
    }

}
