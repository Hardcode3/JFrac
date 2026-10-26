package org.jfractals.render;

import org.jfractals.fractal.Julia;
import org.jfractals.fractal.Mandelbrot;
import org.jfractals.model.FractalType;
import org.jfractals.model.RenderSettings;

import java.util.concurrent.CancellationException;
import java.util.function.BooleanSupplier;
import java.util.stream.IntStream;

/** Computes an image's pixels without depending on JavaFX or UI state. */
public final class FractalRenderer {
    /**
     * Returns an opaque grayscale ARGB pixel array in row-major order.
     * This method may run on a background thread.
     */
    public int[] render(int width, int height, double viewHeight, double centerX, double centerY,
                        RenderSettings settings) {
        return render(width, height, viewHeight, centerX, centerY, settings,
                () -> Thread.currentThread().isInterrupted());
    }

    int[] render(int width, int height, double viewHeight, double centerX, double centerY,
                 RenderSettings settings, BooleanSupplier cancelled) {
        if (width <= 0 || height <= 0) {
            throw new IllegalArgumentException("Image dimensions must be positive");
        }
        if (!Double.isFinite(viewHeight) || viewHeight <= 0.0) {
            throw new IllegalArgumentException("View height must be finite and positive");
        }

        int[] pixels = new int[Math.multiplyExact(width, height)];
        double widthRatio = (double) width / height;
        double xRange = viewHeight * widthRatio;
        double xMin = centerX - xRange / 2.0;
        double yMin = centerY - viewHeight / 2.0;

        IntStream.range(0, height).parallel().forEach(y -> {
            if (cancelled.getAsBoolean()) {
                return;
            }
            double imaginary = yMin + (y / (double) height) * viewHeight;
            for (int x = 0; x < width; x++) {
                if ((x & 0x3f) == 0 && cancelled.getAsBoolean()) {
                    return;
                }
                double real = xMin + (x / (double) width) * xRange;
                double iterations = switch (settings.type()) {
                    case MANDELBROT -> Mandelbrot.iterationCount(real, imaginary, settings);
                    case JULIA -> Julia.iterationCount(real, imaginary, settings);
                };
                pixels[y * width + x] = grayscale(iterations, settings.maxIterations());
            }
        });
        if (cancelled.getAsBoolean()) {
            throw new CancellationException("Render was superseded");
        }
        return pixels;
    }

    private static int grayscale(double iterations, int maxIterations) {
        double normalized = iterations / maxIterations;
        if (!Double.isFinite(normalized) || normalized < 0.0) {
            normalized = 0.0;
        } else if (normalized > 1.0) {
            normalized = 1.0;
        }
        int gray = (int) Math.round(normalized * 255.0);
        return 0xff000000 | gray << 16 | gray << 8 | gray;
    }
}
