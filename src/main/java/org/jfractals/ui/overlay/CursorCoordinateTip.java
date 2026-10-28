package org.jfractals.ui.overlay;

import javafx.scene.control.Label;
import org.jfractals.ui.Viewport;

public final class CursorCoordinateTip extends Label {
    private final static double OFFSET_X_PX = 10;
    private final static double OFFSET_Y_PX = 20;

    public CursorCoordinateTip() {
        getStyleClass().addAll("overlay-label", "cursor-tip");
        setMouseTransparent(true);
        setVisible(false);
    }

    public void showAt(double mouseXPx, double mouseYPx, Viewport viewport) {
        setText(String.format("(%.6f,%.6fi)", viewport.toComplexX(mouseXPx), viewport.toComplexY(mouseYPx)));
        setTranslateX(mouseXPx + OFFSET_X_PX);
        setTranslateY(mouseYPx + OFFSET_Y_PX);
        setVisible(true);
    }

    public void hide() {
        setVisible(false);
    }
}
