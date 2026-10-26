package org.jfractals.fractal;

import org.apache.commons.math3.complex.Complex;
import org.jfractals.model.RenderSettings;

/** Calculates the escape iteration count for points in the Mandelbrot set. */
public final class Mandelbrot {
    private Mandelbrot() {
    }

    public static double iterationCount(double real, double imaginary, RenderSettings settings) {
        Complex c = new Complex(real, imaginary);
        Complex z = Complex.ZERO;

        for (int iteration = 0; iteration < settings.maxIterations(); iteration++) {
            // Commons Math's complex power at zero is undefined for some exponents.
            // The first Mandelbrot step is z = c because the previous z is zero.
            z = z.abs() == 0.0 ? c : z.pow(settings.exponent()).add(c);
            double magnitude = z.abs();
            if (magnitude > settings.escapeRadius()) {
                return smoothIteration(iteration, magnitude, settings);
            }
        }
        return settings.maxIterations();
    }

    private static double smoothIteration(int iteration, double magnitude, RenderSettings settings) {
        if (!settings.smooth() || settings.exponent() == 1.0) {
            return iteration;
        }
        return iteration + 1.0 - Math.log(Math.log(magnitude)) / Math.log(settings.exponent());
    }
}
