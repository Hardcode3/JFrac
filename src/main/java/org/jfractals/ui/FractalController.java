package org.jfractals.ui;

import javafx.animation.PauseTransition;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.Node;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.scene.shape.Line;
import javafx.util.Duration;
import java.util.function.Consumer;
import javafx.scene.control.Label;
import org.jfractals.model.FractalSettings;
import org.jfractals.render.FractalRenderService;
import org.jfractals.render.FractalRenderer;

import java.io.IOException;

/**
 * Manages UI interactions with the fractal view, including mouse panning, zooming,
 * keyboard shortcuts, and coordination with asynchronous background rendering services.
 * Implements {@link AutoCloseable} to properly clean up resources, timers, and open windows.
 */
public final class FractalController implements AutoCloseable {
    private static final double INITIAL_VIEW_HEIGHT = 3.5;
    private static final double ZOOM_SPEED = 0.005;
    private static final Duration RENDER_DEBOUNCE = Duration.millis(150);

    @FXML private StackPane fractalPane;
    @FXML private ImageView fractalImageView;
    @FXML private Label coordinateInfoLabel;
    @FXML private Label scaleLabel;
    @FXML private Pane scaleBarLine;
    @FXML private Pane axisOverlayPane;
    @FXML private Label cursorCoordLabel;

    /** Mutable model containing the current rendering and fractal parameters. */
    private final FractalSettings settings = new FractalSettings();

    /** Background service handling non-blocking rendering tasks. */
    private final FractalRenderService renderService = new FractalRenderService(new FractalRenderer());

    /** Debounce timer to delay re-renders during rapid window resizing or configuration changes. */
    private final PauseTransition renderDebounce = new PauseTransition(RENDER_DEBOUNCE);

    private double pressX;
    private double pressY;
    private double centerX;
    private double centerY;
    private double viewHeight = INITIAL_VIEW_HEIGHT;

    /** Reference to the settings modal stage, if currently open. */
    private Stage settingsStage;

    /**
     * JavaFX initialization hook. Sets up image bindings, debounce transitions,
     * property listeners, and mouse event handlers for panning and zooming.
     */
    @FXML
    private void initialize() {
        fractalImageView.fitWidthProperty().bind(fractalPane.widthProperty());
        fractalImageView.fitHeightProperty().bind(fractalPane.heightProperty());
        renderDebounce.setOnFinished(event -> render());

        fractalPane.widthProperty().addListener(observable -> scheduleRender());
        fractalPane.heightProperty().addListener(observable -> scheduleRender());
        settings.addListener(observable -> scheduleRender());

        fractalPane.widthProperty().addListener(observable -> updateAxisOverlay());
        fractalPane.heightProperty().addListener(observable -> updateAxisOverlay());

        fractalPane.setOnMouseMoved(event -> updateCursorCoordinateTip(event.getX(), event.getY()));
        fractalPane.setOnMouseExited(event -> {
            if (coordinateInfoLabel == null) return;
            cursorCoordLabel.setVisible(false);
        });
        fractalPane.setOnMousePressed(event -> {
            pressX = event.getX();
            pressY = event.getY();
        });
        fractalPane.setOnMouseDragged(event -> {
            double dx = event.getX() - pressX;
            double dy = event.getY() - pressY;
            fractalImageView.setTranslateX(dx);
            fractalImageView.setTranslateY(dy);
            ObservableList<Node> axis = axisOverlayPane.getChildren();
            for (Node n: axis) {
                n.setTranslateX(dx);
                n.setTranslateY(dy);
            }
        });
        fractalPane.setOnMouseReleased(event -> finishPan(event.getX(), event.getY()));
        fractalPane.setOnScroll(event -> zoomAt(event.getX(), event.getY(), event.getDeltaY()));

        updateInfoLabels();
        updateScaleBar();
    }

    public void updateInfoLabels() {
        if (coordinateInfoLabel == null) return;
        double zoomFactor = INITIAL_VIEW_HEIGHT / viewHeight;
        String text = String.format("Zoom: %.2fx \t Center: %.6f %+.6fi", zoomFactor, centerX, centerY);
        coordinateInfoLabel.setText(text);
    }

