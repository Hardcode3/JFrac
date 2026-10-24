module org.example.jfractals {
    requires javafx.controls;
    requires javafx.fxml;
    requires commons.math3;


    opens org.example.jfractals to javafx.fxml;
    exports org.example.jfractals;
}