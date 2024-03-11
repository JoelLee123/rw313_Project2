package org.example.p2_joel;

import java.io.Serializable;

/**
 * This class contains important data that must
 * be reported for the experiments.
 *
 * It wil serve as the object that we send over
 * TCP and RBUDP connections.
 *
 */
public class Data implements Serializable {

    private int numChunks;
    private int blastSize;
    private long fileSize;
    private int packetSize;
    private int packetTotal;
    private String fileName;
    private int totalPackets;
    private State state;
    private String sequenceList;
    /**
     * This is the constructor that will be used by TCP
     * 
     * @param fileName The file that is sent
     * @param fileSize The file size (in bytes)
     */
    public Data(String fileName, long fileSize) {
        this.fileSize = fileSize;
        this.fileName = fileName;
        numChunks = (int) (fileSize / (8 * 1024));
    }

    public Data(String filename, long fileSize, int packetSize, int blastLength, int totalPackets) {
        fileName = filename;
        this.fileSize = fileSize;
        this.packetSize = packetSize;
        blastSize = blastLength;
        totalPackets = totalPackets;
    }

    public Data() {

    }

    public enum State {NO_MISSING_PACKETS, MISSING_PACKETS};

    public int getNumChunks() {
        return numChunks;
    }

    public int getBlastSize() {
        return blastSize;
    }

    public long getFileSize() {
        return fileSize;
    }

    public int getPacketSize() {
        return packetSize;
    }

    public int getPacketTotal() {
        return packetTotal;
    }

    public String getFileName() {
        return fileName;
    }

    public State getState() {
        return state;
    }

    public void setState(State state) {
        this.state = state;
    }
    public void setSequenceList(String missingSeqNum) {
        if (missingSeqNum != null) {
            sequenceList = missingSeqNum;
        } else {
            System.out.println("missingSeqNum is null");
        }

    }

    public String getSequenceList() {
        return this.sequenceList;
    }
}
