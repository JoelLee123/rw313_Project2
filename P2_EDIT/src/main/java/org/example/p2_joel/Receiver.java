package org.example.p2_joel;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.function.BiConsumer;

//This class will contain methods for UDP and TCP setups
public class Receiver {

    private Socket socket;
    private ServerSocket sSocket;
    private OutputStream out;
    private InputStream in;
    private FileOutputStream fileOut;
    private ObjectOutputStream objOut;
    private ObjectInputStream objIn;
    private Data data;
    private int counter;
    private String fileName;
    private int numChunks;
    private String dir;
    private long fileSize;

    private long MBs;
    private BiConsumer<Integer, Integer> progressUpdate;

    /*
        Receiver constructor starts by clearing all
        streams and socket connections
     */
    public Receiver() {
        socket = null;
        sSocket = null;
        out = null;
        in = null;
        fileOut = null;
        objOut = null;
        objIn = null;
    }

    public void setProgressUpdate(BiConsumer<Integer, Integer> progressUpdate) {
        this.progressUpdate = progressUpdate;
    }

    //START OF TCP METHODS


    public String getDir() {
        return dir;
    }
    public void setDir(String dir) {
        this.dir = dir;
    }

    public void receive() {
       counter = 0;
        try {
            byte[] stream = new byte[8*1024];
            int numBytes = 0;
            long totalBytes = 0;

            System.out.println("Receiving data");
            data = (Data) objIn.readObject();
            numChunks = data.getNumChunks();
            fileSize = data.getFileSize();
            fileName = data.getFileName();

            fileOut = new FileOutputStream(dir + fileName);
            in = socket.getInputStream();

            long start = System.currentTimeMillis();
            System.out.println("Receiving file");

            while (fileSize > 0 && (numBytes = in.read(stream, 0, (int) Math.min(stream.length, fileSize))) != -1) {
                fileOut.write(stream, 0, numBytes);
                fileSize -= numBytes;   //Reading the file reduces the number of bytes left to read
                totalBytes += numBytes;
                //Chunk sizes of 10
                if (counter % 10 == 0) {
                    long currTime = System.currentTimeMillis() - start;
                    //DO NOT UNCOMMENT THIS - WILL GIVE A DIVIDE BY 0 ERROR FOR SMALL FILES
                   // MBs = (totalBytes / currTime) / 1000;
                   // if (progressUpdate != null) {
                    //    progressUpdate.accept(counter, numChunks);
                   // }
                }
                System.out.println("Progress: chunk " + counter + " of " + numChunks + " received");
                counter++;
            }
            System.out.println("Process complete\n" + fileName + " stored in " + dir);


        } catch(IOException | ClassNotFoundException e) {
            System.out.println("Socket closed");
        }
    }

    public void initReceiverTCP() { //initObjectReceiver
        try {
            System.out.println("[ReceiverTCP.java] Waiting for connection...");
            socket = sSocket.accept();
            socket.setTcpNoDelay(true);
            System.out.println("[ReceiverTCP.java] Connection established...");
            objIn = new ObjectInputStream(socket.getInputStream());
            objOut = new ObjectOutputStream(socket.getOutputStream());
        } catch (IOException e) {
            System.out.println("IO Exception in initReceiverTCP");
        }
    }

    public void initServerSockTCP() {
        try {
            //Using the same arbitrary port number as sender for TCP
            sSocket = new ServerSocket(1234);
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
            if (in != null) in.close();
            if (fileOut != null) fileOut.close();
            if (objIn != null) objIn.close();
            if (objOut != null) objOut.close();
            if (sSocket != null) sSocket.close();
            if (socket != null) socket.close();

        } catch (IOException e) {
            System.out.println("AN IO Exception occurred when closing TCP");
            e.printStackTrace();
        }
    }




    //END OF TCP METHODS



    //START OF RBUDP METHODS

    //END OF RBUDP METHODS
}


