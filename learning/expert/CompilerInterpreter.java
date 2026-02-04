import java.util.*;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

/**
 * Complete interpreter with lexer, parser, AST, symbol tables, functions, and control flow
 * Supports: variables, functions, if/while, arithmetic, comparisons, print statements
 * Language syntax example: let x = 10; if (x > 5) { print(x); }; fn add(a, b) { return a + b; };
 */
public class CompilerInterpreter {
    
    // ============ LEXER ============
    enum TokenType {
        NUMBER, IDENTIFIER, STRING,
        LET, FN, IF, ELSE, WHILE, RETURN, PRINT,
        PLUS, MINUS, STAR, SLASH, PERCENT,
        EQUAL, EQUAL_EQUAL, NOT_EQUAL, LESS, LESS_EQUAL, GREATER, GREATER_EQUAL,
        AND, OR, NOT,
        LPAREN, RPAREN, LBRACE, RBRACE, COMMA, SEMICOLON,
        EOF
    }
    
    static class Token {
        final TokenType type;
        final String lexeme;
        final Object literal;
        final int line;
        
        Token(TokenType type, String lexeme, Object literal, int line) {
            this.type = type;
            this.lexeme = lexeme;
            this.literal = literal;
            this.line = line;
        }
        
        @Override
        public String toString() {
            return String.format("%s(%s)", type, lexeme);
        }
    }
    
    static class Lexer {
        private final String source;
        private final List<Token> tokens = new ArrayList<>();
        private int start = 0;
        private int current = 0;
        private int line = 1;
        
        private static final Map<String, TokenType> keywords = Map.of(
            "let", TokenType.LET,
            "fn", TokenType.FN,
            "if", TokenType.IF,
            "else", TokenType.ELSE,
            "while", TokenType.WHILE,
            "return", TokenType.RETURN,
            "print", TokenType.PRINT,
            "and", TokenType.AND,
            "or", TokenType.OR,
            "not", TokenType.NOT
        );
        
        Lexer(String source) {
            this.source = source;
        }
        
        List<Token> scanTokens() {
            while (!isAtEnd()) {
                start = current;
                scanToken();
            }
            tokens.add(new Token(TokenType.EOF, "", null, line));
            return tokens;
        }
        
        private void scanToken() {
            char c = advance();
            switch (c) {
                case '(': addToken(TokenType.LPAREN); break;
                case ')': addToken(TokenType.RPAREN); break;
                case '{': addToken(TokenType.LBRACE); break;
                case '}': addToken(TokenType.RBRACE); break;
                case ',': addToken(TokenType.COMMA); break;
                case ';': addToken(TokenType.SEMICOLON); break;
                case '+': addToken(TokenType.PLUS); break;
                case '-': addToken(TokenType.MINUS); break;
                case '*': addToken(TokenType.STAR); break;
                case '/': 
                    if (match('/')) {
                        while (peek() != '\n' && !isAtEnd()) advance();
                    } else {
                        addToken(TokenType.SLASH);
                    }
                    break;
                case '%': addToken(TokenType.PERCENT); break;
                case '=': addToken(match('=') ? TokenType.EQUAL_EQUAL : TokenType.EQUAL); break;
                case '!': addToken(match('=') ? TokenType.NOT_EQUAL : TokenType.NOT); break;
                case '<': addToken(match('=') ? TokenType.LESS_EQUAL : TokenType.LESS); break;
                case '>': addToken(match('=') ? TokenType.GREATER_EQUAL : TokenType.GREATER); break;
                case ' ', '\r', '\t': break;
                case '\n': line++; break;
                case '"': string(); break;
                default:
                    if (isDigit(c)) {
                        number();
                    } else if (isAlpha(c)) {
                        identifier();
                    } else {
                        throw new RuntimeException("Unexpected character: " + c + " at line " + line);
                    }
            }
        }
        
