import java.net.http.HttpClient;
import javax.crypto.SecretKey;
import java.util.UUID;
import java.security.SecureRandom;
import javax.crypto.KeyGenerator;

public class HTTPCommunicationProtocol {
    private static final String[] USER_AGENTS = { //we are hiding the HTTP server as ran through the Apple device
            // iPhone with Safari
            "Mozilla/5.0 (iPhone; CPU iPhone OS 16_5 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/16.5 Mobile/15E148 Safari/604.1",

            // iPad with Safari
            "Mozilla/5.0 (iPad; CPU OS 16_5 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/16.5 Mobile/15E148 Safari/604.1",

            // macOS with Safari
            "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/16.5 Safari/605.1.15",

            // macOS with Chrome (still appears as Mac device)
            "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/114.0.0.0 Safari/537.36"
    };

    private final HttpClient httpClient;
    private final String serverUrl;
    private final String beaconId;
    private final SecretKey encryptionKey;
    private int jitterFactor = 30; // Percentage of randomness in timing

    public HTTPCommunicationProtocol(String serverUrl, HttpClient httpClient, String beaconId, SecretKey encryptionKey) {
        this.serverUrl = serverUrl;
        this.httpClient = httpClient;
        this.beaconId = beaconId;
        this.encryptionKey = encryptionKey;
    }

    // Alternative constructor that generates its own beaconId and encryption key
    public HTTPCommunicationProtocol(String serverUrl, HttpClient httpClient) throws Exception {
        this.serverUrl = serverUrl;
        this.httpClient = httpClient;
        this.beaconId = UUID.randomUUID().toString();

        // Generate encryption key
        KeyGenerator keyGen = KeyGenerator.getInstance("AES");
        keyGen.init(256);
        this.encryptionKey = keyGen.generateKey();
    }

    public int getJitterFactor() {
        return jitterFactor;
    }

    public void setJitterFactor(int jitterFactor) {
        this.jitterFactor = jitterFactor;
    }
}