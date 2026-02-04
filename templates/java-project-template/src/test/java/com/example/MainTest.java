package com.example;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for Main class
 */
public class MainTest {
    @Test
    public void testGreet() {
        assertEquals("Hello, Alice!", Main.greet("Alice"));
        assertEquals("Hello, Bob!", Main.greet("Bob"));
    }
    
    @Test
    public void testGreetEmpty() {
        assertEquals("Hello, !", Main.greet(""));
    }
}
