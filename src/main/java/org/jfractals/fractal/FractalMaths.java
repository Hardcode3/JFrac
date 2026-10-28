package org.jfractals.fractal;

import org.jfractals.model.RenderSettings;

final public class FractalMaths {
    private FractalMaths() { }

    /**
     * Applies a smoothing to the integer iteration reached by the fractal.
     * Can be used with both Mandelbrot and Julia sets.
     * See: <a href="https://linas.org/art-gallery/escape/smooth.html">...</a>
     *
     * @param iteration Iteration count reached before escaping fractal's radius for a given complex point.
     * @param magnitude Magnitude of the complex point.
     * @param settings Information describing the fractal being computed.
     *
     * @return Smoothed iteration count before fractal's escape as a floating point number.
     */
    public static double smoothIteration(int iteration, double magnitude, RenderSettings settings) {
        if (!settings.smooth() || settings.exponent() == 0) {
            return iteration;
        }
        return iteration + 1.0 - Math.log(Math.log(magnitude)) / Math.log(settings.exponent());
    }
}
