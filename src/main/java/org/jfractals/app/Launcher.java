package org.jfractals.app;

import javafx.application.Application;

/** JVM entry point kept separate from the JavaFX application class. */
public final class Launcher {
    private Launcher() {
    }

    public static void main(String[] args) {
        Application.launch(JFractalsApplication.class, args);
    }
}
