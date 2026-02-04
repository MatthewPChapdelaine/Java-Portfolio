import java.util.*;
import java.util.function.Function;

/**
 * DesignPatterns - Comprehensive demonstration of common design patterns.
 * 
 * Implements:
 * 1. Singleton - Thread-safe instance creation
 * 2. Factory - Object creation abstraction
 * 3. Observer - Event notification system
 * 4. Strategy - Algorithm selection at runtime
 * 5. Decorator - Dynamic behavior extension
 * 6. Builder - Complex object construction
 * 
 * Compile: javac DesignPatterns.java
 * Run: java DesignPatterns
 * 
 * @author Advanced Java Learning
 * @version 1.0
 */
public class DesignPatterns {
    
    // ============================================================
    // 1. SINGLETON PATTERN - Thread-safe lazy initialization
    // ============================================================
    
    /**
     * Singleton pattern with double-checked locking for thread safety.
     */
    static class DatabaseConnection {
        private static volatile DatabaseConnection instance;
        private final String connectionString;
        
        private DatabaseConnection() {
            // Simulate expensive initialization
            connectionString = "jdbc:mysql://localhost:3306/mydb";
            System.out.println("  [Singleton] Database connection initialized");
        }
        
        /**
         * Gets the singleton instance using double-checked locking.
         * 
         * @return The singleton instance
         */
        public static DatabaseConnection getInstance() {
            if (instance == null) {
                synchronized (DatabaseConnection.class) {
                    if (instance == null) {
                        instance = new DatabaseConnection();
                    }
                }
            }
            return instance;
        }
        
        public String getConnectionString() {
            return connectionString;
        }
    }
    
    // ============================================================
    // 2. FACTORY PATTERN - Object creation abstraction
    // ============================================================
    
    /**
     * Shape interface for factory pattern.
     */
    interface Shape {
        void draw();
        double area();
    }
    
    static class Circle implements Shape {
        private double radius;
        
        public Circle(double radius) {
            this.radius = radius;
        }
        
        @Override
        public void draw() {
            System.out.println("  [Factory] Drawing Circle with radius " + radius);
        }
        
        @Override
        public double area() {
            return Math.PI * radius * radius;
        }
    }
    
    static class Rectangle implements Shape {
        private double width, height;
        
        public Rectangle(double width, double height) {
            this.width = width;
            this.height = height;
        }
        
        @Override
        public void draw() {
            System.out.println("  [Factory] Drawing Rectangle " + width + "x" + height);
        }
        
        @Override
        public double area() {
            return width * height;
        }
    }
    
    /**
     * Factory for creating Shape objects.
     */
    static class ShapeFactory {
        /**
         * Creates a shape based on type.
         * 
         * @param type The shape type
         * @param dimensions Dimensions for the shape
         * @return A Shape instance
         * @throws IllegalArgumentException if type is invalid
         */
        public static Shape createShape(String type, double... dimensions) {
            switch (type.toUpperCase()) {
                case "CIRCLE":
                    if (dimensions.length < 1) throw new IllegalArgumentException("Circle needs radius");
                    return new Circle(dimensions[0]);
                case "RECTANGLE":
                    if (dimensions.length < 2) throw new IllegalArgumentException("Rectangle needs width and height");
                    return new Rectangle(dimensions[0], dimensions[1]);
                default:
                    throw new IllegalArgumentException("Unknown shape type: " + type);
            }
        }
    }
    
    // ============================================================
    // 3. OBSERVER PATTERN - Event notification system
    // ============================================================
    
    /**
     * Observer interface for receiving notifications.
     */
    interface Observer {
        void update(String event, Object data);
    }
    
    /**
     * Subject that notifies observers of events.
     */
    static class EventPublisher {
        private final List<Observer> observers = new ArrayList<>();
        
        public void subscribe(Observer observer) {
            observers.add(observer);
        }
        
        public void unsubscribe(Observer observer) {
            observers.remove(observer);
        }
        
        /**
         * Notifies all observers of an event.
         * 
         * @param event Event name
         * @param data Event data
         */
        public void notify(String event, Object data) {
            for (Observer observer : observers) {
                observer.update(event, data);
            }
        }
    }
    
    static class EmailSubscriber implements Observer {
        private String email;
        
        public EmailSubscriber(String email) {
            this.email = email;
        }
        
        @Override
        public void update(String event, Object data) {
            System.out.println("  [Observer] Email to " + email + ": " + event + " - " + data);
        }
    }
    