        private void identifier() {
            while (isAlphaNumeric(peek())) advance();
            String text = source.substring(start, current);
            TokenType type = keywords.getOrDefault(text, TokenType.IDENTIFIER);
            addToken(type);
        }
        
        private void number() {
            while (isDigit(peek())) advance();
            if (peek() == '.' && isDigit(peekNext())) {
                advance();
                while (isDigit(peek())) advance();
            }
            addToken(TokenType.NUMBER, Double.parseDouble(source.substring(start, current)));
        }
        
        private void string() {
            while (peek() != '"' && !isAtEnd()) {
                if (peek() == '\n') line++;
                advance();
            }
            if (isAtEnd()) throw new RuntimeException("Unterminated string at line " + line);
            advance();
            String value = source.substring(start + 1, current - 1);
            addToken(TokenType.STRING, value);
        }
        
        private boolean match(char expected) {
            if (isAtEnd() || source.charAt(current) != expected) return false;
            current++;
            return true;
        }
        
        private char peek() {
            return isAtEnd() ? '\0' : source.charAt(current);
        }
        
        private char peekNext() {
            return current + 1 >= source.length() ? '\0' : source.charAt(current + 1);
        }
        
        private boolean isAlpha(char c) {
            return (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || c == '_';
        }
        
        private boolean isDigit(char c) {
            return c >= '0' && c <= '9';
        }
        
        private boolean isAlphaNumeric(char c) {
            return isAlpha(c) || isDigit(c);
        }
        
        private boolean isAtEnd() {
            return current >= source.length();
        }
        
        private char advance() {
            return source.charAt(current++);
        }
        
        private void addToken(TokenType type) {
            addToken(type, null);
        }
        
        private void addToken(TokenType type, Object literal) {
            String text = source.substring(start, current);
            tokens.add(new Token(type, text, literal, line));
        }
    }
    
    // ============ AST ============
    interface Expr {
        <R> R accept(ExprVisitor<R> visitor);
    }
    
    interface ExprVisitor<R> {
        R visitBinaryExpr(Binary expr);
        R visitUnaryExpr(Unary expr);
        R visitLiteralExpr(Literal expr);
        R visitVariableExpr(Variable expr);
        R visitCallExpr(Call expr);
    }
    
    static class Binary implements Expr {
        final Expr left;
        final Token operator;
        final Expr right;
        
        Binary(Expr left, Token operator, Expr right) {
            this.left = left;
            this.operator = operator;
            this.right = right;
        }
        
        public <R> R accept(ExprVisitor<R> visitor) {
            return visitor.visitBinaryExpr(this);
        }
    }
    
    static class Unary implements Expr {
        final Token operator;
        final Expr right;
        
        Unary(Token operator, Expr right) {
            this.operator = operator;
            this.right = right;
        }
        
        public <R> R accept(ExprVisitor<R> visitor) {
            return visitor.visitUnaryExpr(this);
        }
    }
    
    static class Literal implements Expr {
        final Object value;
        
        Literal(Object value) {
            this.value = value;
        }
        
        public <R> R accept(ExprVisitor<R> visitor) {
            return visitor.visitLiteralExpr(this);
        }
    }
    
    static class Variable implements Expr {
        final Token name;
        
        Variable(Token name) {
            this.name = name;
        }
        
        public <R> R accept(ExprVisitor<R> visitor) {
            return visitor.visitVariableExpr(this);
        }
    }
    
    static class Call implements Expr {
        final Token name;
        final List<Expr> arguments;
        
        Call(Token name, List<Expr> arguments) {
            this.name = name;
            this.arguments = arguments;
        }
        
        public <R> R accept(ExprVisitor<R> visitor) {
            return visitor.visitCallExpr(this);
        }
    }
    
    interface Stmt {
        <R> R accept(StmtVisitor<R> visitor);
    }
    
