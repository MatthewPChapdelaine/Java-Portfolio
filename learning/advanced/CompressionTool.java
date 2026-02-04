import java.io.*;
import java.util.*;

/**
 * CompressionTool - Huffman coding compression and decompression CLI tool.
 * 
 * Features:
 * - Huffman tree construction
 * - File compression with Huffman coding
 * - File decompression
 * - Compression statistics
 * - Binary file I/O
 * 
 * Compile: javac CompressionTool.java
 * Run: java CompressionTool compress input.txt output.huff
 *      java CompressionTool decompress output.huff restored.txt
 *      java CompressionTool demo
 * 
 * @author Advanced Java Learning
 * @version 1.0
 */
public class CompressionTool {
    
    /**
     * Node in Huffman tree.
     */
    static class HuffmanNode implements Comparable<HuffmanNode> {
        char character;
        int frequency;
        HuffmanNode left;
        HuffmanNode right;
        
        public HuffmanNode(char character, int frequency) {
            this.character = character;
            this.frequency = frequency;
            this.left = null;
            this.right = null;
        }
        
        public boolean isLeaf() {
            return left == null && right == null;
        }
        
        @Override
        public int compareTo(HuffmanNode other) {
            return this.frequency - other.frequency;
        }
    }
    
    /**
     * Huffman encoder/decoder.
     */
    static class HuffmanCoder {
        private HuffmanNode root;
        private Map<Character, String> encodingMap;
        
        /**
         * Builds Huffman tree from frequency map.
         * 
         * @param frequencies Character frequency map
         */
        public void buildTree(Map<Character, Integer> frequencies) {
            PriorityQueue<HuffmanNode> pq = new PriorityQueue<>();
            
            // Create leaf nodes
            for (Map.Entry<Character, Integer> entry : frequencies.entrySet()) {
                pq.offer(new HuffmanNode(entry.getKey(), entry.getValue()));
            }
            
            // Build tree
            while (pq.size() > 1) {
                HuffmanNode left = pq.poll();
                HuffmanNode right = pq.poll();
                
                HuffmanNode parent = new HuffmanNode('\0', left.frequency + right.frequency);
                parent.left = left;
                parent.right = right;
                
                pq.offer(parent);
            }
            
            root = pq.poll();
            
            // Generate encoding map
            encodingMap = new HashMap<>();
            if (root != null) {
                if (root.isLeaf()) {
                    // Special case: single character
                    encodingMap.put(root.character, "0");
                } else {
                    generateEncodingMap(root, "");
                }
            }
        }
        
        /**
         * Generates encoding map by traversing tree.
         */
        private void generateEncodingMap(HuffmanNode node, String code) {
            if (node == null) return;
            
            if (node.isLeaf()) {
                encodingMap.put(node.character, code);
                return;
            }
            
            generateEncodingMap(node.left, code + "0");
            generateEncodingMap(node.right, code + "1");
        }
        
        /**
         * Encodes text using Huffman coding.
         * 
         * @param text Text to encode
         * @return Encoded binary string
         */
        public String encode(String text) {
            StringBuilder encoded = new StringBuilder();
            for (char c : text.toCharArray()) {
                encoded.append(encodingMap.get(c));
            }
            return encoded.toString();
        }
        
        /**
         * Decodes binary string using Huffman tree.
         * 
         * @param encoded Encoded binary string
         * @return Decoded text
         */
        public String decode(String encoded) {
            StringBuilder decoded = new StringBuilder();
            HuffmanNode current = root;
            
            for (char bit : encoded.toCharArray()) {
                current = (bit == '0') ? current.left : current.right;
                
                if (current.isLeaf()) {
                    decoded.append(current.character);
                    current = root;
                }
            }
            
            return decoded.toString();
        }
        
        /**
         * Gets the encoding map.
         */
        public Map<Character, String> getEncodingMap() {
            return encodingMap;
        }
        
        /**
         * Gets the root of Huffman tree.
         */
        public HuffmanNode getRoot() {
            return root;
        }
    }
    
    /**
     * Compressed file format handler.
     */
    static class CompressionHandler {
        