    public void updateScaleBar() {
        if (scaleLabel == null || scaleBarLine == null) return;
        double height = fractalPane.getHeight();
        if (height <= 0.0) return;

        double unitsPerPixel = viewHeight / height;
        final double SCALEBAR_WIDTH_PX = 150;
        double scaleBarWidthInComplexUnits = SCALEBAR_WIDTH_PX * unitsPerPixel;
        double scaleBarWidthRoundedComplexUnits = roundToNiceNumber(scaleBarWidthInComplexUnits);
        double scaleBarWidthNiceUnitsPx = scaleBarWidthRoundedComplexUnits / unitsPerPixel;

        scaleBarLine.setPrefWidth(scaleBarWidthNiceUnitsPx);
        scaleBarLine.setMaxWidth(scaleBarWidthNiceUnitsPx);
        scaleLabel.setText(formatScaleText(scaleBarWidthRoundedComplexUnits));
    }

    /** Rounds a number to a clean, human-readable increment (1, 2, 5 * 10^n). */
    private double roundToNiceNumber(double number) {
        double exponent = Math.floor(Math.log10(number));
        double fraction = number / Math.pow(10, exponent);
        double niceFraction;

        if (fraction <= 1.0) niceFraction = 1.0;
        else if (fraction <= 2.0) niceFraction = 2.0;
        else if (fraction <= 5.0) niceFraction = 5.0;
        else niceFraction = 10.0;

        return niceFraction * Math.pow(10, exponent);
    }

    /** Formats the scale value nicely, using scientific notation for very small values. */
    private String formatScaleText(double units) {
        if (units >= 1.0) {
            return String.format("%.1f units", units);
        } else {
            // Automatically switch to scientific notation for deep zooms
            return String.format("%.0e units", units);
        }
    }

    public void updateAxisOverlay() {
        if (axisOverlayPane == null) return;

        double width = fractalPane.getWidth();
        double height = fractalPane.getHeight();
        if (width <= 0 || height <= 0) return;

        axisOverlayPane.getChildren().clear();

        double unitsPerPixel = viewHeight / height;
        double originScreenX = (width / 2.0) - centerX / unitsPerPixel;
        double originScreenY = (height / 2.0) - centerY / unitsPerPixel;

        final double AXIS_STROKE_WIDTH_PX = 2.0;
        final Color AXIS_COLOR = Color.GRAY;
        if (originScreenX >= 0 && originScreenX <= width) {
            Line yaxis = new Line(originScreenX, 0, originScreenX, height);
            yaxis.setStroke(AXIS_COLOR);
            yaxis.setStrokeWidth(AXIS_STROKE_WIDTH_PX);
            axisOverlayPane.getChildren().add(yaxis);
        }
        if (originScreenY >= 0 && originScreenY <= height) {
            Line xaxis = new Line(0, originScreenY, width, originScreenY);
            xaxis.setStroke(AXIS_COLOR);
            xaxis.setStrokeWidth(AXIS_STROKE_WIDTH_PX);
            axisOverlayPane.getChildren().add(xaxis);
        }
    }

    private void updateCursorCoordinateTip(double mouseX, double mouseY) {
        if (cursorCoordLabel == null) return;

        double width = fractalPane.getWidth();
        double height = fractalPane.getHeight();
        if (width <= 0 || height <= 0) return;

        double unitsPerPixel = viewHeight / height;
        double cursorXCp = centerX + (mouseX - (width / 2.0)) * unitsPerPixel;
        double cursorYCp = centerY + (mouseY - (height / 2.0)) * unitsPerPixel;
        String text = String.format("(%.6f,%.6fi)", cursorXCp, cursorYCp);
        cursorCoordLabel.setText(text);
        cursorCoordLabel.setVisible(true);

        final double LABEL_OFFSET_X = 60;
        final double LABEL_OFFSET_Y = -10;
        /*
        This is a tricky conversion.
        Pane origin is at the center of the pane. x-axis points east and y-axis points south.
        Mouse origin is top left corner of the window. Axis point in the same direction than the Pane.
        Therefore the (-);
        */
        double labelX = mouseX - (width / 2.0) + LABEL_OFFSET_X;
        double labelY = mouseY - (height / 2.0) + LABEL_OFFSET_Y;
        cursorCoordLabel.setTranslateX(labelX);
        cursorCoordLabel.setTranslateY(labelY);
    }

    /**
     * Called by the application once the primary scene/stage is fully visible.
     * Registers global keyboard shortcuts and triggers the initial render.
     *
     * @param scene The main application scene.
     */
    public void onViewReady(Scene scene) {
        scene.addEventFilter(KeyEvent.KEY_PRESSED, this::handleKeyPressed);
        render();
    }

    /**
     * Handles keyboard shortcuts for resetting the view or opening settings.
     *
     * @param event The key event triggered by the user.
     */
    private void handleKeyPressed(KeyEvent event) {
        switch (event.getCode()) {
            case R -> resetView();
            case S -> openSettings();
            default -> {
                return;
            }
        }
        event.consume();
    }

