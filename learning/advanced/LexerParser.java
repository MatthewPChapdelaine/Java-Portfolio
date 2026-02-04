import java.util.*;
import java.util.regex.*;

/**
 * LexerParser - Expression lexer and parser with AST, evaluation, and operator precedence.
 * 
 * Features:
 * - Tokenization (lexical analysis)
 * - Recursive descent parser
 * - Abstract Syntax Tree (AST) generation
 * - Expression evaluation
 * - Operator precedence handling
 * - Support for: +, -, *, /, ^, parentheses, unary minus
 * - Variables and functions
 * 
 * Compile: javac LexerParser.java
 * Run: java LexerParser
 * 
 * Grammar:
 *   expression  := term (('+' | '-') term)*
 *   term        := factor (('*' | '/') factor)*
 *   factor      := power ('^' power)*
 *   power       := unary
 *   unary       := ('-')* primary
 *   primary     := NUMBER | IDENTIFIER | '(' expression ')' | function
 * 
 * @author Advanced Java Learning
 * @version 1.0
 */
public class LexerParser {
    
    // ============================================================
    // TOKEN DEFINITIONS
    // ============================================================
    
    /**
     * Token types.
     */
    enum TokenType {
        NUMBER,      // 123, 45.67
        IDENTIFIER,  // x, y, foo
        PLUS,        // +
        MINUS,       // -
        MULTIPLY,    // *
        DIVIDE,      // /
        POWER,       // ^
        LPAREN,      // (
        RPAREN,      // )
        COMMA,       // ,
        EOF          // End of input
    }
    
    /**
     * Token representation.
     */
    static class Token {
        final TokenType type;
        final String value;
        final int position;
        
        public Token(TokenType type, String value, int position) {
            this.type = type;
            this.value = value;
            this.position = position;
        }
        
        @Override
        public String toString() {
            return String.format("Token[%s, '%s', pos=%d]", type, value, position);
        }
    }
    
    // ============================================================
    // LEXER (TOKENIZER)
    // ============================================================
    
    /**
     * Lexical analyzer - converts input string to tokens.
     */
    static class Lexer {
        private final String input;
        private int position;
        
        public Lexer(String input) {
            this.input = input;
            this.position = 0;
        }
        
        /**
         * Tokenizes the entire input.
         * 
         * @return List of tokens
         * @throws ParseException if invalid token found
         */
        public List<Token> tokenize() throws ParseException {
            List<Token> tokens = new ArrayList<>();
            
            while (position < input.length()) {
                char current = input.charAt(position);
                
                // Skip whitespace
                if (Character.isWhitespace(current)) {
                    position++;
                    continue;
                }
                
                // Number
                if (Character.isDigit(current) || current == '.') {
                    tokens.add(readNumber());
                    continue;
                }
                
                // Identifier
                if (Character.isLetter(current) || current == '_') {
                    tokens.add(readIdentifier());
                    continue;
                }
                
                // Operators and punctuation
                int startPos = position;
                TokenType type = null;
                
                switch (current) {
                    case '+': type = TokenType.PLUS; break;
                    case '-': type = TokenType.MINUS; break;
                    case '*': type = TokenType.MULTIPLY; break;
                    case '/': type = TokenType.DIVIDE; break;
                    case '^': type = TokenType.POWER; break;
                    case '(': type = TokenType.LPAREN; break;
                    case ')': type = TokenType.RPAREN; break;
                    case ',': type = TokenType.COMMA; break;
                    default:
                        throw new ParseException("Invalid character: '" + current + "'", position);
                }
                
                tokens.add(new Token(type, String.valueOf(current), startPos));
                position++;
            }
            
            tokens.add(new Token(TokenType.EOF, "", position));
            return tokens;
        }
        
        /**
         * Reads a number token.
         */
        private Token readNumber() throws ParseException {
            int startPos = position;
            StringBuilder sb = new StringBuilder();
            boolean hasDecimal = false;
            
            while (position < input.length()) {
                char c = input.charAt(position);
                
                if (Character.isDigit(c)) {
                    sb.append(c);
                    position++;
                } else if (c == '.' && !hasDecimal) {
                    hasDecimal = true;
                    sb.append(c);
                    position++;
                } else {
                    break;
                }
            }
            
            return new Token(TokenType.NUMBER, sb.toString(), startPos);
        }
        
        /**
         * Reads an identifier token.
         */
        private Token readIdentifier() {
            int startPos = position;
            StringBuilder sb = new StringBuilder();
            
            while (position < input.length()) {
                char c = input.charAt(position);
                
                if (Character.isLetterOrDigit(c) || c == '_') {
                    sb.append(c);
                    position++;
                } else {
                    break;
                }
            }
            
            return new Token(TokenType.IDENTIFIER, sb.toString(), startPos);
        }
    }
    
