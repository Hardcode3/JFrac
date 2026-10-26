package org.jfractals.fractal;

import org.apache.commons.math3.complex.Complex;
import org.jfractals.model.RenderSettings;

/** Calculates the escape iteration count for points in the Mandelbrot set. */
public final class Mandelbrot {
    private Mandelbrot() {
    }

    public static double iterationCount(double real, double imaginary, RenderSettings settings) {
        if (settings.exponent() == 2.0) {
            return quadraticIterationCount(real, imaginary, settings);
        }

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

    private static double quadraticIterationCount(double cReal, double cImaginary, RenderSettings settings) {
        double zReal = 0.0;
        double zImaginary = 0.0;
        double escapeRadiusSquared = settings.escapeRadius() * settings.escapeRadius();
        for (int iteration = 0; iteration < settings.maxIterations(); iteration++) {
            double nextReal = Math.fma(-zImaginary, zImaginary, cReal);
            nextReal = Math.fma(zReal, zReal, nextReal);
            double nextImaginary = Math.fma(2.0 * zReal, zImaginary, cImaginary);
            zReal = nextReal;
            zImaginary = nextImaginary;

            double magnitudeSquared = Math.fma(zReal, zReal, zImaginary * zImaginary);
            if (magnitudeSquared > escapeRadiusSquared) {
                if (!settings.smooth() || settings.exponent() == 1.0) {
                    return iteration;
                }
                return smoothIteration(iteration, Math.sqrt(magnitudeSquared), settings);
            }
        }
        return settings.maxIterations();
    }

    private static double smoothIteration(int iteration, double magnitude, RenderSettings settings) {
        if (!settings.smooth() || settings.exponent() == 1.0
                || magnitude <= Math.E || !Double.isFinite(magnitude)) {
            return iteration;
        }
        return iteration + 1.0 - Math.log(Math.log(magnitude)) / Math.log(settings.exponent());
    }
}
