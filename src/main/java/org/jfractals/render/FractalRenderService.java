package org.jfractals.render;

import javafx.application.Platform;
import javafx.scene.image.PixelFormat;
import javafx.scene.image.WritableImage;
import org.jfractals.model.ComputeBackend;
import org.jfractals.model.RenderSettings;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

/** Runs renders off the JavaFX thread and publishes only the latest result. */
public final class FractalRenderService implements AutoCloseable {
    private static final System.Logger LOGGER = System.getLogger(FractalRenderService.class.getName());

    private final FractalRenderer renderer;
    private final HighPrecisionFractalRenderer highPrecisionRenderer = new HighPrecisionFractalRenderer();
    private final OpenClFractalRenderer gpuRenderer = new OpenClFractalRenderer();
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
    public void render(int width, int height, BigDecimal viewHeight, BigDecimal centerX, BigDecimal centerY,
                       RenderSettings settings, Consumer<WritableImage> onRendered,
                       Consumer<String> onGpuFallback) {
        Objects.requireNonNull(settings, "settings");
        Objects.requireNonNull(onRendered, "onRendered");
        Objects.requireNonNull(onGpuFallback, "onGpuFallback");
        if (closed) {
            return;
        }

        long requestGeneration = generation.incrementAndGet();
        if (currentRender != null) {
            currentRender.cancel(true);
        }

        currentRender = executor.submit(() -> {
            boolean needsHighPrecision = FractalRenderer.requiresHighPrecision(
                    width, height, viewHeight, centerX, centerY);
            BooleanSupplier cancelled = () ->
                    Thread.currentThread().isInterrupted() || generation.get() != requestGeneration;
            int[] pixels;
            try {
                if (settings.backend() == ComputeBackend.GPU) {
                    if (needsHighPrecision) {
                        throw new GpuFallbackException(
                                "This zoom is beyond the GPU's reliable 64-bit coordinate precision. "
                                        + "Using arbitrary-precision CPU calculations instead.");
                    }
                    pixels = gpuRenderer.render(width, height, viewHeight, centerX, centerY, settings);
                } else if (needsHighPrecision) {
                    pixels = highPrecisionRenderer.render(width, height, viewHeight, centerX, centerY,
                            settings, cancelled);
                } else {
                    pixels = renderer.render(width, height, viewHeight.doubleValue(),
                            centerX.doubleValue(), centerY.doubleValue(), settings, cancelled);
                }
            } catch (CancellationException ignored) {
                return;
            } catch (GpuFallbackException exception) {
                LOGGER.log(System.Logger.Level.WARNING, "Falling back to arbitrary-precision CPU rendering", exception);
                Platform.runLater(() -> {
                    if (!closed && generation.get() == requestGeneration) {
                        onGpuFallback.accept(exception.getMessage());
                    }
                });
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
        if (closed) {
            return;
        }
        closed = true;
        generation.incrementAndGet();
        if (currentRender != null) {
            currentRender.cancel(true);
        }
        executor.execute(gpuRenderer::close);
        executor.shutdown();
    }
}
