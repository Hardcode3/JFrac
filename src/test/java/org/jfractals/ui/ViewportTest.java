package org.jfractals.ui;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import static org.junit.jupiter.api.Assertions.*;

public class ViewportTest {
    private final double EPS = 1e-9;
    private final Viewport vp = new Viewport(0.5, -0.25, 800, 600, 3.5);

    @ParameterizedTest
    @CsvSource({
        "0.0", // Null view height in complex plane
        "-10.1", // Negative view height in complex plane
    })
    void constructorDefaultsViewHeightCpWhenInvalid(double invalidViewHeightCp) {
        Viewport defaultedViewport = new Viewport(vp.centerXCp(), vp.centerYCp(), vp.widthPx(), vp.heightPx(), invalidViewHeightCp);
        assertEquals(defaultedViewport.heightCp(), Viewport.DEFAULT_HEIGHT_CP, EPS);
    }

    @ParameterizedTest
    @CsvSource({
        "0.0", // Null view width in pixels
        "-120", // Negative view width in pixels
    })
    void constructorDefaultsViewWidthPxWhenInvalid(double invalidViewWidthPx) {
        Viewport defaultedViewport = new Viewport(vp.centerXCp(), vp.centerYCp(), invalidViewWidthPx, vp.heightPx(), vp.heightCp());
        assertEquals(defaultedViewport.widthPx(), Viewport.DEFAULT_WIDTH_PX, EPS);
    }

    @ParameterizedTest
    @CsvSource({
        "0.0", // Null view height in pixels
        "-239", // Negative view height in pixels
    })
    void constructorDefaultsViewHeightPxWhenInvalid(double invalidViewHeightPx) {
        Viewport defaultedViewport = new Viewport(vp.centerXCp(), vp.centerYCp(), vp.widthPx(), invalidViewHeightPx, vp.heightCp());
        assertEquals(defaultedViewport.heightPx(), Viewport.DEFAULT_HEIGHT_PX, EPS);
    }

    @Test
    void screenCenterMapsToViewportCenter() {
        final double convertedXPx = vp.toScreenX(vp.centerXCp());
        final double convertedYPx = vp.toScreenY(vp.centerYCp());
        assertEquals(convertedXPx, vp.widthPx() / 2.0, EPS);
        assertEquals(convertedYPx, vp.heightPx() / 2.0, EPS);
    }

    @ParameterizedTest
    @CsvSource({
            "0, 0", // Screen origin
            "799, 799", // Near bottom right extremity
            "800, 800", // At bottom right extremity
            "500, 400, 1.5", // Bottom right quarter
            "243, 398, 1.5", // Bottom left quarter
            "100, 230, 1.5", // Top left quarter
            "675, 187, 1.5", // Top right quarter
            "-123, 473", // Out of bounds with negative x
            "230, -564", // Out of bounds with negative y
            "1920, 1080", // Out of bounds with positive x and y
            "-1920, -1080", // Out of bounds with negative x and y
    })
    void screenToComplexAndBackIsIdentity(double xPx, double yPx) {
        assertEquals(vp.toScreenX(vp.toComplexX(xPx)), xPx, EPS);
        assertEquals(vp.toScreenY(vp.toComplexY(yPx)), yPx, EPS);
    }

    @ParameterizedTest
    @CsvSource({
            "400, 300, 1.5", // Center
            "500, 400, 1.5", // Bottom right quarter
            "243, 398, 1.5", // Bottom left quarter
            "100, 230, 1.5", // Top left quarter
            "675, 187, 1.5", // Top right quarter
            "800, 600, 1.5", // Bottom right extremity
            "0, 0, 1.5", // Top left extremity (origin of the window coordinate system)
            "-129, 34, 1.5", // Out of bounds but should work
            "576, 318, 0.000001", // With big de-zoom
            "576, 318, 9999999.13", // With big zoom
    })
    void zoomKeepsPointUnderCursorFix(double xPx, double yPx, double zoom) {
        Viewport zoomed = vp.zoomedAt(xPx, yPx, zoom);
        assertEquals(vp.toComplexX(xPx), zoomed.toComplexX(xPx), EPS);
        assertEquals(vp.toComplexY(yPx), zoomed.toComplexY(yPx), EPS);
        assertEquals(vp.heightCp() / zoom, zoomed.heightCp(), EPS);
    }

    @ParameterizedTest
    @CsvSource({
            "2.0", // Zoom in by 2 takes half of the original view
            "1.0", // No zoom does not change the original view
            "0.5", // Zoom out by .5 takes twice the original window
    })
    void zoomingUpdatesCorrectlyZoomFactor(double appliedZoom) {
        Viewport zoomed = vp.zoomedAt(vp.widthPx() * 0.33, vp.heightPx() * 0.8, appliedZoom);
        assertEquals(zoomed.zoomFactor(), Viewport.DEFAULT_HEIGHT_CP / (vp.heightCp() / appliedZoom), EPS);
    }

    @ParameterizedTest
    @CsvSource({
        "0.0", // Null zoom would divide the original view by zero, unauthorized
        "-1.23" // Negative zoom would cause negative view height, unauthorized
    })
    void nullOrNegativeZoomReturnsSameViewport(double invalidZoom) {
        Viewport zoomed = vp.zoomedAt(vp.widthPx() * 0.33, vp.heightPx() * 0.8, invalidZoom);
        assertTrue(vp.equals(zoomed));
    }

    @Test
    void panMovesThePointUnderThePressToTheRelease() {
        double pressX = 200, pressY = 150;
        double dx = 80, dy = -30;

        Viewport panned = vp.panned(dx, dy);

        assertEquals(vp.toComplexX(pressX), panned.toComplexX(pressX + dx), EPS);
        assertEquals(vp.toComplexY(pressY), panned.toComplexY(pressY + dy), EPS);
    }

    @Test
    void resetRestoresDefaultViewButKeepsSize() {
        Viewport reset = vp.zoomedAt(10, 10, 0.01).reset();

        assertEquals(0.0, reset.centerXCp());
        assertEquals(0.0, reset.centerYCp());
        assertEquals(Viewport.DEFAULT_HEIGHT_CP, reset.heightCp());
        assertEquals(vp.widthPx(), reset.widthPx());
        assertEquals(vp.heightPx(), reset.heightPx());
    }

    @Test
    void viewportIsInitialUntilSomeFieldIsDifferentFromDefaults() {
        assertTrue(Viewport.initial().isInitial());
        assertFalse(Viewport.initial().withSize(Viewport.DEFAULT_WIDTH_PX + 1.0, Viewport.DEFAULT_HEIGHT_PX).isInitial());
    }

    @Test
    void toStringProducesValidString() {
        String viewportString = vp.toString();
        assertFalse(viewportString.isEmpty());
        assertTrue(viewportString.contains("Viewport"));
    }

    @Test
    void widthCpCustomGetterWorksWithExpectedFormulae() {
        assertEquals(vp.widthCp(), vp.widthPx() * vp.unitsPerPixel());
    }
}
