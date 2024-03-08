package org.example.p2_joel;

import java.io.*;
import java.net.Socket;
import java.util.function.BiConsumer;

public class Sender {

    Socket socket;
    OutputStream out;
    InputStream in;
    FileInputStream fileIn;
    String filePath;
    ObjectOutputStream objOut;
    float currTime = 0f;
    private String sIP;
    float MBs = 0f;
    int counter;
    long totalBytes;

    //Used to update the progress bar
    private BiConsumer<Integer, Integer> progressUpdate;

    public void setProgressUpdate(BiConsumer<Integer, Integer> progressUpdate) {
        this.progressUpdate = progressUpdate;
    }

    /**
     * The IP address from the sender GUI is
     * used by TCP and RBUDP
     * @param IP The sender IP address from a textfield
     */
    public void setSenderIP(String IP) {
        sIP = IP;
    }

    /**
        Sets the file to be sent, used by TCP AND RBUDP
     */
    public void setFile(String filePath) {
        try {
            this.filePath = filePath;
            this.fileIn = new FileInputStream(this.filePath);
        } catch (FileNotFoundException e) {
            System.out.println("The file could not be found");
        }
    }


    //START OF TCP METHODS


    /**
        Used to send over an object using TCP
     */
    public void initSenderTCP() {   //initObjectSender()
        try {
            //Null check to avoid null pointer exceptions
            if (objOut == null) {
                objOut = new ObjectOutputStream(socket.getOutputStream());
            }
        } catch (IOException e) {
            System.out.println("AN I/O Exception occurred in initSenderTCP");
            e.printStackTrace();
        }
    }

    /**
     * Sets up a socket used by TCP communication
     */
    public void initSocketTCP() {
        try {
            //Initialization is needed
            if (socket == null || !socket.isConnected()) {
                //An arbitrary port number chosen for TCP
                socket = new Socket(sIP, 1234);
                socket.setTcpNoDelay(true); //TRY COMMENT THIS LINE OUT TO CHECK RESULTS
            }
        } catch (IOException e) {
            System.out.println("An IO Exception occurred in initSocketTCP");
        }
    }

    /**
     * Called by the SenderController class when
     * the TCP option is selected.
     */
    public void sendTCP() {

        try {
            int numBytes;
            //This byte array will be sent across the output stream
            byte[] stream = new byte[8 * 1024];
            long start;
            //long end;
            counter = 0;

            File file = new File(filePath);
            fileIn = new FileInputStream(file);

            //Assuming ubuntu directory structure
            //Use \ for windows
            String fileName = filePath.substring(filePath.lastIndexOf("/") + 1);
            Data data = new Data(fileName, file.length());
            System.out.println("Packing data");     //REMOVE THIS LINE LATER!

            objOut.writeObject(data);
            System.out.println("NOT REACHED");
            objOut.flush();
            System.out.println("Sending data");     //REMOVE THIS LINE LATER!

            //Write to stream
            out = socket.getOutputStream();
            //NOT SURE ABOUT HOW IMPORTANT TIMERS ARE:
            start = System.currentTimeMillis();

            //Reads data from the FileInpuStream into the stream byte array
            //until the end of the file is reached
            while ((numBytes = fileIn.read(stream)) != -1) {
                out.write(stream, 0, numBytes);
                out.flush();
                //Chunk size is set to 10 - don't think we need this
                //This code is for testing purposes and the progress bar
                //Come back to this later?
                totalBytes += numBytes;
                if (counter % 5 == 0) {
                    progressUpdate.accept(counter, data.getNumChunks());
                    currTime = System.currentTimeMillis() - start;
                    MBs = (totalBytes / currTime) / 1024;
                    currTime = Math.round(currTime / 1000);
                    //Add the information for the scroll bar later
                }
                counter++;
            }

            System.out.println(fileName + " has been sent");


        } catch (IOException e) {
            System.out.println("An IO Exception occurred in senderTCP");
            e.printStackTrace();
        }
    }


    //END OF TCP METHODS



    //START OF RBUDP METHODS



    //END OF RBUDP METHODS
}
