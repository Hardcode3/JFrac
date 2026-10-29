package org.jfractals.ui;

public record Viewport(
        double centerXCp, double centerYCp,
        double widthPx, double heightPx,
        double heightCp
) {
    public static final double DEFAULT_WIDTH_PX = 800;
    public static final double DEFAULT_HEIGHT_PX = 600;
    public static final double DEFAULT_HEIGHT_CP = 3.5;
    public static final double DEFAULT_CENTER_X_CP = 0.0;
    public static final double DEFAULT_CENTER_Y_CP = 0.0;

    public Viewport {
        if (heightCp <= 0.0d) {
            heightCp = DEFAULT_HEIGHT_CP;
        }
        if (widthPx <= 0.0d) {
            widthPx = DEFAULT_WIDTH_PX;
        }
        if (heightPx <= 0.0d) {
            heightPx = DEFAULT_HEIGHT_PX;
        }
    }

    public static Viewport initial() {
        return new Viewport(DEFAULT_CENTER_X_CP, DEFAULT_CENTER_Y_CP, DEFAULT_WIDTH_PX, DEFAULT_HEIGHT_CP, DEFAULT_HEIGHT_CP);
    }
    public Viewport reset() {
        return new Viewport(DEFAULT_CENTER_X_CP, DEFAULT_CENTER_Y_CP, widthPx, heightPx, DEFAULT_HEIGHT_CP);
    }
    public Viewport withSize(double newWidthPx, double newHeightPx) {
        return new Viewport(centerXCp, centerYCp, newWidthPx, newHeightPx, heightCp);
    }
    public boolean isInitial() {
        return this.equals(Viewport.initial());
    }
    public double widthCp() {
        return widthPx * unitsPerPixel();
    }
    public double unitsPerPixel() {
        return heightCp / heightPx;
    }
    public double zoomFactor() {
        return DEFAULT_HEIGHT_CP / heightCp;
    }
    public double toComplexX(double screenXPx) {
        return centerXCp + (screenXPx - widthPx / 2.0) * unitsPerPixel();
    }
    public double toComplexY(double screenYPx) {
        return centerYCp + (screenYPx - heightPx / 2.0) * unitsPerPixel();
    }
    public double toScreenX(double complexXCp) {
        return widthPx / 2.0 + (complexXCp - centerXCp) / unitsPerPixel();
    }
    public double toScreenY(double complexYCp) {
        return heightPx / 2.0 + (complexYCp - centerYCp) / unitsPerPixel();
    }
    public Viewport panned(double dxPx, double dyPx) {
        double upp = unitsPerPixel();
        return new Viewport(
                centerXCp - dxPx * upp,
                centerYCp - dyPx * upp,
                widthPx, heightPx, heightCp
        );
    }
    public Viewport zoomedAt(double xPx, double yPx, double zoom) {
        if (zoom <= 0.0 || heightPx <= 0) return this;

        double real = toComplexX(xPx);
        double imag = toComplexY(yPx);

        double newViewHeightCp = heightCp / zoom;
        double newUnitsPerPixels = newViewHeightCp / heightPx;

        double offsetToScreenCenterXPx = xPx - widthPx / 2.0;
        double offsetToScreenCenterYPx = yPx - heightPx / 2.0;

        double newCenterXCp = real - offsetToScreenCenterXPx * newUnitsPerPixels;
        double newCenterYCp = imag - offsetToScreenCenterYPx * newUnitsPerPixels;

        return new Viewport(newCenterXCp, newCenterYCp, widthPx, heightPx, newViewHeightCp);
    }
    public String toString() {
        return String.format("Viewport object: \n center (complex plane) = %.4f,%.4fi \n view dimensions (pixels): %.1f,%.1f) \n view height (complex plane): %.2f", centerXCp, centerYCp, widthPx, heightPx, heightCp);
    }
}
