module org.example.project2 {
    requires javafx.controls;
    requires javafx.fxml;


    opens org.example.project2 to javafx.fxml;
    exports org.example.project2;
}