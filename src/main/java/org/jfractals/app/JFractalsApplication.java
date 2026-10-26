package org.jfractals.app;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.jfractals.ui.FractalController;

import java.io.IOException;

/** Creates and owns the primary application window. */
public final class JFractalsApplication extends Application {
    private static final double INITIAL_WIDTH = 1280;
    private static final double INITIAL_HEIGHT = 720;

    private FractalController fractalController;

    @Override
    public void start(Stage primaryStage) throws IOException {
        FXMLLoader loader = new FXMLLoader(JFractalsApplication.class.getResource("/org/jfractals/ui/fractal-view.fxml"));
        Parent root = loader.load();
        fractalController = loader.getController();

        primaryStage.setTitle("JFractals");
        Scene scene = new Scene(root, INITIAL_WIDTH, INITIAL_HEIGHT);
        primaryStage.setScene(scene);
        primaryStage.setOnShown(event -> fractalController.onViewReady(scene));
        primaryStage.show();
    }

    @Override
    public void stop() {
        if (fractalController != null) {
            fractalController.close();
        }
    }
}
