import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.BiFunction;
import java.util.regex.*;

/**
 * WebFramework - A mini web framework with HTTP server, routing, and middleware.
 * 
 * Features:
 * - HTTP/1.1 server with GET, POST, PUT, DELETE support
 * - Pattern-based routing with path parameters
 * - Middleware chain (logging, auth, CORS)
 * - JSON response helpers
 * - Static error handling
 * 
 * Compile: javac WebFramework.java
 * Run: java WebFramework
 * Test: curl http://localhost:3000/api/users/123
 * 
 * @author Advanced Java Learning
 * @version 1.0
 */
public class WebFramework {
    
    /**
     * HTTP Request representation.
     */
    static class Request {
        private final String method;
        private final String path;
        private final Map<String, String> headers;
        private final Map<String, String> params;
        private final String body;
        
        public Request(String method, String path, Map<String, String> headers, 
                      Map<String, String> params, String body) {
            this.method = method;
            this.path = path;
            this.headers = headers;
            this.params = params;
            this.body = body;
        }
        
        public String getMethod() { return method; }
        public String getPath() { return path; }
        public Map<String, String> getHeaders() { return headers; }
        public Map<String, String> getParams() { return params; }
        public String getBody() { return body; }
        public String getHeader(String name) { return headers.get(name.toLowerCase()); }
        public String getParam(String name) { return params.get(name); }
    }
    
    /**
     * HTTP Response builder.
     */
    static class Response {
        private int statusCode = 200;
        private String statusMessage = "OK";
        private final Map<String, String> headers = new HashMap<>();
        private String body = "";
        
        public Response status(int code, String message) {
            this.statusCode = code;
            this.statusMessage = message;
            return this;
        }
        
        public Response header(String name, String value) {
            headers.put(name, value);
            return this;
        }
        
        public Response json(String json) {
            header("Content-Type", "application/json");
            this.body = json;
            return this;
        }
        
        public Response text(String text) {
            header("Content-Type", "text/plain");
            this.body = text;
            return this;
        }
        
        public Response html(String html) {
            header("Content-Type", "text/html");
            this.body = html;
            return this;
        }
        
        public int getStatusCode() { return statusCode; }
        public String getStatusMessage() { return statusMessage; }
        public Map<String, String> getHeaders() { return headers; }
        public String getBody() { return body; }
    }
    
    /**
     * Handler function interface.
     */
    @FunctionalInterface
    interface Handler {
        Response handle(Request request, Response response) throws Exception;
    }
    
    /**
     * Middleware function interface.
     */
    @FunctionalInterface
    interface Middleware {
        boolean process(Request request, Response response) throws Exception;
    }
    
    /**
     * Route definition.
     */
    static class Route {
        private final String method;
        private final Pattern pattern;
        private final List<String> paramNames;
        private final Handler handler;
        
        public Route(String method, String path, Handler handler) {
            this.method = method;
            this.handler = handler;
            this.paramNames = new ArrayList<>();
            
            // Convert path pattern to regex (e.g., /users/:id -> /users/([^/]+))
            String regex = path.replaceAll(":(\\w+)", (match) -> {
                paramNames.add(match.group(1));
                return "([^/]+)";
            });
            this.pattern = Pattern.compile("^" + regex + "$");
        }
        
        public boolean matches(String method, String path) {
            return this.method.equals(method) && pattern.matcher(path).matches();
        }
        
        public Map<String, String> extractParams(String path) {
            Map<String, String> params = new HashMap<>();
            Matcher matcher = pattern.matcher(path);
            if (matcher.matches()) {
                for (int i = 0; i < paramNames.size(); i++) {
                    params.put(paramNames.get(i), matcher.group(i + 1));
                }
            }
            return params;
        }
        
        public Handler getHandler() { return handler; }
    }
    
    /**
     * The web framework/server.
     */
    static class Server {
        private final int port;
        private final List<Route> routes = new ArrayList<>();
        private final List<Middleware> middlewares = new ArrayList<>();
        private ServerSocket serverSocket;
        private final ExecutorService threadPool;
        private volatile boolean running = false;
        
