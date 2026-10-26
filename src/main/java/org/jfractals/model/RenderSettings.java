package org.jfractals.model;

import java.util.Objects;

/**
 * Immutable snapshot of the parameters needed for one render.
 * The mutable version of this class lays in org.jfractals.model.FractalSettings.
 * */
public record RenderSettings(
        FractalType type,
        int maxIterations,
        double escapeRadius,
        boolean smooth,
        double exponent,
        double juliaReal,
        double juliaImag) {

    public RenderSettings {
        Objects.requireNonNull(type, "type");
        if (maxIterations < 1) {
            throw new IllegalArgumentException("Maximum iterations must be positive");
        }
        if (!Double.isFinite(escapeRadius) || escapeRadius <= 0.0) {
            throw new IllegalArgumentException("Escape radius must be finite and positive");
        }
        if (!Double.isFinite(exponent) || exponent <= 0.0) {
            throw new IllegalArgumentException("Exponent must be finite and positive");
        }
        if (!Double.isFinite(juliaReal) || !Double.isFinite(juliaImag)) {
            throw new IllegalArgumentException("Julia constants must be finite");
        }
    }
}
