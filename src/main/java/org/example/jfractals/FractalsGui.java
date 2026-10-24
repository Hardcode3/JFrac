package org.example.jfractals;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class FractalsGui extends Application {
    @Override
    public void start(Stage stage) throws IOException {
        int scaling = 1;
        int width = 1280 * scaling;
        int height = 1280 * scaling;

        FXMLLoader fxmlLoader = new FXMLLoader(FractalsGui.class.getResource("hello-view.fxml"));
        Parent root = fxmlLoader.load();
        Scene scene = new Scene(root, width, height);

        stage.setTitle("JFractals");
        stage.setScene(scene);

        stage.show();
    }
}