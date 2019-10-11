import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class FastFtpTest {

    private FastFtp fastFtp = new FastFtp(10, 10);

    @Test
    void testGetServerUDPPass() {
        String serverName = "localhost";
        String filePath = "C:\\Users\\austi\\Desktop\\a1\\src\\Client\\test.pdf";
        int tcpPort = 2225;
        int result;

        result = fastFtp.getServerUDP(serverName, tcpPort, filePath);
        assertNotEquals(-1, result);
    }

    @Test
    void testGetServerUDPbadPort() {
        String serverName = "localhost";
        String filePath = "C:\\Users\\austi\\Desktop\\a1\\src\\Client\\test.pdf";
        int tcpPort = 2222;

        assertThrows(RuntimeException.class, () -> {
            fastFtp.getServerUDP(serverName, tcpPort, filePath);
        });
    }
    
}