package org.jfractals.ui;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;
import org.jfractals.model.FractalSettings;

import java.io.IOException;

final class SettingsWindow implements AutoCloseable {
    private static final String WIN_TITLE = "Fractal Settings";

    private Stage stage;

    void show(Window owner, FractalSettings settings) {
        if (stage != null && stage.isShowing()) {
            stage.toFront();
            return;
        }
        try {
            FXMLLoader loader = new FXMLLoader(SettingsWindow.class.getResource("settings-view.fxml"));
            Parent content = loader.load();
            loader.<SettingsController>getController().setSettings(settings);

            stage = new Stage();
            stage.setTitle(WIN_TITLE);
            stage.initOwner(owner);
            stage.initModality(Modality.WINDOW_MODAL);
            stage.setScene(new Scene(content));
            stage.setOnHidden(_ -> stage = null);
            stage.show();
        } catch (IOException e) {
            throw new IllegalStateException("Could not load the settings window", e);
        }
    }

    @Override
    public void close() {
        if (stage != null) stage.close();
    }
}
