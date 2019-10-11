/**
 * FastFtp Class
 *
 * @author Austin Brodeur
 * @version 1.0
 *
 */

import java.net.*;
import java.util.*;
import java.io.*;
import java.nio.file.*;
import cpsc441.a3.shared.*;

public class FastFtp {

	private final int MAX_SEGMENT_SIZE = 1000;
	private final int THREAD_KILL_DELAY = 500;

	private Socket tcpConnection;
	private DatagramSocket udpSocket;
	private ReceiverThread receiverThread;
	private Thread timeoutManager;
	private Integer windowSize;
	private Integer nextSequence = 0;
	private Integer timeoutTime;
	private Integer[] retransmitCount = new Integer[1];
	private TxQueue transmitQueue;
	private Timer timer = new Timer(true);

	/**
     * Constructor to initialize the program 
     * 
     * @param windowSize	Size of the window for Go-Back-N in terms of segments
     * @param rtoTimer		The time-out interval for the retransmission timer
     */
	public FastFtp(Integer wSize, Integer rtoTimer) {
		windowSize = wSize;
		timeoutTime = rtoTimer;
		transmitQueue = new TxQueue(windowSize);
	}

	/**
	 * Attempts to get the UDP port from the server. Will quit after 10 tries.
	 *
	 * @param serverName	Server address or name
	 * @param serverTCPPort	Server TCP port for handshake
	 * @param fileName		Path of file to be transferred
	 * @return				UDP port of the server retrieved by the handshake
	 */
	public int getServerUDP(String serverName, Integer serverTCPPort, String fileName) {
		Integer serverUDPPort = -1;
		Integer retry_count = 1;
		while (serverUDPPort == -1) {
			try {
				System.out.println("Trying to obtain UDP port from server... Attempt " + retry_count);

				createSendUdp();

				serverUDPPort = openTCP(serverName, serverTCPPort, fileName);
				if (serverUDPPort == -1) {
					throw new Exception("UDP port could not be received from the server");
				}
				System.out.println("Server UDP port received: " + serverUDPPort);
			} catch (Exception e) {
				System.out.println("Error getting UDP port from server: ");
				e.printStackTrace();
			}
			if (retry_count == 10) {
				throw new RuntimeException();
			}
			retry_count += 1;
		}
		return serverUDPPort;
	}

	/**
	 * Initializes the threads required for the send method
	 *
	 * @param serverName	Server address or name
	 * @param serverUDPPort	Server UDP port from handshake
	 * @param fileChunks	2D byte array of file after being divided into chunks
	 */
	public void initializeThreads(String serverName, Integer serverUDPPort, byte[][] fileChunks) {
		timeoutManager = new Thread(new TimeoutManager(transmitQueue, udpSocket, serverName, serverUDPPort, timeoutTime, timer, retransmitCount));
		timeoutManager.start();

		receiverThread = new ReceiverThread(udpSocket, transmitQueue, fileChunks.length, serverName, serverUDPPort);
		receiverThread.start();
	}


	/**
	 * Cleans up threads and generates report after file is transferred
	 */
	public void teardownSend() {
		try {
			System.out.println("Number of retransmits: " + retransmitCount[0]);
			System.out.println("Closing ports and stopping client..");
			receiverThread.requestStop();
			Thread.sleep(THREAD_KILL_DELAY);
			timer.cancel();
			timeoutManager.interrupt();
			tcpConnection.close();
		}
		catch (Exception e) {
			System.out.println("Error tearing down: ");
			e.printStackTrace();
		}
	}




    /**
     * Sends the specified file to the specified destination host:
     * 1. send file/connection infor over TCP
     * 2. start receving thread to process coming ACKs
     * 3. send file segment by segment
     * 4. wait until transmit queue is empty, i.e., all segments are ACKed
     * 5. clean up (cancel timer, interrupt receving thread, close sockets/files)
     * 
     * @param serverName	Name of the remote server
     * @param serverPort	Port number of the remote server
     * @param fileName		Name of the file to be trasferred to the remote server
     */
	public void send(String serverName, Integer serverPort, String fileName) {
		Integer serverUdpPort;
		byte[][] fileChunks;
		retransmitCount[0] = 0;

		try {
			fileChunks = splitFile(filetoBytes(fileName));
			System.out.println("Number of chunks: " + fileChunks.length);

			createSendUdp();
			serverUdpPort = getServerUDP(serverName, serverPort, fileName);
			initializeThreads(serverName, serverUdpPort, fileChunks);

			for (byte[] fileChunk : fileChunks) {
				addToQueue(fileChunk, serverName, serverUdpPort);
				reTransmit(serverName, serverUdpPort);
			}
			while (true) {
				if (transmitQueue.isEmpty()) {
					teardownSend();
					return;
				}
			}
		}
		catch (Exception e) {
			System.out.println("Error in send method: " + e);
			System.exit(1);
		}
	}
	

