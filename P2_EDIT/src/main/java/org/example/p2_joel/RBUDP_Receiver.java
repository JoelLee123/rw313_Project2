package org.example.p2_joel;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.function.BiConsumer;

/**
 * Handles the reception of files over UDP, confirms received packets, and
 * manages missing packets.
 */
public class RBUDP_Receiver {

	/* Fields for socket communication */
	private Socket socket;
	private PrintWriter printWriter;
	private ServerSocket tcpSocket;
	private DatagramSocket udpSocket;
	private BufferedReader bufferedReader;
	private ObjectOutputStream objectOutStream;
	private ObjectInputStream objectInputStream;

	/* Fields for file handling */
	private String dir;
	private String fileName;
	private long fileSize;
	private int packetSize;
	private int totalPackets;
	private int packetsReceived;
	private long bytesWrittenToFile;
	private InputStreamReader inputStreamReader;
	private OutputStreamWriter outputStreamWriter;
	private FileOutputStream fileOutputStream;

	/* Fields for progress tracking */
	private String sendStatus;
	private int packetCount;
	private int timeout;
	private double MBps;
	private long timeTaken;
	private int packetBlastSize;
	private long currentTime;
	private long totalReceived;
	private int missingPackets;
	private BiConsumer<Integer, Integer> progressUpdate;

	/* Fields for packet tracking */
	private Data metaData;
	private List<Integer> sequenceList = new ArrayList<>();
	private List<DatagramPacket> receivedPackets = new ArrayList<>();

	public RBUDP_Receiver() {

	}

