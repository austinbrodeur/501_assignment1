import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.DatagramSocket;
import java.net.Socket;

public class ServerConnection {

    private Socket tcpConnection;
    private DatagramSocket udpSocket;
    private Integer serverUdpPort;


    public ServerConnection() {
        createSendUdp();
    }

    public ServerConnection(String serverName, Integer tcpPort, String fileName) {
        createSendUdp();
        serverUdpPort = getServerUDP(serverName, tcpPort, fileName);
    }


    public Integer getServerUdpPort() {
        return serverUdpPort;
    }

    public DatagramSocket getUdpSocket() {
        return udpSocket;
    }

    public void closeTcpConnection() {
        try {
            tcpConnection.close();
        }
        catch (Exception e) {
            System.out.println("Error closing TCP Connection: ");
            e.printStackTrace();
        }
    }

    /**
     * Attempts to get the UDP port from the server. Will quit after 10 tries.
     *
     * @param serverName	Server address or name
     * @param tcpPort	    Server TCP port for handshake
     * @param fileName		Path of file to be transferred
     * @return				UDP port of the server retrieved by the handshake
     */
    public int getServerUDP(String serverName, Integer tcpPort, String fileName) {
        Integer serverUDPPort = -1;
        Integer retry_count = 1;
        while (serverUDPPort == -1) {
            try {
                System.out.println("Trying to obtain UDP port from server... Attempt " + retry_count);

                createSendUdp();

                serverUDPPort = openTCP(serverName, tcpPort, fileName);
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
     * Opens TCP connection with the server and does the initial handshake. Returns UDP port.
     *
     * @param sname 	Server name
     * @param port 	Port for the server
     * @param fname 	Name of file to be sent
     * @return Integer Port for the UDP connection of the server. If this is -1 after the method is finished running, there was an error.
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
            System.out.println("Error creating initial TCP connection: ");
            e.printStackTrace();
        }

        try {
            file = new File(fname);

            dataOut.writeUTF(fname);
            dataOut.writeLong(file.length());
            dataOut.writeInt(udpSocket.getLocalPort());
            dataOut.flush();
            udpPort = dataIn.readInt();
        }
        catch (Exception e) {
            System.out.println("Error in TCP handshake: ");
            e.printStackTrace();
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
            System.out.println("Error opening DatagramSocket: ");
            e.printStackTrace();
        }
    }

}
