/*
 * API Client - REST API client with GET/POST/PUT/DELETE, authentication, retries
 * 
 * Compile: javac ApiClient.java
 * Run: java ApiClient
 * 
 * Note: Uses Java 11+ HTTP Client
 */

import java.net.*;
import java.net.http.*;
import java.time.Duration;
import java.util.*;

public class ApiClient {
    
    private HttpClient client;
    private String baseUrl;
    private Map<String, String> defaultHeaders;
    private int maxRetries = 3;
    private int timeout = 10;
    
    public ApiClient(String baseUrl) {
        this.baseUrl = baseUrl;
        this.defaultHeaders = new HashMap<>();
        this.defaultHeaders.put("Content-Type", "application/json");
        this.defaultHeaders.put("User-Agent", "ApiClient/1.0");
        
        this.client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(timeout))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
    }
    
    // Set authentication token
    public void setAuthToken(String token) {
        defaultHeaders.put("Authorization", "Bearer " + token);
    }
    
    // Set basic authentication
    public void setBasicAuth(String username, String password) {
        String auth = username + ":" + password;
        String encodedAuth = Base64.getEncoder().encodeToString(auth.getBytes());
        defaultHeaders.put("Authorization", "Basic " + encodedAuth);
    }
    
    // Add custom header
    public void addHeader(String key, String value) {
        defaultHeaders.put(key, value);
    }
    
    // Build full URL
    private String buildUrl(String endpoint) {
        if (endpoint.startsWith("http")) {
            return endpoint;
        }
        return baseUrl + (endpoint.startsWith("/") ? endpoint : "/" + endpoint);
    }
    
    // Execute request with retries
    private HttpResponse<String> executeWithRetry(HttpRequest request) throws Exception {
        Exception lastException = null;
        
        for (int attempt = 0; attempt < maxRetries; attempt++) {
            try {
                HttpResponse<String> response = client.send(request, 
                        HttpResponse.BodyHandlers.ofString());
                
                // Check for server errors and retry
                if (response.statusCode() >= 500 && attempt < maxRetries - 1) {
                    System.out.println("Server error (HTTP " + response.statusCode() + 
                            "), retrying... (attempt " + (attempt + 2) + "/" + maxRetries + ")");
                    Thread.sleep(1000 * (attempt + 1)); // Exponential backoff
                    continue;
                }
                
                return response;
                
            } catch (Exception e) {
                lastException = e;
                if (attempt < maxRetries - 1) {
                    System.out.println("Request failed: " + e.getMessage() + 
                            ", retrying... (attempt " + (attempt + 2) + "/" + maxRetries + ")");
                    Thread.sleep(1000 * (attempt + 1));
                }
            }
        }
        
        throw new Exception("Request failed after " + maxRetries + " attempts: " + 
                (lastException != null ? lastException.getMessage() : "Unknown error"));
    }
    
    // GET request
    public ApiResponse get(String endpoint) throws Exception {
        return get(endpoint, null);
    }
    
    public ApiResponse get(String endpoint, Map<String, String> queryParams) throws Exception {
        String url = buildUrl(endpoint);
        
        // Add query parameters
        if (queryParams != null && !queryParams.isEmpty()) {
            StringBuilder query = new StringBuilder("?");
            for (Map.Entry<String, String> param : queryParams.entrySet()) {
                query.append(URLEncoder.encode(param.getKey(), "UTF-8"))
                     .append("=")
                     .append(URLEncoder.encode(param.getValue(), "UTF-8"))
                     .append("&");
            }
            url += query.substring(0, query.length() - 1);
        }
        
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .GET();
        
        // Add headers
        for (Map.Entry<String, String> header : defaultHeaders.entrySet()) {
            builder.header(header.getKey(), header.getValue());
        }
        
        HttpResponse<String> response = executeWithRetry(builder.build());
        return new ApiResponse(response.statusCode(), response.body(), response.headers().map());
    }
    
    // POST request
    public ApiResponse post(String endpoint, String body) throws Exception {
        String url = buildUrl(endpoint);
        
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .POST(HttpRequest.BodyPublishers.ofString(body));
        
        // Add headers
        for (Map.Entry<String, String> header : defaultHeaders.entrySet()) {
            builder.header(header.getKey(), header.getValue());
        }
        
        HttpResponse<String> response = executeWithRetry(builder.build());
        return new ApiResponse(response.statusCode(), response.body(), response.headers().map());
    }
    
    // PUT request
    public ApiResponse put(String endpoint, String body) throws Exception {
        String url = buildUrl(endpoint);
        
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .PUT(HttpRequest.BodyPublishers.ofString(body));
        
        // Add headers
        for (Map.Entry<String, String> header : defaultHeaders.entrySet()) {
            builder.header(header.getKey(), header.getValue());
        }
        
        HttpResponse<String> response = executeWithRetry(builder.build());
        return new ApiResponse(response.statusCode(), response.body(), response.headers().map());
    }
    
    // DELETE request
    public ApiResponse delete(String endpoint) throws Exception {
        String url = buildUrl(endpoint);
        
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .DELETE();
        
        // Add headers
        for (Map.Entry<String, String> header : defaultHeaders.entrySet()) {
            builder.header(header.getKey(), header.getValue());
        }
        
        HttpResponse<String> response = executeWithRetry(builder.build());
        return new ApiResponse(response.statusCode(), response.body(), response.headers().map());
    }
    
    // Response class
    static class ApiResponse {
        private int statusCode;
        private String body;
        private Map<String, List<String>> headers;
        
        public ApiResponse(int statusCode, String body, Map<String, List<String>> headers) {
            this.statusCode = statusCode;
            this.body = body;
            this.headers = headers;
        }
        
        public int getStatusCode() {
            return statusCode;
        }
        
        public String getBody() {
            return body;
        }
        
        public Map<String, List<String>> getHeaders() {
            return headers;
        }
        
        public boolean isSuccess() {
            return statusCode >= 200 && statusCode < 300;
        }
        
        public void print() {
            System.out.println("Status: " + statusCode);
            System.out.println("Body: " + (body.length() > 200 ? body.substring(0, 200) + "..." : body));
        }
    }
    
    public static void main(String[] args) {
        System.out.println("=== API Client Demo ===\n");
        
        // Use JSONPlaceholder API for testing
        ApiClient client = new ApiClient("https://jsonplaceholder.typicode.com");
        
        try {
            // Example 1: GET request
            System.out.println("1. GET Request");
            System.out.println("-".repeat(60));
            ApiResponse response1 = client.get("/posts/1");
            response1.print();
            System.out.println();
            
            // Example 2: GET with query parameters
            System.out.println("2. GET Request with Query Parameters");
            System.out.println("-".repeat(60));
            Map<String, String> params = new HashMap<>();
            params.put("userId", "1");
            ApiResponse response2 = client.get("/posts", params);
            System.out.println("Status: " + response2.getStatusCode());
            System.out.println("Found " + response2.getBody().split("\\{").length + " posts");
            System.out.println();
            
            // Example 3: POST request
            System.out.println("3. POST Request");
            System.out.println("-".repeat(60));
            String postData = "{"
                    + "\"title\":\"Test Post\","
                    + "\"body\":\"This is a test post\","
                    + "\"userId\":1"
                    + "}";
            ApiResponse response3 = client.post("/posts", postData);
            response3.print();
            System.out.println();
            
            // Example 4: PUT request
            System.out.println("4. PUT Request");
            System.out.println("-".repeat(60));
            String putData = "{"
                    + "\"id\":1,"
                    + "\"title\":\"Updated Post\","
                    + "\"body\":\"This post has been updated\","
                    + "\"userId\":1"
                    + "}";
            ApiResponse response4 = client.put("/posts/1", putData);
            response4.print();
            System.out.println();
            
            // Example 5: DELETE request
            System.out.println("5. DELETE Request");
            System.out.println("-".repeat(60));
            ApiResponse response5 = client.delete("/posts/1");
            System.out.println("Status: " + response5.getStatusCode());
            System.out.println("Success: " + response5.isSuccess());
            System.out.println();
            
            // Example 6: Custom headers
            System.out.println("6. Request with Custom Headers");
            System.out.println("-".repeat(60));
            client.addHeader("X-Custom-Header", "CustomValue");
            ApiResponse response6 = client.get("/posts/2");
            response6.print();
            System.out.println();
            
            // Example 7: Error handling
            System.out.println("7. Error Handling (404)");
            System.out.println("-".repeat(60));
            try {
                ApiResponse response7 = client.get("/posts/99999");
                System.out.println("Status: " + response7.getStatusCode());
                System.out.println("Success: " + response7.isSuccess());
            } catch (Exception e) {
                System.out.println("Error: " + e.getMessage());
            }
            
            System.out.println("\n=== Demo Complete ===");
            
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