    interface StmtVisitor<R> {
        R visitExprStmt(ExprStmt stmt);
        R visitVarStmt(VarStmt stmt);
        R visitBlockStmt(Block stmt);
        R visitIfStmt(If stmt);
        R visitWhileStmt(While stmt);
        R visitFunctionStmt(Function stmt);
        R visitReturnStmt(Return stmt);
    }
    
    static class ExprStmt implements Stmt {
        final Expr expression;
        
        ExprStmt(Expr expression) {
            this.expression = expression;
        }
        
        public <R> R accept(StmtVisitor<R> visitor) {
            return visitor.visitExprStmt(this);
        }
    }
    
    static class VarStmt implements Stmt {
        final Token name;
        final Expr initializer;
        
        VarStmt(Token name, Expr initializer) {
            this.name = name;
            this.initializer = initializer;
        }
        
        public <R> R accept(StmtVisitor<R> visitor) {
            return visitor.visitVarStmt(this);
        }
    }
    
    static class Block implements Stmt {
        final List<Stmt> statements;
        
        Block(List<Stmt> statements) {
            this.statements = statements;
        }
        
        public <R> R accept(StmtVisitor<R> visitor) {
            return visitor.visitBlockStmt(this);
        }
    }
    
    static class If implements Stmt {
        final Expr condition;
        final Stmt thenBranch;
        final Stmt elseBranch;
        
        If(Expr condition, Stmt thenBranch, Stmt elseBranch) {
            this.condition = condition;
            this.thenBranch = thenBranch;
            this.elseBranch = elseBranch;
        }
        
        public <R> R accept(StmtVisitor<R> visitor) {
            return visitor.visitIfStmt(this);
        }
    }
    
    static class While implements Stmt {
        final Expr condition;
        final Stmt body;
        
        While(Expr condition, Stmt body) {
            this.condition = condition;
            this.body = body;
        }
        
        public <R> R accept(StmtVisitor<R> visitor) {
            return visitor.visitWhileStmt(this);
        }
    }
    
    static class Function implements Stmt {
        final Token name;
        final List<Token> params;
        final List<Stmt> body;
        
        Function(Token name, List<Token> params, List<Stmt> body) {
            this.name = name;
            this.params = params;
            this.body = body;
        }
        
        public <R> R accept(StmtVisitor<R> visitor) {
            return visitor.visitFunctionStmt(this);
        }
    }
    
    static class Return implements Stmt {
        final Token keyword;
        final Expr value;
        
        Return(Token keyword, Expr value) {
            this.keyword = keyword;
            this.value = value;
        }
        
        public <R> R accept(StmtVisitor<R> visitor) {
            return visitor.visitReturnStmt(this);
        }
    }
    
    // ============ PARSER ============
    static class Parser {
        private final List<Token> tokens;
        private int current = 0;
        
        Parser(List<Token> tokens) {
            this.tokens = tokens;
        }
        
        List<Stmt> parse() {
            List<Stmt> statements = new ArrayList<>();
            while (!isAtEnd()) {
                statements.add(declaration());
            }
            return statements;
        }
        
        private Stmt declaration() {
            try {
                if (match(TokenType.LET)) return varDeclaration();
                if (match(TokenType.FN)) return function();
                return statement();
            } catch (Exception e) {
                synchronize();
                throw e;
            }
        }
        
        private Stmt varDeclaration() {
            Token name = consume(TokenType.IDENTIFIER, "Expected variable name");
            Expr initializer = null;
            if (match(TokenType.EQUAL)) {
                initializer = expression();
            }
            consume(TokenType.SEMICOLON, "Expected ';' after variable declaration");
            return new VarStmt(name, initializer);
        }
        
        private Stmt function() {
            Token name = consume(TokenType.IDENTIFIER, "Expected function name");
            consume(TokenType.LPAREN, "Expected '(' after function name");
            List<Token> parameters = new ArrayList<>();
            if (!check(TokenType.RPAREN)) {
                do {
                    parameters.add(consume(TokenType.IDENTIFIER, "Expected parameter name"));
                } while (match(TokenType.COMMA));
            }
            consume(TokenType.RPAREN, "Expected ')' after parameters");
            consume(TokenType.LBRACE, "Expected '{' before function body");
            List<Stmt> body = block();
            return new Function(name, parameters, body);
        }
        