    // ============================================================
    // 4. STRATEGY PATTERN - Algorithm selection at runtime
    // ============================================================
    
    /**
     * Strategy interface for sorting algorithms.
     */
    interface SortStrategy {
        void sort(int[] array);
        String getName();
    }
    
    static class BubbleSort implements SortStrategy {
        @Override
        public void sort(int[] array) {
            for (int i = 0; i < array.length - 1; i++) {
                for (int j = 0; j < array.length - i - 1; j++) {
                    if (array[j] > array[j + 1]) {
                        int temp = array[j];
                        array[j] = array[j + 1];
                        array[j + 1] = temp;
                    }
                }
            }
        }
        
        @Override
        public String getName() {
            return "BubbleSort";
        }
    }
    
    static class QuickSort implements SortStrategy {
        @Override
        public void sort(int[] array) {
            quickSort(array, 0, array.length - 1);
        }
        
        private void quickSort(int[] arr, int low, int high) {
            if (low < high) {
                int pi = partition(arr, low, high);
                quickSort(arr, low, pi - 1);
                quickSort(arr, pi + 1, high);
            }
        }
        
        private int partition(int[] arr, int low, int high) {
            int pivot = arr[high];
            int i = low - 1;
            for (int j = low; j < high; j++) {
                if (arr[j] < pivot) {
                    i++;
                    int temp = arr[i];
                    arr[i] = arr[j];
                    arr[j] = temp;
                }
            }
            int temp = arr[i + 1];
            arr[i + 1] = arr[high];
            arr[high] = temp;
            return i + 1;
        }
        
        @Override
        public String getName() {
            return "QuickSort";
        }
    }
    
    /**
     * Context that uses a sorting strategy.
     */
    static class Sorter {
        private SortStrategy strategy;
        
        public void setStrategy(SortStrategy strategy) {
            this.strategy = strategy;
        }
        
        public void sort(int[] array) {
            if (strategy == null) {
                throw new IllegalStateException("Strategy not set");
            }
            long start = System.nanoTime();
            strategy.sort(array);
            long end = System.nanoTime();
            System.out.println("  [Strategy] Sorted with " + strategy.getName() + 
                             " in " + (end - start) / 1000 + " μs");
        }
    }
    
    // ============================================================
    // 5. DECORATOR PATTERN - Dynamic behavior extension
    // ============================================================
    
    /**
     * Component interface for decorator pattern.
     */
    interface Coffee {
        String getDescription();
        double getCost();
    }
    
    static class SimpleCoffee implements Coffee {
        @Override
        public String getDescription() {
            return "Simple Coffee";
        }
        
        @Override
        public double getCost() {
            return 2.0;
        }
    }
    
    /**
     * Abstract decorator for coffee.
     */
    abstract static class CoffeeDecorator implements Coffee {
        protected Coffee coffee;
        
        public CoffeeDecorator(Coffee coffee) {
            this.coffee = coffee;
        }
    }
    
    static class MilkDecorator extends CoffeeDecorator {
        public MilkDecorator(Coffee coffee) {
            super(coffee);
        }
        
        @Override
        public String getDescription() {
            return coffee.getDescription() + ", Milk";
        }
        
        @Override
        public double getCost() {
            return coffee.getCost() + 0.5;
        }
    }
    
    static class SugarDecorator extends CoffeeDecorator {
        public SugarDecorator(Coffee coffee) {
            super(coffee);
        }
        
        @Override
        public String getDescription() {
            return coffee.getDescription() + ", Sugar";
        }
        
        @Override
        public double getCost() {
            return coffee.getCost() + 0.25;
        }
    }
    
    // ============================================================
    // 6. BUILDER PATTERN - Complex object construction
    // ============================================================
    
    /**
     * Computer class with many optional parameters.
     */
    static class Computer {
        private final String cpu;
        private final int ram;
        private final int storage;
        private final String gpu;
        private final boolean bluetooth;
        private final boolean wifi;
        
        private Computer(Builder builder) {
            this.cpu = builder.cpu;
            this.ram = builder.ram;
            this.storage = builder.storage;
            this.gpu = builder.gpu;
            this.bluetooth = builder.bluetooth;
            this.wifi = builder.wifi;
        }
        
        /**
         * Builder for Computer objects.
         */
        public static class Builder {
            private final String cpu;
            private final int ram;
            private int storage = 256;
            private String gpu = "Integrated";
            private boolean bluetooth = false;
            private boolean wifi = true;
            
