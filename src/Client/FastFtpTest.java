import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class FastFtpTest {

    private FastFtp fastFtp = new FastFtp(10, 10);

    @Test
    void testcheckArgsTooFew() {
        String[] testArgs = {"string1", "string2", "string3", "string4"};

        assertThrows(RuntimeException.class, () -> {
            fastFtp.checkArgs(testArgs);
        });
    }

    @Test
    void testcheckArgsTooMany() {
        String[] testArgs = {"string1", "string2", "string3", "string4", "string5", "string6"};

        assertThrows(RuntimeException.class, () -> {
            fastFtp.checkArgs(testArgs);
        });
    }

    @Test
    void testcheckArgsPass() {
        String[] testArgs = {"string1", "string2", "string3", "string4", "string5"};

        fastFtp.checkArgs(testArgs);
        assertTrue(true);
    }

    @Test
    void testsendToServer() {
        String[] testArgs = {"localhost", "2225", "C:\\Users\\austi\\Desktop\\a1\\src\\Client\\test.pdf", "10", "10"};

        fastFtp.sendToServer(testArgs);
        assertTrue(true);
    }
}