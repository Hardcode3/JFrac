package org.example.jfractals;

import javafx.fxml.FXML;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;

public class FractalsController {
    @FXML
    private ImageView fractalImageView;

    public void initialize(){
        int width = 1280;
        int height = 1280;
        WritableImage image = new WritableImage(width, height);
        fractalImageView.setImage(image);

        FractalsRenderer.renderProgressively(width, height, image);
    }
}
