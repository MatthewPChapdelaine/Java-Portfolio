/**
 * File Reader - Read and display file contents
 * Compile: javac FileReader.java
 * Run: java FileReader <filename>
 */

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class FileReader {
    public static void main(String[] args) {
        if (args.length != 1) {
            System.out.println("Usage: java FileReader <filename>");
            System.exit(1);
        }
        
        String filename = args[0];
        Path path = Paths.get(filename);
        
        try {
            if (!Files.exists(path)) {
                System.out.println("Error: File '" + filename + "' not found");
                return;
            }
            
            String contents = Files.readString(path);
            System.out.println("=== Contents of " + filename + " ===");
            System.out.println(contents);
            System.out.println("\n=== End of file (" + contents.length() + " characters) ===");
            
        } catch (IOException e) {
            System.out.println("Error reading file: " + e.getMessage());
        }
    }
}
