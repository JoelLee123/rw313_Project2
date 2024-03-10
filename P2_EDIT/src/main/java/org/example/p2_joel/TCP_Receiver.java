package org.example.p2_joel;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;

public class TCP_Receiver {

    private Data data;
    private String dir;
    private int counter;
    private long fileSize;
    private int numChunks;
    private Socket socket;
    private InputStream in;
    private String fileName;
    private ServerSocket sSocket;
    private ObjectInputStream objIn;
    private FileOutputStream fileOut;
    private ObjectOutputStream objOut;

    /*
     * Receiver constructor starts by clearing all
     * streams and socket connections
     */
    public TCP_Receiver() {
        socket = null;
        sSocket = null;
        in = null;
        fileOut = null;
        objOut = null;
        objIn = null;
    }

    public void receive() {
        counter = 0; // Initialize chunk counter

        try {
            // Initialize byte stream buffer and other variables
            byte[] buffer = new byte[8 * 1024];
            int bytesRead;
            long totalBytesRead = 0;
            long startTime = System.nanoTime();

            // Read metadata from sender
            data = (Data) objIn.readObject();
            numChunks = data.getNumChunks();
            fileSize = data.getFileSize();
            fileName = data.getFileName();

            // Prepare file output stream
            fileOut = new FileOutputStream(dir + fileName);
            in = socket.getInputStream();

            System.out.println("Receiving file: " + fileName);

            // Continue reading until the entire file is received
            while (fileSize > 0) {
                bytesRead = readChunk(buffer);
                if (bytesRead == -1)
                    break; // End of stream reached

                writeChunkToFile(buffer, bytesRead);
                updateProgress(bytesRead, startTime, totalBytesRead);
            }

            printProgress(startTime, totalBytesRead);
            System.out.println("File reception complete: " + fileName + " saved to " + dir);

        } catch (IOException | ClassNotFoundException e) {
            System.out.println("Error during file reception: " + e.getMessage());
        }
    }

    private int readChunk(byte[] buffer) {
        try {
            int maxBytesToRead = (int) Math.min(buffer.length, fileSize);
            return in.read(buffer, 0, maxBytesToRead);
        } catch (IOException e) {
            System.out.println("IOException occurred in readChunk");
            return -1;
        }
    }

    private void writeChunkToFile(byte[] buffer, int bytesRead) {
        try {
            fileOut.write(buffer, 0, bytesRead);
            fileSize -= bytesRead; // Update remaining file size
        } catch (IOException e) {
            System.out.println("IOException occurred in writeChunkToFile");
        }
    }

    private void updateProgress(int bytesRead, long startTime, long totalBytesRead) {
        totalBytesRead += bytesRead; // Update total bytes read
        counter++; // Increment chunk counter

        if (counter % 2 == 0) {
            printProgress(startTime, totalBytesRead);
        }
    }

    private void printProgress(long startTime, long totalBytesRead) {
        long currentTime = System.nanoTime();
        double transferRate = (totalBytesRead / (1024.0 * 1024.0)) / ((currentTime - startTime) / 1000000000.0);
        System.out.printf("Progress: chunk %d of %d received at %.2f MB/s%n", counter, numChunks, transferRate);
    }

    public void initReceiverTCP() { // initObjectReceiver
        try {
            System.out.println("[ReceiverTCP.java] Waiting for connection...");
            socket = sSocket.accept();
            socket.setTcpNoDelay(true);
            System.out.println("[ReceiverTCP.java] Connection established...");
            objIn = new ObjectInputStream(socket.getInputStream());
            objOut = new ObjectOutputStream(socket.getOutputStream());
        } catch (IOException e) {
            System.out.println("IOException occured in initReceiverTCP");
        }
    }

    public void initServerSockTCP() {
        try {
            // Using the same arbitrary port number as sender for TCP
            sSocket = new ServerSocket(4044);
        } catch (IOException e) {
            System.out.println("An IO Exception occurred in initServerSockTCP");
            e.printStackTrace();
        }
    }

    /**
     * Used to close socket and serverSocket connections for TCP
     */
    public void closeTCP() {
        try {
            if (in != null)
                in.close();
            if (fileOut != null)
                fileOut.close();
            if (objIn != null)
                objIn.close();
            if (objOut != null)
                objOut.close();
            if (sSocket != null)
                sSocket.close();
            if (socket != null)
                socket.close();

        } catch (IOException e) {
            System.out.println("AN IO Exception occurred when closing TCP");
            e.printStackTrace();
        }

    }

    public String getDirectory() {
        return dir;
    }

    public void setDirectory(String dir) {
        this.dir = dir;
    }
}
