package org.example.project2;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.function.BiConsumer;

/*
 * This class is used to receive files over TCP.
 */
public class ReceiverTCP {
    private BiConsumer<Integer, Integer> progressUpdate;
    int port;
    ServerSocket serverSocket;
    Socket socketToSender;
    FileOutputStream fileOutStream; 
    ObjectInputStream objectIn;
    InputStream inputStream;
    Meta meta;
    int metaChunks;
     

    String filename;
    String dir; 

    int byteCounter;

    long totalReceived;
    int counter;

    float currentTime, mbPerSec = 0;
    int currentITime = 0;

    /*
     * This constructor initializes the variables.
     * 
     */
    public ReceiverTCP() {

        this.serverSocket = null;  
        this.socketToSender = null;
        this.fileOutStream = null;
        this.objectIn = null; 
        this.meta = null;
        this.byteCounter = 0;

    }

    /*
     * This method is used to set the progress update function.
     * 
     * @param progressUpdate The function to be called when the progress is updated
     * 
     */
    public void setProgressUpdate(BiConsumer<Integer, Integer> progressUpdate) {
        this.progressUpdate = progressUpdate;
    }

    /*
     * This method is used to set the port number.
     * 
     * @param port The port number
     */
    public void setPort(int port) {
        this.port = port;
    }

    /*
     * This method is used to initialize the server socket.
     * 
     * @return true if the server socket is initialized successfully, false otherwise
     * 
     */
    public boolean initServerSocket() {
        try {
            this.serverSocket = new ServerSocket(this.port);
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    /*
     * This method is used to initialize the socket to the sender.
     * 
     * @return true if the socket is initialized successfully, false otherwise
     * 
     */
    public boolean initObjectReceiver() {
        try {
            System.out.println("[ReceiverTCP.java] Waiting for connection...");
            this.socketToSender = this.serverSocket.accept();  /* Blocking call, waits for sender's connection -> returns true */
            socketToSender.setTcpNoDelay(true);
            System.out.println("[ReceiverTCP.java] Connection established...");
            ObjectOutputStream objectOut = new ObjectOutputStream(this.socketToSender.getOutputStream());
            this.objectIn = new ObjectInputStream(this.socketToSender.getInputStream());
            return true;
        } catch (IOException e) {
            System.out.println("Socket Closed");
            return false;
        }
    }
    
    /*
     * This method is used to set the directory to save the file.
     * 
     * @param dir The directory to save the file
     * 
     */
    public void setDir(String dir) {
        this.dir = dir;
    }

    /*
     * This method is used to get the directory to save the file.
     * 
     * @return The directory to save the file
     * 
     */
    public String getDir() {
        return this.dir;
    }

    /*
     * This method is used to get the filename.
     * 
     * @return The filename
     * 
     */
    public void accept() {
        counter = 0;
        try {
            System.out.println("Receiving Meta Data");
            this.meta = (Meta) objectIn.readObject(); /* get meta */
            this.filename = this.meta.getFilename(); 
            this.metaChunks = this.meta.getChunks();
            long size = this.meta.getByteLength();
            
            this.fileOutStream = new FileOutputStream(dir + this.filename); /* create file for output */

            this.inputStream = this.socketToSender.getInputStream();

            byte[] buffer = new byte[8*1024];
            int bytes = 0;
            long startTime = System.currentTimeMillis();
            System.out.println("Receiving File");
            while (size > 0 && (bytes = this.inputStream.read(buffer, 0, (int)Math.min(buffer.length, size))) != -1) {
                this.fileOutStream.write(buffer,0,bytes);
                size -= bytes;      // read upto file size
                totalReceived += bytes;
                if (counter % 5 == 0) {
                    currentTime = ((System.currentTimeMillis() - startTime));
                    mbPerSec = (totalReceived / currentTime) / 1024;
                    currentITime = Math.round(currentTime / 1000);
                    if (progressUpdate != null) {
                        progressUpdate.accept(counter, meta.getChunks());
                    }
                }            
                System.out.println("Progress: chunk " + counter + " of " + metaChunks + " received");
                counter++;
            }
            System.out.println("Process complete\nFile stored in " + this.dir);
        } catch (ClassNotFoundException | IOException e) {
            System.out.println("Socket Closed.");
        }
    }

    /*
     * This method is used to close the socket.
     * 
     */
    public void disconnect() {
        try {
            if (this.socketToSender != null)
                this.socketToSender.close();
            if (this.serverSocket != null)
                this.serverSocket.close();
            if (this.inputStream != null)
                this.inputStream.close();
            if (this.fileOutStream != null)
                this.fileOutStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

}