            public Builder(String cpu, int ram) {
                this.cpu = cpu;
                this.ram = ram;
            }
            
            public Builder storage(int storage) {
                this.storage = storage;
                return this;
            }
            
            public Builder gpu(String gpu) {
                this.gpu = gpu;
                return this;
            }
            
            public Builder bluetooth(boolean bluetooth) {
                this.bluetooth = bluetooth;
                return this;
            }
            
            public Builder wifi(boolean wifi) {
                this.wifi = wifi;
                return this;
            }
            
            public Computer build() {
                return new Computer(this);
            }
        }
        
        @Override
        public String toString() {
            return String.format("Computer[CPU=%s, RAM=%dGB, Storage=%dGB, GPU=%s, BT=%s, WiFi=%s]",
                cpu, ram, storage, gpu, bluetooth, wifi);
        }
    }
    
    // ============================================================
    // DEMO METHODS
    // ============================================================
    
    private static void demoSingleton() {
        System.out.println("\n=== SINGLETON PATTERN ===");
        DatabaseConnection db1 = DatabaseConnection.getInstance();
        DatabaseConnection db2 = DatabaseConnection.getInstance();
        System.out.println("  Same instance: " + (db1 == db2));
        System.out.println("  Connection: " + db1.getConnectionString());
    }
    
    private static void demoFactory() {
        System.out.println("\n=== FACTORY PATTERN ===");
        Shape circle = ShapeFactory.createShape("CIRCLE", 5.0);
        Shape rectangle = ShapeFactory.createShape("RECTANGLE", 4.0, 6.0);
        
        circle.draw();
        System.out.println("  Area: " + String.format("%.2f", circle.area()));
        rectangle.draw();
        System.out.println("  Area: " + String.format("%.2f", rectangle.area()));
    }
    
    private static void demoObserver() {
        System.out.println("\n=== OBSERVER PATTERN ===");
        EventPublisher publisher = new EventPublisher();
        
        Observer sub1 = new EmailSubscriber("alice@example.com");
        Observer sub2 = new EmailSubscriber("bob@example.com");
        
        publisher.subscribe(sub1);
        publisher.subscribe(sub2);
        
        publisher.notify("NEW_POST", "Check out our new article!");
        publisher.unsubscribe(sub1);
        publisher.notify("SALE", "50% off everything!");
    }
    
    private static void demoStrategy() {
        System.out.println("\n=== STRATEGY PATTERN ===");
        Sorter sorter = new Sorter();
        
        int[] arr1 = {64, 34, 25, 12, 22, 11, 90};
        int[] arr2 = arr1.clone();
        
        sorter.setStrategy(new BubbleSort());
        sorter.sort(arr1);
        System.out.println("  Result: " + Arrays.toString(arr1));
        
        sorter.setStrategy(new QuickSort());
        sorter.sort(arr2);
        System.out.println("  Result: " + Arrays.toString(arr2));
    }
    
    private static void demoDecorator() {
        System.out.println("\n=== DECORATOR PATTERN ===");
        Coffee coffee = new SimpleCoffee();
        System.out.println("  " + coffee.getDescription() + " - $" + coffee.getCost());
        
        coffee = new MilkDecorator(coffee);
        System.out.println("  " + coffee.getDescription() + " - $" + coffee.getCost());
        
        coffee = new SugarDecorator(coffee);
        System.out.println("  " + coffee.getDescription() + " - $" + coffee.getCost());
    }
    
    private static void demoBuilder() {
        System.out.println("\n=== BUILDER PATTERN ===");
        Computer basic = new Computer.Builder("Intel i5", 8).build();
        System.out.println("  " + basic);
        
        Computer gaming = new Computer.Builder("AMD Ryzen 9", 32)
            .storage(1024)
            .gpu("RTX 4080")
            .bluetooth(true)
            .build();
        System.out.println("  " + gaming);
    }
    
    /**
     * Main method - Demonstrates all design patterns.
     * 
     * @param args Command line arguments (unused)
     */
    public static void main(String[] args) {
        System.out.println("Design Patterns Demonstration");
        System.out.println("=============================");
        
        try {
            demoSingleton();
            demoFactory();
            demoObserver();
            demoStrategy();
            demoDecorator();
            demoBuilder();
            
            System.out.println("\n=== ALL PATTERNS DEMONSTRATED SUCCESSFULLY ===");
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
