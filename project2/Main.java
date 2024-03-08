package org.example.project2;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

/*
 * This is the main class of the application. It is responsible for starting the application and
 *  closing the application.
 */
public class Main extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception{
        System.out.println("Get here");
        Parent root = FXMLLoader.load(getClass().getResource("/org/example/project2/main.fxml"));
        System.out.println("Error here");
        primaryStage.setTitle("Jeeves");
        primaryStage.setScene(new Scene(root, 1016, 473));
        primaryStage.setResizable(false);
        primaryStage.setOnCloseRequest(e -> {
            try {
                stop();
            } catch (Exception e1) {
                // TODO Auto-generated catch block
                e1.printStackTrace();
            }
        });
        primaryStage.show();
    }

    @Override
    public void stop() throws Exception {
        super.stop();
        Platform.exit();
        System.exit(0);
    }

    public static void main(String[] args) {
        launch(args);
    }
}