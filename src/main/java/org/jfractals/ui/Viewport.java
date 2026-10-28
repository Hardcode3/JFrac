package org.jfractals.ui;

public record Viewport(
        double centerXCp, double centerYCp,
        double widthPx, double heightPx,
        double viewHeightCp
) {
    private static final double INITIAL_HEIGHT_CP = 3.5;

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
        return viewHeightCp / widthPx;
    }
    public double zoomFactor() {
        return INITIAL_HEIGHT_CP / viewHeightCp;
    }
    public double toComplexX(double screenXPx) {
        return centerXCp + (screenXPx - widthPx / 2.0) * unitsPerPixel();
    }
    public double toComplexY(double screenYPx) {
        return centerYCp + (screenYPx - heightPx / 2.0) * unitsPerPixel();
    }
    public double toScreenX(double complexX) {
        return widthPx / 2.0 + (complexX - centerXCp) / unitsPerPixel();
    }
    public double toScreenY(double complexY) {
        return heightPx / 2.0 + (complexY - centerYCp) / unitsPerPixel();
    }
    public Viewport panned(double dxPx, double dyPx) {
        double upp = unitsPerPixel();
        return new Viewport(
                centerXCp - dxPx * upp,
                centerYCp - dyPx * upp,
                widthPx, heightPx, viewHeightCp
        );
    }
    public Viewport zoomAt(double xPx, double yPx, double zoom) {
        double real = toComplexX(xPx);
        double imag = toComplexY(yPx);
        double newHeight = viewHeightCp / zoom;
        double newUnitsPerPixels = newHeight / heightPx;
        return new Viewport(
                real - (xPx - widthPx / 2) * newUnitsPerPixels,
                yPx - (imag - heightPx / 2) * newUnitsPerPixels,
                widthPx, heightPx, newHeight
        );
    }
}