    private void resetTranslations() {
        fractalImageView.setTranslateX(0.0);
        fractalImageView.setTranslateY(0.0);
        if (axisOverlayPane != null){
            for (Node n : axisOverlayPane.getChildren()) {
                n.setTranslateX(0);
                n.setTranslateY(0);
            }
        }

    }

    /**
     * Finalizes a panning gesture by translating screen coordinates into complex plane offsets,
     * resetting image view translations, and triggering a fresh render.
     *
     * @param x The final X coordinate of the mouse release.
     * @param y The final Y coordinate of the mouse release.
     */
    private void finishPan(double x, double y) {
        double deltaX = x - pressX;
        double deltaY = y - pressY;
        if ((deltaX == 0.0 && deltaY == 0.0) || fractalPane.getHeight() <= 0.0) {
            resetTranslations();
            return;
        }

        double unitsPerPixel = viewHeight / fractalPane.getHeight();
        centerX -= deltaX * unitsPerPixel;
        centerY -= deltaY * unitsPerPixel;

        int width = (int) fractalPane.getWidth();
        int height = (int) fractalPane.getHeight();
        if (width <= 0 || height <= 0) {
            resetTranslations();
            return;
        }

        render();
    }

    /**
     * Zooms in or out centered around the cursor position based on mouse scroll deltas.
     *
     * @param x The mouse X coordinate where the zoom occurs.
     * @param y The mouse Y coordinate where the zoom occurs.
     * @param deltaY The scroll wheel delta magnitude.
     */
    private void zoomAt(double x, double y, double deltaY) {
        double width = fractalPane.getWidth();
        double height = fractalPane.getHeight();
        if (deltaY == 0.0 || width <= 0.0 || height <= 0.0) {
            return;
        }

        double oldUnitsPerPixel = viewHeight / height;
        double pointX = centerX + (x - width / 2.0) * oldUnitsPerPixel;
        double pointY = centerY + (y - height / 2.0) * oldUnitsPerPixel;

        viewHeight *= Math.exp(-deltaY * ZOOM_SPEED);
        double newUnitsPerPixel = viewHeight / height;
        centerX = pointX - (x - width / 2.0) * newUnitsPerPixel;
        centerY = pointY - (y - height / 2.0) * newUnitsPerPixel;
        render();
    }

    /**
     * Opens or focuses the modal settings window allowing the user to modify fractal properties.
     *
     * @throws IllegalStateException if the FXML settings layout cannot be loaded.
     */
    private void openSettings() {
        if (settingsStage != null && settingsStage.isShowing()) {
            settingsStage.toFront();
            return;
        }

        try {
            FXMLLoader loader = new FXMLLoader(FractalController.class.getResource("settings-view.fxml"));
            Parent content = loader.load();
            loader.<SettingsController>getController().setSettings(settings);

            settingsStage = new Stage();
            settingsStage.setTitle("Fractal Settings");
            settingsStage.initOwner(fractalPane.getScene().getWindow());
            settingsStage.initModality(Modality.WINDOW_MODAL);
            settingsStage.setScene(new Scene(content));
            settingsStage.setOnHidden(event -> settingsStage = null);
            settingsStage.show();
        } catch (IOException exception) {
            throw new IllegalStateException("Could not load the settings window", exception);
        }
    }

    /** Resets the viewport center and view height back to initial defaults. */
    private void resetView() {
        centerX = 0.0;
        centerY = 0.0;
        viewHeight = INITIAL_VIEW_HEIGHT;
        fractalImageView.setTranslateX(0.0);
        fractalImageView.setTranslateY(0.0);
        render();
    }

    /** Restarts the render debounce timer to prevent redundant heavy renders during rapid UI updates. */
    private void scheduleRender() {
        renderDebounce.playFromStart();
    }

    /** Requests an asynchronous render from the render service using current dimensions and view states. */
    private void render() {
        int width = (int) fractalPane.getWidth();
        int height = (int) fractalPane.getHeight();
        if (width <= 0 || height <= 0) {
            return;
        }
        updateInfoLabels();
        updateScaleBar();
        Consumer<WritableImage> onRendered = image -> {
            fractalImageView.setImage(image);
            resetTranslations();
            updateAxisOverlay();
        };
        renderService.render(width, height, viewHeight, centerX, centerY, settings.snapshot(), onRendered);
    }

    /**
     * Cleans up controller resources, stopping active debounce timers,
     * closing the settings window if open, and shutting down background rendering services.
     */
    @Override
    public void close() {
        renderDebounce.stop();
        if (settingsStage != null) {
            settingsStage.close();
        }
        renderService.close();
    }
}