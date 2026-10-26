package org.jfractals.model;

/** Hardware used to calculate fractal pixels. */
public enum ComputeBackend {
    CPU("CPU"),
    GPU("GPU (OpenCL)");

    private final String displayName;

    ComputeBackend(String displayName) {
        this.displayName = displayName;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
