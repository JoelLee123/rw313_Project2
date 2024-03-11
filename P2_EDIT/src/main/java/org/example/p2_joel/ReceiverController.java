package org.example.p2_joel;

import java.io.File;
import java.io.IOException;

import javafx.fxml.FXML;
import javafx.stage.Stage;
import javafx.scene.control.*;
import javafx.event.ActionEvent;
import javafx.application.Platform;
import javafx.stage.DirectoryChooser;

/**
 * Controller class for the Receiver UI.
 * Handles user interaction with the Receiver application.
 */
public class ReceiverController {
    // FXML UI components
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

    private TCP_Receiver receiverTCP; // Instance of Receiver to handle file reception
    private RBUDP_Receiver receiverRBUDP;

    /* Threads for handling TCP and RBUDP reception */
    public volatile Thread rTCPThread;
    public volatile Thread rRBUDPThread;

    public boolean isRunning = true; // Flag to control the receiverTCP's running state

    /**
     * Initializes the ReceiverController and the Receiver instance.
     */
    public void initialize() {
        receiverTCP = new TCP_Receiver(); // Create a new tcp thread
        receiverRBUDP = new RBUDP_Receiver();   //Create the rbudp thread
    }

    /**
     * Displays a warning dialog with a specified message.
     * 
     * @param message The message to display in the warning dialog.
     */
    public static void warningReceiver(String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Receiver Warning");
            alert.setHeaderText(message);
            alert.setContentText("");
            alert.showAndWait();
        });
    }

    /**
     * Handles the Start button click event.
     * Initializes and starts the receiverTCP threads if the IP address and storage
     * location are set.
     * 
     * @param event The ActionEvent triggered by clicking the Start button.
     */
    @FXML
    public void btnStartClicked(ActionEvent event) {
        isRunning = true;
        // Check if IP address field is empty and storage location is set
        if (rFieldIP.getText().isEmpty() || rLabelStorage.getText().equals("Storage Location")) {
            warningReceiver("IP Address cannot be empty and a storage location must be set");
        } else {
            // Disable Start and Storage buttons, enable Stop button
            rBtnStart.setDisable(true);
            rBtnStorage.setDisable(true);
            rBtnStop.setDisable(false);

            // Start a new thread for TCP reception
            rTCPThread = new Thread(() -> {
                receiverTCP.initServerSockTCP(); // Initialize server socket for TCP
                receiverTCP.initReceiverTCP(); // Initialize receiverTCP for TCP
                while (isRunning) { // Keep receiving files while the receiverTCP is running
                    receiverTCP.receive();
                }
            });
            rTCPThread.start(); // Start the TCP reception thread

            rRBUDPThread = new Thread(() -> {
                receiverRBUDP.initSocket();
                while (isRunning) {
                    try {
                        receiverRBUDP.buildMetaData();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                //return null;
            });
            rRBUDPThread.start();
        }
    }

    /**
     * Handles the Stop button click event.
     * Stops the receiverTCP threads and closes the TCP connection.
     * 
     * @param event The ActionEvent triggered by clicking the Stop button.
     */
    @FXML
    public void btnStopClicked(ActionEvent event) {
        isRunning = false; // Set running flag to false to stop receiverTCP threads
        // Re-enable Start and Storage buttons, disable Stop button
        rBtnStart.setDisable(false);
        rBtnStorage.setDisable(false);
        stopThreads(); // Stop the threads
        receiverTCP.closeTCP(); // Close the TCP connection
        // TODO: Close UDP connection if needed
    }

    /**
     * Handles the Storage button click event.
     * Opens a directory chooser and sets the selected directory as the storage
     * location.
     * 
     * @param event The ActionEvent triggered by clicking the Storage button.
     */
    @FXML
    public void btnStorageClicked(ActionEvent event) {
        String directoryPath;
        Stage stage = (Stage) rBtnStorage.getScene().getWindow();
        final DirectoryChooser directoryChoice = new DirectoryChooser();
        File file = directoryChoice.showDialog(stage);
        if (file == null) {
            warningReceiver("Please select a directory to store received files.");
        } else {
            directoryPath = getDirectory(file); // Get the directory path
            receiverTCP.setDirectory(directoryPath); // Set the directory in receiverTCP
        }
    }

    /**
     * Retrieves the directory path from the selected file.
     * Updates the storage location label with the new path.
     * 
     * @param file The directory selected by the user.
     * @return The absolute path to the directory.
     */
    public String getDirectory(File file) {
        String directoryPath = file.getAbsolutePath();
        String newPath = ".." + directoryPath.substring(directoryPath.lastIndexOf("/"),
                directoryPath.length()) + "/";
        directoryPath = directoryPath + "/";
        rLabelStorage.setText(newPath); // Update the storage location label
        return directoryPath;
    }

    /**
     * Stops the receiverTCP threads by interrupting them.
     */
    public void stopThreads() {
        // Interrupt the TCP thread
        Thread interruptThreadTCP = rTCPThread;
        rTCPThread = null;
        if (interruptThreadTCP != null) {
            interruptThreadTCP.interrupt();
        }

        // TODO: Interrupt the RBUDP thread, if it exists
        Thread interruptThreadRBUDP = rRBUDPThread;
        rRBUDPThread = null;
        if (interruptThreadRBUDP != null) {
            interruptThreadRBUDP.interrupt();
        }
    }
}
