package org.jfractals.ui.overlay;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.control.Label;
import org.jfractals.ui.Viewport;

public final class ScaleBar extends VBox {
    private static final double SCALE_TARGET_WIDTH_PX = 150;
    private static final Pos VBOX_CHILD_ALIGNMENT = Pos.BOTTOM_RIGHT;
    private static final double VBOX_SPACING = 2.0d;
    private static final double VBOX_INSET_SPACING_PX = 15;

    private final Label label = new Label();
    private final Region bar = new Region();

    public ScaleBar() {
        label.getStyleClass().addAll("overlay-label", "scale-label");
        bar.getStyleClass().add("scale-bar-line");
        getChildren().addAll(label, bar);

        setAlignment(VBOX_CHILD_ALIGNMENT);
        setSpacing(VBOX_SPACING);
        Insets innerPadding = new Insets(VBOX_INSET_SPACING_PX);
        setPadding(innerPadding);
        setMouseTransparent(true);
        setMaxSize(USE_PREF_SIZE, USE_PREF_SIZE);
    }

    public void update(Viewport viewport) {
        if (viewport.isInitial()) return;

        double upp = viewport.unitsPerPixel();
        double niceUnits = roundToNiceNumber(SCALE_TARGET_WIDTH_PX * upp);
        double barWidth = niceUnits / upp;

        bar.setPrefWidth(barWidth);
        bar.setMaxWidth(barWidth);

        label.setText(niceUnits >= 1.0 ? String.format("%.1f units", niceUnits)
                                       : String.format("%.1e units", niceUnits));
    }

    private static double roundToNiceNumber(double number) {
        double exponent = Math.floor(Math.log10(number));
        double fraction = number / Math.pow(10, exponent);
        double nice = fraction <= 1 ? 1 : fraction <= 2 ? 2 : fraction <= 5 ? 5 : 10;
        return nice * Math.pow(10, exponent);
    }
}
