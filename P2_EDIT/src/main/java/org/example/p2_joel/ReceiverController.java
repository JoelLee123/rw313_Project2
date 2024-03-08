package org.example.p2_joel;

import javafx.application.Platform;
import javafx.concurrent.Service;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;

import java.io.File;

public class ReceiverController {
    @FXML
    private TextField rFieldIP;
    @FXML
    private Button rBtnStart;
    @FXML
    private Button rBtnStorage;
    @FXML
    private Button rBtnStop;
    @FXML
    private Label rLabelStorage;
    @FXML
    private ProgressBar rProgress;
    private Receiver receiver;
    //Adding volatile to these instance variables eliminates race conditions
    //The volatile variable will be read from main memory rather than CPU cache
    public volatile Thread rTCPThread;
    public volatile Thread rRBUDPThread;
    public boolean isRunning = true;

   /* public void setReceiver(Receiver receiver) {
        this.receiver = receiver;
    }*/
    public void initialize() {
        receiver = new Receiver();
    }

    public static void warningReceiver(String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Receiver Warning");
            alert.setHeaderText(message);
            alert.setContentText("");
            alert.showAndWait();
        });
    }

    @FXML
    public void btnStartClicked(ActionEvent event) {
        isRunning = true;
        System.out.println("START RECEIEVER TEST");
        if (rFieldIP.getText().isEmpty() || rLabelStorage.getText().equals("Storage Location")) {
            warningReceiver("IP Address cannot be empty and a storage location must be set");
        } else {
            rBtnStart.setDisable(true);
            rBtnStorage.setDisable(true);
            rBtnStop.setDisable(false);
            //WE CAN START THREADS FOR TCP AND RBUDP
            //configureReceiverStart();
            System.out.println("Fine here");
            //LAMBDA EXPRESSION - Hidden details under () ->
            rTCPThread = new Thread(() -> {
                receiver.initServerSockTCP();
                receiver.initReceiverTCP();
                while (isRunning) {
                    receiver.receive();
                }
            });
            rTCPThread.start();
        }


    }
    @FXML
    public void btnStopClicked(ActionEvent event) {
        System.out.println("STOP RECEIEVER TEST");
        isRunning = false;
        //Enable the start button again
        rBtnStart.setDisable(false);
        rBtnStorage.setDisable(false);
        stopThreads();
        receiver.closeTCP();
        //receiver.closeUDP(); - Maybe do a check for this?
    }

    @FXML
    public void btnStorageClicked(ActionEvent event) {
        System.out.println("STORAGE BUTTON TEST");
        Stage stage = (Stage) rBtnStorage.getScene().getWindow();
        String dirPath;

        final DirectoryChooser dirChooser = new DirectoryChooser();

        File file = dirChooser.showDialog(stage);
        if (file != null) {
            dirPath = getDirectory(file);
            receiver.setDir(dirPath);
        } else {
            warningReceiver("Please select a directory to store received files.");
        }
    }

    /**
     * ReceiverController gets a directory from a user.
     * This directory will be where the incoming file will be stored
     *
     * @param file Directory that user selects
     * @return
     */
    public String getDirectory(File file) {
        String dirPath = file.getAbsolutePath();
        int lastSlash = dirPath.lastIndexOf("/");
        String newPath = ".." + dirPath.substring(lastSlash, dirPath.length()) + "/";
        rLabelStorage.setText(newPath);
        dirPath = dirPath + "/";
        return dirPath;
    }

    public void stopThreads() {
        //DO THE SAME FOR RBUDP
        Thread interruptThreadTCP = rTCPThread;
        rTCPThread = null;
        interruptThreadTCP.interrupt();
    }

}
