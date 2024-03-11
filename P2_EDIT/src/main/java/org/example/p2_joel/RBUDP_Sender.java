package org.example.p2_joel;

import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Queue;
import java.util.function.BiConsumer;

/* This class will be acting as the 'client side' of the local peer and will send
 * the files that need to be send to the remote peer client
 *
 */
public class RBUDP_Sender {
    DatagramSocket datagramSocket;
    Socket tcpSocket;
    InetAddress inetAddress;
    int port;
    File file;
    String fileDest;
    FileInputStream fileInputStream;
    int blastLength;
    int packetSize;
    double fileSize;/*total amount of bytes that need to be sent */
    Queue<DatagramPacket> blastBuffer;
    ArrayList<Integer> seqList;
    int NPacketsSentSuccessfully;
    int totalPackets;
    InputStreamReader inputStreamReader;
    OutputStreamWriter outputStreamWriter;
    BufferedReader bufferedReader;
    PrintWriter printWriter;
    int totalBytesSent;
    String status;
    int count;
    private BiConsumer<Integer, Integer> progressUpdate;

    ObjectOutputStream objOut;
    ObjectInputStream objIn;

    Data data;

    long totalSent;
    float currentTime, mbPerSec = 0;
    int currentITime = 0;

    /*
     * This constructor initializes the variables.
     */
    public RBUDP_Sender(){

    }

