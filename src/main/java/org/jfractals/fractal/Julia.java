package org.jfractals.fractal;

import org.apache.commons.math3.complex.Complex;
import org.jfractals.model.RenderSettings;

/** Calculates the escape iteration count for points in a Julia set. */
public final class Julia {
    private Julia() { }

    public static double iterationCount(double real, double imaginary, RenderSettings settings) {
        Complex c = new Complex(settings.juliaReal(), settings.juliaImag());
        Complex z = new Complex(real, imaginary);

        for (int iteration = 0; iteration < settings.maxIterations(); iteration++) {
            z = z.abs() == 0.0 ? c : z.pow(settings.exponent()).add(c);
            double magnitude = z.abs();
            if (magnitude > settings.escapeRadius()) {
                return FractalMaths.smoothIteration(iteration, magnitude, settings);
            }
        }
        return settings.maxIterations();
    }
}
