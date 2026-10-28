module org.jfractals {
    requires javafx.controls;
    requires javafx.fxml;
    requires commons.math3;
    requires java.desktop;

    // For tests;
    requires org.junit.jupiter;

    opens org.jfractals.ui to javafx.fxml;

    exports org.jfractals.app;
    exports org.jfractals.ui;
    exports org.jfractals.ui.overlay;
    exports org.jfractals.model;
}
