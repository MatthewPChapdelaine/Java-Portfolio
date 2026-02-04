/*
 * JSON Parser - Parse JSON, manipulate objects, validate schema, write back
 * 
 * Compile: javac JsonParser.java
 * Run: java JsonParser
 */

import java.util.*;
import java.io.*;

public class JsonParser {
    
    // Simple JSON value representation
    static abstract class JsonValue {
        abstract String toJsonString();
    }
    
    static class JsonObject extends JsonValue {
        Map<String, JsonValue> data = new LinkedHashMap<>();
        
        void put(String key, JsonValue value) {
            data.put(key, value);
        }
        
        JsonValue get(String key) {
            return data.get(key);
        }
        
        @Override
        String toJsonString() {
            StringBuilder sb = new StringBuilder("{");
            boolean first = true;
            for (Map.Entry<String, JsonValue> entry : data.entrySet()) {
                if (!first) sb.append(",");
                sb.append("\"").append(entry.getKey()).append("\":");
                sb.append(entry.getValue().toJsonString());
                first = false;
            }
            sb.append("}");
            return sb.toString();
        }
    }
    
    static class JsonArray extends JsonValue {
        List<JsonValue> data = new ArrayList<>();
        
        void add(JsonValue value) {
            data.add(value);
        }
        
        @Override
        String toJsonString() {
            StringBuilder sb = new StringBuilder("[");
            for (int i = 0; i < data.size(); i++) {
                if (i > 0) sb.append(",");
                sb.append(data.get(i).toJsonString());
            }
            sb.append("]");
            return sb.toString();
        }
    }
    
    static class JsonString extends JsonValue {
        String value;
        
        JsonString(String value) {
            this.value = value;
        }
        
        @Override
        String toJsonString() {
            return "\"" + value.replace("\"", "\\\"") + "\"";
        }
    }
    
    static class JsonNumber extends JsonValue {
        double value;
        
        JsonNumber(double value) {
            this.value = value;
        }
        
        @Override
        String toJsonString() {
            if (value == (long) value) {
                return String.valueOf((long) value);
            }
            return String.valueOf(value);
        }
    }
    
    static class JsonBoolean extends JsonValue {
        boolean value;
        
        JsonBoolean(boolean value) {
            this.value = value;
        }
        
        @Override
        String toJsonString() {
            return String.valueOf(value);
        }
    }
    
    static class JsonNull extends JsonValue {
        @Override
        String toJsonString() {
            return "null";
        }
    }
    
    // Simple JSON parser
    static class Parser {
        private String json;
        private int pos = 0;
        
        Parser(String json) {
            this.json = json;
        }
        
        JsonValue parse() throws Exception {
            skipWhitespace();
            return parseValue();
        }
        
        private JsonValue parseValue() throws Exception {
            skipWhitespace();
            char c = peek();
            
            if (c == '{') return parseObject();
            if (c == '[') return parseArray();
            if (c == '"') return parseString();
            if (c == 't' || c == 'f') return parseBoolean();
            if (c == 'n') return parseNull();
            if (c == '-' || Character.isDigit(c)) return parseNumber();
            
            throw new Exception("Unexpected character: " + c);
        }
        
        private JsonObject parseObject() throws Exception {
            consume('{');
            JsonObject obj = new JsonObject();
            skipWhitespace();
            
            if (peek() == '}') {
                consume('}');
                return obj;
            }
            
            while (true) {
                skipWhitespace();
                String key = parseString().value;
                skipWhitespace();
                consume(':');
                JsonValue value = parseValue();
                obj.put(key, value);
                skipWhitespace();
                
                if (peek() == '}') {
                    consume('}');
                    break;
                }
                consume(',');
            }
            
            return obj;
        }
        
        private JsonArray parseArray() throws Exception {
            consume('[');
            JsonArray arr = new JsonArray();
            skipWhitespace();
            
            if (peek() == ']') {
                consume(']');
                return arr;
            }
            
            while (true) {
                arr.add(parseValue());
                skipWhitespace();
                
                if (peek() == ']') {
                    consume(']');
                    break;
                }
                consume(',');
            }
            
            return arr;
        }
        
        private JsonString parseString() throws Exception {
            consume('"');
            StringBuilder sb = new StringBuilder();
            
            while (peek() != '"') {
                char c = consume();
                if (c == '\\') {
                    char next = consume();
                    if (next == 'n') sb.append('\n');
                    else if (next == 't') sb.append('\t');
                    else if (next == 'r') sb.append('\r');
                    else sb.append(next);
                } else {
                    sb.append(c);
                }
            }
            
            consume('"');
            return new JsonString(sb.toString());
        }
        