    // ============================================================
    // ABSTRACT SYNTAX TREE (AST) NODES
    // ============================================================
    
    /**
     * Base AST node interface.
     */
    interface ASTNode {
        double evaluate(Map<String, Double> variables) throws EvaluationException;
        String toExpressionString();
    }
    
    /**
     * Number literal node.
     */
    static class NumberNode implements ASTNode {
        final double value;
        
        public NumberNode(double value) {
            this.value = value;
        }
        
        @Override
        public double evaluate(Map<String, Double> variables) {
            return value;
        }
        
        @Override
        public String toExpressionString() {
            return String.valueOf(value);
        }
        
        @Override
        public String toString() {
            return "NumberNode(" + value + ")";
        }
    }
    
    /**
     * Variable reference node.
     */
    static class VariableNode implements ASTNode {
        final String name;
        
        public VariableNode(String name) {
            this.name = name;
        }
        
        @Override
        public double evaluate(Map<String, Double> variables) throws EvaluationException {
            if (!variables.containsKey(name)) {
                throw new EvaluationException("Undefined variable: " + name);
            }
            return variables.get(name);
        }
        
        @Override
        public String toExpressionString() {
            return name;
        }
        
        @Override
        public String toString() {
            return "VariableNode(" + name + ")";
        }
    }
    
    /**
     * Binary operation node.
     */
    static class BinaryOpNode implements ASTNode {
        final String operator;
        final ASTNode left;
        final ASTNode right;
        
        public BinaryOpNode(String operator, ASTNode left, ASTNode right) {
            this.operator = operator;
            this.left = left;
            this.right = right;
        }
        
        @Override
        public double evaluate(Map<String, Double> variables) throws EvaluationException {
            double leftVal = left.evaluate(variables);
            double rightVal = right.evaluate(variables);
            
            switch (operator) {
                case "+": return leftVal + rightVal;
                case "-": return leftVal - rightVal;
                case "*": return leftVal * rightVal;
                case "/":
                    if (rightVal == 0) {
                        throw new EvaluationException("Division by zero");
                    }
                    return leftVal / rightVal;
                case "^": return Math.pow(leftVal, rightVal);
                default:
                    throw new EvaluationException("Unknown operator: " + operator);
            }
        }
        
        @Override
        public String toExpressionString() {
            return "(" + left.toExpressionString() + " " + operator + " " + 
                   right.toExpressionString() + ")";
        }
        
        @Override
        public String toString() {
            return "BinaryOpNode(" + operator + ", " + left + ", " + right + ")";
        }
    }
    
    /**
     * Unary operation node.
     */
    static class UnaryOpNode implements ASTNode {
        final String operator;
        final ASTNode operand;
        
        public UnaryOpNode(String operator, ASTNode operand) {
            this.operator = operator;
            this.operand = operand;
        }
        
        @Override
        public double evaluate(Map<String, Double> variables) throws EvaluationException {
            double value = operand.evaluate(variables);
            
            switch (operator) {
                case "-": return -value;
                case "+": return value;
                default:
                    throw new EvaluationException("Unknown unary operator: " + operator);
            }
        }
        
        @Override
        public String toExpressionString() {
            return operator + operand.toExpressionString();
        }
        
        @Override
        public String toString() {
            return "UnaryOpNode(" + operator + ", " + operand + ")";
        }
    }
    
    /**
     * Function call node.
     */
    static class FunctionNode implements ASTNode {
        final String name;
        final List<ASTNode> arguments;
        
        public FunctionNode(String name, List<ASTNode> arguments) {
            this.name = name;
            this.arguments = arguments;
        }
        
        @Override
        public double evaluate(Map<String, Double> variables) throws EvaluationException {
            switch (name.toLowerCase()) {
                case "sin":
                    if (arguments.size() != 1) throw new EvaluationException("sin requires 1 argument");
                    return Math.sin(arguments.get(0).evaluate(variables));
                
                case "cos":
                    if (arguments.size() != 1) throw new EvaluationException("cos requires 1 argument");
                    return Math.cos(arguments.get(0).evaluate(variables));
                
                case "sqrt":
                    if (arguments.size() != 1) throw new EvaluationException("sqrt requires 1 argument");
                    return Math.sqrt(arguments.get(0).evaluate(variables));
                
                case "abs":
                    if (arguments.size() != 1) throw new EvaluationException("abs requires 1 argument");
                    return Math.abs(arguments.get(0).evaluate(variables));
                
                case "max":
                    if (arguments.size() != 2) throw new EvaluationException("max requires 2 arguments");
                    return Math.max(arguments.get(0).evaluate(variables), 
                                  arguments.get(1).evaluate(variables));
                
                case "min":
                    if (arguments.size() != 2) throw new EvaluationException("min requires 2 arguments");
                    return Math.min(arguments.get(0).evaluate(variables), 
                                  arguments.get(1).evaluate(variables));
                
                default:
                    throw new EvaluationException("Unknown function: " + name);
            }
        }
        
