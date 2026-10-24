package org.example.jfractals;

import org.apache.commons.math3.complex.Complex;

public class Mandelbrot {
    public static double sequence(double real, double imag, int max_it,
                                  double escape_radius, boolean smooth, double k) {
        smooth = (k == 1.0d) ? false : smooth;
        Complex c = new Complex(real, imag);
        Complex z = Complex.ZERO;
        for (int it = 0; it < max_it; ++it) {
            z = (z.abs() == 0) ? c : z.pow(k).add(c);
            double mag = z.abs();
            if (mag > escape_radius) {
                if (!smooth) return it;
                return it + 1 - Math.log(Math.log(mag)) / Math.log(k);
            }
        }
        return max_it;
    }
}