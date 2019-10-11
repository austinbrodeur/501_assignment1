/**
  * This class handles incoming ACKs
**/


import java.net.*;
import java.util.*;
import java.io.*;
import cpsc441.a3.shared.*;
	

public class ReceiverThread extends Thread {

	private DatagramSocket socket;
	private TxQueue transmitQueue;
	private int serverUdpPort;
	private int lastSegment;
	private String serverName;
	private Thread timeoutManager;
	private volatile boolean stop = false;

	public ReceiverThread(DatagramSocket s, TxQueue queue, int lchunk, String name, int port)
	{
		socket = s; // Uses UDP socket from main class
		transmitQueue = queue; // Queue address from main class
		lastSegment = lchunk;
		serverName = name;
		serverUdpPort = port;
	}

	/**
	  * Starts waiting for ACKs
	**/
	public void run()
	{
		byte[] receiveData = new byte[1000]; // Max size of segment payload

		try {
			DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
			while (stop == false) {
				socket.receive(receivePacket);
				Segment ackSeg = new Segment(receiveData);
				int ackNum = ackSeg.getSeqNum();
				deQueue(ackNum);
			}
		}
		catch (Exception e) {
			System.out.println("Error in receiver thread: " + e);
		}
	}




	/**
	  * Dequeues segment(s) when an ack is received
	  *
	  * @param seqnum 	The sequence number of the ACK received
	**/
	public synchronized void deQueue(int seqnum) throws InterruptedException
	{
		if (transmitQueue.element() != null) {
			int end = seqnum - 1;
			if (transmitQueue.element().getSeqNum() == end) {
				transmitQueue.remove();
			}
			else {
				for (int f = transmitQueue.element().getSeqNum(); f <= end; f++) {
					transmitQueue.remove();
				}
			}
		}
	}


	/**
	  * Stops this thread gracefully when called
	**/
	public void requestStop()
	{
		stop = true;
	}
}