        @Override
        public String toExpressionString() {
            StringBuilder sb = new StringBuilder(name).append("(");
            for (int i = 0; i < arguments.size(); i++) {
                if (i > 0) sb.append(", ");
                sb.append(arguments.get(i).toExpressionString());
            }
            sb.append(")");
            return sb.toString();
        }
        
        @Override
        public String toString() {
            return "FunctionNode(" + name + ", " + arguments + ")";
        }
    }
    
    // ============================================================
    // PARSER
    // ============================================================
    
    /**
     * Recursive descent parser.
     */
    static class Parser {
        private final List<Token> tokens;
        private int position;
        
        public Parser(List<Token> tokens) {
            this.tokens = tokens;
            this.position = 0;
        }
        
        /**
         * Parses the token stream into an AST.
         * 
         * @return Root AST node
         * @throws ParseException if syntax error
         */
        public ASTNode parse() throws ParseException {
            ASTNode result = parseExpression();
            
            if (current().type != TokenType.EOF) {
                throw new ParseException("Unexpected token: " + current(), current().position);
            }
            
            return result;
        }
        
        /**
         * Parses an expression (lowest precedence).
         */
        private ASTNode parseExpression() throws ParseException {
            ASTNode node = parseTerm();
            
            while (current().type == TokenType.PLUS || current().type == TokenType.MINUS) {
                String operator = current().value;
                advance();
                node = new BinaryOpNode(operator, node, parseTerm());
            }
            
            return node;
        }
        
        /**
         * Parses a term (multiplication/division).
         */
        private ASTNode parseTerm() throws ParseException {
            ASTNode node = parseFactor();
            
            while (current().type == TokenType.MULTIPLY || current().type == TokenType.DIVIDE) {
                String operator = current().value;
                advance();
                node = new BinaryOpNode(operator, node, parseFactor());
            }
            
            return node;
        }
        
        /**
         * Parses a factor (exponentiation).
         */
        private ASTNode parseFactor() throws ParseException {
            ASTNode node = parseUnary();
            
            if (current().type == TokenType.POWER) {
                String operator = current().value;
                advance();
                // Right associative
                node = new BinaryOpNode(operator, node, parseFactor());
            }
            
            return node;
        }
        
        /**
         * Parses unary operators.
         */
        private ASTNode parseUnary() throws ParseException {
            if (current().type == TokenType.MINUS || current().type == TokenType.PLUS) {
                String operator = current().value;
                advance();
                return new UnaryOpNode(operator, parseUnary());
            }
            
            return parsePrimary();
        }
        
        /**
         * Parses primary expressions (highest precedence).
         */
        private ASTNode parsePrimary() throws ParseException {
            Token token = current();
            
            // Number
            if (token.type == TokenType.NUMBER) {
                advance();
                return new NumberNode(Double.parseDouble(token.value));
            }
            
            // Identifier (variable or function)
            if (token.type == TokenType.IDENTIFIER) {
                String name = token.value;
                advance();
                
                // Function call
                if (current().type == TokenType.LPAREN) {
                    advance();
                    List<ASTNode> args = new ArrayList<>();
                    
                    if (current().type != TokenType.RPAREN) {
                        args.add(parseExpression());
                        
                        while (current().type == TokenType.COMMA) {
                            advance();
                            args.add(parseExpression());
                        }
                    }
                    
                    expect(TokenType.RPAREN);
                    return new FunctionNode(name, args);
                }
                
                // Variable
                return new VariableNode(name);
            }
            
            // Parentheses
            if (token.type == TokenType.LPAREN) {
                advance();
                ASTNode node = parseExpression();
                expect(TokenType.RPAREN);
                return node;
            }
            
            throw new ParseException("Unexpected token: " + token, token.position);
        }
        
        /**
         * Gets current token.
         */
        private Token current() {
            return tokens.get(position);
        }
        
        /**
         * Advances to next token.
         */
        private void advance() {
            if (position < tokens.size() - 1) {
                position++;
            }
        }
        
        /**
         * Expects a specific token type.
         */
        private void expect(TokenType type) throws ParseException {
            if (current().type != type) {
                throw new ParseException(
                    "Expected " + type + " but got " + current().type,
                    current().position
                );
            }
            advance();
        }
    }
    
    // ============================================================
    // EXCEPTIONS
    // ============================================================
    
