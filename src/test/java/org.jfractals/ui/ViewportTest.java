package org.jfractals.ui;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import static org.junit.jupiter.api.Assertions.*;

public class ViewportTest {
    private final double EPS = 1e-9;
    private final Viewport vp = new Viewport(0.5, -0.25, 800, 600, 3.5);

    @Test
    void screenCenterMapsToViewportCenter() {
        final double convertedXPx = vp.toScreenX(vp.centerXCp());
        final double convertedYPx = vp.toScreenY(vp.centerYCp());
        assertEquals(convertedXPx, vp.widthPx() / 2.0, EPS);
        assertEquals(convertedYPx, vp.heightPx() / 2.0, EPS);
    }

    @ParameterizedTest
    @CsvSource({
            "0, 0",
            "100, 200",
            "-123, 473",
            "-32, -37",
            "74, -109",
            "100.1, 202.1",
            "720, 480.0",
            "1920.0, 1080",
            "799, 799"
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
        assertEquals(vp.viewHeightCp() / zoom, zoomed.viewHeightCp(), EPS);
    }

    @Test
    void zoomingInByHalfDoublesZoomFactor() {
        Viewport zoomed = vp.zoomedAt(vp.widthPx() * 0.33, vp.heightPx() * 0.8, 0.5);
        assertEquals(2.0, zoomed.zoomFactor(), EPS);
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
        assertEquals(Viewport.INITIAL_HEIGHT_CP, reset.viewHeightCp());
        assertEquals(vp.widthPx(), reset.widthPx());
        assertEquals(vp.heightPx(), reset.heightPx());
    }

    @Test
    void viewportIsEmptyUntilItHasASize() {
        assertTrue(Viewport.initial().isEmpty());
        assertFalse(Viewport.initial().withSize(800, 600).isEmpty());
    }
}
