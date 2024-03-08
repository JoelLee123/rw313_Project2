module org.example.p2_joel {
    requires javafx.controls;
    requires javafx.fxml;


    opens org.example.p2_joel to javafx.fxml;
    exports org.example.p2_joel;
}