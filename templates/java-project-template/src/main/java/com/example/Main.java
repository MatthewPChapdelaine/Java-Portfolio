package com.example;

/**
 * Main application entry point
 */
public class Main {
    public static String greet(String name) {
        return "Hello, " + name + "!";
    }
    
    public static void main(String[] args) {
        System.out.println("Java Project Template");
        System.out.println(greet("World"));
    }
}
