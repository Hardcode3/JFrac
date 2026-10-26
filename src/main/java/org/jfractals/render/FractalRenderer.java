package org.jfractals.render;

import org.jfractals.fractal.Julia;
import org.jfractals.fractal.Mandelbrot;
import org.jfractals.model.RenderSettings;

import java.util.concurrent.CancellationException;
import java.util.function.BooleanSupplier;
import java.util.stream.IntStream;

/**
 * Computes an image's pixels for various fractals without depending on JavaFX or UI state.
 * Supports parallel execution and cooperative cancellation.
 */
public final class FractalRenderer {

    /**
     * Computes a pixel array in row-major order in a background thread using the
     * current thread's interruption status as a cancellation signal.
     *
     * @param width Width of the fractal image in pixels.
     * @param height Height of the fractal image in pixels.
     * @param viewHeight Height of the fractal image in complex plane units.
     * @param centerX Center's X coordinate of the fractal image in complex plane units.
     * @param centerY Center's Y coordinate of the fractal image in complex plane units.
     * @param settings Settings depicting the fractal being calculated.
     * @return An opaque grayscale ARGB pixel array in row-major order.
     * @throws IllegalArgumentException if dimensions or view height are invalid.
     * @throws CancellationException if the rendering process is cancelled or interrupted.
     */
    public int[] render(int width, int height, double viewHeight, double centerX, double centerY,
                        RenderSettings settings) {
        BooleanSupplier cancelled = () -> Thread.currentThread().isInterrupted();
        return render(width, height, viewHeight, centerX, centerY, settings, cancelled);
    }

    /**
     * Computes a pixel array in row-major order in a background thread using a custom
     * cancellation supplier for cooperative aborts.
     *
     * @param width Width of the fractal image in pixels.
     * @param height Height of the fractal image in pixels.
     * @param viewHeight Height of the fractal image in complex plane units.
     * @param centerX Center's X coordinate of the fractal image in complex plane units.
     * @param centerY Center's Y coordinate of the fractal image in complex plane units.
     * @param settings Settings depicting the fractal being calculated.
     * @param cancelled A supplier queried periodically to check if the computation should be aborted.
     * @return An opaque grayscale ARGB pixel array in row-major order.
     * @throws IllegalArgumentException if dimensions or view height are invalid.
     * @throws CancellationException if the rendering process is superseded or cancelled.
     */
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

        final int CHECK_FOR_CANCELL_EVERY_N_PIXELS = 128;
        IntStream.range(0, height).parallel().forEach(y -> {
            if (cancelled.getAsBoolean()) {
                return;
            }
            double imaginary = yMin + (y / (double) height) * viewHeight;
            for (int x = 0; x < width; x++) {
                if (x % CHECK_FOR_CANCELL_EVERY_N_PIXELS == 0 && cancelled.getAsBoolean()) {
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

    /**
     * Converts a raw iteration count into an opaque grayscale ARGB integer pixel value.
     *
     * @param iterations The escape iteration count computed for the pixel.
     * @param maxIterations The maximum allowed iterations used to normalize the value.
     * @return An ARGB integer representing a fully opaque shade of gray.
     */
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