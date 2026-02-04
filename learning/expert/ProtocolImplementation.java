import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;
import java.util.concurrent.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * WebSocket Protocol Implementation (RFC 6455)
 * Complete server/client with handshake, framing, masking, ping/pong, chat demo
 */
public class ProtocolImplementation {
    
    // ============ WEBSOCKET OPCODES ============
    enum OpCode {
        CONTINUATION(0x0),
        TEXT(0x1),
        BINARY(0x2),
        CLOSE(0x8),
        PING(0x9),
        PONG(0xA);
        
        final int value;
        OpCode(int value) { this.value = value; }
        
        static OpCode fromValue(int value) {
            for (OpCode op : values()) {
                if (op.value == value) return op;
            }
            throw new IllegalArgumentException("Unknown opcode: " + value);
        }
    }
    
    // ============ WEBSOCKET FRAME ============
    static class WebSocketFrame {
        final boolean fin;
        final OpCode opCode;
        final boolean masked;
        final byte[] payload;
        
        WebSocketFrame(boolean fin, OpCode opCode, boolean masked, byte[] payload) {
            this.fin = fin;
            this.opCode = opCode;
            this.masked = masked;
            this.payload = payload;
        }
        
        static WebSocketFrame text(String message) {
            return new WebSocketFrame(true, OpCode.TEXT, false, 
                message.getBytes(StandardCharsets.UTF_8));
        }
        
        static WebSocketFrame ping() {
            return new WebSocketFrame(true, OpCode.PING, false, new byte[0]);
        }
        
        static WebSocketFrame pong() {
            return new WebSocketFrame(true, OpCode.PONG, false, new byte[0]);
        }
        
        static WebSocketFrame close() {
            return new WebSocketFrame(true, OpCode.CLOSE, false, new byte[0]);
        }
        
        byte[] encode() {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            
            // Byte 0: FIN, RSV, OpCode
            int byte0 = (fin ? 0x80 : 0x00) | opCode.value;
            baos.write(byte0);
            
            // Byte 1: MASK, Payload length
            int payloadLength = payload.length;
            int byte1 = masked ? 0x80 : 0x00;
            
            if (payloadLength < 126) {
                byte1 |= payloadLength;
                baos.write(byte1);
            } else if (payloadLength < 65536) {
                byte1 |= 126;
                baos.write(byte1);
                baos.write((payloadLength >> 8) & 0xFF);
                baos.write(payloadLength & 0xFF);
            } else {
                byte1 |= 127;
                baos.write(byte1);
                for (int i = 7; i >= 0; i--) {
                    baos.write((int) ((payloadLength >> (i * 8)) & 0xFF));
                }
            }
            
            // Masking key (if masked)
            byte[] maskingKey = null;
            if (masked) {
                maskingKey = new byte[4];
                new Random().nextBytes(maskingKey);
                baos.write(maskingKey, 0, 4);
            }
            
            // Payload
            if (masked && maskingKey != null) {
                byte[] maskedPayload = new byte[payload.length];
                for (int i = 0; i < payload.length; i++) {
                    maskedPayload[i] = (byte) (payload[i] ^ maskingKey[i % 4]);
                }
                baos.write(maskedPayload, 0, maskedPayload.length);
            } else {
                baos.write(payload, 0, payload.length);
            }
            
            return baos.toByteArray();
        }
        
        static WebSocketFrame decode(InputStream in) throws IOException {
            // Read byte 0: FIN, RSV, OpCode
            int byte0 = in.read();
            if (byte0 == -1) throw new IOException("Connection closed");
            
            boolean fin = (byte0 & 0x80) != 0;
            OpCode opCode = OpCode.fromValue(byte0 & 0x0F);
            
            // Read byte 1: MASK, Payload length
            int byte1 = in.read();
            if (byte1 == -1) throw new IOException("Connection closed");
            
            boolean masked = (byte1 & 0x80) != 0;
            long payloadLength = byte1 & 0x7F;
            
            if (payloadLength == 126) {
                payloadLength = ((in.read() & 0xFF) << 8) | (in.read() & 0xFF);
            } else if (payloadLength == 127) {
                payloadLength = 0;
                for (int i = 0; i < 8; i++) {
                    payloadLength = (payloadLength << 8) | (in.read() & 0xFF);
                }
            }
            
            // Read masking key
            byte[] maskingKey = null;
            if (masked) {
                maskingKey = new byte[4];
                in.read(maskingKey);
            }
            
            // Read payload
            byte[] payload = new byte[(int) payloadLength];
            int totalRead = 0;
            while (totalRead < payloadLength) {
                int read = in.read(payload, totalRead, (int) payloadLength - totalRead);
                if (read == -1) throw new IOException("Connection closed");
                totalRead += read;
            }
            
            // Unmask payload
            if (masked && maskingKey != null) {
                for (int i = 0; i < payload.length; i++) {
                    payload[i] = (byte) (payload[i] ^ maskingKey[i % 4]);
                }
            }
            
            return new WebSocketFrame(fin, opCode, masked, payload);
        }
        
