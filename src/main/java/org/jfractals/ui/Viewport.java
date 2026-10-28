package org.jfractals.ui;

import org.jspecify.annotations.NonNull;

public record Viewport(
        double centerXCp, double centerYCp,
        double widthPx, double heightPx,
        double viewHeightCp
) {
    public static final double INITIAL_HEIGHT_CP = 3.5;

    public static Viewport initial() {
        return new Viewport(0, 0, 0, 0, INITIAL_HEIGHT_CP);
    }
    public Viewport reset() {
        return new Viewport(0, 0, widthPx, heightPx, INITIAL_HEIGHT_CP);
    }
    public Viewport withSize(double newWidthPx, double newHeightPx) {
        return new Viewport(centerXCp, centerYCp, newWidthPx, newHeightPx, viewHeightCp);
    }
    public boolean isEmpty() {
        return widthPx <= 0 || heightPx <= 0;
    }
    public double unitsPerPixel() {
        return viewHeightCp / heightPx;
    }
    public double zoomFactor() {
        return INITIAL_HEIGHT_CP / viewHeightCp;
    }
    public double toComplexX(double screenXPx) { return centerXCp + (screenXPx - widthPx / 2.0) * unitsPerPixel(); }
    public double toComplexY(double screenYPx) {
        return centerYCp - (screenYPx - heightPx / 2.0) * unitsPerPixel();
    }
    public double toScreenX(double complexXCp) {
        return widthPx / 2.0 + (complexXCp - centerXCp) / unitsPerPixel();
    }
    public double toScreenY(double complexYCp) {
        return heightPx / 2.0 - (complexYCp - centerYCp) / unitsPerPixel();
    }
    public Viewport panned(double dxPx, double dyPx) {
        double upp = unitsPerPixel();
        return new Viewport(
                centerXCp - dxPx * upp,
                centerYCp + dyPx * upp,
                widthPx, heightPx, viewHeightCp
        );
    }
    public Viewport zoomedAt(double xPx, double yPx, double zoom) {
        double real = toComplexX(xPx);
        double imag = toComplexY(yPx);

        double newViewHeightCp = viewHeightCp / zoom;
        double newUnitsPerPixels = newViewHeightCp / heightPx;

        double offsetToScreenCenterXPx = xPx - widthPx / 2.0;
        double offsetToScreenCenterYPx = yPx - heightPx / 2.0;

        double newCenterXCp = real - offsetToScreenCenterXPx * newUnitsPerPixels;
        double newCenterYCp = imag + offsetToScreenCenterYPx * newUnitsPerPixels;

        return new Viewport(newCenterXCp, newCenterYCp, widthPx, heightPx, newViewHeightCp);
    }

    @NonNull
    public String toString() {
        return String.format("Viewport object: \n center (complex plane) = %.4f,%.4fi \n view dimensions (pixels): %.1f,%.1f) \n view height (complex plane): %.2f", centerXCp, centerYCp, widthPx, heightPx, viewHeightCp);
    }
}
