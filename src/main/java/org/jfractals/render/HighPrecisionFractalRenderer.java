package org.jfractals.render;

import ch.obermuhlner.math.big.BigComplex;
import ch.obermuhlner.math.big.BigComplexMath;
import org.jfractals.model.FractalType;
import org.jfractals.model.RenderSettings;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.concurrent.CancellationException;
import java.util.function.BooleanSupplier;
import java.util.stream.IntStream;

/** Arbitrary-precision CPU renderer used when double precision cannot resolve pixels. */
public final class HighPrecisionFractalRenderer {
    private static final BigDecimal TWO = BigDecimal.valueOf(2);
    private static final int GUARD_DIGITS = 36;
    private static final int MIN_PRECISION = 40;
    private static final int MAX_PRECISION = 2048;

    public int[] render(int width, int height, BigDecimal viewHeight, BigDecimal centerX, BigDecimal centerY,
                        RenderSettings settings, BooleanSupplier cancelled) {
        if (width <= 0 || height <= 0 || viewHeight.signum() <= 0) {
            throw new IllegalArgumentException("Image dimensions and view height must be positive");
        }

        MathContext mathContext = mathContextFor(viewHeight, centerX, centerY);
        BigDecimal widthDecimal = BigDecimal.valueOf(width);
        BigDecimal heightDecimal = BigDecimal.valueOf(height);
        BigDecimal xRange = viewHeight.multiply(widthDecimal, mathContext)
                .divide(heightDecimal, mathContext);
        BigDecimal xStep = xRange.divide(widthDecimal, mathContext);
        BigDecimal yStep = viewHeight.divide(heightDecimal, mathContext);
        BigDecimal xMin = centerX.subtract(xRange.divide(TWO, mathContext), mathContext);
        BigDecimal yMin = centerY.subtract(viewHeight.divide(TWO, mathContext), mathContext);
        BigDecimal escapeRadius = BigDecimal.valueOf(settings.escapeRadius());
        BigDecimal escapeRadiusSquared = escapeRadius.multiply(escapeRadius, mathContext);
        BigDecimal exponent = BigDecimal.valueOf(settings.exponent());
        BigComplex juliaConstant = BigComplex.valueOf(
                BigDecimal.valueOf(settings.juliaReal()), BigDecimal.valueOf(settings.juliaImag()));
        int[] pixels = new int[Math.multiplyExact(width, height)];

        IntStream.range(0, height).parallel().forEach(y -> {
            if (cancelled.getAsBoolean()) {
                return;
            }
            BigDecimal imaginary = yMin.add(yStep.multiply(BigDecimal.valueOf(y), mathContext), mathContext);
            for (int x = 0; x < width; x++) {
                if ((x & 0x0f) == 0 && cancelled.getAsBoolean()) {
                    return;
                }
                BigDecimal real = xMin.add(xStep.multiply(BigDecimal.valueOf(x), mathContext), mathContext);
                double iterations = settings.type() == FractalType.MANDELBROT
                        ? mandelbrot(real, imaginary, escapeRadiusSquared, settings, exponent, mathContext, cancelled)
                        : julia(real, imaginary, juliaConstant, escapeRadiusSquared, settings, exponent, mathContext, cancelled);
                pixels[y * width + x] = FractalRenderer.grayscale(iterations, settings.maxIterations());
            }
        });

        if (cancelled.getAsBoolean()) {
            throw new CancellationException("High-precision render was superseded");
        }
        return pixels;
    }

    private static double mandelbrot(BigDecimal cReal, BigDecimal cImaginary, BigDecimal escapeRadiusSquared,
                                     RenderSettings settings, BigDecimal exponent, MathContext mathContext,
                                     BooleanSupplier cancelled) {
        if (settings.exponent() == 2.0) {
            return quadraticOrbit(BigDecimal.ZERO, BigDecimal.ZERO, cReal, cImaginary,
                    escapeRadiusSquared, settings, mathContext, cancelled);
        }
        return generalOrbit(BigComplex.ZERO, BigComplex.valueOf(cReal, cImaginary), escapeRadiusSquared,
                settings, exponent, mathContext, cancelled);
    }