        public Server(int port) {
            this.port = port;
            this.threadPool = Executors.newFixedThreadPool(20);
        }
        
        /**
         * Registers a GET route.
         */
        public Server get(String path, Handler handler) {
            routes.add(new Route("GET", path, handler));
            return this;
        }
        
        /**
         * Registers a POST route.
         */
        public Server post(String path, Handler handler) {
            routes.add(new Route("POST", path, handler));
            return this;
        }
        
        /**
         * Registers a PUT route.
         */
        public Server put(String path, Handler handler) {
            routes.add(new Route("PUT", path, handler));
            return this;
        }
        
        /**
         * Registers a DELETE route.
         */
        public Server delete(String path, Handler handler) {
            routes.add(new Route("DELETE", path, handler));
            return this;
        }
        
        /**
         * Adds middleware to the chain.
         */
        public Server use(Middleware middleware) {
            middlewares.add(middleware);
            return this;
        }
        
        /**
         * Starts the server.
         */
        public void start() throws IOException {
            serverSocket = new ServerSocket(port);
            running = true;
            System.out.println("Server listening on http://localhost:" + port);
            
            while (running) {
                try {
                    Socket client = serverSocket.accept();
                    threadPool.execute(() -> handleClient(client));
                } catch (SocketException e) {
                    if (!running) break;
                }
            }
        }
        
        /**
         * Handles a client connection.
         */
        private void handleClient(Socket socket) {
            try (
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                PrintWriter out = new PrintWriter(socket.getOutputStream())
            ) {
                // Parse HTTP request
                String requestLine = in.readLine();
                if (requestLine == null || requestLine.isEmpty()) return;
                
                String[] parts = requestLine.split(" ");
                if (parts.length < 3) return;
                
                String method = parts[0];
                String path = parts[1];
                
                // Parse headers
                Map<String, String> headers = new HashMap<>();
                String line;
                while ((line = in.readLine()) != null && !line.isEmpty()) {
                    int colon = line.indexOf(':');
                    if (colon > 0) {
                        String key = line.substring(0, colon).trim().toLowerCase();
                        String value = line.substring(colon + 1).trim();
                        headers.put(key, value);
                    }
                }
                
                // Read body if present
                StringBuilder bodyBuilder = new StringBuilder();
                if (headers.containsKey("content-length")) {
                    int contentLength = Integer.parseInt(headers.get("content-length"));
                    char[] buffer = new char[contentLength];
                    in.read(buffer, 0, contentLength);
                    bodyBuilder.append(buffer);
                }
                
                // Find matching route
                Route matchedRoute = null;
                Map<String, String> params = new HashMap<>();
                
                for (Route route : routes) {
                    if (route.matches(method, path)) {
                        matchedRoute = route;
                        params = route.extractParams(path);
                        break;
                    }
                }
                
                Request request = new Request(method, path, headers, params, bodyBuilder.toString());
                Response response = new Response();
                
                // Execute middleware chain
                boolean middlewarePassed = true;
                for (Middleware middleware : middlewares) {
                    if (!middleware.process(request, response)) {
                        middlewarePassed = false;
                        break;
                    }
                }
                
                // Execute handler if middleware passed
                if (middlewarePassed) {
                    if (matchedRoute != null) {
                        try {
                            response = matchedRoute.getHandler().handle(request, response);
                        } catch (Exception e) {
                            response.status(500, "Internal Server Error")
                                   .text("Error: " + e.getMessage());
                        }
                    } else {
                        response.status(404, "Not Found")
                               .text("404 Not Found: " + path);
                    }
                }
                
                // Send response
                sendResponse(out, response);
                
            } catch (Exception e) {
                System.err.println("Error handling request: " + e.getMessage());
            } finally {
                try {
                    socket.close();
                } catch (IOException e) {
                    // Ignore
                }
            }
        }
        
        /**
         * Sends HTTP response to client.
         */
        private void sendResponse(PrintWriter out, Response response) {
            out.print("HTTP/1.1 " + response.getStatusCode() + " " + 
                     response.getStatusMessage() + "\r\n");
            
            response.getHeaders().forEach((key, value) -> 
                out.print(key + ": " + value + "\r\n"));
            
            out.print("Content-Length: " + response.getBody().length() + "\r\n");
            out.print("\r\n");
            out.print(response.getBody());
            out.flush();
        }
        