        private JsonNumber parseNumber() throws Exception {
            StringBuilder sb = new StringBuilder();
            
            if (peek() == '-') {
                sb.append(consume());
            }
            
            while (pos < json.length() && (Character.isDigit(peek()) || peek() == '.')) {
                sb.append(consume());
            }
            
            return new JsonNumber(Double.parseDouble(sb.toString()));
        }
        
        private JsonBoolean parseBoolean() throws Exception {
            if (json.startsWith("true", pos)) {
                pos += 4;
                return new JsonBoolean(true);
            } else if (json.startsWith("false", pos)) {
                pos += 5;
                return new JsonBoolean(false);
            }
            throw new Exception("Invalid boolean");
        }
        
        private JsonNull parseNull() throws Exception {
            if (json.startsWith("null", pos)) {
                pos += 4;
                return new JsonNull();
            }
            throw new Exception("Invalid null");
        }
        
        private char peek() {
            return json.charAt(pos);
        }
        
        private char consume() {
            return json.charAt(pos++);
        }
        
        private void consume(char expected) throws Exception {
            if (peek() != expected) {
                throw new Exception("Expected '" + expected + "' but got '" + peek() + "'");
            }
            pos++;
        }
        
        private void skipWhitespace() {
            while (pos < json.length() && Character.isWhitespace(peek())) {
                pos++;
            }
        }
    }
    
    // Schema validator
    static boolean validateSchema(JsonValue value, Map<String, String> schema) {
        if (!(value instanceof JsonObject)) return false;
        JsonObject obj = (JsonObject) value;
        
        for (Map.Entry<String, String> entry : schema.entrySet()) {
            String key = entry.getKey();
            String type = entry.getValue();
            JsonValue val = obj.get(key);
            
            if (val == null) return false;
            
            if (type.equals("string") && !(val instanceof JsonString)) return false;
            if (type.equals("number") && !(val instanceof JsonNumber)) return false;
            if (type.equals("boolean") && !(val instanceof JsonBoolean)) return false;
            if (type.equals("object") && !(val instanceof JsonObject)) return false;
            if (type.equals("array") && !(val instanceof JsonArray)) return false;
        }
        
        return true;
    }
    
    public static void main(String[] args) {
        try {
            // Example 1: Parse and manipulate JSON
            System.out.println("=== JSON Parser Demo ===\n");
            
            String jsonText = "{\"name\":\"John\",\"age\":30,\"active\":true,\"scores\":[85,90,78]}";
            System.out.println("Original JSON: " + jsonText);
            
            Parser parser = new Parser(jsonText);
            JsonValue parsed = parser.parse();
            System.out.println("Parsed and stringified: " + parsed.toJsonString());
            
            // Manipulate the object
            if (parsed instanceof JsonObject) {
                JsonObject obj = (JsonObject) parsed;
                obj.put("age", new JsonNumber(31)); // Update age
                obj.put("city", new JsonString("New York")); // Add new field
                System.out.println("After manipulation: " + obj.toJsonString());
            }
            
            // Example 2: Schema validation
            System.out.println("\n=== Schema Validation ===\n");
            
            Map<String, String> schema = new HashMap<>();
            schema.put("name", "string");
            schema.put("age", "number");
            schema.put("active", "boolean");
            
            boolean isValid = validateSchema(parsed, schema);
            System.out.println("Schema validation result: " + (isValid ? "VALID" : "INVALID"));
            
            // Example 3: Create complex JSON
            System.out.println("\n=== Creating Complex JSON ===\n");
            
            JsonObject user = new JsonObject();
            user.put("id", new JsonNumber(1));
            user.put("username", new JsonString("alice"));
            
            JsonArray hobbies = new JsonArray();
            hobbies.add(new JsonString("reading"));
            hobbies.add(new JsonString("coding"));
            hobbies.add(new JsonString("hiking"));
            user.put("hobbies", hobbies);
            
            JsonObject address = new JsonObject();
            address.put("street", new JsonString("123 Main St"));
            address.put("city", new JsonString("Boston"));
            address.put("zip", new JsonString("02101"));
            user.put("address", address);
            
            System.out.println("Created JSON: " + user.toJsonString());
            
            // Write to file
            try (PrintWriter writer = new PrintWriter("output.json")) {
                writer.println(user.toJsonString());
                System.out.println("\nJSON written to output.json");
            }
            
            System.out.println("\n=== Demo Complete ===");
            
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
