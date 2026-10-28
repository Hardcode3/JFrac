package org.jfractals.app;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import java.io.IOException;
import java.net.URL;
import org.jfractals.ui.FractalController;

/** Creates and owns the primary application window. */
public final class JFractalsApplication extends Application {
    private static final double INITIAL_WIN_WIDTH_PX = 720;
    private static final double INITIAL_WIN_HEIGHT_PX = 480;
    private static final String WIN_TITLE = "JFractals";

    private FractalController fractalController;

    @Override
    public void start(Stage primaryStage) throws IOException {
        URL resource = getClass().getResource("/org/jfractals/ui/fractal-view.fxml");
        FXMLLoader loader = new FXMLLoader(resource);
        Parent root = loader.load();
        fractalController = loader.getController();

        primaryStage.setTitle(WIN_TITLE);
        Scene scene = new Scene(root, INITIAL_WIN_WIDTH_PX, INITIAL_WIN_HEIGHT_PX);
        primaryStage.setScene(scene);
        primaryStage.setOnShown(_ -> fractalController.onViewReady(scene));
        primaryStage.show();
    }

    @Override
    public void stop() {
        if (fractalController != null) {
            fractalController.close();
        }
    }
}