        /**
         * Stops the server.
         */
        public void stop() {
            running = false;
            try {
                if (serverSocket != null) serverSocket.close();
                threadPool.shutdown();
            } catch (IOException e) {
                System.err.println("Error stopping server: " + e.getMessage());
            }
        }
    }
    
    // ============================================================
    // MIDDLEWARE EXAMPLES
    // ============================================================
    
    /**
     * Logging middleware.
     */
    static class LoggerMiddleware implements Middleware {
        @Override
        public boolean process(Request request, Response response) {
            System.out.println("[" + new Date() + "] " + 
                             request.getMethod() + " " + request.getPath());
            return true;
        }
    }
    
    /**
     * CORS middleware.
     */
    static class CorsMiddleware implements Middleware {
        @Override
        public boolean process(Request request, Response response) {
            response.header("Access-Control-Allow-Origin", "*")
                   .header("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE")
                   .header("Access-Control-Allow-Headers", "Content-Type");
            return true;
        }
    }
    
    /**
     * Simple authentication middleware.
     */
    static class AuthMiddleware implements Middleware {
        @Override
        public boolean process(Request request, Response response) {
            String auth = request.getHeader("Authorization");
            if (auth == null || !auth.equals("Bearer secret-token")) {
                response.status(401, "Unauthorized")
                       .json("{\"error\": \"Unauthorized\"}");
                return false;
            }
            return true;
        }
    }
    
    // ============================================================
    // MAIN - DEMO APPLICATION
    // ============================================================
    
    /**
     * Main method - Demonstrates the web framework.
     */
    public static void main(String[] args) {
        Server server = new Server(3000);
        
        // Add middleware
        server.use(new LoggerMiddleware())
              .use(new CorsMiddleware());
        
        // Define routes
        server.get("/", (req, res) -> 
            res.html("<h1>Welcome to WebFramework</h1>" +
                    "<p>Try: <a href='/api/users/123'>/api/users/123</a></p>")
        );
        
        server.get("/api/users/:id", (req, res) -> 
            res.json(String.format("{\"id\": \"%s\", \"name\": \"User %s\", \"email\": \"user%s@example.com\"}", 
                    req.getParam("id"), req.getParam("id"), req.getParam("id")))
        );
        
        server.get("/api/products", (req, res) -> 
            res.json("[{\"id\": 1, \"name\": \"Laptop\", \"price\": 999.99}, " +
                    "{\"id\": 2, \"name\": \"Mouse\", \"price\": 29.99}]")
        );
        
        server.post("/api/users", (req, res) -> 
            res.status(201, "Created")
               .json("{\"message\": \"User created\", \"body\": \"" + req.getBody() + "\"}")
        );
        
        server.get("/protected", (req, res) -> {
            // This route requires auth
            String auth = req.getHeader("Authorization");
            if (auth == null || !auth.equals("Bearer secret-token")) {
                return res.status(401, "Unauthorized")
                         .json("{\"error\": \"Unauthorized\"}");
            }
            return res.json("{\"message\": \"Protected resource\"}");
        });
        
        server.get("/health", (req, res) -> 
            res.json("{\"status\": \"healthy\", \"timestamp\": " + System.currentTimeMillis() + "}")
        );
        
        // Start server
        try {
            System.out.println("\nTest the server with:");
            System.out.println("  curl http://localhost:3000/");
            System.out.println("  curl http://localhost:3000/api/users/123");
            System.out.println("  curl http://localhost:3000/api/products");
            System.out.println("  curl -X POST http://localhost:3000/api/users -d '{\"name\":\"Alice\"}'");
            System.out.println("  curl http://localhost:3000/protected -H 'Authorization: Bearer secret-token'");
            System.out.println();
            
            server.start();
        } catch (IOException e) {
            System.err.println("Failed to start server: " + e.getMessage());
        }
    }
}
