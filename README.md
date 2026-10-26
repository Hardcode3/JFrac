# JFractals

A JavaFX application for exploring Mandelbrot and Julia sets.

## Run

Install JDK 25 or newer, then run the Maven wrapper:

```powershell
.\mvnw.cmd javafx:run
```

You can drag to pan, scroll to zoom, press **R** to reset the view, and press **S** to open settings.

The settings window lets you choose CPU or GPU (OpenCL) computation. GPU mode requires a compatible GPU driver, an OpenCL runtime, and FP64 support. GPU calculations use 64-bit floating point. The app prefers the GPU with the highest reported compute-unit and clock-frequency product, though some graphics cards have limited FP64 throughput and may be slower than the CPU. When the view gets too deep for GPU coordinates to distinguish adjacent pixels, the app switches to arbitrary-precision CPU rendering and explains the fallback.

## Project layout

| Package | Responsibility |
| --- | --- |
| `org.jfractals.app` | Starts JavaFX and creates the main window. |
| `org.jfractals.ui` | FXML views and controllers for user interaction. |
| `org.jfractals.model` | Fractal settings and immutable render configuration. |
| `org.jfractals.fractal` | Mandelbrot and Julia escape-time calculations. |
| `org.jfractals.render` | Pixel calculation and background render scheduling. |

The settings form and renderer share a `FractalSettings` model. Each render receives an immutable `RenderSettings` snapshot so background work does not read live JavaFX properties. `OpenClFractalRenderer` dispatches one OpenCL work item per pixel, `FractalRenderer` handles standard 64-bit CPU calculations, and `HighPrecisionFractalRenderer` uses arbitrary-precision arithmetic for deep zooms. The CPU precision is dynamically sized up to 2,048 significant decimal digits.
