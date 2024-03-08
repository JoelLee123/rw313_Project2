package org.example.project2;

import java.net.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.function.BiConsumer;

import java.io.*;
/* This class will be acting as the 'server side' of the local peer and will recieve
 * the files sent from the remote peer client
 */
public class ReceiverUDP {

	int port;
	DatagramSocket datagramSocket;
	ServerSocket serverSocket;
	Socket socket;
	FileOutputStream fileOutputStream;
	File file;
	InetAddress inetAddress;
	String dir;
	String fileName;
	double fileSize;
	int packetSize;
	int blastLength;
	ArrayList<DatagramPacket> receiveBuffer = new ArrayList<>();
	;
	ArrayList<Integer> seqList = new ArrayList<>();
	;
	int NPacketsReceivedSuccessfully;
	int NMissingPackets = 0;
	int totalPackets;
	int timeout;
	int count;
	InputStreamReader inputStreamReader;
	OutputStreamWriter outputStreamWriter;
	BufferedReader bufferedReader;
	PrintWriter printWriter;
	boolean missingPackets;
	double bytesWrittenToFile;
	String senderStatus;
	private BiConsumer<Integer, Integer> progressUpdate;

	ObjectOutputStream objectOutStream;
	ObjectInputStream objectInputStream;

	Meta meta;
	/*
	 * This constructor initializes the variables.
	 */

	long totalReceived;
	float currentTime, mbPerSec = 0;
	int currentITime = 0;

	public ReceiverUDP() {

	}

	/*
	 * This method is used to set the port number.
	 *
	 * @param port The port number to be set
	 */
	public void setPort(int port) {
		this.port = port;
	}

