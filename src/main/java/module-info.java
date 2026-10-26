module org.jfractals {
    requires javafx.controls;
    requires javafx.fxml;
    requires commons.math3;
    requires jocl;
    requires ch.obermuhlner.math.big;

    opens org.jfractals.ui to javafx.fxml;
    exports org.jfractals.app;
}
