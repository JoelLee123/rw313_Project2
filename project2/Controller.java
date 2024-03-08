package org.example.project2;

import java.io.File;
import java.io.IOException;
import java.net.SocketException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javafx.application.Platform;
import javafx.concurrent.Service;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.RadioButton;
import javafx.scene.control.Slider;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleGroup;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

/* 
 * This is the controller class for the application. It is responsible for handling all of the
 * user input and updating the GUI.
 *
 */
public class Controller {

    public static SenderTCP tcpSender;
    public static ReceiverTCP tcpReceiver;
    public static SenderUDP udpSender;
    public static ReceiverUDP udpReceiver;
    public static Color bgColor;
    public static ScheduledExecutorService sExecService;

    public volatile Thread receiverTCPThread;
    public volatile Thread receiverUDPThread;
    public volatile boolean run = true;

    private long fileSize;
    private double kilobytes;
    private double megabytes;
    private double gigabytes;

    @FXML
    private Label selectedFileLabel;

    @FXML
    private Label fileSizeLabel;

    @FXML
    private Label selectedDirLabel;

    @FXML
    private Label socketInformationLabel;

    @FXML
    private Label socketStatusLabel;

    @FXML
    private TextField tcpPort;

    @FXML
    private TextField udpPort;

    @FXML
    private TextField ipAddress;

    @FXML
    private Button sendFileButton;

    @FXML
    private Button fileButton;

    @FXML
    private Button startReceiverButton;

    @FXML
    private Button stopReceiverButton;

    @FXML
    private Button receiverDirectoryButton;

    @FXML
    private Rectangle fileRectangle;

    @FXML
    private Rectangle protocolRectangle;

    @FXML
    private RadioButton tcpRadio;

    @FXML
    private RadioButton udpRadio;

    @FXML
    private Slider blastLengthSlider;

    @FXML
    private Slider packetSizeSlider;

    @FXML
    private ProgressBar fileProgressBar;

    @FXML
    private CategoryAxis xAxis;

    @FXML
    private NumberAxis yAxis;

    @FXML
    private LineChart<String, Number> analyticsChart;

    public static XYChart.Series<String, Number> s;

    /*
     * This method is called when the application is started. It initializes all of the
     * necessary variables and objects.
     * 
     */
    public void initialize() {

        sExecService = Executors.newSingleThreadScheduledExecutor();

        ToggleGroup connectionRadio = new ToggleGroup();
        tcpRadio.setToggleGroup(connectionRadio);
        udpRadio.setToggleGroup(connectionRadio);

        blastLengthSlider.disableProperty().bind(tcpRadio.selectedProperty());
        packetSizeSlider.disableProperty().bind(tcpRadio.selectedProperty());

        bgColor = Color.web("#414a50");

        tcpSender = new SenderTCP();
        udpSender = new SenderUDP();
        tcpReceiver = new ReceiverTCP();
        udpReceiver = new ReceiverUDP();

        s = new XYChart.Series<>();
        analyticsChart.getData().add(s);
        analyticsChart.setAnimated(false);
    }

    /*
     * This method is called when the user clicks the "Choose File" button. It opens a
     * file chooser and allows the user to select a file to send.
     * 
     * @param file The file that the user selected.
     * 
     */
    private String getFileDirectory(File file) {
        String filePath = file.getAbsolutePath();
        int lastSlash = filePath.lastIndexOf("/");
        String newPath = ".." + filePath.substring(lastSlash, filePath.length());
        fileSize = file.length();
        kilobytes = (double) (fileSize / 1000.0);
        megabytes = kilobytes / 1000.0;
        gigabytes = megabytes / 1000.0;
        selectedFileLabel.setText(newPath);
        if (fileSize < 1000) {
            fileSizeLabel.setText(Long.toString(fileSize) + " Bytes");
        } else if (fileSize >= 1000.0 && fileSize < 1000000.0) {
            fileSizeLabel.setText(String.format("%,.2f", kilobytes) + " KB");
        } else if (fileSize >= 1000000.0 && fileSize < 1000000000.0) {
            fileSizeLabel.setText(String.format("%,.2f", megabytes) + " MB");
        } else if (fileSize >= 1000000000.0 && fileSize < 1000000000000.0) {
            fileSizeLabel.setText(String.format("%,.2f", gigabytes) + " GB");
        }
        return filePath;
    }