        private Stmt statement() {
            if (match(TokenType.IF)) return ifStatement();
            if (match(TokenType.WHILE)) return whileStatement();
            if (match(TokenType.RETURN)) return returnStatement();
            if (match(TokenType.LBRACE)) return new Block(block());
            return expressionStatement();
        }
        
        private Stmt ifStatement() {
            consume(TokenType.LPAREN, "Expected '(' after 'if'");
            Expr condition = expression();
            consume(TokenType.RPAREN, "Expected ')' after condition");
            Stmt thenBranch = statement();
            Stmt elseBranch = null;
            if (match(TokenType.ELSE)) {
                elseBranch = statement();
            }
            return new If(condition, thenBranch, elseBranch);
        }
        
        private Stmt whileStatement() {
            consume(TokenType.LPAREN, "Expected '(' after 'while'");
            Expr condition = expression();
            consume(TokenType.RPAREN, "Expected ')' after condition");
            Stmt body = statement();
            return new While(condition, body);
        }
        
        private Stmt returnStatement() {
            Token keyword = previous();
            Expr value = null;
            if (!check(TokenType.SEMICOLON)) {
                value = expression();
            }
            consume(TokenType.SEMICOLON, "Expected ';' after return value");
            return new Return(keyword, value);
        }
        
        private List<Stmt> block() {
            List<Stmt> statements = new ArrayList<>();
            while (!check(TokenType.RBRACE) && !isAtEnd()) {
                statements.add(declaration());
            }
            consume(TokenType.RBRACE, "Expected '}' after block");
            return statements;
        }
        
        private Stmt expressionStatement() {
            Expr expr = expression();
            consume(TokenType.SEMICOLON, "Expected ';' after expression");
            return new ExprStmt(expr);
        }
        
        private Expr expression() {
            return or();
        }
        
        private Expr or() {
            Expr expr = and();
            while (match(TokenType.OR)) {
                Token operator = previous();
                Expr right = and();
                expr = new Binary(expr, operator, right);
            }
            return expr;
        }
        
        private Expr and() {
            Expr expr = equality();
            while (match(TokenType.AND)) {
                Token operator = previous();
                Expr right = equality();
                expr = new Binary(expr, operator, right);
            }
            return expr;
        }
        
        private Expr equality() {
            Expr expr = comparison();
            while (match(TokenType.EQUAL_EQUAL, TokenType.NOT_EQUAL)) {
                Token operator = previous();
                Expr right = comparison();
                expr = new Binary(expr, operator, right);
            }
            return expr;
        }
        
        private Expr comparison() {
            Expr expr = term();
            while (match(TokenType.GREATER, TokenType.GREATER_EQUAL, TokenType.LESS, TokenType.LESS_EQUAL)) {
                Token operator = previous();
                Expr right = term();
                expr = new Binary(expr, operator, right);
            }
            return expr;
        }
        
        private Expr term() {
            Expr expr = factor();
            while (match(TokenType.MINUS, TokenType.PLUS)) {
                Token operator = previous();
                Expr right = factor();
                expr = new Binary(expr, operator, right);
            }
            return expr;
        }
        
        private Expr factor() {
            Expr expr = unary();
            while (match(TokenType.SLASH, TokenType.STAR, TokenType.PERCENT)) {
                Token operator = previous();
                Expr right = unary();
                expr = new Binary(expr, operator, right);
            }
            return expr;
        }
        
        private Expr unary() {
            if (match(TokenType.NOT, TokenType.MINUS)) {
                Token operator = previous();
                Expr right = unary();
                return new Unary(operator, right);
            }
            return call();
        }
        