	/**
	  * Opens TCP connection with the server and does the initial handshake. Returns UDP port.
	  *
	  * @param sname 	Server name
	  * @param port 	Port for the server
	  * @param fname 	Name of file to be sent
	  * @return sPort 	Port for the UDP connection of the server. If this is -1 after the method is finished running, there was an error.
	**/
	public Integer openTCP(String sname, Integer port, String fname)
	{
		DataInputStream dataIn = null;
		DataOutputStream dataOut = null;
		File file;
		Integer udpPort = -1;

		try {
			tcpConnection = new Socket(sname, port);

			dataIn = new DataInputStream(tcpConnection.getInputStream());
			dataOut = new DataOutputStream(tcpConnection.getOutputStream());
		}
		catch (IOException e) {
			System.out.println("Error creating initial TCP connection: " + e);
		}

		try {
			file = new File(fname);

			dataOut.writeUTF(fname);
			dataOut.writeLong(file.length());
			dataOut.writeInt(udpSocket.getLocalPort()); //Port for the UDP connection of the client
			dataOut.flush();
			udpPort = dataIn.readInt();
		}
		catch (Exception e) {
			System.out.println("Error in TCP handshake: " + e);
		}
		return udpPort;
	}


	/**
	  * Opens UDP connection with the server
	**/
	public void createSendUdp()
	{
		try {
			udpSocket = new DatagramSocket();
		}
		catch(IOException e) {
			System.out.println("Error opening DatagramSocket: " + e);
		}
	}



	/**
	  * Converts file into an array of bytes
	  *
	  * @param fpath	The filepath of the file to be converted into byte array
	  * @return byte[]	Byte array of file after conversion
	**/
	public byte[] filetoBytes(String fpath)
	{
		Path file = Paths.get(fpath);
		byte[] res = null;
		try {
			res = Files.readAllBytes(file);
		}
		catch (Exception e) {
			System.out.println("Error converting file into byte array: " + e);
		}
		return res;
	}



	/**
	  * Divides the full byte array of the entire file into the segments to be sent in
	  * taken from https://stackoverflow.com/questions/3405195/divide-array-into-smaller-parts
	  * 
	  * @param fullPayload	The full byte array payload to be divided
	  * @return byte[][]	Divided byte array
	**/
	public byte[][] splitFile(byte[] fullPayload)
	{
		byte[][] ret = new byte[(int)Math.ceil(fullPayload.length / (double)MAX_SEGMENT_SIZE)][];
		Integer start = 0;
		for (int i = 0; i < ret.length; i++) {
			if (start + MAX_SEGMENT_SIZE > fullPayload.length) {
				ret[i] = new byte[fullPayload.length-start];
				System.arraycopy(fullPayload, start, ret[i], 0, fullPayload.length - start);
			}
			else {
				ret[i] = new byte[MAX_SEGMENT_SIZE];
				System.arraycopy(fullPayload, start, ret[i], 0, MAX_SEGMENT_SIZE);
			}
			start += MAX_SEGMENT_SIZE;
		}
		return ret;
	}



	/**
	  * Adds new segment to queue and transmits.
	  *
	  * @param chunk 	Chunk to be added to the queue
	  * @param serverName	Address of the server
	  * @param sPort 	UDP port of the server
	**/
	public synchronized void addToQueue(byte[] chunk, String serverName, Integer sPort)
	{
		try {
			Segment segmentSend = new Segment(nextSequence, chunk);
			DatagramPacket sendPacket = new DatagramPacket(segmentSend.getBytes(), segmentSend.getBytes().length, InetAddress.getByName(serverName), sPort);
			udpSocket.send(sendPacket);
			transmitQueue.add(segmentSend);
		}
		catch (Exception e) {
			System.out.println("Error adding chunk to queue: " + e);
		}
		nextSequence += 1;
	}


	/**
	  * Retransmits the queue if there is no difference in the queue after the timeout delay.
	  *
	  * @param serverName	Address of the server
	  * @param udpPort 	UDP port of the server
	**/
	public void reTransmit(String serverName, Integer udpPort)
	{
		while (!transmitQueue.isEmpty()) {
			try {
				Segment[] temp = transmitQueue.toArray();
				Thread.sleep(timeoutTime);
				if (!temp.equals(transmitQueue.toArray())) {
					timer.cancel();
					timer = new Timer(true);
					timer.schedule(new TimeoutHandler(transmitQueue, udpSocket, serverName, udpPort, retransmitCount), timeoutTime);
				}
			}
			catch (InterruptedException e) {
				System.out.println("Error restarting timer in addtoQueue: " + e);
			}
		}
	}


	
    /**
     * A simple test driver
     * 
     */
	public static void main(String[] args) {
		// all arguments should be provided
		// as described in the assignment description 
		if (args.length != 5) {
			System.out.println("incorrect usage, try again.");
			System.out.println("usage: FastFtp server port file window timeout");
			System.exit(1);
		}
		
		// parse the command line arguments
		// assume no errors
		String serverName = args[0];
		Integer serverPort = Integer.parseInt(args[1]);
		String fileName = args[2];
		Integer windowSize = Integer.parseInt(args[3]);
		Integer timeout = Integer.parseInt(args[4]);

		// send the file to server
		FastFtp ftp = new FastFtp(windowSize, timeout);
		System.out.printf("sending file \'%s\' to server...\n", fileName);
		ftp.send(serverName, serverPort, fileName);
		System.out.println("File transfer completed.");
		System.exit(0);
	}
}
