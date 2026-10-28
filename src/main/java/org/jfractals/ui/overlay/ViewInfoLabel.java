package org.jfractals.ui.overlay;

import org.jfractals.ui.Viewport;
import javafx.scene.control.Label;

public final class ViewInfoLabel extends Label {
    public ViewInfoLabel() {
        getStyleClass().addAll("overlay-label", "view-info");
        setMouseTransparent(true);
    }

    public void update(Viewport viewport) {
        setText(String.format("Zoom: %.2fx \t Center: %.6f %+.6fi",
                viewport.zoomFactor(), viewport.centerXCp(), viewport.centerYCp())
        );
    }
}