        /**
         * Compresses a file using Huffman coding.
         * 
         * @param inputFile Input file path
         * @param outputFile Output file path
         * @throws IOException if file operations fail
         */
        public static void compressFile(String inputFile, String outputFile) throws IOException {
            // Read input file
            String text = readFile(inputFile);
            if (text.isEmpty()) {
                throw new IOException("Input file is empty");
            }
            
            // Calculate frequencies
            Map<Character, Integer> frequencies = new HashMap<>();
            for (char c : text.toCharArray()) {
                frequencies.put(c, frequencies.getOrDefault(c, 0) + 1);
            }
            
            // Build Huffman tree
            HuffmanCoder coder = new HuffmanCoder();
            coder.buildTree(frequencies);
            
            // Encode text
            String encoded = coder.encode(text);
            
            // Write compressed file
            try (DataOutputStream out = new DataOutputStream(
                    new BufferedOutputStream(new FileOutputStream(outputFile)))) {
                
                // Write header: number of unique characters
                out.writeInt(frequencies.size());
                
                // Write frequency table
                for (Map.Entry<Character, Integer> entry : frequencies.entrySet()) {
                    out.writeChar(entry.getKey());
                    out.writeInt(entry.getValue());
                }
                
                // Write original length (number of bits)
                out.writeInt(encoded.length());
                
                // Write encoded data as bytes
                writeEncodedData(out, encoded);
            }
            
            // Print statistics
            long originalSize = new File(inputFile).length();
            long compressedSize = new File(outputFile).length();
            double ratio = 100.0 * (1.0 - (double) compressedSize / originalSize);
            
            System.out.println("Compression Statistics:");
            System.out.println("  Original size: " + originalSize + " bytes");
            System.out.println("  Compressed size: " + compressedSize + " bytes");
            System.out.println("  Compression ratio: " + String.format("%.2f%%", ratio));
            System.out.println("  Unique characters: " + frequencies.size());
        }
        
        /**
         * Decompresses a file.
         * 
         * @param inputFile Compressed file path
         * @param outputFile Output file path
         * @throws IOException if file operations fail
         */
        public static void decompressFile(String inputFile, String outputFile) throws IOException {
            try (DataInputStream in = new DataInputStream(
                    new BufferedInputStream(new FileInputStream(inputFile)))) {
                
                // Read header
                int uniqueChars = in.readInt();
                
                // Read frequency table
                Map<Character, Integer> frequencies = new HashMap<>();
                for (int i = 0; i < uniqueChars; i++) {
                    char c = in.readChar();
                    int freq = in.readInt();
                    frequencies.put(c, freq);
                }
                
                // Rebuild Huffman tree
                HuffmanCoder coder = new HuffmanCoder();
                coder.buildTree(frequencies);
                
                // Read encoded data length
                int encodedLength = in.readInt();
                
                // Read and decode data
                String encoded = readEncodedData(in, encodedLength);
                String decoded = coder.decode(encoded);
                
                // Write output file
                try (BufferedWriter writer = new BufferedWriter(new FileWriter(outputFile))) {
                    writer.write(decoded);
                }
                
                System.out.println("Decompression complete:");
                System.out.println("  Output file: " + outputFile);
                System.out.println("  Size: " + decoded.length() + " characters");
            }
        }
        
        /**
         * Writes encoded data as packed bytes.
         */
        private static void writeEncodedData(DataOutputStream out, String encoded) throws IOException {
            int byteCount = (encoded.length() + 7) / 8;
            
            for (int i = 0; i < byteCount; i++) {
                int end = Math.min((i + 1) * 8, encoded.length());
                String byteStr = encoded.substring(i * 8, end);
                
                // Pad if necessary
                while (byteStr.length() < 8) {
                    byteStr += "0";
                }
                
                int byteValue = Integer.parseInt(byteStr, 2);
                out.writeByte(byteValue);
            }
        }
        
        /**
         * Reads encoded data from packed bytes.
         */
        private static String readEncodedData(DataInputStream in, int bitCount) throws IOException {
            StringBuilder encoded = new StringBuilder();
            int byteCount = (bitCount + 7) / 8;
            
            for (int i = 0; i < byteCount; i++) {
                int byteValue = in.readByte() & 0xFF;
                String bits = String.format("%8s", Integer.toBinaryString(byteValue))
                    .replace(' ', '0');
                encoded.append(bits);
            }
            
            // Trim to actual bit count
            return encoded.substring(0, bitCount);
        }
        
