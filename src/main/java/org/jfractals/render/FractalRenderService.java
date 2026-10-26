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
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

/**
 * Manages background fractal rendering tasks off the JavaFX Application Thread,
 * ensuring that only the most recent render request is processed and published to the UI.
 * Implements {@link AutoCloseable} to clean up underlying background executor resources.
 */
public final class FractalRenderService implements AutoCloseable {
    private static final System.Logger LOGGER = System.getLogger(FractalRenderService.class.getName());

    /** The underlying calculator/renderer used to compute pixel data. */
    private final FractalRenderer renderer;

    /** Dedicated single-thread background executor running daemon threads for rendering. */
    private final ExecutorService executor = Executors.newSingleThreadExecutor(task -> {
        Thread thread = new Thread(task, "fractal-renderer");
        thread.setDaemon(true);
        return thread;
    });

    /** Monotonically increasing counter to track and supersede outdated render generations. */
    private final AtomicLong generation = new AtomicLong();

    /** Handle to the currently active background render task, if any. */
    private Future<?> currentRender;

    /** Flag indicating whether this service has been closed and can no longer accept requests. */
    private boolean closed;

    /**
     * Constructs a new render service with the specified core renderer.
     *
     * @param renderer The renderer used to calculate fractal pixel arrays.
     * @throws NullPointerException if renderer is null.
     */
    public FractalRenderService(FractalRenderer renderer) {
        this.renderer = Objects.requireNonNull(renderer, "renderer");
    }

    /**
     * Submits an asynchronous render request. If a previous render is still in progress,
     * it is cancelled immediately and superseded by this new request.
     * Once completed successfully, the resulting image is published back on the JavaFX Application Thread.
     * <p>
     * Must be called on the JavaFX Application Thread.
     *
     * @param width Width of the output image in pixels.
     * @param height Height of the output image in pixels.
     * @param viewHeight Height of the viewport in complex plane units.
     * @param centerX Center X-coordinate in complex plane units.
     * @param centerY Center Y-coordinate in complex plane units.
     * @param settings The configuration settings for the fractal render.
     * @param onRendered Callback executed on the JavaFX thread with the freshly generated image.
     * @throws NullPointerException if settings or onRendered is null.
     */
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
                final BooleanSupplier isCancelled = () -> Thread.currentThread().isInterrupted() || generation.get() != requestGeneration;
                pixels = renderer.render(width, height, viewHeight, centerX, centerY, settings, isCancelled);
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

    /**
     * Shuts down the service, cancelling any ongoing render tasks and releasing background threads.
     * Once closed, subsequent calls to {@link #render} will be ignored.
     */
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