    private static double julia(BigDecimal zReal, BigDecimal zImaginary, BigComplex constant,
                                BigDecimal escapeRadiusSquared, RenderSettings settings, BigDecimal exponent,
                                MathContext mathContext, BooleanSupplier cancelled) {
        if (settings.exponent() == 2.0) {
            return quadraticOrbit(zReal, zImaginary, constant.re, constant.im,
                    escapeRadiusSquared, settings, mathContext, cancelled);
        }
        return generalOrbit(BigComplex.valueOf(zReal, zImaginary), constant, escapeRadiusSquared,
                settings, exponent, mathContext, cancelled);
    }

    private static double quadraticOrbit(BigDecimal zReal, BigDecimal zImaginary,
                                         BigDecimal cReal, BigDecimal cImaginary,
                                         BigDecimal escapeRadiusSquared, RenderSettings settings,
                                         MathContext mathContext, BooleanSupplier cancelled) {
        for (int iteration = 0; iteration < settings.maxIterations(); iteration++) {
            if ((iteration & 0x07) == 0 && cancelled.getAsBoolean()) {
                throw new CancellationException("High-precision render was superseded");
            }
            BigDecimal nextReal = zReal.multiply(zReal, mathContext)
                    .subtract(zImaginary.multiply(zImaginary, mathContext), mathContext)
                    .add(cReal, mathContext);
            BigDecimal nextImaginary = zReal.multiply(zImaginary, mathContext)
                    .multiply(TWO, mathContext)
                    .add(cImaginary, mathContext);
            zReal = nextReal;
            zImaginary = nextImaginary;

            BigDecimal magnitudeSquared = zReal.multiply(zReal, mathContext)
                    .add(zImaginary.multiply(zImaginary, mathContext), mathContext);
            if (magnitudeSquared.compareTo(escapeRadiusSquared) > 0) {
                return smoothIteration(iteration, magnitudeSquared, settings, mathContext);
            }
        }
        return settings.maxIterations();
    }

    private static double generalOrbit(BigComplex z, BigComplex c, BigDecimal escapeRadiusSquared,
                                       RenderSettings settings, BigDecimal exponent, MathContext mathContext,
                                       BooleanSupplier cancelled) {
        for (int iteration = 0; iteration < settings.maxIterations(); iteration++) {
            if ((iteration & 0x07) == 0 && cancelled.getAsBoolean()) {
                throw new CancellationException("High-precision render was superseded");
            }
            z = z.absSquare(mathContext).signum() == 0
                    ? c
                    : complexPower(z, exponent, mathContext).add(c, mathContext);
            BigDecimal magnitudeSquared = z.absSquare(mathContext);
            if (magnitudeSquared.compareTo(escapeRadiusSquared) > 0) {
                return smoothIteration(iteration, magnitudeSquared, settings, mathContext);
            }
        }
        return settings.maxIterations();
    }

    private static double smoothIteration(int iteration, BigDecimal magnitudeSquared,
                                          RenderSettings settings, MathContext mathContext) {
        if (!settings.smooth() || settings.exponent() == 1.0) {
            return iteration;
        }
        double magnitude = Math.sqrt(magnitudeSquared.doubleValue());
        if (!Double.isFinite(magnitude) || magnitude <= Math.E) {
            return iteration;
        }
        return iteration + 1.0 - Math.log(Math.log(magnitude)) / Math.log(settings.exponent());
    }

    private static BigComplex complexPower(BigComplex value, BigDecimal exponent, MathContext mathContext) {
        try {
            return BigComplexMath.pow(value, exponent.longValueExact(), mathContext);
        } catch (ArithmeticException fractionalExponent) {
            return BigComplexMath.pow(value, exponent, mathContext);
        }
    }

    private static MathContext mathContextFor(BigDecimal viewHeight, BigDecimal centerX, BigDecimal centerY) {
        int viewExponent = decimalExponent(viewHeight);
        int centerExponent = Math.max(decimalExponent(centerX.abs()), decimalExponent(centerY.abs()));
        int requiredPrecision = Math.max(MIN_PRECISION, centerExponent - viewExponent + GUARD_DIGITS);
        return new MathContext(Math.min(requiredPrecision, MAX_PRECISION), RoundingMode.HALF_EVEN);
    }

    private static int decimalExponent(BigDecimal value) {
        if (value.signum() == 0) {
            return 0;
        }
        return value.precision() - value.scale() - 1;
    }
}
