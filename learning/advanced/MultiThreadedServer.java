import java.io.*;
import java.net.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * MultiThreadedServer - A concurrent TCP server using thread pool to handle multiple clients.
 * 
 * Features:
 * - Thread pool for efficient resource management
 * - Graceful shutdown handling
 * - Client statistics tracking
 * - Configurable pool size and port
 * 
 * Compile: javac MultiThreadedServer.java
 * Run Server: java MultiThreadedServer
 * Test with: telnet localhost 8080 (or nc localhost 8080)
 * 
 * @author Advanced Java Learning
 * @version 1.0
 */
public class MultiThreadedServer {
    private static final int DEFAULT_PORT = 8080;
    private static final int THREAD_POOL_SIZE = 10;
    private static final AtomicInteger clientCounter = new AtomicInteger(0);
    
    private final int port;
    private final ExecutorService threadPool;
    private volatile boolean running = false;
    private ServerSocket serverSocket;
    
    /**
     * Constructs a MultiThreadedServer with specified port.
     * 
     * @param port The port number to listen on
     */
    public MultiThreadedServer(int port) {
        this.port = port;
        this.threadPool = Executors.newFixedThreadPool(THREAD_POOL_SIZE);
    }
    
    /**
     * Starts the server and begins accepting client connections.
     * 
     * @throws IOException if server cannot start
     */
    public void start() throws IOException {
        serverSocket = new ServerSocket(port);
        running = true;
        
        log("Server started on port " + port);
        log("Thread pool size: " + THREAD_POOL_SIZE);
        log("Waiting for client connections...");
        
        // Add shutdown hook for graceful termination
        Runtime.getRuntime().addShutdownHook(new Thread(this::shutdown));
        
        while (running) {
            try {
                Socket clientSocket = serverSocket.accept();
                int clientId = clientCounter.incrementAndGet();
                log("Client #" + clientId + " connected from " + 
                    clientSocket.getInetAddress().getHostAddress());
                
                // Submit client handler to thread pool
                threadPool.execute(new ClientHandler(clientSocket, clientId));
                
            } catch (SocketException e) {
                if (!running) {
                    break; // Server is shutting down
                }
                log("Socket error: " + e.getMessage());
            }
        }
    }
    
    /**
     * Shuts down the server gracefully.
     */
    public void shutdown() {
        if (!running) return;
        
        log("Shutting down server...");
        running = false;
        
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
        } catch (IOException e) {
            log("Error closing server socket: " + e.getMessage());
        }
        
        threadPool.shutdown();
        try {
            if (!threadPool.awaitTermination(10, TimeUnit.SECONDS)) {
                threadPool.shutdownNow();
            }
            log("Server shutdown complete. Total clients served: " + clientCounter.get());
        } catch (InterruptedException e) {
            threadPool.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
    
    /**
     * ClientHandler - Handles individual client connections.
     */
    private static class ClientHandler implements Runnable {
        private final Socket socket;
        private final int clientId;
        
        public ClientHandler(Socket socket, int clientId) {
            this.socket = socket;
            this.clientId = clientId;
        }
        
        @Override
        public void run() {
            try (
                BufferedReader in = new BufferedReader(
                    new InputStreamReader(socket.getInputStream()));
                PrintWriter out = new PrintWriter(socket.getOutputStream(), true)
            ) {
                // Send welcome message
                out.println("Welcome to MultiThreadedServer!");
                out.println("You are client #" + clientId);
                out.println("Commands: TIME, ECHO <msg>, STATS, QUIT");
                out.println("---");
                
                String inputLine;
                while ((inputLine = in.readLine()) != null) {
                    String response = processCommand(inputLine.trim());
                    out.println(response);
                    
                    if (inputLine.trim().equalsIgnoreCase("QUIT")) {
                        break;
                    }
                }
                
            } catch (IOException e) {
                log("Error handling client #" + clientId + ": " + e.getMessage());
            } finally {
                try {
                    socket.close();
                    log("Client #" + clientId + " disconnected");
                } catch (IOException e) {
                    log("Error closing socket for client #" + clientId);
                }
            }
        }
        
        /**
         * Processes client commands.
         * 
         * @param command The command to process
         * @return Response string
         */
        private String processCommand(String command) {
            if (command.isEmpty()) {
                return "ERROR: Empty command";
            }
            
            String[] parts = command.split("\\s+", 2);
            String cmd = parts[0].toUpperCase();
            
            switch (cmd) {
                case "TIME":
                    return "Server time: " + LocalDateTime.now()
                        .format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
                
                case "ECHO":
                    if (parts.length < 2) {
                        return "ERROR: ECHO requires a message";
                    }
                    return "ECHO: " + parts[1];
                
                case "STATS":
                    return String.format(
                        "Active threads: %d, Total clients: %d, Your ID: %d",
                        Thread.activeCount(), clientCounter.get(), clientId
                    );
                
                case "QUIT":
                    return "Goodbye!";
                
                default:
                    return "ERROR: Unknown command '" + cmd + "'";
            }
        }
    }
    
    /**
     * Logs a message with timestamp.
     * 
     * @param message The message to log
     */
    private static void log(String message) {
        String timestamp = LocalDateTime.now()
            .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        System.out.println("[" + timestamp + "] " + message);
    }
    
    /**
     * Main method - Demonstrates the server.
     * 
     * @param args Command line arguments (optional: port number)
     */
    public static void main(String[] args) {
        int port = DEFAULT_PORT;
        
        if (args.length > 0) {
            try {
                port = Integer.parseInt(args[0]);
                if (port < 1024 || port > 65535) {
                    System.err.println("Port must be between 1024 and 65535");
                    System.exit(1);
                }
            } catch (NumberFormatException e) {
                System.err.println("Invalid port number: " + args[0]);
                System.exit(1);
            }
        }
        
        try {
            MultiThreadedServer server = new MultiThreadedServer(port);
            server.start();
        } catch (IOException e) {
            System.err.println("Failed to start server: " + e.getMessage());
            System.exit(1);
        }
    }
}
