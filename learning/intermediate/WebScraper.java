/*
 * Web Scraper - HTTP requests, parse HTML/JSON, extract data, error handling
 * 
 * Compile: javac WebScraper.java
 * Run: java WebScraper
 * 
 * Note: Uses built-in Java HTTP client (Java 11+)
 */

import java.net.*;
import java.net.http.*;
import java.io.*;
import java.time.Duration;
import java.util.*;
import java.util.regex.*;

public class WebScraper {
    
    private HttpClient client;
    private int retryCount = 3;
    private int timeout = 10;
    
    public WebScraper() {
        this.client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(timeout))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
    }
    
    // Make HTTP GET request with retries
    public String fetchUrl(String urlString) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(urlString))
                .header("User-Agent", "Mozilla/5.0 (WebScraper/1.0)")
                .GET()
                .build();
        
        Exception lastException = null;
        
        for (int i = 0; i < retryCount; i++) {
            try {
                HttpResponse<String> response = client.send(request, 
                        HttpResponse.BodyHandlers.ofString());
                
                if (response.statusCode() == 200) {
                    return response.body();
                } else if (response.statusCode() >= 500 && i < retryCount - 1) {
                    // Retry on server errors
                    System.out.println("Server error, retrying... (attempt " + (i + 2) + ")");
                    Thread.sleep(1000 * (i + 1)); // Exponential backoff
                    continue;
                } else {
                    throw new Exception("HTTP " + response.statusCode());
                }
            } catch (Exception e) {
                lastException = e;
                if (i < retryCount - 1) {
                    System.out.println("Request failed, retrying... (attempt " + (i + 2) + ")");
                    Thread.sleep(1000 * (i + 1));
                }
            }
        }
        
        throw new Exception("Failed after " + retryCount + " attempts: " + 
                (lastException != null ? lastException.getMessage() : "Unknown error"));
    }
    
    // Simple HTML parser to extract text content
    public String extractText(String html) {
        // Remove script and style tags
        html = html.replaceAll("(?s)<script[^>]*>.*?</script>", "");
        html = html.replaceAll("(?s)<style[^>]*>.*?</style>", "");
        
        // Remove HTML tags
        html = html.replaceAll("<[^>]+>", " ");
        
        // Decode HTML entities
        html = html.replaceAll("&nbsp;", " ");
        html = html.replaceAll("&lt;", "<");
        html = html.replaceAll("&gt;", ">");
        html = html.replaceAll("&amp;", "&");
        html = html.replaceAll("&quot;", "\"");
        
        // Clean up whitespace
        html = html.replaceAll("\\s+", " ");
        
        return html.trim();
    }
    
    // Extract all links from HTML
    public List<String> extractLinks(String html, String baseUrl) {
        List<String> links = new ArrayList<>();
        Pattern pattern = Pattern.compile("href=[\"']([^\"']+)[\"']", Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(html);
        
        while (matcher.find()) {
            String link = matcher.group(1);
            
            // Convert relative URLs to absolute
            if (link.startsWith("/")) {
                try {
                    URI base = new URI(baseUrl);
                    link = base.getScheme() + "://" + base.getHost() + link;
                } catch (Exception e) {
                    continue;
                }
            } else if (!link.startsWith("http")) {
                continue; // Skip invalid links
            }
            
            links.add(link);
        }
        
        return links;
    }
    
    // Extract specific elements by tag
    public List<String> extractByTag(String html, String tag) {
        List<String> results = new ArrayList<>();
        Pattern pattern = Pattern.compile("<" + tag + "[^>]*>(.*?)</" + tag + ">", 
                Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
        Matcher matcher = pattern.matcher(html);
        
        while (matcher.find()) {
            results.add(matcher.group(1).trim());
        }
        
        return results;
    }
    
    // Parse JSON response
    public Map<String, Object> parseSimpleJson(String json) {
        Map<String, Object> result = new HashMap<>();
        
        // Very simple JSON parser for demo - just handles flat objects
        json = json.trim();
        if (json.startsWith("{") && json.endsWith("}")) {
            json = json.substring(1, json.length() - 1);
            
            Pattern pattern = Pattern.compile("\"([^\"]+)\"\\s*:\\s*([^,}]+)");
            Matcher matcher = pattern.matcher(json);
            
            while (matcher.find()) {
                String key = matcher.group(1);
                String value = matcher.group(2).trim();
                
                // Remove quotes from strings
                if (value.startsWith("\"") && value.endsWith("\"")) {
                    value = value.substring(1, value.length() - 1);
                    result.put(key, value);
                } else if (value.equals("true") || value.equals("false")) {
                    result.put(key, Boolean.parseBoolean(value));
                } else {
                    try {
                        result.put(key, Double.parseDouble(value));
                    } catch (NumberFormatException e) {
                        result.put(key, value);
                    }
                }
            }
        }
        
        return result;
    }
    
    // Scrape and analyze a webpage
    public void scrapeAndAnalyze(String url) {
        System.out.println("Scraping: " + url);
        System.out.println("=" + "=".repeat(60));
        
        try {
            String content = fetchUrl(url);
            
            System.out.println("Content length: " + content.length() + " bytes");
            
            // Extract title
            List<String> titles = extractByTag(content, "title");
            if (!titles.isEmpty()) {
                System.out.println("Title: " + extractText(titles.get(0)));
            }
            
            // Extract headings
            List<String> h1s = extractByTag(content, "h1");
            System.out.println("\nH1 Headings (" + h1s.size() + "):");
            for (int i = 0; i < Math.min(5, h1s.size()); i++) {
                System.out.println("  - " + extractText(h1s.get(i)));
            }
            
            // Extract links
            List<String> links = extractLinks(content, url);
            System.out.println("\nLinks found: " + links.size());
            System.out.println("First 5 links:");
            for (int i = 0; i < Math.min(5, links.size()); i++) {
                System.out.println("  - " + links.get(i));
            }
            
            // Word count
            String text = extractText(content);
            String[] words = text.split("\\s+");
            System.out.println("\nWord count: " + words.length);
            
        } catch (Exception e) {
            System.err.println("Error scraping " + url + ": " + e.getMessage());
        }
        
        System.out.println();
    }
    
    public static void main(String[] args) {
        WebScraper scraper = new WebScraper();
        
        System.out.println("=== Web Scraper Demo ===\n");
        
        // Example 1: Scrape a public API (JSON)
        System.out.println("Example 1: Fetching JSON data\n");
        try {
            String jsonUrl = "https://jsonplaceholder.typicode.com/posts/1";
            String jsonData = scraper.fetchUrl(jsonUrl);
            System.out.println("Fetched JSON:");
            System.out.println(jsonData.substring(0, Math.min(200, jsonData.length())) + "...");
            
            Map<String, Object> parsed = scraper.parseSimpleJson(jsonData);
            System.out.println("\nParsed fields: " + parsed.keySet());
            System.out.println();
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
        }
        
        // Example 2: Scrape HTML (use a simple, public page)
        System.out.println("Example 2: Scraping HTML\n");
        try {
            scraper.scrapeAndAnalyze("https://example.com");
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
        }
        
        // Example 3: Error handling demo
        System.out.println("Example 3: Error handling (invalid URL)\n");
        try {
            scraper.fetchUrl("https://this-domain-definitely-does-not-exist-12345.com");
        } catch (Exception e) {
            System.out.println("Caught expected error: " + e.getMessage());
        }
        
        System.out.println("\n=== Demo Complete ===");
    }
}
