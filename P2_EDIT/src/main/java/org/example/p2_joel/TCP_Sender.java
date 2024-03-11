package org.example.p2_joel;

import java.io.*;
import java.net.Socket;
import java.util.function.BiConsumer;

/**
 * The Sender class is responsible for sending files over a network using TCP.
 */
public class TCP_Sender {

    int counter;
    Socket socket;
    InputStream in;
    long totalBytes;
    String filePath;
    OutputStream out;
    private String sIP;
    FileInputStream fileIn;
    ObjectOutputStream objOut;

    // BiConsumer used to update the progress bar in the GUI
    private BiConsumer<Integer, Integer> progressUpdate;

    /**
     * Sets the BiConsumer that updates the progress bar.
     * 
     * @param progressUpdate A BiConsumer that takes two integers (current progress
     *                       and total progress) and updates the progress bar
     *                       accordingly.
     */
    public void setProgressUpdate(BiConsumer<Integer, Integer> progressUpdate) {
        this.progressUpdate = progressUpdate;
    }

    /**
     * Sets the IP address of the senderTCP. This IP address is used for both TCP and
     * RBUDP communication.
     * 
     * @param IP The senderTCP's IP address, as entered in a textfield in the GUI.
     */
    public void setSenderIP(String IP) {
        sIP = IP;
    }

    /**
     * Sets the file to be sent over the network. This method is used for both TCP
     * and RBUDP communication.
     * 
     * @param filePath The path to the file to be sent.
     */
    public void setFile(String filePath) {
        try {
            this.filePath = filePath;
            this.fileIn = new FileInputStream(this.filePath);
        } catch (FileNotFoundException e) {
            System.out.println("The file could not be found");
        }
    }

    /**
     * Initializes the ObjectOutputStream used to send objects over TCP.
     */
    public void initSenderTCP() {
        try {
            // Check if objOut is null to avoid null pointer exceptions
            if (objOut == null) {
                objOut = new ObjectOutputStream(socket.getOutputStream());
            }
        } catch (IOException e) {
            System.out.println("IOException occurred in initSenderTCP()");
        }
    }

    /**
     * Sets up a socket for TCP communication.
     */
    public void initSocketTCP() {
        try {
            // Initialize the socket if it's null or not connected
            if (socket == null || !socket.isConnected()) {
                socket = new Socket(sIP, 1234);
                socket.setTcpNoDelay(true); // Disables Nagle's algorithm for better performance
            }
        } catch (IOException e) {
            System.out.println("IOException occurred in initSocketTCP()");
        }
    }

    /**
     * Sends a file over TCP. This method is called by the SenderController class
     * when the TCP option is selected in the GUI.
     */
    public void sendTCP() {
        try {
            counter = 0;
            int numBytes;
            byte[] stream = new byte[8 * 1024];

            File file = new File(filePath);
            fileIn = new FileInputStream(file);

            // Extract the file name from the file path
            String fileName = filePath.substring(filePath.lastIndexOf("/") + 1);
            Data data = new Data(fileName, file.length());
            System.out.println("Wrapping Data...");

            // Send the Data object over TCP
            objOut.writeObject(data);
            objOut.flush();

            // Prepare to write to the output stream
            out = socket.getOutputStream();
            System.out.println("Sending Data...");

            // Read data from FileInputStream until eof
            while ((numBytes = fileIn.read(stream)) != -1) {
                out.write(stream, 0, numBytes);
                out.flush();
                totalBytes += numBytes; // Keep track of the total number of bytes sent

                if (counter % 2 == 0) {
                    progressUpdate.accept(counter, data.getNumChunks());
                }
                counter++;
            }

        } catch (IOException e) {
            System.out.println("An IO Exception occurred in senderTCP");
            e.printStackTrace();
        }
    }
}