        String getTextPayload() {
            return new String(payload, StandardCharsets.UTF_8);
        }
    }
    
    // ============ WEBSOCKET HANDSHAKE ============
    static class WebSocketHandshake {
        private static final String MAGIC_STRING = "258EAFA5-E914-47DA-95CA-C5AB0DC85B11";
        
        static String generateAcceptKey(String clientKey) {
            try {
                MessageDigest md = MessageDigest.getInstance("SHA-1");
                String concat = clientKey + MAGIC_STRING;
                byte[] hash = md.digest(concat.getBytes(StandardCharsets.UTF_8));
                return Base64.getEncoder().encodeToString(hash);
            } catch (Exception e) {
                throw new RuntimeException("Failed to generate accept key", e);
            }
        }
        
        static Map<String, String> parseHttpHeaders(BufferedReader reader) throws IOException {
            Map<String, String> headers = new HashMap<>();
            String line;
            
            // Read request line
            String requestLine = reader.readLine();
            if (requestLine != null) {
                headers.put("REQUEST", requestLine);
            }
            
            // Read headers
            while ((line = reader.readLine()) != null && !line.isEmpty()) {
                int colonIndex = line.indexOf(':');
                if (colonIndex > 0) {
                    String key = line.substring(0, colonIndex).trim();
                    String value = line.substring(colonIndex + 1).trim();
                    headers.put(key, value);
                }
            }
            
            return headers;
        }
        
        static String buildServerHandshake(String acceptKey) {
            return "HTTP/1.1 101 Switching Protocols\r\n" +
                   "Upgrade: websocket\r\n" +
                   "Connection: Upgrade\r\n" +
                   "Sec-WebSocket-Accept: " + acceptKey + "\r\n" +
                   "\r\n";
        }
        
        static String buildClientHandshake(String host, String path) {
            String key = Base64.getEncoder().encodeToString(
                UUID.randomUUID().toString().getBytes(StandardCharsets.UTF_8));
            
            return "GET " + path + " HTTP/1.1\r\n" +
                   "Host: " + host + "\r\n" +
                   "Upgrade: websocket\r\n" +
                   "Connection: Upgrade\r\n" +
                   "Sec-WebSocket-Key: " + key + "\r\n" +
                   "Sec-WebSocket-Version: 13\r\n" +
                   "\r\n";
        }
    }
    
    // ============ WEBSOCKET CONNECTION ============
    static class WebSocketConnection {
        private final Socket socket;
        private final InputStream input;
        private final OutputStream output;
        private final boolean isClient;
        private volatile boolean closed = false;
        
        WebSocketConnection(Socket socket, boolean isClient) throws IOException {
            this.socket = socket;
            this.input = socket.getInputStream();
            this.output = socket.getOutputStream();
            this.isClient = isClient;
        }
        
        void sendFrame(WebSocketFrame frame) throws IOException {
            if (closed) throw new IOException("Connection closed");
            
            // Client must mask frames
            WebSocketFrame maskedFrame = isClient ? 
                new WebSocketFrame(frame.fin, frame.opCode, true, frame.payload) : frame;
            
            synchronized (output) {
                output.write(maskedFrame.encode());
                output.flush();
            }
        }
        
        void sendText(String message) throws IOException {
            sendFrame(WebSocketFrame.text(message));
        }
        
        WebSocketFrame receiveFrame() throws IOException {
            if (closed) throw new IOException("Connection closed");
            return WebSocketFrame.decode(input);
        }
        
        void sendPing() throws IOException {
            sendFrame(WebSocketFrame.ping());
        }
        
        void sendPong() throws IOException {
            sendFrame(WebSocketFrame.pong());
        }
        