	/*
	 * This method is used to set the directory to save the file.
	 *
	 * @param dir The directory to be set
	 */
	public boolean initSocket() {
		try {
			this.datagramSocket = new DatagramSocket(this.port);
			this.serverSocket = new ServerSocket(this.port);
			socket = serverSocket.accept();

			inputStreamReader = new InputStreamReader(socket.getInputStream());
			bufferedReader = new BufferedReader(inputStreamReader);

			outputStreamWriter = new OutputStreamWriter(socket.getOutputStream());
			printWriter = new PrintWriter(outputStreamWriter);

			this.objectOutStream = new ObjectOutputStream(this.socket.getOutputStream());
			this.objectInputStream = new ObjectInputStream(this.socket.getInputStream());

			return true;
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
	}

	/*
	 * This method is used to set the timeout.
	 *
	 * @param timeout The timeout to be set
	 */
	public void setTimeout(int timeout) throws SocketException {
		this.timeout = timeout;
		datagramSocket.setSoTimeout(0);
	}

	/*
	 * This method is used to set the directory to save the file.
	 *
	 * @param dir The directory to be set
	 */
	public void setDir(String dir) {
		this.dir = dir;
	}

	/*
	 * This method is used to set the directory to save the file.
	 *
	 * @param dir The directory to be set
	 */
	public String getDir() {
		return this.dir;
	}

	/*
	 * This method is used to set the directory to save the file.
	 *
	 * @param dir The directory to be set
	 */
	public void setStatus(String status) {
		senderStatus = status;
		printWriter.write(senderStatus);
		printWriter.flush();
	}

	/*
	 * This method is used to set the directory to save the file.
	 *
	 * @param dir The directory to be set
	 */
	public String getStatus() throws IOException {
		senderStatus = bufferedReader.readLine();
		return senderStatus;
	}

	/*
	 * This method is used to set the directory to save the file.
	 *
	 * @param dir The directory to be set
	 */
	public void setProgressUpdate(BiConsumer<Integer, Integer> progressUpdate) {
		this.progressUpdate = progressUpdate;
	}


	public void buildMetaData() throws ClassNotFoundException {
		/*the first packet recieved will contain the metadata of the file
		 * i.e. filename # fileSize # packetSize # blastLength
		 */
		try {

			this.meta = (Meta) this.objectInputStream.readObject();
			System.out.println("Metadatagram received!");

			fileName = this.meta.getFilename();
			fileSize = this.meta.getUdpFileSize();
			packetSize = this.meta.getUdpPacketSize();
			blastLength = this.meta.getUdpBlastLength();
			totalPackets = this.meta.getUdpTotalPackets();
			fileOutputStream = new FileOutputStream(dir + fileName);
			missingPackets = false;

			/*now that the metadata is all set up, it's time to start building the file */
			startReceiving();
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	/*
	 * This method is used to set the directory to save the file.
	 *
	 * @param dir The directory to be set
	 */
	public void startReceiving() throws IOException, ClassNotFoundException {
		NPacketsReceivedSuccessfully = 0;
		bytesWrittenToFile = 0;
		while (NPacketsReceivedSuccessfully < totalPackets) {
			receive();
			System.out.println("done with receive");
			tcpReceiverConfirmation();
			System.out.println("done with confirm");

		}
		System.out.println("[ReceiverUDP] File has been received!");
		//close();
	}

	/*
	 * This method is used to set the directory to save the file.
	 *
	 * @param dir The directory to be set
	 */
	public void receive() throws IOException {
		/*before writing to the file, the packets will first be added to a buffer, and
		only once each sequence number has been accounted for and order has been established
		will the packets then be written to the file.
		 */
		int i = 1;
		DatagramPacket datagramPacket;
		byte[] buff;
		receiveBuffer.clear();
		seqList.clear();

		/*when the last batch of packets is to be received */
		if (totalPackets - NPacketsReceivedSuccessfully < blastLength) {
			blastLength = totalPackets - NPacketsReceivedSuccessfully;
		}
		long startTime = System.currentTimeMillis();
		while (receiveBuffer.size() < blastLength && NPacketsReceivedSuccessfully <= totalPackets) {
			//while(getStatus().equals("sending")) {
			buff = new byte[packetSize];
			datagramPacket = new DatagramPacket(buff, packetSize);
			/*receive a packet from the sender */
			datagramSocket.receive(datagramPacket);
			totalReceived += packetSize;
			currentTime = ((System.currentTimeMillis() - startTime));
			mbPerSec = (totalReceived / currentTime) / 1024;
			currentITime = Math.round(currentTime / 1000);
			if (progressUpdate != null) {
				progressUpdate.accept(count, totalPackets);
			}
			count++;
			NPacketsReceivedSuccessfully++;
			//System.out.println("Received packet: " + NPacketsReceivedSuccessfully);
			/*add the packet to the buffer */
			receiveBuffer.add(datagramPacket);
			/*add the sequence number of the packet to the sequence List */
			seqList.add((int) buff[0]);
			i++;
		}
	}

	public void receiveMissingPackets() throws IOException, ClassNotFoundException {
		DatagramPacket datagramPacket;
		byte[] buff;
		receiveBuffer.clear();
		seqList.clear();

		int tempTimeout = datagramSocket.getSoTimeout();
		datagramSocket.setSoTimeout(0);/*sets the timeout for missing packets to be received */

		for (int i = 0; i < NMissingPackets; i++) {
			buff = new byte[packetSize];
			datagramPacket = new DatagramPacket(buff, packetSize);
			/*receive a packet from the sender */
			datagramSocket.receive(datagramPacket);

			NPacketsReceivedSuccessfully++;
			//System.out.println("Received packet: " + NPacketsReceivedSuccessfully);
			/*add the packet to the buffer */
			receiveBuffer.add(datagramPacket);
			/*add the sequence number of the packet to the sequence List */
			seqList.add((int) buff[0]);
		}
		datagramSocket.setSoTimeout(tempTimeout);
		tcpReceiverConfirmation();
	}

	/*
	 * This method is used to set the directory to save the file.
	 *
	 * @param dir The directory to be set
	 */
	public void tcpReceiverConfirmation() throws IOException, ClassNotFoundException {
		/*ALWAYS SORT SEQUENCE LIST BEFORE SENDING! */
		//boolean msgDelivered = false;

		//while(!msgDelivered) {
		/*store the sequence list from sender */
		this.meta = (Meta) this.objectInputStream.readObject();
		System.out.println("ObjectRead");
		String seqListFromSender = this.meta.getSequenceList();
		System.out.println(seqListFromSender);
		/*if an empty list is received,  nothing was sent */
		if (seqListFromSender.equals("")) {
			//break
			return;
		}
		/*find the missing sequence numbers */
		String missingSequenceNumbers = findMissingSequenceNumbers(seqListFromSender);

		if (missingSequenceNumbers.equals("empty")) {
			meta = new Meta();
			meta.setState(Meta.State.NO_MISSING_PACKETS);
			this.objectOutStream.writeObject(meta);
			this.objectOutStream.flush();
			System.out.println("write to file");
			writeToFile();
			//msgDelivered = true;
			missingPackets = false;
			NMissingPackets = 0;
		} else {
			meta = new Meta();
			meta.setState(Meta.State.MISSING_PACKETS);
			meta.setSequenceList(missingSequenceNumbers);
			this.objectOutStream.writeObject(meta);
			this.objectOutStream.flush();
			missingPackets = true;
			receiveMissingPackets();
			//break;
			/*break, and go back to receiving packets */
		}
		//}
	}

	/*
	 * This method is used to set the directory to save the file.
	 *
	 * @param dir The directory to be set
	 */
	public void close() {
		try {
			if (datagramSocket != null)
				datagramSocket.close();
			if (serverSocket != null)
				serverSocket.close();
			if (socket != null)
				socket.close();
			if (fileOutputStream != null)
				fileOutputStream.close();
			if (printWriter != null)
				printWriter.close();
			if (bufferedReader != null)
				bufferedReader.close();
		} catch (IOException e) {
			System.out.println("UDP Receiver Stopped");
		}
	}

	/*
	 * This method is used to set the directory to save the file.
	 *
	 * @param dir The directory to be set
	 */
	private void writeToFile() throws IOException {
		/*make sure the packets in receivedBuffer are in order
		 * this can be done by checking in seqList is in order, since they are
		 * populated simultaneously and are therefore parallel.
		 */

		parallelSort();
		for (DatagramPacket dPacket : receiveBuffer) {
			/*writes all of the packets in the buffer to the file */
			if (fileSize - bytesWrittenToFile < packetSize) {
				fileOutputStream.write(dPacket.getData(), 1, (int) (fileSize - bytesWrittenToFile));
				fileOutputStream.flush();
				bytesWrittenToFile += (int) (fileSize - bytesWrittenToFile);
			} else {
				fileOutputStream.write(dPacket.getData(), 1, packetSize - 1);
				fileOutputStream.flush();
				bytesWrittenToFile += (int) (packetSize - 1);
			}
		}
		//System.out.println("Received " + bytesWrittenToFile + " Bytes of " + fileSize + " Bytes");
	}

	/*sorts the sequence list and the receive buffer in parallel */
	private void parallelSort() {
		/*insertion sort. fasted algorithm if packets are already sorted */
		int i, temp1, j;
		DatagramPacket temp2;
		for (i = 1; i < seqList.size(); i++) {
			temp1 = seqList.get(i);
			temp2 = receiveBuffer.get(i);
			j = i - 1;

			while (j >= 0 && seqList.get(j) > temp1) {
				seqList.set(j + 1, seqList.get(j));
				receiveBuffer.set(j + 1, receiveBuffer.get(j));
				j = j - 1;
			}
			seqList.set(j + 1, temp1);
			receiveBuffer.set(j + 1, temp2);
		}
	}

	/*completed */
	private String findMissingSequenceNumbers(String seqListFromSender) {
		/*compares the list of received packet's seqNums with the seqListFromSender
		 * and stores the missing seqNums in a concatenated string missingSequenceNumbers
		 */
		String missingSequenceNumbers = null;
		ArrayList<Integer> missingList = new ArrayList<>();
		String[] arr = seqListFromSender.split("#");
		System.out.println(arr.length);
		ArrayList<Integer> seqFromSender = toArrayList(arr);

		for (int seqNum : seqFromSender) {
			if (!seqList.contains(seqNum)) {
				missingList.add(seqNum);
				NMissingPackets++;
				NPacketsReceivedSuccessfully--;
			}
		}
		if (missingList.isEmpty()) return missingSequenceNumbers = "empty";
		else if (missingList.size() > 1) Collections.sort(missingList);
		/*sort the missing sequence number list in ascending order */

		missingSequenceNumbers = toStringWithDelimiter(missingList);
		return missingSequenceNumbers;
	}

	/**
	 * Auxiliary method to convert an ArrayList to a String.
	 *
	 * @param list
	 * @return String
	 */
	private String toStringWithDelimiter(ArrayList<Integer> list) {
		String out = "";
		for (int i = 0; i < list.size(); i++) {
			out += list.get(i) + "#";
		}
		// out += "\n";
		return out;
	}

	/**
	 * An Auxiliary method to convert a String array to an Queue
	 * of type Integer.
	 *
	 * @param list
	 * @return ArrayList<Integer>
	 */
	private ArrayList<Integer> toArrayList(String[] list) {
		ArrayList<Integer> arrayList = new ArrayList<>();
		for (String c : list) {
			arrayList.add(Integer.parseInt(c));
		}
		return arrayList;
	}
}
