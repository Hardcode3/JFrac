package org.jfractals.render;

import javafx.application.Platform;
import javafx.scene.image.PixelFormat;
import javafx.scene.image.WritableImage;
import org.jfractals.model.RenderSettings;

import java.util.Objects;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

/** Runs renders off the JavaFX thread and publishes only the latest result. */
public final class FractalRenderService implements AutoCloseable {
    private static final System.Logger LOGGER = System.getLogger(FractalRenderService.class.getName());

    private final FractalRenderer renderer;
    private final ExecutorService executor = Executors.newSingleThreadExecutor(task -> {
        Thread thread = new Thread(task, "fractal-renderer");
        thread.setDaemon(true);
        return thread;
    });
    private final AtomicLong generation = new AtomicLong();

    private Future<?> currentRender;
    private boolean closed;

    public FractalRenderService(FractalRenderer renderer) {
        this.renderer = Objects.requireNonNull(renderer, "renderer");
    }

    /** Must be called on the JavaFX Application Thread. */
    public void render(int width, int height, double viewHeight, double centerX, double centerY,
                       RenderSettings settings, Consumer<WritableImage> onRendered) {
        Objects.requireNonNull(settings, "settings");
        Objects.requireNonNull(onRendered, "onRendered");
        if (closed) {
            return;
        }

        long requestGeneration = generation.incrementAndGet();
        if (currentRender != null) {
            currentRender.cancel(true);
        }

        currentRender = executor.submit(() -> {
            int[] pixels;
            try {
                pixels = renderer.render(width, height, viewHeight, centerX, centerY, settings,
                        () -> Thread.currentThread().isInterrupted() || generation.get() != requestGeneration);
            } catch (CancellationException ignored) {
                return;
            } catch (RuntimeException exception) {
                LOGGER.log(System.Logger.Level.ERROR, "Fractal render failed", exception);
                return;
            }
            if (Thread.currentThread().isInterrupted()) {
                return;
            }
            Platform.runLater(() -> {
                if (closed || generation.get() != requestGeneration) {
                    return;
                }
                WritableImage image = new WritableImage(width, height);
                image.getPixelWriter().setPixels(0, 0, width, height,
                        PixelFormat.getIntArgbInstance(), pixels, 0, width);
                onRendered.accept(image);
            });
        });
    }

    @Override
    public void close() {
        closed = true;
        generation.incrementAndGet();
        if (currentRender != null) {
            currentRender.cancel(true);
        }
        executor.shutdownNow();
    }
}