        void close() throws IOException {
            if (closed) return;
            closed = true;
            
            try {
                sendFrame(WebSocketFrame.close());
            } catch (IOException ignored) {
            }
            
            socket.close();
        }
        
        boolean isClosed() {
            return closed || socket.isClosed();
        }
    }
    
    // ============ WEBSOCKET SERVER ============
    static class WebSocketServer {
        private final int port;
        private final Map<String, WebSocketConnection> connections = new ConcurrentHashMap<>();
        private final ExecutorService executor = Executors.newCachedThreadPool();
        private ServerSocket serverSocket;
        private volatile boolean running = false;
        
        interface MessageHandler {
            void onMessage(String clientId, String message);
        }
        
        private MessageHandler messageHandler;
        
        WebSocketServer(int port) {
            this.port = port;
        }
        
        void setMessageHandler(MessageHandler handler) {
            this.messageHandler = handler;
        }
        
        void start() throws IOException {
            serverSocket = new ServerSocket(port);
            running = true;
            
            System.out.printf("[Server] Started on port %d\n", port);
            
            executor.submit(() -> {
                while (running) {
                    try {
                        Socket client = serverSocket.accept();
                        executor.submit(() -> handleClient(client));
                    } catch (IOException e) {
                        if (running) {
                            System.err.println("[Server] Accept error: " + e.getMessage());
                        }
                    }
                }
            });
        }
        
        private void handleClient(Socket client) {
            String clientId = null;
            try {
                // Perform handshake
                BufferedReader reader = new BufferedReader(
                    new InputStreamReader(client.getInputStream(), StandardCharsets.UTF_8));
                
                Map<String, String> headers = WebSocketHandshake.parseHttpHeaders(reader);
                String wsKey = headers.get("Sec-WebSocket-Key");
                
                if (wsKey == null) {
                    client.close();
                    return;
                }
                
                String acceptKey = WebSocketHandshake.generateAcceptKey(wsKey);
                String response = WebSocketHandshake.buildServerHandshake(acceptKey);
                
                client.getOutputStream().write(response.getBytes(StandardCharsets.UTF_8));
                client.getOutputStream().flush();
                
                // Create connection
                WebSocketConnection conn = new WebSocketConnection(client, false);
                clientId = client.getRemoteSocketAddress().toString();
                connections.put(clientId, conn);
                
                System.out.printf("[Server] Client connected: %s\n", clientId);
                
                // Handle messages
                while (!conn.isClosed()) {
                    WebSocketFrame frame = conn.receiveFrame();
                    
                    switch (frame.opCode) {
                        case TEXT -> {
                            String message = frame.getTextPayload();
                            System.out.printf("[Server] Received from %s: %s\n", clientId, message);
                            if (messageHandler != null) {
                                messageHandler.onMessage(clientId, message);
                            }
                        }
                        case PING -> conn.sendPong();
                        case CLOSE -> {
                            System.out.printf("[Server] Client closing: %s\n", clientId);
                            conn.close();
                        }
                        default -> System.out.printf("[Server] Unknown opcode: %s\n", frame.opCode);
                    }
                }
                
            } catch (IOException e) {
                System.err.printf("[Server] Client error: %s\n", e.getMessage());
            } finally {
                if (clientId != null) {
                    connections.remove(clientId);
                }
            }
        }
        
        void broadcast(String message) {
            for (Map.Entry<String, WebSocketConnection> entry : connections.entrySet()) {
                try {
                    entry.getValue().sendText(message);
                } catch (IOException e) {
                    System.err.printf("[Server] Broadcast error to %s: %s\n", 
                        entry.getKey(), e.getMessage());
                }
            }
        }
        
        void sendTo(String clientId, String message) throws IOException {
            WebSocketConnection conn = connections.get(clientId);
            if (conn != null) {
                conn.sendText(message);
            }
        }
        
        void stop() {
            running = false;
            
            for (WebSocketConnection conn : connections.values()) {
                try {
                    conn.close();
                } catch (IOException ignored) {
                }
            }
            
            try {
                if (serverSocket != null) {
                    serverSocket.close();
                }
            } catch (IOException ignored) {
            }
            
            executor.shutdown();
            System.out.println("[Server] Stopped");
        }
        
        int getConnectionCount() {
            return connections.size();
        }
    }
    
    // ============ WEBSOCKET CLIENT ============
    static class WebSocketClient {
        private final String host;
        private final int port;
        private final String path;
        private WebSocketConnection connection;
        
