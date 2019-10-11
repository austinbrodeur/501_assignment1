/**
  * This class manages timeouts
**/



import java.net.*;
import java.util.*;
import java.io.*;
import cpsc441.a3.shared.*;
import java.util.TimerTask;


public class TimeoutManager implements Runnable
{
	private TxQueue transmitQueue;
	private DatagramSocket socket;
	private String serverName;
	private int serverUdpPort;
	private int timeoutTime;
	private Segment currentSegment = null;
	private Timer timer;
	private int[] retransmitCount;


	/**
	  * Constructor
	**/
	public TimeoutManager(TxQueue q, DatagramSocket s, String address, int port, int tO, Timer t, int[] count)
	{
		transmitQueue = q;
		socket = s;
		serverName = address;
		serverUdpPort = port;
		timeoutTime = tO;
		timer = t;
		retransmitCount = count;
	}

	/**
	  * Run method override
	**/
	public void run()
	{
		manageTimer();
	}

	/**
	  * Detects when the front of the queue changes and updates the timout timer accordingly.
	**/
	public synchronized void manageTimer()
	{
		while(true)
		{
			if ((currentSegment == null) && (transmitQueue.size() != 0) || (currentSegment != transmitQueue.element() && transmitQueue.element() != null)) {
				currentSegment = transmitQueue.element();
				timer.cancel();
				timer = new Timer(true);
				timer.schedule(new TimeoutHandler(transmitQueue, socket, serverName, serverUdpPort, retransmitCount), timeoutTime);
			}
		}
	}
}