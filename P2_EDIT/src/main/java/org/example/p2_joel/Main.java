package org.example.p2_joel;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

/**
 * This is the main entry point of the program.
 * NB! - Remember to include the details that we need to send over
 */
public class Main extends Application {
    @Override
    public void start(Stage primaryStage) throws Exception {

        // Loading Sender UI
        FXMLLoader senderLoader = new FXMLLoader(getClass().getResource("sender.fxml"));
        Parent senderRoot = senderLoader.load();
        Stage senderStage = new Stage();
        senderStage.setTitle("Sender");
        senderStage.setScene(new Scene(senderRoot));
        senderStage.show();

        // Loading Receiver UI
        FXMLLoader receiverLoader = new FXMLLoader(getClass().getResource("receiver.fxml"));
        Parent receiverRoot = receiverLoader.load();
        Stage receiverStage = new Stage();
        receiverStage.setTitle("Receiver");
        receiverStage.setScene(new Scene(receiverRoot));
        receiverStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