        private Expr call() {
            Expr expr = primary();
            if (match(TokenType.LPAREN)) {
                List<Expr> arguments = new ArrayList<>();
                if (!check(TokenType.RPAREN)) {
                    do {
                        arguments.add(expression());
                    } while (match(TokenType.COMMA));
                }
                consume(TokenType.RPAREN, "Expected ')' after arguments");
                if (expr instanceof Variable) {
                    return new Call(((Variable) expr).name, arguments);
                }
            }
            return expr;
        }
        
        private Expr primary() {
            if (match(TokenType.NUMBER, TokenType.STRING)) {
                return new Literal(previous().literal);
            }
            if (match(TokenType.IDENTIFIER)) {
                return new Variable(previous());
            }
            if (match(TokenType.LPAREN)) {
                Expr expr = expression();
                consume(TokenType.RPAREN, "Expected ')' after expression");
                return expr;
            }
            throw new RuntimeException("Expected expression at " + peek());
        }
        
        private boolean match(TokenType... types) {
            for (TokenType type : types) {
                if (check(type)) {
                    advance();
                    return true;
                }
            }
            return false;
        }
        
        private Token consume(TokenType type, String message) {
            if (check(type)) return advance();
            throw new RuntimeException(message + " at " + peek());
        }
        
        private boolean check(TokenType type) {
            return !isAtEnd() && peek().type == type;
        }
        
        private Token advance() {
            if (!isAtEnd()) current++;
            return previous();
        }
        
        private boolean isAtEnd() {
            return peek().type == TokenType.EOF;
        }
        
        private Token peek() {
            return tokens.get(current);
        }
        
        private Token previous() {
            return tokens.get(current - 1);
        }
        
        private void synchronize() {
            advance();
            while (!isAtEnd()) {
                if (previous().type == TokenType.SEMICOLON) return;
                switch (peek().type) {
                    case LET, FN, IF, WHILE, RETURN, PRINT -> { return; }
                }
                advance();
            }
        }
    }
    
    // ============ ENVIRONMENT (Symbol Table) ============
    static class Environment {
        final Map<String, Object> values = new HashMap<>();
        final Environment enclosing;
        
        Environment() {
            this.enclosing = null;
        }
        
        Environment(Environment enclosing) {
            this.enclosing = enclosing;
        }
        
        void define(String name, Object value) {
            values.put(name, value);
        }
        
        Object get(Token name) {
            if (values.containsKey(name.lexeme)) {
                return values.get(name.lexeme);
            }
            if (enclosing != null) return enclosing.get(name);
            throw new RuntimeException("Undefined variable '" + name.lexeme + "'");
        }
        
        void assign(Token name, Object value) {
            if (values.containsKey(name.lexeme)) {
                values.put(name.lexeme, value);
                return;
            }
            if (enclosing != null) {
                enclosing.assign(name, value);
                return;
            }
            throw new RuntimeException("Undefined variable '" + name.lexeme + "'");
        }
    }
    
    // ============ INTERPRETER ============
    static class ReturnException extends RuntimeException {
        final Object value;
        
        ReturnException(Object value) {
            this.value = value;
        }
    }
    
    static class UserFunction {
        final Function declaration;
        final Environment closure;
        
        UserFunction(Function declaration, Environment closure) {
            this.declaration = declaration;
            this.closure = closure;
        }
        
        Object call(Interpreter interpreter, List<Object> arguments) {
            Environment environment = new Environment(closure);
            for (int i = 0; i < declaration.params.size(); i++) {
                environment.define(declaration.params.get(i).lexeme, arguments.get(i));
            }
            
            try {
                interpreter.executeBlock(declaration.body, environment);
            } catch (ReturnException returnValue) {
                return returnValue.value;
            }
            return null;
        }
    }
    
    static class Interpreter implements ExprVisitor<Object>, StmtVisitor<Void> {
        private Environment environment = new Environment();
        
