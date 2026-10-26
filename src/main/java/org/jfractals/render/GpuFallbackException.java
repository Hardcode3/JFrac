package org.jfractals.render;

/** Explains why a GPU render must be routed to the arbitrary-precision CPU renderer. */
public final class GpuFallbackException extends RuntimeException {
    public GpuFallbackException(String message) {
        super(message);
    }

    public GpuFallbackException(String message, Throwable cause) {
        super(message, cause);
    }
}
