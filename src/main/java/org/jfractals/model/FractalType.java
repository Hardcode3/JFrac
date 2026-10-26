package org.jfractals.model;

/** The kind of fractal set to render. */
public enum FractalType {
    MANDELBROT("Mandelbrot"),
    JULIA("Julia");

    private final String displayName;

    FractalType(String displayName) {
        this.displayName = displayName;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
