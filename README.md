# JFractals

A JavaFX application for exploring Mandelbrot and Julia sets.

## Run

Install JDK 25 or newer, then run the Maven wrapper:

```powershell
.\mvnw.cmd javafx:run
```

You can drag to pan, scroll to zoom, press **R** to reset the view, and press **S** to open settings.

## Project layout

| Package | Responsibility |
| --- | --- |
| `org.jfractals.app` | Starts JavaFX and creates the main window. |
| `org.jfractals.ui` | FXML views and controllers for user interaction. |
| `org.jfractals.model` | Fractal settings and immutable render configuration. |
| `org.jfractals.fractal` | Mandelbrot and Julia escape-time calculations. |
| `org.jfractals.render` | Pixel calculation and background render scheduling. |

The settings form and renderer share a `FractalSettings` model. Each render receives an immutable `RenderSettings` snapshot so background work does not read live JavaFX properties.
