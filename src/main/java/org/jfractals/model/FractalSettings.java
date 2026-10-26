package org.jfractals.model;

import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;

/**
 * Mutable JavaFX model for the settings edited by the user.
 * Immutable version of this class lays in org.jfractals.model.RenderSettings.
 * */
public final class FractalSettings {
    private final ObjectProperty<FractalType> type = new SimpleObjectProperty<>(this, "type", FractalType.MANDELBROT);
    private final IntegerProperty maxIterations = new SimpleIntegerProperty(this, "maxIterations", 18);
    private final DoubleProperty escapeRadius = new SimpleDoubleProperty(this, "escapeRadius", 2.0);
    private final BooleanProperty smooth = new SimpleBooleanProperty(this, "smooth", true);
    private final DoubleProperty exponent = new SimpleDoubleProperty(this, "exponent", 2.0);
    private final DoubleProperty juliaReal = new SimpleDoubleProperty(this, "juliaReal", 0.285);
    private final DoubleProperty juliaImag = new SimpleDoubleProperty(this, "juliaImag", 0.01);

    public FractalType getType() {
        return type.get();
    }

    public void setType(FractalType type) {
        this.type.set(type);
    }

    public ObjectProperty<FractalType> typeProperty() {
        return type;
    }

    public int getMaxIterations() {
        return maxIterations.get();
    }

    public void setMaxIterations(int maxIterations) {
        this.maxIterations.set(maxIterations);
    }

    public IntegerProperty maxIterationsProperty() {
        return maxIterations;
    }

    public double getEscapeRadius() {
        return escapeRadius.get();
    }

    public void setEscapeRadius(double escapeRadius) {
        this.escapeRadius.set(escapeRadius);
    }

    public DoubleProperty escapeRadiusProperty() {
        return escapeRadius;
    }

    public boolean isSmooth() {
        return smooth.get();
    }

    public void setSmooth(boolean smooth) {
        this.smooth.set(smooth);
    }

    public BooleanProperty smoothProperty() {
        return smooth;
    }

    public double getExponent() {
        return exponent.get();
    }

    public void setExponent(double exponent) {
        this.exponent.set(exponent);
    }

    public DoubleProperty exponentProperty() {
        return exponent;
    }

    public double getJuliaReal() {
        return juliaReal.get();
    }

    public void setJuliaReal(double juliaReal) {
        this.juliaReal.set(juliaReal);
    }

    public DoubleProperty juliaRealProperty() {
        return juliaReal;
    }

    public double getJuliaImag() {
        return juliaImag.get();
    }

    public void setJuliaImag(double juliaImag) {
        this.juliaImag.set(juliaImag);
    }

    public DoubleProperty juliaImagProperty() {
        return juliaImag;
    }

    /** Lets the UI schedule an update when any setting changes. */
    public void addListener(InvalidationListener listener) {
        Observable[] properties = {
                type, maxIterations, escapeRadius, smooth, exponent, juliaReal, juliaImag
        };
        for (Observable property : properties) {
            property.addListener(listener);
        }
    }

    /** Copies UI state into an immutable value (thread) safe to pass to a renderer. */
    public RenderSettings snapshot() {
        return new RenderSettings(getType(), getMaxIterations(), getEscapeRadius(), isSmooth(),
                getExponent(), getJuliaReal(), getJuliaImag());
    }
}