        interface MessageHandler {
            void onMessage(String message);
        }
        
        private MessageHandler messageHandler;
        
        WebSocketClient(String host, int port, String path) {
            this.host = host;
            this.port = port;
            this.path = path;
        }
        
        void setMessageHandler(MessageHandler handler) {
            this.messageHandler = handler;
        }
        
        void connect() throws IOException {
            Socket socket = new Socket(host, port);
            
            // Send handshake
            String handshake = WebSocketHandshake.buildClientHandshake(host, path);
            socket.getOutputStream().write(handshake.getBytes(StandardCharsets.UTF_8));
            socket.getOutputStream().flush();
            
            // Read handshake response
            BufferedReader reader = new BufferedReader(
                new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
            
            Map<String, String> headers = WebSocketHandshake.parseHttpHeaders(reader);
            
            if (!headers.containsKey("Sec-WebSocket-Accept")) {
                throw new IOException("Invalid handshake response");
            }
            
            connection = new WebSocketConnection(socket, true);
            System.out.printf("[Client] Connected to %s:%d\n", host, port);
            
            // Start message receiver
            new Thread(this::receiveMessages).start();
        }
        
        private void receiveMessages() {
            try {
                while (!connection.isClosed()) {
                    WebSocketFrame frame = connection.receiveFrame();
                    
                    switch (frame.opCode) {
                        case TEXT -> {
                            String message = frame.getTextPayload();
                            System.out.printf("[Client] Received: %s\n", message);
                            if (messageHandler != null) {
                                messageHandler.onMessage(message);
                            }
                        }
                        case PING -> connection.sendPong();
                        case CLOSE -> {
                            System.out.println("[Client] Server closed connection");
                            connection.close();
                        }
                        default -> System.out.printf("[Client] Unknown opcode: %s\n", frame.opCode);
                    }
                }
            } catch (IOException e) {
                System.err.printf("[Client] Receive error: %s\n", e.getMessage());
            }
        }
        
        void send(String message) throws IOException {
            connection.sendText(message);
        }
        
        void close() throws IOException {
            if (connection != null) {
                connection.close();
            }
        }
    }
    
    // ============ DEMO: CHAT APPLICATION ============
    public static void main(String[] args) throws Exception {
        System.out.println("=== WebSocket Protocol Implementation Demo ===\n");
        
        // Start server
        WebSocketServer server = new WebSocketServer(8080);
        server.setMessageHandler((clientId, message) -> {
            System.out.printf("[Chat] Broadcasting message from %s\n", clientId);
            server.broadcast(String.format("%s: %s", clientId.substring(0, 10), message));
        });
        
        server.start();
        Thread.sleep(500);
        
        // Create clients
        System.out.println("\n=== Creating Clients ===\n");
        
        WebSocketClient client1 = new WebSocketClient("localhost", 8080, "/chat");
        WebSocketClient client2 = new WebSocketClient("localhost", 8080, "/chat");
        WebSocketClient client3 = new WebSocketClient("localhost", 8080, "/chat");
        
        client1.connect();
        Thread.sleep(200);
        client2.connect();
        Thread.sleep(200);
        client3.connect();
        Thread.sleep(500);
        
        System.out.printf("\n[Server] Active connections: %d\n\n", server.getConnectionCount());
        
        // Simulate chat messages
        System.out.println("=== Chat Simulation ===\n");
        
        client1.send("Hello everyone!");
        Thread.sleep(300);
        
        client2.send("Hi there!");
        Thread.sleep(300);
        
        client3.send("Good to see you all!");
        Thread.sleep(300);
        
        client1.send("How's the weather?");
        Thread.sleep(300);
        
        client2.send("It's sunny here!");
        Thread.sleep(300);
        
        // Server broadcast
        System.out.println("\n[Server] Sending system message\n");
        server.broadcast("SYSTEM: Server maintenance in 5 minutes");
        Thread.sleep(500);
        
        client3.send("Thanks for the heads up!");
        Thread.sleep(500);
        
        // Cleanup
        System.out.println("\n=== Closing Connections ===\n");
        client1.close();
        Thread.sleep(200);
        client2.close();
        Thread.sleep(200);
        client3.close();
        Thread.sleep(500);
        
        server.stop();
        
        System.out.println("\n=== Demo Complete ===");
    }
}
