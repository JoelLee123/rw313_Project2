package org.example.project2;

import java.io.Serializable;


/**
 * This class is used to store file metadata.
 * 
 * It stores the filename and the size of the file in bytes.
 * 
 * It also calculates the number of chunks the file will be split into.
 * 
 * The chunk size is 4k by default.
 * 
 */
public class Meta implements Serializable {
    
    private String filename; 
    private int chunks;
    private long length;

    String sequenceList; 
    private double udpFileSize; 
    private int udpPacketSize;
    private int udpBlastLength;  
    private int udpTotalPackets; 

    private State state; 

    public Meta() {

    }

    /*
     * This constructor is used to store the metadata for TCP files.
     * 
     * It stores the filename and the size of the file in bytes.
     * 
     * @param filename The name of the file
     * @param bytes The size of the file in bytes
     * 
     */
    public Meta(String filename, long bytes) {
        this.filename = filename;
        this.length = bytes;
        this.chunks = (int) (this.length / (8 * 1024));

        if (chunks == 0) this.chunks = 1;
    }

    /*
     * This constructor is used to store the metadata for UDP files.
     * 
     * It stores the filename, the size of the file in bytes, the size of the packets, the blast length, and the total number of packets.
     * 
     * It also calculates the number of chunks the file will be split into.
     * 
     * @param filename The name of the file
     * @param fileSize The size of the file in bytes
     * @param packetSize The size of the packets in bytes
     * @param blastLength The number of packets to be sent at once
     * @param totalPackets The total number of packets to be sent
     * 
     */
    public Meta(String filename, double fileSize, int packetSize, int blastLength, int totalPackets) {
        this.filename = filename; 
        this.udpFileSize = fileSize; 
        this.udpPacketSize = packetSize;
        this.udpBlastLength = blastLength;  
        this.udpTotalPackets = totalPackets; 
    }

    public enum State {NO_MISSING_PACKETS, MISSING_PACKETS}; 

    public State getState() {
        return this.state; 
    }

    public String getSequenceList() {
        return this.sequenceList;
    }

    public void setSequenceList(String missingSeqNum) {
        this.sequenceList = missingSeqNum; 
    }

    public void setState(State state) {
        this.state = state; 
    }

    public int getChunks() {
        return chunks;
    }

    public String getFilename() {
        return this.filename;
    }

    public long getByteLength() {
        return this.length;
    } 

    public double getUdpFileSize() {
        return this.udpFileSize;
    }

    public int getUdpPacketSize() {
        return this.udpPacketSize;
    }

    public int getUdpBlastLength() {
        return this.udpBlastLength;
    }

    public int getUdpTotalPackets() {
        return this.udpTotalPackets;
    }

}
