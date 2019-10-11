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

	private Socket TcpCon;
	private DatagramSocket UdpSocket;
	private ReceiverThread receiver;
	private Thread toM;
	private int wSize;
	private int nextSeq = 0;
	private int toTime;
	private int[] rtCount = new int[1];
	private TxQueue tQueue;
	private Timer timer = new Timer(true);

	/**
     * Constructor to initialize the program 
     * 
     * @param windowSize	Size of the window for Go-Back-N in terms of segments
     * @param rtoTimer		The time-out interval for the retransmission timer
     */
	public FastFtp(int windowSize, int rtoTimer) {
		wSize = windowSize;
		toTime = rtoTimer;
		tQueue = new TxQueue(windowSize);
	}

	/**
	 * Attempts to get the UDP port from the server. Will quit after 10 tries.
	 *
	 * @param serverName	Server address or name
	 * @param serverTCPPort	Server TCP port for handshake
	 * @param fileName		Path of file to be transferred
	 * @return				UDP port of the server retrieved by the handshake
	 */
	public int getServerUDP(String serverName, int serverTCPPort, String fileName) {
		int serverUDPPort = -1;
		int retry_count = 1;
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
	public void initializeThreads(String serverName, int serverUDPPort, byte[][] fileChunks) {
		toM = new Thread(new TimeoutManager(tQueue, UdpSocket, serverName, serverUDPPort, toTime, timer, rtCount));
		toM.start();

		receiver = new ReceiverThread(UdpSocket, tQueue, fileChunks.length, serverName, serverUDPPort);
		receiver.start();
	}


	/**
	 * Cleans up threads and generates report after file is transferred
	 */
	public void teardownSend() {
		try {
			System.out.println("Number of retransmits: " + rtCount[0]);
			System.out.println("Closing ports and stopping client..");
			receiver.requestStop();
			Thread.sleep(500);
			timer.cancel();
			toM.interrupt();
			TcpCon.close();
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
	public void send(String serverName, int serverPort, String fileName) {
		int SUDPport;
		byte[][] fileChunks;
		rtCount[0] = 0;

		try {
			fileChunks = splitFile(filetoBytes(fileName));
			System.out.println("Number of chunks: " + fileChunks.length);

			createSendUdp();
			SUDPport = getServerUDP(serverName, serverPort, fileName);
			initializeThreads(serverName, SUDPport, fileChunks);

			for (int i = 0; i < fileChunks.length; i++) {
				addtoQueue(fileChunks[i], serverName, SUDPport);
				reTransmit(serverName, SUDPport);
			}
			while (true) {
				if (tQueue.isEmpty()) {
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
	public int openTCP(String sname, int port, String fname)
	{
		DataInputStream dIn = null;
		DataOutputStream dOut = null;
		File file;
		int sPort = -1;

		try {
			TcpCon = new Socket(sname, port);

			dIn = new DataInputStream(TcpCon.getInputStream());
			dOut = new DataOutputStream(TcpCon.getOutputStream());
		}
		catch (IOException e) {
			System.out.println("Error creating initial TCP connection: " + e);
		}

		try {
			file = new File(fname);

			dOut.writeUTF(fname);
			dOut.writeLong(file.length());
			dOut.writeInt(UdpSocket.getLocalPort()); //Port for the UDP connection of the client
			dOut.flush();
			sPort = dIn.readInt(); 
		}
		catch (Exception e) {
			System.out.println("Error in TCP handshake: " + e);
		}
		return sPort;
	}


	/**
	  * Opens UDP connection with the server
	**/
	public void createSendUdp()
	{
		try {
			UdpSocket = new DatagramSocket();
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
		System.out.println("Working Directory = " + System.getProperty("user.dir"));
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
		int CHUNK_SIZE = 1000;

		byte[][] ret = new byte[(int)Math.ceil(fullPayload.length / (double)CHUNK_SIZE)][];
		int start = 0;
		for (int i = 0; i < ret.length; i++) {
			if (start + CHUNK_SIZE > fullPayload.length) {
				ret[i] = new byte[fullPayload.length-start];
				System.arraycopy(fullPayload, start, ret[i], 0, fullPayload.length - start);
			}
			else {
				ret[i] = new byte[CHUNK_SIZE];
				System.arraycopy(fullPayload, start, ret[i], 0, CHUNK_SIZE);
			}
			start += CHUNK_SIZE;
		}
		return ret;
	}



	/**
	  * Adds new segment to queue and transmits.
	  *
	  * @param chunk 	Chunk to be added to the queue
	  * @param serverName	Address of the server
	  * @param sPort 	UDP port of the server
	  * @return boolean	Returns true if the segment was able to be added to the queue, false if the queue is full
	**/
	public synchronized void addtoQueue(byte[] chunk, String serverName, int sPort)
	{
		try {
			Segment segmentsend = new Segment(nextSeq, chunk);
			//System.out.println("Sending " + segmentsend.getBytes().length + " bytes of data to " + InetAddress.getByName(serverName).toString() + ":" + sPort + " with seqnum " + segmentsend.getSeqNum());
			DatagramPacket sendPacket = new DatagramPacket(segmentsend.getBytes(), segmentsend.getBytes().length, InetAddress.getByName(serverName), sPort);
			UdpSocket.send(sendPacket);
			tQueue.add(segmentsend);
		}
		catch (Exception e) {
			System.out.println("Error adding chunk to queue: " + e);
		}
		//System.out.println("Queue size: " + tQueue.size());
		nextSeq += 1;
	}


	/**
	  * Retransmits the queue if there is no difference in the queue after the timeout delay.
	  *
	  * @param serverName	Address of the server
	  * @param sPort 	UDP port of the server
	**/
	public void reTransmit(String serverName, int sPort)
	{
		while (!tQueue.isEmpty()) {
			try {
				Segment[] temp = tQueue.toArray();
				Thread.sleep(toTime);
				if (!temp.equals(tQueue.toArray())) {
					timer.cancel();
					timer = new Timer(true);
					timer.schedule(new TimeoutHandler(tQueue, UdpSocket, serverName, sPort, rtCount), toTime);
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
		int serverPort = Integer.parseInt(args[1]);
		String fileName = args[2];
		int windowSize = Integer.parseInt(args[3]);
		int timeout = Integer.parseInt(args[4]);

		// send the file to server
		FastFtp ftp = new FastFtp(windowSize, timeout);
		System.out.printf("sending file \'%s\' to server...\n", fileName);
		ftp.send(serverName, serverPort, fileName);
		System.out.println("File transfer completed.");
		System.exit(0);
	}
}