        Interpreter() {
            environment.define("print", new Object());
        }
        
        void interpret(List<Stmt> statements) {
            for (Stmt statement : statements) {
                execute(statement);
            }
        }
        
        private void execute(Stmt stmt) {
            stmt.accept(this);
        }
        
        private Object evaluate(Expr expr) {
            return expr.accept(this);
        }
        
        @Override
        public Object visitBinaryExpr(Binary expr) {
            Object left = evaluate(expr.left);
            Object right = evaluate(expr.right);
            
            return switch (expr.operator.type) {
                case PLUS -> {
                    if (left instanceof Double && right instanceof Double) {
                        yield (Double) left + (Double) right;
                    }
                    if (left instanceof String || right instanceof String) {
                        yield stringify(left) + stringify(right);
                    }
                    throw new RuntimeException("Operands must be numbers or strings");
                }
                case MINUS -> (Double) left - (Double) right;
                case STAR -> (Double) left * (Double) right;
                case SLASH -> (Double) left / (Double) right;
                case PERCENT -> (Double) left % (Double) right;
                case GREATER -> (Double) left > (Double) right;
                case GREATER_EQUAL -> (Double) left >= (Double) right;
                case LESS -> (Double) left < (Double) right;
                case LESS_EQUAL -> (Double) left <= (Double) right;
                case EQUAL_EQUAL -> isEqual(left, right);
                case NOT_EQUAL -> !isEqual(left, right);
                case AND -> isTruthy(left) && isTruthy(right);
                case OR -> isTruthy(left) || isTruthy(right);
                default -> throw new RuntimeException("Unknown binary operator: " + expr.operator.type);
            };
        }
        
        @Override
        public Object visitUnaryExpr(Unary expr) {
            Object right = evaluate(expr.right);
            return switch (expr.operator.type) {
                case MINUS -> -(Double) right;
                case NOT -> !isTruthy(right);
                default -> throw new RuntimeException("Unknown unary operator");
            };
        }
        
        @Override
        public Object visitLiteralExpr(Literal expr) {
            return expr.value;
        }
        
        @Override
        public Object visitVariableExpr(Variable expr) {
            return environment.get(expr.name);
        }
        
        @Override
        public Object visitCallExpr(Call expr) {
            if (expr.name.lexeme.equals("print")) {
                List<Object> args = expr.arguments.stream()
                    .map(this::evaluate)
                    .collect(Collectors.toList());
                System.out.println(args.stream()
                    .map(this::stringify)
                    .collect(Collectors.joining(" ")));
                return null;
            }
            
            Object callee = environment.get(expr.name);
            if (!(callee instanceof UserFunction)) {
                throw new RuntimeException("Can only call functions");
            }
            
            UserFunction function = (UserFunction) callee;
            List<Object> arguments = expr.arguments.stream()
                .map(this::evaluate)
                .collect(Collectors.toList());
            
            if (arguments.size() != function.declaration.params.size()) {
                throw new RuntimeException("Expected " + function.declaration.params.size() + 
                    " arguments but got " + arguments.size());
            }
            
            return function.call(this, arguments);
        }
        
        @Override
        public Void visitExprStmt(ExprStmt stmt) {
            evaluate(stmt.expression);
            return null;
        }
        
        @Override
        public Void visitVarStmt(VarStmt stmt) {
            Object value = null;
            if (stmt.initializer != null) {
                value = evaluate(stmt.initializer);
            }
            environment.define(stmt.name.lexeme, value);
            return null;
        }
        
        @Override
        public Void visitBlockStmt(Block stmt) {
            executeBlock(stmt.statements, new Environment(environment));
            return null;
        }
        
        void executeBlock(List<Stmt> statements, Environment environment) {
            Environment previous = this.environment;
            try {
                this.environment = environment;
                for (Stmt statement : statements) {
                    execute(statement);
                }
            } finally {
                this.environment = previous;
            }
        }
        
