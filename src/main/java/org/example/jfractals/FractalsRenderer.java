package org.example.jfractals;

import javafx.application.Platform;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;
import java.util.stream.IntStream;

public class FractalsRenderer {
    public static void renderProgressively(int width, int height, WritableImage image) {
        new Thread(() -> {

            double widthRatio = (double) width / height;

            double yMin = -1.25d;
            double yMax =  1.25d;
            double yRange = yMax - yMin;
            double xRange = yRange * widthRatio;
            double xCenter = -0.75d;
            double xMin = xCenter - (xRange / 2.0);
            double xMax = xCenter + (xRange / 2.0);

            int max_it = 22;
            double escape_radius = 3.0d;
            boolean smooth = true;
            double k = 2.0d;

            IntStream.range(0, height).parallel().forEach(y -> {
                double[] rowIterations = new double[width];

                for (int x = 0; x < width; x++) {
                    double cReal = xMin + (x / (double) width) * (xMax - xMin);
                    double cIm = yMin + (y / (double) height) * (yMax - yMin);
                    rowIterations[x] = Mandelbrot.sequence(cReal, cIm, max_it, escape_radius, smooth, k);
                }

                Platform.runLater(() -> {
                    PixelWriter pixelWriter = image.getPixelWriter();
                    for (int x = 0; x < width; x++) {
                        double it = rowIterations[x];
                        double interp = (double) it / max_it;
                        if (interp > 1.0) interp = 1.0;

                        Color color = Color.BLACK.interpolate(Color.WHITE, interp);
                        pixelWriter.setColor(x, y, color);
                    }
                });
            });
        }).start();
    }
}
