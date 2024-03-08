package org.example.project2;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.function.BiConsumer;

/*
 * This class is used to send files over TCP.
 */
public class SenderTCP {
    String fileDest;
    FileInputStream fileInputStream; 
    Socket socket; 

    ObjectOutputStream objectOutputStream; 
    OutputStream outputStream; 
    
    String IP;
    int port;

    private BiConsumer<Integer, Integer> progressUpdate;

    long totalSent;
    int count;

    float currentTime, mbPerSec = 0;
    int currentITime = 0;

    /*
     * This constructor initializes the variables.
     * 
     */
    public SenderTCP() {
        
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
     * This method is used to send the file.
     * 
     * @param file The file to be sent
     * @param IP The IP address of the receiver
     * @param port The port number of the receiver
     * 
     */
    public boolean initSocket() {
        try {
            if (this.socket != null && this.socket.isConnected()) {
                return true;
            } else {
                this.socket = new Socket(this.IP, this.port);
                this.socket.setTcpNoDelay(true);
                return true;
            }
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    /*
     * This method is used to send the file.
     */
    public boolean initObjectSender() {
        try {
            if (this.objectOutputStream != null) {
                return true;
            } else {
                this.objectOutputStream = new ObjectOutputStream(this.socket.getOutputStream());
                return true;
            }
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    /*
     * This method is used to set the IP address of the receiver.
     */
    public void setIP(String IP) {
        this.IP = IP;
    }

    /*
     * This method is used to set the port number of the receiver.
     */
    public void setPort(int port) {
        this.port = port;
    }

    /*
     * This method is used to set the file to be sent.
     */
    public boolean setFileDest(String fileDest) {
        try {
            this.fileDest = fileDest;
            this.fileInputStream = new FileInputStream(this.fileDest);
            return true;
        } catch (FileNotFoundException e) {
            return false;
        }
    }

    /*
     * This method is used to send the file.
     */
    public void send() {
        count = 0;
        try {

            /* read file */
            File file = new File(this.fileDest);
            this.fileInputStream = new FileInputStream(file);

            /* pack meta data to send */
            String filename = fileDest.substring(this.fileDest.lastIndexOf("/") + 1);
            Meta meta = new Meta(filename, file.length());
            System.out.println("Packing meta-data...");

            /* send meta data */
            this.objectOutputStream.writeObject(meta);
            this.objectOutputStream.flush();
            System.out.println("Sending meta-data...");

            /* semd file */
            this.outputStream = this.socket.getOutputStream();
            int bytes;
            
            byte[] buffer = new byte[8 * 1024];
            long startTime = System.currentTimeMillis();
            
            while ((bytes = this.fileInputStream.read(buffer)) != -1) {
                this.outputStream.write(buffer, 0, bytes);
                this.outputStream.flush();
                totalSent += bytes;
                if (count % 5 == 0) {
                    currentTime = ((System.currentTimeMillis() - startTime));
                    mbPerSec = (totalSent / currentTime) / 1024;
                    currentITime = Math.round(currentTime / 1000);
                    if (progressUpdate != null) {
                        progressUpdate.accept(count, meta.getChunks());
                    }      
                }
                
                count++;
            }
        
            System.out.println("File sent");

        } catch (IOException e) {

            e.printStackTrace();
        }
    }

}