        @Override
        public Void visitIfStmt(If stmt) {
            if (isTruthy(evaluate(stmt.condition))) {
                execute(stmt.thenBranch);
            } else if (stmt.elseBranch != null) {
                execute(stmt.elseBranch);
            }
            return null;
        }
        
        @Override
        public Void visitWhileStmt(While stmt) {
            while (isTruthy(evaluate(stmt.condition))) {
                execute(stmt.body);
            }
            return null;
        }
        
        @Override
        public Void visitFunctionStmt(Function stmt) {
            UserFunction function = new UserFunction(stmt, environment);
            environment.define(stmt.name.lexeme, function);
            return null;
        }
        
        @Override
        public Void visitReturnStmt(Return stmt) {
            Object value = null;
            if (stmt.value != null) {
                value = evaluate(stmt.value);
            }
            throw new ReturnException(value);
        }
        
        private boolean isTruthy(Object object) {
            if (object == null) return false;
            if (object instanceof Boolean) return (Boolean) object;
            return true;
        }
        
        private boolean isEqual(Object a, Object b) {
            if (a == null && b == null) return true;
            if (a == null) return false;
            return a.equals(b);
        }
        
        private String stringify(Object object) {
            if (object == null) return "null";
            if (object instanceof Double) {
                String text = object.toString();
                if (text.endsWith(".0")) {
                    text = text.substring(0, text.length() - 2);
                }
                return text;
            }
            return object.toString();
        }
    }
    
    // ============ REPL ============
    public static void main(String[] args) {
        System.out.println("=== Compiler Interpreter Demo ===\n");
        
        // Demo programs
        String[] programs = {
            // Arithmetic and variables
            """
            let x = 10;
            let y = 20;
            print("x + y =", x + y);
            print("x * y =", x * y);
            """,
            
            // Control flow
            """
            let n = 5;
            if (n > 3) {
                print("n is greater than 3");
            } else {
                print("n is not greater than 3");
            };
            """,
            
            // Loops
            """
            let i = 1;
            while (i <= 5) {
                print("Count:", i);
                i = i + 1;
            };
            """,
            
            // Functions
            """
            fn factorial(n) {
                if (n <= 1) {
                    return 1;
                };
                return n * factorial(n - 1);
            };
            print("Factorial of 5:", factorial(5));
            """,
            
            // Fibonacci
            """
            fn fib(n) {
                if (n <= 1) {
                    return n;
                };
                return fib(n - 1) + fib(n - 2);
            };
            let i = 0;
            while (i < 10) {
                print("fib(", i, ") =", fib(i));
                i = i + 1;
            };
            """
        };
        
        for (int i = 0; i < programs.length; i++) {
            System.out.println("--- Program " + (i + 1) + " ---");
            try {
                Lexer lexer = new Lexer(programs[i]);
                List<Token> tokens = lexer.scanTokens();
                
                Parser parser = new Parser(tokens);
                List<Stmt> statements = parser.parse();
                
                Interpreter interpreter = new Interpreter();
                interpreter.interpret(statements);
            } catch (Exception e) {
                System.err.println("Error: " + e.getMessage());
                e.printStackTrace();
            }
            System.out.println();
        }
        
        // Interactive REPL
        System.out.println("\n=== Starting REPL (type 'exit' to quit) ===");
        Scanner scanner = new Scanner(System.in);
        Interpreter replInterpreter = new Interpreter();
        
        while (true) {
            System.out.print("> ");
            String line = scanner.nextLine();
            if (line.equals("exit")) break;
            
            try {
                Lexer lexer = new Lexer(line);
                List<Token> tokens = lexer.scanTokens();
                Parser parser = new Parser(tokens);
                List<Stmt> statements = parser.parse();
                replInterpreter.interpret(statements);
            } catch (Exception e) {
                System.err.println("Error: " + e.getMessage());
            }
        }
        
        scanner.close();
        System.out.println("Goodbye!");
    }
}
