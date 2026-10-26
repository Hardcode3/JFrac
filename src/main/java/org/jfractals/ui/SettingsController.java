package org.jfractals.ui;

import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import org.jfractals.model.FractalSettings;
import org.jfractals.model.FractalType;

import java.util.function.Predicate;

/** Binds the settings form to the application's shared settings model. */
public final class SettingsController {
    @FXML private ComboBox<FractalType> typeChoice;
    @FXML private Spinner<Integer> maxIterationsSpinner;
    @FXML private Spinner<Double> escapeRadiusSpinner;
    @FXML private CheckBox smoothCheckBox;
    @FXML private Spinner<Double> exponentSpinner;
    @FXML private Spinner<Double> juliaRealSpinner;
    @FXML private Spinner<Double> juliaImagSpinner;

    @FXML
    private void initialize() {
        typeChoice.getItems().setAll(FractalType.values());
        maxIterationsSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 10000, 18));
        escapeRadiusSpinner.setValueFactory(doubleFactory(2.0, 0.1, 1000.0, 0.1));
        exponentSpinner.setValueFactory(doubleFactory(2.0, 0.1, 16.0, 0.1));
        juliaRealSpinner.setValueFactory(doubleFactory(0.285, -2.0, 2.0, 0.005));
        juliaImagSpinner.setValueFactory(doubleFactory(0.01, -2.0, 2.0, 0.005));

        commitOnEditorActionOrFocusLoss(maxIterationsSpinner,
                value -> value != null && value >= 1 && value <= 10000);
        commitOnEditorActionOrFocusLoss(escapeRadiusSpinner,
                value -> value != null && value >= 0.1 && value <= 1000.0);
        commitOnEditorActionOrFocusLoss(exponentSpinner,
                value -> value != null && value >= 0.1 && value <= 16.0);
        commitOnEditorActionOrFocusLoss(juliaRealSpinner,
                value -> value != null && value >= -2.0 && value <= 2.0);
        commitOnEditorActionOrFocusLoss(juliaImagSpinner,
                value -> value != null && value >= -2.0 && value <= 2.0);

        juliaRealSpinner.disableProperty().bind(typeChoice.valueProperty().isNotEqualTo(FractalType.JULIA));
        juliaImagSpinner.disableProperty().bind(typeChoice.valueProperty().isNotEqualTo(FractalType.JULIA));
    }

    /** Called by the window that loads this controller. */
    public void setSettings(FractalSettings settings) {
        typeChoice.valueProperty().bindBidirectional(settings.typeProperty());
        maxIterationsSpinner.getValueFactory().valueProperty()
                .bindBidirectional(settings.maxIterationsProperty().asObject());
        escapeRadiusSpinner.getValueFactory().valueProperty()
                .bindBidirectional(settings.escapeRadiusProperty().asObject());
        smoothCheckBox.selectedProperty().bindBidirectional(settings.smoothProperty());
        exponentSpinner.getValueFactory().valueProperty()
                .bindBidirectional(settings.exponentProperty().asObject());
        juliaRealSpinner.getValueFactory().valueProperty()
                .bindBidirectional(settings.juliaRealProperty().asObject());
        juliaImagSpinner.getValueFactory().valueProperty()
                .bindBidirectional(settings.juliaImagProperty().asObject());
    }

    private static SpinnerValueFactory<Double> doubleFactory(double initial, double min, double max, double step) {
        return new SpinnerValueFactory.DoubleSpinnerValueFactory(min, max, initial, step);
    }

    private static <T> void commitOnEditorActionOrFocusLoss(Spinner<T> spinner, Predicate<T> isValid) {
        Runnable commit = () -> {
            try {
                T value = spinner.getValueFactory().getConverter().fromString(spinner.getEditor().getText());
                if (!isValid.test(value)) {
                    throw new IllegalArgumentException("Value is outside the allowed range");
                }
                spinner.getValueFactory().setValue(value);
            } catch (RuntimeException ignored) {
                spinner.getEditor().setText(spinner.getValueFactory().getConverter()
                        .toString(spinner.getValue()));
            }
        };
        spinner.getEditor().setOnAction(event -> commit.run());
        spinner.getEditor().focusedProperty().addListener((observable, wasFocused, isFocused) -> {
            if (!isFocused) {
                commit.run();
            }
        });
    }
}
