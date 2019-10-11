/**
  * This class handldes what happens after a timeout expires
**/


import java.net.*;
import java.util.*;
import java.io.*;
import cpsc441.a3.shared.*;
import java.util.TimerTask;

public class TimeoutHandler extends TimerTask
{

	private TxQueue transmitQueue;
	private DatagramSocket socket;
	private String serverName;
	private Integer serverUdpPort;
	private Integer[] retransmitCount;

	/**
	  * Constructor
	**/
	public TimeoutHandler(TxQueue q, DatagramSocket s, String address, Integer port, Integer[] count)
	{
		transmitQueue = q;
		socket = s;
		serverName = address;
		serverUdpPort = port;
		retransmitCount = count;
	}

	/**
	  * Run method override
	**/
	public void run()
	{
		retransmit();
	}

	/**
	  * Retransmits the entire queue when the timer expires
	**/
	public synchronized void retransmit()
	{
		if (!transmitQueue.isEmpty()) {
			retransmitCount[0] += 1;
			Segment[] tArray = transmitQueue.toArray();
			for (Segment s : tArray) {
				try {
					DatagramPacket sendPacket = new DatagramPacket(s.getBytes(), s.getBytes().length, InetAddress.getByName(serverName), serverUdpPort);
					socket.send(sendPacket);
				}
				catch (IOException e) {
					System.out.println("Error during retransmission: " + e);
				}
			}
		}
	}
}