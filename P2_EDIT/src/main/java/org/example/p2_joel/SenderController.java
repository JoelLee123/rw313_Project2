package org.example.p2_joel;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.concurrent.Service;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import java.io.*;

/*
    This is the controller class for sender.fxml
    This class therefore handles all attributes and button clicks
    on the UI.
 */
public class SenderController {
    // FXML GUI ATTRIBUTES
    @FXML
    private TextField sFieldIP;
    @FXML
    private TextField sFieldPacket;
    @FXML
    private TextField sFieldBlast;
    @FXML
    private RadioButton sRadioTCP;
    @FXML
    private RadioButton sRadioRBUDP;
    @FXML
    private Label sLabelFile;
    @FXML
    private Label sLabelPacket;
    @FXML
    private Label sLabelBlast;
    @FXML
    private Button sBtnDirectory;
    @FXML
    private Button sBtnSend;
    @FXML
    private ProgressBar sProgress;

    // SOCKET ATTRIBUTES
    private TCP_Sender sender;

    private long fileSize;
    private double kilobytes;
    private double megabytes;
    private double gigabytes;

    /*
     * public void setSender(Sender sender) {
     * this.sender = sender;
     * }
     */

    /**
     * The initialize() method is a recognized method in JavaFX.
     * JavaFX instantiates the SenderController class which then
     * can instantiate the classes we need.
     *
     * This also handler visibility of RBUDP elements
     * when the radio buttons are selected.
     */
    public void initialize() {
        sender = new TCP_Sender();

        ToggleGroup tGroup = new ToggleGroup();
        sRadioTCP.setToggleGroup(tGroup);
        sRadioRBUDP.setToggleGroup(tGroup);

        // When TCP SELECTED
        sRadioTCP.setOnAction(event -> {
            sLabelPacket.setDisable(true);
            sLabelBlast.setDisable(true);
            sFieldPacket.setDisable(true);
            sFieldBlast.setDisable(true);
        });

        sRadioRBUDP.setOnAction(event -> {
            sLabelPacket.setDisable(false);
            sLabelBlast.setDisable(false);
            sFieldPacket.setDisable(false);
            sFieldBlast.setDisable(false);
        });
    }

    public static void warningSender(String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Sender Warning!");
            alert.setHeaderText(message);
            alert.setContentText("");
            alert.showAndWait();
        });
    }

    @FXML
    public void btnSendFileClicked(ActionEvent event) {

        // VALIDATION BEFORE SENDING

        if (sFieldIP.getText().isEmpty() || sLabelFile.getText().equals("File")) {
            // IP ADDRESS CANNOT BE EMPTY AND A FILE MUST BE SELECTED
            warningSender("IP Address cannot be empty and a file must be selected");
            return;
        }

        sender.setSenderIP(sFieldIP.getText());
        // One of the two options must always be selected
        if (sRadioRBUDP.isSelected()) {
            // INITIALIZE RBUDP
            // USE DATAGRAM METHODS
            // sender.RBUDP_methods
            System.out.println("RBUDP CASE HANDLED");

            // Make all RBUDP invisible

        } else {
            // INITIALIZE TCP
            System.out.println("TCP CASE HANDLED");
            // sender.getData().clear();
            // Make all RBUDP fields invisible

            sender.initSocketTCP();
            sender.initSenderTCP(); // tcpSender.iniSocket
            Service<Void> progressBarService = new Service<Void>() {
                @Override
                protected Task<Void> createTask() {
                    // LAMBDA EXPRESSION - Details sometimes hidden under () ->
                    return new Task<Void>() {
                        @Override
                        protected Void call() throws Exception {
                            sender.setProgressUpdate(
                                    (workDone, totalWork) -> updateProgress(workDone, totalWork));
                            sender.sendTCP();
                            // sExecService.shutdownNow();
                            return null;
                        }
                    };
                }
            };
            sProgress.progressProperty().bind(progressBarService.progressProperty());
            Platform.runLater(() -> progressBarService.restart());
        }
        // Setting up progress bar (should be the same for both TCP and RBUDP)
    }

    @FXML
    public void btnSelectDirClicked(ActionEvent event) {
        System.out.println("SELECT FOLDER TEST");
        String filePath = "";
        // sender.load_directory_method()

        Stage stage = (Stage) sBtnDirectory.getScene().getWindow();
        // JavaFX feature to select a folder
        FileChooser fChooser = new FileChooser();

        File file = fChooser.showOpenDialog(stage);
        if (file == null) {
            warningSender("No file selected. Please select a file.");
        } else {
            filePath = getWholePath(file);
            sender.setFile(filePath);
            // Do the same for UDP?
        }
    }

    /**
     * SenderController gets a file from a user.
     * This will be sent over to the receiver when
     * the send button is clicked.
     *
     * @param file The file to send
     * @return The complete filePath as a string
     */
    public String getWholePath(File file) {
        String filePath = file.getAbsolutePath();
        int lastSlash = filePath.lastIndexOf("/");
        String newPath = ".." + filePath.substring(lastSlash, filePath.length());
        fileSize = file.length();
        kilobytes = (double) (fileSize / 1000.0);
        megabytes = kilobytes / 1000.0;
        gigabytes = megabytes / 1000.0;
        sLabelFile.setText(newPath);
        if (fileSize < 1000) {
            sLabelFile.setText(Long.toString(fileSize) + " Bytes");
        } else if (fileSize >= 1000.0 && fileSize < 1000000.0) {
            sLabelFile.setText(String.format("%,.2f", kilobytes) + " KB");
        } else if (fileSize >= 1000000.0 && fileSize < 1000000000.0) {
            sLabelFile.setText(String.format("%,.2f", megabytes) + " MB");
        } else if (fileSize >= 1000000000.0 && fileSize < 1000000000000.0) {
            sLabelFile.setText(String.format("%,.2f", gigabytes) + " GB");
        }
        return filePath;
    }

}
