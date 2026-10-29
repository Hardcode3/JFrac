package org.jfractals.ui.overlay;

import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;
import org.jfractals.ui.Viewport;

public final class AxisOverlay extends Pane {
    private final static Color AXIS_COLOR = Color.GRAY;
    private final static double AXIS_WIDTH = 1.5d;

    private final Line xAxis = newAxis();
    private final Line yAxis = newAxis();

    public AxisOverlay() {
        setMouseTransparent(true);
        getChildren().addAll(xAxis, yAxis);
    }

    public void update(Viewport viewport) {
        if (viewport.isInitial()) return;

        final double originXPx = viewport.toScreenX(0.0d);
        final double originYPx = viewport.toScreenY(0.0d);

        final boolean yAxisIsInViewport = originXPx >= 0 && originYPx <= viewport.heightPx();
        final boolean xAxisIsInViewport = originYPx >= 0 && originYPx <= viewport.widthPx();

        yAxis.setVisible(yAxisIsInViewport);
        yAxis.setStartX(originXPx);
        yAxis.setStartY(0.0d);
        yAxis.setEndX(originXPx);
        yAxis.setEndY(viewport.heightPx());

        xAxis.setVisible(xAxisIsInViewport);
        xAxis.setStartX(0.0d);
        xAxis.setStartY(originYPx);
        xAxis.setEndX(viewport.widthPx());
        xAxis.setEndY(originYPx);
    }

    private static Line newAxis() {
        Line line = new Line();
        line.setStroke(AXIS_COLOR);
        line.setStrokeWidth(AXIS_WIDTH);

        // Axis are independent of the parent layout and manually positioned.
        // Therefore, their layout should not be recomputed when parent's layout updates.
        // Also saves CPU cycles.
        line.setManaged(false);
        return line;
    }
}
