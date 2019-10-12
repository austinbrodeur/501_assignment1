/**
 * FastFtp Class
 *
 * @author Austin Brodeur
 * @version 1.0
 *
 */

import java.net.*;
import java.util.*;
import java.nio.file.*;
import cpsc441.a3.shared.*;

public class FastFtp {

	private final int MAX_SEGMENT_SIZE = 1000;
	private final int THREAD_KILL_DELAY = 500;

	private ServerConnection serverConnection;
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
	 * Initializes the threads required for the send method
	 *
	 * @param serverName	Server address or name
	 * @param serverUDPPort	Server UDP port from handshake
	 * @param fileChunks	2D byte array of file after being divided into chunks
	 */
	public void initializeThreads(String serverName, Integer serverUDPPort, byte[][] fileChunks) {
		timeoutManager = new Thread(new TimeoutManager(transmitQueue, serverConnection.getUdpSocket(), serverName, serverUDPPort, timeoutTime, timer, retransmitCount));
		timeoutManager.start();

		receiverThread = new ReceiverThread(serverConnection.getUdpSocket(), transmitQueue, fileChunks.length, serverName, serverUDPPort);
		receiverThread.start();
	}


	/**
	 * Reports success, cleans up threads and generates report after file is transferred. Is the teardown for the send method.
	 */
	public void teardownSend() {
		try {
			System.out.println("Number of retransmits: " + retransmitCount[0]);
			System.out.println("Closing ports and stopping client..");
			receiverThread.requestStop();
			Thread.sleep(THREAD_KILL_DELAY);
			timer.cancel();
			timeoutManager.interrupt();
			serverConnection.closeTcpConnection();
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
		byte[][] fileChunks;
		retransmitCount[0] = 0;

		try {
			fileChunks = splitFile(filetoBytes(fileName));
			System.out.println("Number of chunks: " + fileChunks.length);

			serverConnection = new ServerConnection(serverName, serverPort, fileName);
			Integer serverUdpPort = serverConnection.getServerUdpPort();

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
		DatagramSocket udpSocket = serverConnection.getUdpSocket();
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
					timer.schedule(new TimeoutHandler(transmitQueue, serverConnection.getUdpSocket(), serverName, udpPort, retransmitCount), timeoutTime);
				}
			}
			catch (InterruptedException e) {
				System.out.println("Error restarting timer in addtoQueue: " + e);
			}
		}
	}


	//************************************ Below methods are for main method only ************************************


	/**
	 * Checks if there is an appropriate number of command line args. Raises runtime error if there isn't.
	 *
	 * @param args List of command line arguments
	 */
	public static void checkArgs(String[] args) {
		if (args.length != 5) {
			System.out.println("Incorrect usage, try again.");
			System.out.println("Usage: FastFtp <server address> <server port> <path of file to transfer> <file window size> <timeout time>");
			throw new RuntimeException();
		}
	}


	/**
	 * Uses commandline args to instantiate FastFtp to call send
	 *
	 * @param args List of command line arguments
	 */
	public static void sendToServer(String[] args) {
		String serverName = args[0];
		Integer serverPort = Integer.parseInt(args[1]);
		String fileName = args[2];
		Integer windowSize = Integer.parseInt(args[3]);
		Integer timeout = Integer.parseInt(args[4]);

		FastFtp ftp = new FastFtp(windowSize, timeout);
		System.out.printf("Sending file \'%s\' to server...\n", fileName);
		ftp.send(serverName, serverPort, fileName);
		System.out.println("File transfer completed.");
		System.exit(0);
	}


	/**
	 *
	 * @param args Command line args
	 */
	public static void main(String[] args) {
		checkArgs(args);
		sendToServer(args);
	}
}