	/**
	 * Initializes the necessary sockets for TCP and UDP communication.
	 * It sets up a DatagramSocket for UDP, tcpSocket and Socket for TCP,
	 * and the necessary input and output streams for data transmission.
	 *
	 * @return true if the sockets and streams are successfully initialized, false
	 *         otherwise.
	 */
	public boolean initSocket() {
		try {
			this.udpSocket = new DatagramSocket(4044); // UDP DatagramSocket
			this.tcpSocket = new ServerSocket(4044); // TCP tcpSocket

			// Wait for and accept incoming TCP connection
			socket = tcpSocket.accept();

			// Set up reader to receive data from the TCP connection
			inputStreamReader = new InputStreamReader(socket.getInputStream());
			bufferedReader = new BufferedReader(inputStreamReader);

			// Set up writer to send data over the TCP connection
			outputStreamWriter = new OutputStreamWriter(socket.getOutputStream());
			printWriter = new PrintWriter(outputStreamWriter);

			// Set up object output stream to send objects over the TCP connection
			this.objectOutStream = new ObjectOutputStream(this.socket.getOutputStream());

			// Set up object input stream to receive objects over the TCP connection
			this.objectInputStream = new ObjectInputStream(this.socket.getInputStream());

			return true;
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
	}

	public void setStatus(String status) {
		sendStatus = status;
		printWriter.write(sendStatus);
		printWriter.flush();
	}

	public String getStatus() throws IOException {
		sendStatus = bufferedReader.readLine();
		return sendStatus;
	}

	public void setDir(String dir) {
		this.dir = dir;
	}

	public String getDir() {
		return this.dir;
	}

	public void setProgressUpdate(BiConsumer<Integer, Integer> progressUpdate) {
		this.progressUpdate = progressUpdate;
	}

	public void setTimeout(int timeout) throws SocketException {
		this.timeout = timeout;
		udpSocket.setSoTimeout(0);
	}

	public void buildMetaData() throws ClassNotFoundException {
		try {

			this.metaData = (Data) this.objectInputStream.readObject();
			System.out.println("Metadatagram received!");

			fileName = this.metaData.getFileName();
			fileSize = this.metaData.getFileSize();
			packetSize = this.metaData.getPacketSize();
			packetBlastSize = this.metaData.getBlastSize();
			totalPackets = this.metaData.getPacketTotal();
			fileOutputStream = new FileOutputStream(dir + fileName);
			// missingPackets = false;

			startReceiving();

		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	public void startReceiving() throws IOException, ClassNotFoundException {
		packetsReceived = 0;
		bytesWrittenToFile = 0;
		while (packetsReceived < totalPackets) {
			receivePackets();
			System.out.println("done with receive");
			tcpReceiverConfirmation();
			System.out.println("done with confirm");

		}
		System.out.println("[ReceiverUDP] File has been received!");
	}

	/**
	 * Receives packets over UDP and stores them in a buffer.
	 * Updates the progress of packet reception and calculates the reception speed.
	 *
	 * @throws IOException if an I/O error occurs.
	 */
	public void receivePackets() throws IOException {
		int remainingPackets = totalPackets - packetsReceived;
		long receiveStart = System.currentTimeMillis();

		receivedPackets.clear();
		sequenceList.clear();

		// Adjust the packetBlastSize if the remaining packets are less than the
		// packetBlastSize
		if (remainingPackets < packetBlastSize) {
			packetBlastSize = remainingPackets;
		}

		while (receivingPackets()) {
			byte[] packetBuffer = new byte[packetSize];
			DatagramPacket udpPacket = new DatagramPacket(packetBuffer, packetSize);
			udpSocket.receive(udpPacket);

			totalReceived += packetSize;

			// Calculate the current time and speed of reception
			currentTime = System.currentTimeMillis() - receiveStart;
			MBps = (totalReceived / currentTime) / 1024;
			timeTaken = Math.round(currentTime / 1000);

			// Update the progress if a progress update function is provided
			if (progressUpdate != null) {
				progressUpdate.accept(packetCount, totalPackets);
			}

			packetCount++;
			packetsReceived++;

			// Add the received packet to the buffer and its sequence number to the sequence
			// number list
			receivedPackets.add(udpPacket);
			sequenceList.add((int) packetBuffer[0]);
		}
	}

	private boolean receivingPackets() {
		return receivedPackets.size() < packetBlastSize && packetsReceived <= totalPackets;
	}

	public void retrieveMissingPackets() throws IOException, ClassNotFoundException {
		DatagramPacket udpPacket;
		byte[] buff;
		receivedPackets.clear();
		sequenceList.clear();

		int tempTimeout = udpSocket.getSoTimeout();
		udpSocket.setSoTimeout(0);

		for (int i = 0; i < missingPackets; i++) {
			buff = new byte[packetSize];
			udpPacket = new DatagramPacket(buff, packetSize);
			udpSocket.receive(udpPacket);

			packetsReceived++;
			receivedPackets.add(udpPacket);
			sequenceList.add((int) buff[0]);
		}
		udpSocket.setSoTimeout(tempTimeout);
		tcpReceiverConfirmation();
	}

	public void tcpReceiverConfirmation() {
		try {
			this.metaData = (Data) this.objectInputStream.readObject();
			String sequenceListFromSender = this.metaData.getSequenceList();
			String missingSequenceNumbers = findMissingSequenceNumbers(sequenceListFromSender);

			if (sequenceListFromSender.equals("")) {
				return;
			}

			if (!missingSequenceNumbers.equals("e")) {
				metaData = new Data();
				metaData.setState(metaData.State.MISSING_PACKETS);
				metaData.setSequenceList(missingSequenceNumbers);
				this.objectOutStream.writeObject(metaData);
				this.objectOutStream.flush();
				retrieveMissingPackets();
			} else {
				metaData = new Data();
				metaData.setState(metaData.State.NO_MISSING_PACKETS);
				this.objectOutStream.writeObject(metaData);
				this.objectOutStream.flush();
				writeToFile();
				missingPackets = 0;
			}
		} catch (IOException | ClassNotFoundException e) {
			if (e instanceof ClassCastException)
				System.err.println("ClassNotFoundException in ReceiverConfirmation");
			else
				System.err.println("IOException in ReceiverConfirmation");
		}
	}

	public void close() {
		try {
			if (fileOutputStream != null)
				fileOutputStream.close();
			if (printWriter != null)
				printWriter.close();
			if (socket != null)
				socket.close();
			if (udpSocket != null)
				udpSocket.close();
			if (tcpSocket != null)
				tcpSocket.close();
			if (bufferedReader != null)
				bufferedReader.close();
		} catch (IOException e) {
			System.out.println("UDP Receiver Stopped");
		}
	}

	private void writeToFile() throws IOException {
		parallelSort();
		for (DatagramPacket dPacket : receivedPackets) {
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
	}

	private void parallelSort() {
		int i, temp1, j;
		DatagramPacket temp2;
		for (i = 1; i < sequenceList.size(); i++) {
			temp1 = sequenceList.get(i);
			temp2 = receivedPackets.get(i);
			j = i - 1;

			while (j >= 0 && sequenceList.get(j) > temp1) {
				sequenceList.set(j + 1, sequenceList.get(j));
				receivedPackets.set(j + 1, receivedPackets.get(j));
				j = j - 1;
			}
			sequenceList.set(j + 1, temp1);
			receivedPackets.set(j + 1, temp2);
		}
	}

	private String findMissingSequenceNumbers(String sequenceListFromSender) {
		String missingSequenceNumbers = null;
		ArrayList<Integer> missingList = new ArrayList<>();
		String[] arr = sequenceListFromSender.split("#");
		ArrayList<Integer> sequenceFromSender = toArrayList(arr);

		for (int sequenceNumber : sequenceFromSender) {
			if (!sequenceList.contains(sequenceNumber)) {
				missingList.add(sequenceNumber);
				packetsReceived--;
				missingPackets++;
			}
		}
		if (missingList.isEmpty())
			return missingSequenceNumbers = "e";
		else if (missingList.size() > 1)
			Collections.sort(missingList);

		missingSequenceNumbers = toStringWithDelimiter(missingList);
		return missingSequenceNumbers;
	}

	private String toStringWithDelimiter(ArrayList<Integer> list) {
		String out = "";
		for (int i = 0; i < list.size(); i++) {
			out += list.get(i) + "#";
		}
		return out;
	}

	private ArrayList<Integer> toArrayList(String[] list) {
		ArrayList<Integer> arrayList = new ArrayList<>();
		for (String c : list) {
			arrayList.add(Integer.parseInt(c));
		}
		return arrayList;
	}
}