    /*
     * This method is used to set the progress update function.
     *
     * @param progressUpdate The function to be called when the progress is updated
     *
     */
    public boolean initSocket() {
        try {
            //create a new datagram socket that links to the senderTCP to receiverTCP.
            if (this.datagramSocket != null && this.datagramSocket.isConnected()) {
                return true;
            } else {
                this.datagramSocket = new DatagramSocket();
                return true;
            }
        } catch(IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    /*
     * This method is used to set the progress update function.
     */
    public void setInetAddress(String inetAddress) throws IOException {
        this.inetAddress = InetAddress.getByName(inetAddress);
    }

    /*
     * This method is used to set the progress update function.
     */
    public void setPort(int port) throws IOException {
        this.port = port;

        if (tcpSocket == null) {
            tcpSocket = new Socket(inetAddress, this.port);
        }

        outputStreamWriter = new OutputStreamWriter(tcpSocket.getOutputStream());
        printWriter = new PrintWriter(outputStreamWriter);
        this.objOut = new ObjectOutputStream(this.tcpSocket.getOutputStream());
        this.objIn = new ObjectInputStream(this.tcpSocket.getInputStream());

        bufferedReader = new BufferedReader(new InputStreamReader(tcpSocket.getInputStream()));
    }

    /*
     * This method is used to set the progress update function.
     */
    public boolean setFileDest(String fileDest) {
        try {
            this.fileDest = fileDest;
            this.fileInputStream = new FileInputStream(fileDest);
            return true;
        } catch(FileNotFoundException e) {
            e.printStackTrace();
            return false;
        }
    }

    /*
     * This method is used to set the progress update function.
     */
    public void setPacketSize(int packetSize) {
        this.packetSize = packetSize;
    }

    /*
     * This method is used to set the progress update function.
     */
    public void setBlastLength(int blastLength) {
        this.blastLength = blastLength;
    }

    /*
     * This method is used to set the progress update function.
     */
    private void setFileSize(double fileSize) {
        this.fileSize = fileSize;
    }


    /*
     * This method is used to set the progress update function.
     */
    private int getTotalPackets() {
        /*Calculate the total number of packets that will be needed to transmit the complete file */
        if ((double) (fileSize/(packetSize-1)) % 1 != 0) totalPackets = (int)((double) (fileSize)/(packetSize-1)) + 1;
        else totalPackets = (int)((double) (fileSize)/(packetSize-1));
        return totalPackets;
    }

    /*
     * This method is used to set the progress update function.
     */
    public void setProgressUpdate(BiConsumer<Integer, Integer> progressUpdate) {
        this.progressUpdate = progressUpdate;
    }

    /*
     *
     */
    public void send() {
        try {
            //Loads file in from input directory.
            file = new File(this.fileDest);

            String filename = fileDest.substring(this.fileDest.lastIndexOf("/") + 1);
            setFileSize(file.length());

            //The very first packet will contain  data on the file being sent.
            System.out.println("Packing -data...");
            data = new Data(filename, file.length(), packetSize, blastLength, getTotalPackets());
            this.objOut.writeObject(data);
            this.objOut.flush();
            System.out.println("Sent -data...");

            //start file transmission.
            startSending();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void startSending() {
        try {
            boolean stillHavePacketsToBlast = true;
            int seqNum, seqEnd;
            blastBuffer = new LinkedList<>();
            seqList = new ArrayList<>();

            seqNum = 1;
            seqEnd = blastLength;
            NPacketsSentSuccessfully = 0;
            totalBytesSent = 0;

            /*start blasting until the end of the current window is reached */
            /*leave 3 bytes for unique sequence number
             *	-> scratch that. having a hard time converting a seqNum bigger than 255
             * 		into bytes spanning 2-3 array entries.
             * 	instead, I'll use one entry and wrap seqNum around range(0, 127), (java uses signed int).
             */
            int i = 1;
            int flag = 0;
            long startTime = System.currentTimeMillis();
            while(stillHavePacketsToBlast) {

                if((totalPackets-NPacketsSentSuccessfully) < blastLength && flag == 0) {
                    /*when the amount of packets left are less than the blastLength */
                    seqEnd -= blastLength;
                    blastLength = totalPackets - NPacketsSentSuccessfully;
                    seqEnd += blastLength;
                    flag = 1;
                    System.out.println("Seq End " + seqEnd);
                    System.out.println("Sequence Num" + seqNum);

                }
                /*buff that stores the file data to be packed into the packet */
                byte[] buff = new byte[packetSize];
                /*append the unique sequence number to the first entry in the buff */
                buff[0] = (byte) seqNum;
                /*read (packetSize-1) many bytes from current place in file into the buff */
                fileInputStream.readNBytes(buff, 1, packetSize-1);
                /*create a new datagram packet to be sent off */
                DatagramPacket dp = new DatagramPacket(buff, buff.length, inetAddress, port);
                /*send the created socket to the receiverTCP */
                datagramSocket.send(dp);
                totalSent += packetSize;
                currentTime = ((System.currentTimeMillis() - startTime));
                mbPerSec = (totalSent / currentTime);
                currentITime = Math.round(currentTime / 1000);
                if (progressUpdate != null) {
                    progressUpdate.accept(count, totalPackets);
                }
                count++;
                /*enque the datagram packet into the blastBuffer for retransmission incase any funny shit goes down */
                blastBuffer.add(dp);
                /*add seqNum to sequence list to be checked on receiverTCP side */
                seqList.add(seqNum);
                /*increment seqNum as soon as packet has been sent and buffered */

                /****************************************************************** */
                if(seqNum == seqEnd) {
                    /*blastLength has to be less than 128 */
                    /*when seqNum % blastLength == 0, we send a TCP signal to confirm that all packets were successfully received.
                     * when true
                     * 		seqNum continues
                     * when false
                     * 		->resend the packets whose seqNum's are in the received seqList from receiverTCP.
                     */
                    while(!tcpSenderConfirmation()){
                        //wait (make sure to update NPacketsSentSuccessfully)
                    }
                    //System.out.println("Sent " + totalBytesSent + " Bytes of " + fileSize + " Bytes.");
                    seqList.clear();
                    blastBuffer.clear();
                    seqEnd += blastLength;
                    /*	wrap-around process
                     * 	---------------------------------
                     * 	when the next seqEnd is equal than 127, we reset it to 1
                     */
                    if((seqNum + blastLength) >= 127) {
                        seqNum = 0;	/* */
                        seqEnd = blastLength;
                    }
                }

                seqNum++;
                if(NPacketsSentSuccessfully == totalPackets) stillHavePacketsToBlast = false;
                System.out.println(NPacketsSentSuccessfully + " of " + totalPackets);
                // if (flag == 1) {System.exit(0);}`
                i++;
            }
            //	close();
        } catch(Exception e) {
            e.printStackTrace();
        }
    }

    private boolean tcpSenderConfirmation() throws IOException, ClassNotFoundException{
        boolean packetsConfirmed = false;

        /*if the seqList is not empty, send it over the tcp connection and wait for response */
        if(!seqList.isEmpty()) {
            while(!packetsConfirmed) {
                System.out.println(toStringWithDelimiter(seqList));
                data = new Data();
                data.setSequenceList(toStringWithDelimiter(seqList));
                this.objOut.writeObject(data); /*send seqList delimited by # to receiverTCP. */
                this.objOut.flush();
                /*wait for sequence list with missing packets from the receiverTCP */

                this.data = (Data) this.objIn.readObject();

                /* send the missing packets from the buffer*/
                if(this.data.getState() == Data.State.MISSING_PACKETS) {
                    //setStatus("sending\n");	
                    String seqListReturned = this.data.getSequenceList();
                    blastFromBuffer(seqListReturned);
                    System.out.println("AAAA");
                }
                else {
                    /*this confirms that blastLength many sequences were sent and received. */
                    NPacketsSentSuccessfully += blastLength;
                    totalBytesSent += (packetSize-1)*blastLength;
                    packetsConfirmed = true;
                }
            }
        } else {
            data = new Data();
            data.setSequenceList("");
            this.objOut.writeObject(data);
            this.objOut.flush();
            return true;
            /*seqList is empty which means we're done sending
             *OR
             *it was never populated
             */
        }

        return packetsConfirmed;
    }

    private void blastFromBuffer(String seqListReturned) throws IOException {
        /* this method will blast only the packets that were missing;
         * i.e. the packets whose seqNum are in seqListReturned.
         */
        String[] s = seqListReturned.split("#");
        Queue<Integer> seqReturned = toQueue(s);
        DatagramPacket dPacket;
        Queue<DatagramPacket> tempBlastBuffer = new LinkedList<>();
        ArrayList<Integer> tempSeqList = new ArrayList<>();

        //NPacketsSentSuccessfully += blastLength - (s.length);

        while(!seqReturned.isEmpty()) {
            boolean sent = false;
            while(!sent) {
                /*this assumes that the returned seqList will be IN ORDER */
                dPacket = blastBuffer.remove();
                byte[] buff = dPacket.getData();
                if((int) buff[0] == seqReturned.peek()) {
                    /*send the packet with a seqNum corresponding to the missing
                     * seqNum
                     */
                    datagramSocket.send(dPacket);
                    /*add the seqNum of the re-sent datagram packet to a temporary seqList. */
                    /*the packet with corresponding seqNum has been sent therefore, it can be removed from
                     * receiverTCP's list.
                     */
                    tempSeqList.add(seqReturned.remove());
                    /*add the re-sent packet to a temporary blastBuffer. This ensures that we can blast
                     * these packets continuosly until they are received by the receiverTCP.
                     */
                    tempBlastBuffer.add(dPacket);
                    sent = true;
                }
            }
        }
        seqList = tempSeqList;
        blastBuffer = tempBlastBuffer;
    }

    private void close() {
        try{
            if (datagramSocket != null)
                datagramSocket.close();
            if (tcpSocket != null)
                tcpSocket.close();
            if (fileInputStream != null)
                fileInputStream.close();
            if (printWriter != null)
                printWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    /**
     * Auxiliary method to convert an ArrayList to a String.
     * @param list
     * @return String
     */
    private String toStringWithDelimiter(ArrayList<Integer> list) {
        String out = "";
        for(int i = 0;i < list.size();i++) {
            out += list.get(i) + "#";
        }
        // out += "\n";
        return out;
    }
    /**
     * An Auxiliary method to convert a String array to an Queue
     * of type Integer.
     * @param list
     * @return Queue<Integer>
     */
    private Queue<Integer> toQueue(String[] list) {
        Queue<Integer> queue = new LinkedList<>();
        for(String c:list) {
            queue.add(Integer.parseInt(c));
        }
        return queue;
    }
}
