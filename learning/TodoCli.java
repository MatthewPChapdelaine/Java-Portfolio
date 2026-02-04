/**
 * TODO CLI - Simple task manager
 * Compile: javac TodoCli.java
 * Run: java TodoCli
 */

import java.io.*;
import java.util.*;

class Todo {
    String task;
    boolean done;
    
    Todo(String task) {
        this.task = task;
        this.done = false;
    }
}

public class TodoCli {
    private static final String TODO_FILE = "todos.txt";
    private static List<Todo> todos = new ArrayList<>();
    
    public static void main(String[] args) {
        loadTodos();
        Scanner scanner = new Scanner(System.in);
        
        while (true) {
            System.out.println("\n=== TODO CLI ===");
            System.out.println("1. List tasks");
            System.out.println("2. Add task");
            System.out.println("3. Complete task");
            System.out.println("4. Exit");
            System.out.print("\nChoice: ");
            
            String choice = scanner.nextLine();
            
            switch(choice) {
                case "1":
                    listTodos();
                    break;
                case "2":
                    System.out.print("Enter task: ");
                    String task = scanner.nextLine();
                    addTodo(task);
                    break;
                case "3":
                    listTodos();
                    System.out.print("Task number to complete: ");
                    try {
                        int num = Integer.parseInt(scanner.nextLine());
                        completeTodo(num);
                    } catch (NumberFormatException e) {
                        System.out.println("Invalid number");
                    }
                    break;
                case "4":
                    System.out.println("Goodbye!");
                    scanner.close();
                    return;
                default:
                    System.out.println("Invalid choice");
            }
        }
    }
    
    private static void loadTodos() {
        try (BufferedReader reader = new BufferedReader(new FileReader(TODO_FILE))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split("\\|");
                if (parts.length == 2) {
                    Todo todo = new Todo(parts[0]);
                    todo.done = Boolean.parseBoolean(parts[1]);
                    todos.add(todo);
                }
            }
        } catch (IOException e) {
            // File doesn't exist yet, start with empty list
        }
    }
    
    private static void saveTodos() {
        try (PrintWriter writer = new PrintWriter(new FileWriter(TODO_FILE))) {
            for (Todo todo : todos) {
                writer.println(todo.task + "|" + todo.done);
            }
        } catch (IOException e) {
            System.out.println("Error saving todos: " + e.getMessage());
        }
    }
    
    private static void listTodos() {
        if (todos.isEmpty()) {
            System.out.println("No tasks yet!");
            return;
        }
        
        System.out.println("\n=== Your Tasks ===");
        for (int i = 0; i < todos.size(); i++) {
            Todo todo = todos.get(i);
            String status = todo.done ? "✓" : " ";
            System.out.println((i + 1) + ". [" + status + "] " + todo.task);
        }
    }
    
    private static void addTodo(String task) {
        todos.add(new Todo(task));
        saveTodos();
        System.out.println("Added: " + task);
    }
    
    private static void completeTodo(int index) {
        if (index > 0 && index <= todos.size()) {
            todos.get(index - 1).done = true;
            saveTodos();
            System.out.println("Completed: " + todos.get(index - 1).task);
        } else {
            System.out.println("Invalid task number");
        }
    }
}
