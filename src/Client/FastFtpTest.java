import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class FastFtpTest {

    private FastFtp fastFtp = new FastFtp(10, 10);

    @Test
    void testGetServerUDP() {
        String serverName = "localhost";
        String filePath = "C:\\Users\\austi\\Desktop\\a1\\src\\Client\\test.pdf"; // Change this to whatever file you wish to transfer
        int tcpPort = 2225;
        int result;

        result = fastFtp.getServerUDP(serverName, tcpPort, filePath);
        assertNotEquals(-1, result);
    }

}