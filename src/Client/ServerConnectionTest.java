import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ServerConnectionTest {

    private ServerConnection serverConnection = new ServerConnection();

    @Test
    void testGetServerUDPPass() {
        String serverName = "localhost";
        String filePath = "C:\\Users\\austi\\Desktop\\a1\\src\\Client\\test.pdf";
        int tcpPort = 2225;
        int result;

        result = serverConnection.getServerUDP(serverName, tcpPort, filePath);
        assertNotEquals(-1, result);
    }

    @Test
    void testGetServerUDPbadPort() {
        String serverName = "localhost";
        String filePath = "C:\\Users\\austi\\Desktop\\a1\\src\\Client\\test.pdf";
        int tcpPort = 2222;

        assertThrows(RuntimeException.class, () -> {
            serverConnection.getServerUDP(serverName, tcpPort, filePath);
        });
    }

    @Test
    void testopenTcpPass() {
        String serverName = "localhost";
        String filePath = "C:\\Users\\austi\\Desktop\\a1\\src\\Client\\test.pdf";
        int tcpPort = 2225;
        int result;

        result = serverConnection.openTCP(serverName, tcpPort, filePath);
        assertNotEquals(-1, result);
    }

    @Test
    void testopenTcpPortFail() {
        String serverName = "localhost";
        String filePath = "C:\\Users\\austi\\Desktop\\a1\\src\\Client\\test.pdf";
        int tcpPort = 2222;
        int result;

        result = serverConnection.openTCP(serverName, tcpPort, filePath);
        assertEquals(-1, result);
    }

    @Test
    void testopenTcpHostFail() {
        String serverName = "1.1.1.1";
        String filePath = "C:\\Users\\austi\\Desktop\\a1\\src\\Client\\test.pdf";
        int tcpPort = 2225;
        int result;

        result = serverConnection.openTCP(serverName, tcpPort, filePath);
        assertEquals(-1, result);
    }
}