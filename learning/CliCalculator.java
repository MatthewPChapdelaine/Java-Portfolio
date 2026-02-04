/**
 * CLI Calculator - Perform basic arithmetic operations
 * Compile: javac CliCalculator.java
 * Run: java CliCalculator
 */

import java.util.Scanner;

public class CliCalculator {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        
        System.out.println("=== CLI Calculator ===");
        System.out.println("Operations: +, -, *, /");
        
        try {
            System.out.print("Enter first number: ");
            double num1 = scanner.nextDouble();
            
            System.out.print("Enter operator (+, -, *, /): ");
            String operator = scanner.next();
            
            System.out.print("Enter second number: ");
            double num2 = scanner.nextDouble();
            
            double result;
            
            switch(operator) {
                case "+":
                    result = num1 + num2;
                    break;
                case "-":
                    result = num1 - num2;
                    break;
                case "*":
                    result = num1 * num2;
                    break;
                case "/":
                    if (num2 == 0) {
                        System.out.println("Error: Cannot divide by zero");
                        return;
                    }
                    result = num1 / num2;
                    break;
                default:
                    System.out.println("Error: Invalid operator");
                    return;
            }
            
            System.out.printf("Result: %.2f %s %.2f = %.2f%n", num1, operator, num2, result);
            
        } catch (Exception e) {
            System.out.println("Error: Invalid input");
        } finally {
            scanner.close();
        }
    }
}
