package org.jfractals.ui;

import javafx.animation.PauseTransition;
import javafx.beans.InvalidationListener;
import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.scene.Node;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.StackPane;
import javafx.util.Duration;

import java.util.List;
import java.util.function.Consumer;

import org.jfractals.model.FractalSettings;
import org.jfractals.render.FractalRenderService;
import org.jfractals.render.FractalRenderer;
import org.jfractals.ui.overlay.AxisOverlay;
import org.jfractals.ui.overlay.CursorCoordinateTip;
import org.jfractals.ui.overlay.ScaleBar;
import org.jfractals.ui.overlay.ViewInfoLabel;

public final class FractalController implements AutoCloseable {
    private static final double ZOOM_SPEED = 0.005;
    private static final Duration RENDER_DEBOUNCE_MS = Duration.millis(150);

    private final FractalSettings settings = new FractalSettings();
    private final FractalRenderService renderService = new FractalRenderService(new FractalRenderer());
    private final PauseTransition renderDebounce = new PauseTransition(RENDER_DEBOUNCE_MS);
    private final SettingsWindow settingsWindow = new SettingsWindow();

    private Viewport viewport = Viewport.initial();
    private List<Node> panLayers;
    private double pressX;
    private double pressY;

    @FXML private StackPane fractalPane;
    @FXML private ImageView fractalImageView;
    @FXML private AxisOverlay axisOverlay;
    @FXML private ViewInfoLabel infoLabel;
    @FXML private ScaleBar scaleBar;
    @FXML private CursorCoordinateTip cursorTip;

    @FXML
    private void initialize() {
        panLayers = List.of(fractalImageView, axisOverlay);

        fractalImageView.fitWidthProperty().bind(fractalPane.widthProperty());
        fractalImageView.fitHeightProperty().bind(fractalPane.heightProperty());

        renderDebounce.setOnFinished(_ -> render());
        settings.addListener(_ -> scheduleRender());

        InvalidationListener onResize = _ -> {
            syncViewportSize();
            axisOverlay.update(viewport);
            scheduleRender();
        };
        fractalPane.widthProperty().addListener(onResize);
        fractalPane.heightProperty().addListener(onResize);

        fractalPane.setOnMouseMoved(e -> cursorTip.showAt(e.getX(), e.getY(), viewport));
        fractalPane.setOnMouseExited(_ -> cursorTip.hide());
        fractalPane.setOnMousePressed(e -> { pressX = e.getX(); pressY = e.getY(); });
        fractalPane.setOnMouseDragged(e -> setPanOffset(e.getX() - pressX, e.getY() - pressY));
        fractalPane.setOnMouseReleased(e -> finishPan(e.getX() - pressX, e.getY() - pressY));
        fractalPane.setOnScroll(e -> zoomAt(e.getX(), e.getY(), e.getDeltaY()));
    }

    public void onViewReady(Scene scene) {
        scene.addEventFilter(KeyEvent.KEY_PRESSED, this::handleKeyPressed);
        syncViewportSize();
        render();
    }

    private void handleKeyPressed(KeyEvent event) {
        switch (event.getCode()) {
            case R -> resetView();
            case S -> settingsWindow.show(fractalPane.getScene().getWindow(), settings);
            default -> { return; }
        }
        event.consume();
    }

    private void finishPan(double dxPx, double dyPx) {
        if (viewport.isInitial() || (dxPx == 0.0 && dyPx == 0.0)) {
            resetPan();
            return;
        }
        viewport = viewport.panned(dxPx, dyPx);
        render();
    }

    private void zoomAt(double x, double y, double scrollDelta) {
        if (viewport.isInitial() || scrollDelta == 0.0) return;
        viewport = viewport.zoomedAt(x, y, Math.exp(scrollDelta * ZOOM_SPEED));
        render();
    }

    private void resetView() {
        viewport = viewport.reset();
        resetPan();
        render();
    }

    private void setPanOffset(double dxPx, double dyPx) {
        for (Node layer : panLayers) {
            layer.setTranslateX(dxPx);
            layer.setTranslateY(dyPx);
        }
    }

    private void resetPan() {
        setPanOffset(0, 0);
    }

    private void syncViewportSize() {
        viewport = viewport.withSize(fractalPane.getWidth(), fractalPane.getHeight());
    }

    private void scheduleRender() {
        renderDebounce.playFromStart();
    }

    private void render() {
        if (viewport.isInitial()) return;
        Viewport vp = viewport;
        infoLabel.update(vp);
        scaleBar.update(vp);
        Consumer<WritableImage> onRendered = image -> {
            fractalImageView.setImage(image);
            resetPan();
            axisOverlay.update(vp);
        };
        renderService.render(
                (int) vp.widthPx(), (int) vp.heightPx(),
                vp.heightCp(),
                vp.centerXCp(), vp.centerYCp(),
                settings.snapshot(),
                onRendered
        );
    }

    @Override
    public void close() {
        renderDebounce.stop();
        settingsWindow.close();
        renderService.close();
    }
}