        /**
         * Reads entire file as string.
         */
        private static String readFile(String path) throws IOException {
            StringBuilder content = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new FileReader(path))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    content.append(line).append("\n");
                }
            }
            return content.toString();
        }
    }
    
    // ============================================================
    // CLI INTERFACE
    // ============================================================
    
    /**
     * Prints usage information.
     */
    private static void printUsage() {
        System.out.println("Usage:");
        System.out.println("  java CompressionTool compress <input> <output>");
        System.out.println("  java CompressionTool decompress <input> <output>");
        System.out.println("  java CompressionTool demo");
        System.out.println();
        System.out.println("Commands:");
        System.out.println("  compress    - Compress a file using Huffman coding");
        System.out.println("  decompress  - Decompress a Huffman-coded file");
        System.out.println("  demo        - Run demonstration");
    }
    
    /**
     * Runs a demonstration.
     */
    private static void runDemo() {
        System.out.println("Huffman Coding Demonstration");
        System.out.println("============================\n");
        
        String text = "this is an example of huffman coding algorithm";
        System.out.println("Original text: \"" + text + "\"");
        System.out.println("Length: " + text.length() + " characters");
        
        // Calculate frequencies
        Map<Character, Integer> frequencies = new HashMap<>();
        for (char c : text.toCharArray()) {
            frequencies.put(c, frequencies.getOrDefault(c, 0) + 1);
        }
        
        System.out.println("\nCharacter frequencies:");
        frequencies.entrySet().stream()
            .sorted(Map.Entry.<Character, Integer>comparingByValue().reversed())
            .forEach(e -> System.out.printf("  '%c': %d\n", e.getKey(), e.getValue()));
        
        // Build Huffman tree and encode
        HuffmanCoder coder = new HuffmanCoder();
        coder.buildTree(frequencies);
        String encoded = coder.encode(text);
        
        System.out.println("\nHuffman codes:");
        coder.getEncodingMap().entrySet().stream()
            .sorted(Comparator.comparing(e -> e.getValue().length()))
            .forEach(e -> System.out.printf("  '%c': %s\n", e.getKey(), e.getValue()));
        
        System.out.println("\nEncoded: " + encoded);
        System.out.println("Encoded length: " + encoded.length() + " bits");
        
        // Decode
        String decoded = coder.decode(encoded);
        System.out.println("\nDecoded: \"" + decoded + "\"");
        System.out.println("Match: " + text.equals(decoded));
        
        // Calculate compression
        int originalBits = text.length() * 8; // ASCII is 8 bits per character
        int compressedBits = encoded.length();
        double ratio = 100.0 * (1.0 - (double) compressedBits / originalBits);
        
        System.out.println("\nCompression analysis:");
        System.out.println("  Original: " + originalBits + " bits");
        System.out.println("  Compressed: " + compressedBits + " bits");
        System.out.println("  Compression ratio: " + String.format("%.2f%%", ratio));
        
        // Test file compression
        System.out.println("\n--- File Compression Test ---");
        try {
            // Create test file
            String testFile = "test_input.txt";
            String compressedFile = "test_compressed.huff";
            String restoredFile = "test_restored.txt";
            
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(testFile))) {
                writer.write(text.repeat(10)); // Repeat for better compression
            }
            
            System.out.println("\nCompressing file...");
            CompressionHandler.compressFile(testFile, compressedFile);
            
            System.out.println("\nDecompressing file...");
            CompressionHandler.decompressFile(compressedFile, restoredFile);
            
            // Verify
            String original = CompressionHandler.readFile(testFile);
            String restored = CompressionHandler.readFile(restoredFile);
            System.out.println("\nVerification: " + 
                (original.equals(restored) ? "SUCCESS" : "FAILED"));
            
            // Cleanup
            new File(testFile).delete();
            new File(compressedFile).delete();
            new File(restoredFile).delete();
            
        } catch (IOException e) {
            System.err.println("Error during file test: " + e.getMessage());
        }
    }
    
    /**
     * Main method - CLI entry point.
     */
    public static void main(String[] args) {
        try {
            if (args.length == 0 || args[0].equals("demo")) {
                runDemo();
                return;
            }
            
            String command = args[0].toLowerCase();
            
            switch (command) {
                case "compress":
                    if (args.length < 3) {
                        System.err.println("Error: Missing arguments");
                        printUsage();
                        System.exit(1);
                    }
                    CompressionHandler.compressFile(args[1], args[2]);
                    break;
                
                case "decompress":
                    if (args.length < 3) {
                        System.err.println("Error: Missing arguments");
                        printUsage();
                        System.exit(1);
                    }
                    CompressionHandler.decompressFile(args[1], args[2]);
                    break;
                
                default:
                    System.err.println("Error: Unknown command '" + command + "'");
                    printUsage();
                    System.exit(1);
            }
            
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
}