    /*
     * This method is called when the user clicks the "Choose Directory" button. It
     * opens a directory chooser and allows the user to select a directory to save
     * received files.
     * 
     * @param file The directory that the user selected.
     */
    private String getDirectory(File file) {
        String dirPath = file.getAbsolutePath();
        int lastSlash = dirPath.lastIndexOf("/");
        String newPath = ".." + dirPath.substring(lastSlash, dirPath.length()) + "/";
        selectedDirLabel.setText(newPath);
        dirPath = dirPath + "/";
        return dirPath;
    }

    /*
     * This method is called when the user clicks the "Choose File" button. It opens a
     * file chooser and allows the user to select a file to send.
     * 
     * @param event The event that triggered the method.
     */
    public void openFileButtonAction(ActionEvent event) {
        Stage stage = (Stage) sendFileButton.getScene().getWindow();
        String filePath;

        final FileChooser fileChooser = new FileChooser();

        File file = fileChooser.showOpenDialog(stage);
        if (file != null) {
            filePath = getFileDirectory(file);
            tcpSender.setFileDest(filePath);
            udpSender.setFileDest(filePath);
        } else {
            errorDialog("Please select a file to send.");
        }
    }

    /*
     * This method is called when the user clicks the "Choose Directory" button. It
     * opens a directory chooser and allows the user to select a directory to save
     * received files.
     * 
     * @param event The event that triggered the method.
     */
    public void openDirectoryButtonAction(ActionEvent event) {
        Stage stage = (Stage) sendFileButton.getScene().getWindow();
        String dirPath;

        final DirectoryChooser dirChooser = new DirectoryChooser();

        File file = dirChooser.showDialog(stage);
        if (file != null) {
            dirPath = getDirectory(file);
            tcpReceiver.setDir(dirPath);
            udpReceiver.setDir(dirPath);
        } else {
            errorDialog("Please select a directory to store received files.");
        }
    }