    /**
     * Parse exception.
     */
    static class ParseException extends Exception {
        final int position;
        
        public ParseException(String message, int position) {
            super(message + " at position " + position);
            this.position = position;
        }
    }
    
    /**
     * Evaluation exception.
     */
    static class EvaluationException extends Exception {
        public EvaluationException(String message) {
            super(message);
        }
    }
    
    // ============================================================
    // DEMO AND TESTS
    // ============================================================
    
    /**
     * Evaluates an expression with given variables.
     */
    public static double evaluate(String expression, Map<String, Double> variables) 
            throws ParseException, EvaluationException {
        Lexer lexer = new Lexer(expression);
        List<Token> tokens = lexer.tokenize();
        
        Parser parser = new Parser(tokens);
        ASTNode ast = parser.parse();
        
        return ast.evaluate(variables);
    }
    
    /**
     * Evaluates an expression without variables.
     */
    public static double evaluate(String expression) 
            throws ParseException, EvaluationException {
        return evaluate(expression, new HashMap<>());
    }
    
    /**
     * Tests the lexer/parser.
     */
    private static void runTests() {
        System.out.println("\n=== RUNNING TESTS ===\n");
        
        String[] testExpressions = {
            "2 + 3",
            "2 * 3 + 4",
            "2 + 3 * 4",
            "(2 + 3) * 4",
            "2 ^ 3",
            "2 ^ 3 ^ 2",
            "-5 + 3",
            "2 * -3",
            "10 / 2 + 3",
            "sqrt(16)",
            "sin(0)",
            "max(5, 10)",
            "2 * x + y",
            "x^2 + 2*x + 1",
            "a * b + c / d"
        };
        
        Map<String, Double> vars = new HashMap<>();
        vars.put("x", 3.0);
        vars.put("y", 4.0);
        vars.put("a", 2.0);
        vars.put("b", 5.0);
        vars.put("c", 8.0);
        vars.put("d", 2.0);
        
        int passed = 0;
        int failed = 0;
        
        for (String expr : testExpressions) {
            try {
                Lexer lexer = new Lexer(expr);
                List<Token> tokens = lexer.tokenize();
                
                Parser parser = new Parser(tokens);
                ASTNode ast = parser.parse();
                
                double result = ast.evaluate(vars);
                
                System.out.println("✓ Expression: " + expr);
                System.out.println("  AST: " + ast);
                System.out.println("  Result: " + result);
                System.out.println();
                
                passed++;
                
            } catch (Exception e) {
                System.out.println("✗ Expression: " + expr);
                System.out.println("  Error: " + e.getMessage());
                System.out.println();
                failed++;
            }
        }
        
        System.out.println("=== TEST SUMMARY ===");
        System.out.println("Passed: " + passed);
        System.out.println("Failed: " + failed);
    }
    
    /**
     * Interactive demo.
     */
    private static void interactiveDemo() {
        System.out.println("\n=== INTERACTIVE DEMO ===");
        System.out.println("Enter expressions to evaluate (or 'quit' to exit)");
        System.out.println("Supported: +, -, *, /, ^, (), sin, cos, sqrt, abs, max, min");
        System.out.println("Example: 2^3 + sqrt(16) * sin(0)\n");
        
        Scanner scanner = new Scanner(System.in);
        Map<String, Double> variables = new HashMap<>();
        
        while (true) {
            System.out.print("> ");
            String input = scanner.nextLine().trim();
            
            if (input.equalsIgnoreCase("quit") || input.equalsIgnoreCase("exit")) {
                break;
            }
            
            if (input.isEmpty()) {
                continue;
            }
            
            // Check for variable assignment (x = 5)
            if (input.contains("=") && !input.contains("==")) {
                String[] parts = input.split("=", 2);
                String varName = parts[0].trim();
                String expr = parts[1].trim();
                
                try {
                    double value = evaluate(expr, variables);
                    variables.put(varName, value);
                    System.out.println(varName + " = " + value);
                } catch (Exception e) {
                    System.out.println("Error: " + e.getMessage());
                }
                continue;
            }
            
            // Evaluate expression
            try {
                double result = evaluate(input, variables);
                System.out.println("= " + result);
            } catch (Exception e) {
                System.out.println("Error: " + e.getMessage());
            }
        }
        
        scanner.close();
    }
    
    /**
     * Main method - Runs tests and demo.
     */
    public static void main(String[] args) {
        System.out.println("Expression Lexer & Parser");
        System.out.println("========================");
        
        runTests();
        
        // Uncomment for interactive mode
        // interactiveDemo();
        
        System.out.println("\n=== DEMO COMPLETE ===");
        System.out.println("Uncomment interactiveDemo() in main() for interactive mode");
    }
}