    /*
     * This method is called when the user clicks the "Start Receiver" button. It
     * starts the receiver thread.
     * 
     * @param event The event that triggered the method.
     */
    @FXML
    public void sendFileButtonAction(ActionEvent event) {
        if (tcpRadio.isSelected()) {
            if (ipAddress.getText().equals("") || ipAddress.getText() == null || tcpPort.getText().equals("")
                    || tcpPort.getText() == null) {
                errorDialog("Please enter a IP Address and TCP Port Number to attempt sending a file.");
            } else if (selectedFileLabel.getText().equals("No File Currently Selected")) {
                errorDialog("Please select a file before trying to send.");
            } else {
                s.getData().clear();
                tcpSender.setIP(ipAddress.getText());
                tcpSender.setPort(Integer.parseInt(tcpPort.getText()));
                tcpSender.initSocket();
                tcpSender.initObjectSender();
                updateSenderChart();
                Service<Void> progressBarService = new Service<Void>() {
                    @Override
                    protected Task<Void> createTask() {
                        return new Task<Void>() {
                            @Override
                            protected Void call() throws Exception {
                                tcpSender.setProgressUpdate(
                                        (workDone, totalWork) -> updateProgress(workDone, totalWork));
                                tcpSender.send();
                                //sExecService.shutdownNow();
                                return null;
                            }
                        };
                    }
                };
                fileProgressBar.progressProperty().bind(progressBarService.progressProperty());
                progressBarService.restart();
            }
        } else if (udpRadio.isSelected()) {
            if (ipAddress.getText().equals("") || ipAddress.getText() == null || udpPort.getText().equals("")
                    || udpPort.getText() == null) {
                errorDialog("Please enter a IP Address and UDP Port Number to attempt sending a file.");
            } else if (selectedFileLabel.getText().equals("No File Currently Selected")) {
                errorDialog("Please select a file before trying to send.");
            } else {
                s.getData().clear();
                int packetSize = (int) packetSizeSlider.getValue();
                int blastLength = (int) blastLengthSlider.getValue();
                try {
                    udpSender.setInetAddress(ipAddress.getText());
                    udpSender.initSocket();
                    udpSender.setPort(Integer.parseInt(udpPort.getText()));
                    updateSenderChart();

                    if (packetSize > 0) {
                        udpSender.setPacketSize(packetSize);
                    } else {
                        errorDialog("Please select a packet size greater than 0");
                    }
                    if (blastLength > 0) {
                        udpSender.setBlastLength(blastLength);
                    } else {
                        errorDialog("Please select blast length greater than 0");
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
                Service<Void> progressBarService = new Service<Void>() {
                    @Override
                    protected Task<Void> createTask() {
                        return new Task<Void>() {
                            @Override
                            protected Void call() throws Exception {
                                udpSender.setProgressUpdate(
                                        (workDone, totalWork) -> updateProgress(workDone, totalWork));
                                udpSender.send();
                                return null;
                            }
                        };
                    }
                };
                fileProgressBar.progressProperty().bind(progressBarService.progressProperty());
                progressBarService.restart();
            }
        }
    }

    /*
     * This method is called when the user clicks the "Start Receiver" button. It
     * starts the receiver thread.
     * 
     * @param event The event that triggered the method.
     */
    public void startReceiverButtonAction(ActionEvent event) {
        if (ipAddress.getText().equals("") || ipAddress.getText() == null || tcpPort.getText().equals("")
                || tcpPort.getText() == null || udpPort.getText().equals("") || udpPort.getText() == null
                || selectedDirLabel.getText().equals("No Directory Selected")) {
            errorDialog("Please ensure all connection information is filled in correctly before starting the Receiver");
        } else if (tcpPort.getText().equals(udpPort.getText())) {
            errorDialog("Please ensure that the TCP port number and UDP port number is not the same.");
        } else {
            configureReceiverStart();
            receiverTCPThread = new Thread(new Runnable() {
                @Override
                public void run() {
                    tcpReceiver.setPort(Integer.parseInt(tcpPort.getText()));
                    tcpReceiver.initServerSocket();
                    tcpReceiver.initObjectReceiver();
                    updateTCPReceiverChart();
                    Service<Void> progressBarService = new Service<Void>() {
                        @Override
                        protected Task<Void> createTask() {
                            return new Task<Void>() {
                                @Override
                                protected Void call() throws Exception {
                                    tcpReceiver.setProgressUpdate(
                                            (workDone, totalWork) -> updateProgress(workDone, totalWork));
                                    while (run) {
                                        tcpReceiver.accept();
                                    }
                                    return null;
                                }
                            };
                        }
                    };
                    fileProgressBar.progressProperty().bind(progressBarService.progressProperty());
                    progressBarService.restart();
                }
            });
            receiverTCPThread.start();

            receiverUDPThread = new Thread(new Runnable() {
                @Override
                public void run() {
                    udpReceiver.setPort(Integer.parseInt(udpPort.getText()));
                    udpReceiver.initSocket();
                    updateUDPReceiverChart();
                    Service<Void> progressBarService = new Service<Void>() {
                        @Override
                        protected Task<Void> createTask() {
                            return new Task<Void>() {
                                @Override
                                protected Void call() throws Exception {
                                    udpReceiver.setProgressUpdate(
                                            (workDone, totalWork) -> updateProgress(workDone, totalWork));
                                    while (run) {
                                        udpReceiver.buildMetaData();
                                    }
                                    return null;
                                }
                            };
                        }
                    };
                    fileProgressBar.progressProperty().bind(progressBarService.progressProperty());
                    progressBarService.restart();
                }
            });
            receiverUDPThread.start();
        }
    }

    /*
     * This method is called when the user clicks the "Stop Receiver" button. It
     * stops the receiver thread.
     * 
     * @param event The event that triggered the method.
     */
    public void stopReceiverButtonAction(ActionEvent event) {
        run = false;
        stopThread();
        tcpReceiver.disconnect();
        udpReceiver.close();
        configureReceiverStop();
    }

    /*
     * This method is called when the user clicks the "Stop Sender" button. It stops
     * the sender thread.
     * 
     */
    public void updateSenderChart() {
        if(tcpRadio.isSelected()) {
            sExecService.scheduleAtFixedRate(() -> {
                // Update the chart
                Platform.runLater(() -> {
                    s.getData().add(new XYChart.Data<>(Integer.toString(tcpSender.currentITime), tcpSender.mbPerSec));
                });
            }, 0, 1, TimeUnit.SECONDS);
        } else if (udpRadio.isSelected()) {
            sExecService.scheduleAtFixedRate(() -> {
                // Update the chart
                Platform.runLater(() -> {
                    s.getData().add(new XYChart.Data<>(Integer.toString(udpSender.currentITime), udpSender.mbPerSec));
                });
            }, 0, 1, TimeUnit.SECONDS);
        }
    }

    public void updateTCPReceiverChart() {
        sExecService.scheduleAtFixedRate(() -> {
            // Update the chart
            Platform.runLater(() -> {
                s.getData().add(new XYChart.Data<>(Integer.toString(tcpReceiver.currentITime), tcpReceiver.mbPerSec));
            });
        }, 0, 1, TimeUnit.SECONDS);
    }
    /*
     * This method is called when the user clicks the "Stop Sender" button. It stops
     * the sender thread.
     * 
     */
    public void updateUDPReceiverChart() {
        sExecService.scheduleAtFixedRate(() -> {
            // Update the chart
            Platform.runLater(() -> {
                s.getData().add(new XYChart.Data<>(Integer.toString(udpReceiver.currentITime), udpReceiver.mbPerSec));
            });
        }, 0, 1, TimeUnit.SECONDS);
    }

    /*
     * This method is called when the user clicks the "Stop Sender" button. It stops
     * the sender thread.
     * 
     */
    public void configureReceiverStart() {
        sendFileButton.setDisable(true);
        tcpPort.setDisable(true);
        udpPort.setDisable(true);
        ipAddress.setDisable(true);
        receiverDirectoryButton.setDisable(true);
        startReceiverButton.setOpacity(0.0);
        startReceiverButton.setDisable(true);
        stopReceiverButton.setOpacity(1.0);
        stopReceiverButton.setDisable(false);
        fileRectangle.setFill(bgColor);
        fileRectangle.toFront();
        protocolRectangle.setFill(bgColor);
        protocolRectangle.toFront();
        socketInformationLabel.setOpacity(1.0);
        socketInformationLabel.toFront();
        socketStatusLabel.setOpacity(1.0);
        socketStatusLabel.toFront();
    }

    /*
     * This method is called when the user clicks the "Stop Sender" button. It stops
     * the sender thread.
     * 
     */
    public void configureReceiverStop() {
        tcpPort.setDisable(false);
        udpPort.setDisable(false);
        ipAddress.setDisable(false);
        receiverDirectoryButton.setDisable(false);
        startReceiverButton.setOpacity(1.0);
        startReceiverButton.setDisable(false);
        stopReceiverButton.setOpacity(0.0);
        stopReceiverButton.setDisable(true);
        sendFileButton.setDisable(false);
        fileRectangle.setFill(Color.TRANSPARENT);
        fileRectangle.toBack();
        protocolRectangle.setFill(Color.TRANSPARENT);
        protocolRectangle.toBack();
        socketInformationLabel.setOpacity(0.0);
        socketInformationLabel.toBack();
        socketStatusLabel.setOpacity(0.0);
        socketStatusLabel.toBack();
    }

    /*
     * This method is called when the user clicks the "Stop Sender" button. It stops
     * the sender thread.
     * 
     */
    public void stopThread() {
        Thread interruptThread1 = receiverTCPThread;
        Thread interruptThread2 = receiverUDPThread;
        receiverTCPThread = null;
        receiverUDPThread = null;
        interruptThread1.interrupt();
        interruptThread2.interrupt();
        sExecService.shutdownNow();
    }

    /*
     * This method is called when the user clicks the "Stop Sender" button. It stops
     * the sender thread.
     * 
     */
    public static void errorDialog(String error) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Warning!");
            alert.setHeaderText(error);
            alert.setContentText("");
            alert.showAndWait();
        });
    }
}