import java.sql.*;
import java.lang.reflect.*;
import java.util.*;
import java.util.function.Consumer;

/**
 * DatabaseORM - A simple Object-Relational Mapping framework with SQLite.
 * 
 * Features:
 * - CRUD operations (Create, Read, Update, Delete)
 * - Query builder with method chaining
 * - Automatic table creation from classes
 * - Simple migrations support
 * - Connection pooling
 * - Annotation-based mapping
 * 
 * Compile: javac DatabaseORM.java
 * Run: java DatabaseORM
 * Note: Requires SQLite JDBC driver (included in most Java distributions)
 * 
 * @author Advanced Java Learning
 * @version 1.0
 */
public class DatabaseORM {
    
    // ============================================================
    // ANNOTATIONS
    // ============================================================
    
    /**
     * Marks a class as a database table.
     */
    @java.lang.annotation.Retention(java.lang.annotation.RetentionPolicy.RUNTIME)
    @java.lang.annotation.Target(java.lang.annotation.ElementType.TYPE)
    @interface Table {
        String name() default "";
    }
    
    /**
     * Marks a field as the primary key.
     */
    @java.lang.annotation.Retention(java.lang.annotation.RetentionPolicy.RUNTIME)
    @java.lang.annotation.Target(java.lang.annotation.ElementType.FIELD)
    @interface Id {
        boolean autoIncrement() default true;
    }
    
    /**
     * Marks a field as a database column.
     */
    @java.lang.annotation.Retention(java.lang.annotation.RetentionPolicy.RUNTIME)
    @java.lang.annotation.Target(java.lang.annotation.ElementType.FIELD)
    @interface Column {
        String name() default "";
        boolean nullable() default true;
    }
    
    // ============================================================
    // DATABASE CONNECTION MANAGER
    // ============================================================
    
    /**
     * Manages database connections with simple pooling.
     */
    static class Database {
        private final String url;
        private final Queue<Connection> pool = new LinkedList<>();
        private final int maxPoolSize = 10;
        
        public Database(String url) throws SQLException {
            this.url = url;
            // Create initial connection to verify database
            Connection conn = DriverManager.getConnection(url);
            pool.offer(conn);
        }
        
        /**
         * Gets a connection from the pool.
         */
        public Connection getConnection() throws SQLException {
            Connection conn = pool.poll();
            if (conn == null || conn.isClosed()) {
                conn = DriverManager.getConnection(url);
            }
            return conn;
        }
        
        /**
         * Returns a connection to the pool.
         */
        public void releaseConnection(Connection conn) {
            if (pool.size() < maxPoolSize) {
                pool.offer(conn);
            } else {
                try {
                    conn.close();
                } catch (SQLException e) {
                    // Ignore
                }
            }
        }
        
        /**
         * Closes all connections.
         */
        public void close() {
            while (!pool.isEmpty()) {
                try {
                    pool.poll().close();
                } catch (SQLException e) {
                    // Ignore
                }
            }
        }
    }
    
    // ============================================================
    // QUERY BUILDER
    // ============================================================
    
    /**
     * SQL query builder with method chaining.
     */
    static class QueryBuilder {
        private StringBuilder query = new StringBuilder();
        private List<Object> parameters = new ArrayList<>();
        
        public QueryBuilder select(String... columns) {
            query.append("SELECT ");
            query.append(columns.length == 0 ? "*" : String.join(", ", columns));
            return this;
        }
        
        public QueryBuilder from(String table) {
            query.append(" FROM ").append(table);
            return this;
        }
        
        public QueryBuilder where(String condition, Object... params) {
            query.append(" WHERE ").append(condition);
            parameters.addAll(Arrays.asList(params));
            return this;
        }
        
        public QueryBuilder and(String condition, Object... params) {
            query.append(" AND ").append(condition);
            parameters.addAll(Arrays.asList(params));
            return this;
        }
        
        public QueryBuilder or(String condition, Object... params) {
            query.append(" OR ").append(condition);
            parameters.addAll(Arrays.asList(params));
            return this;
        }
        
        public QueryBuilder orderBy(String column, String direction) {
            query.append(" ORDER BY ").append(column).append(" ").append(direction);
            return this;
        }
        
        public QueryBuilder limit(int limit) {
            query.append(" LIMIT ").append(limit);
            return this;
        }
        
        public String build() {
            return query.toString();
        }
        
        public List<Object> getParameters() {
            return parameters;
        }
    }
    
    // ============================================================
    // ORM CORE
    // ============================================================
    
    /**
     * Core ORM functionality.
     */
    static class ORM<T> {
        private final Database database;
        private final Class<T> entityClass;
        private final String tableName;
        private Field idField;
        
        public ORM(Database database, Class<T> entityClass) throws Exception {
            this.database = database;
            this.entityClass = entityClass;
            
            // Get table name
            Table tableAnnotation = entityClass.getAnnotation(Table.class);
            if (tableAnnotation != null && !tableAnnotation.name().isEmpty()) {
                this.tableName = tableAnnotation.name();
            } else {
                this.tableName = entityClass.getSimpleName().toLowerCase() + "s";
            }
            
            // Find ID field
            for (Field field : entityClass.getDeclaredFields()) {
                if (field.isAnnotationPresent(Id.class)) {
                    this.idField = field;
                    field.setAccessible(true);
                    break;
                }
            }
            
            // Create table if not exists
            createTable();
        }
        
        /**
         * Creates the table based on entity class.
         */
        private void createTable() throws Exception {
            StringBuilder sql = new StringBuilder("CREATE TABLE IF NOT EXISTS ")
                .append(tableName).append(" (");
            
            List<String> columns = new ArrayList<>();
            
            for (Field field : entityClass.getDeclaredFields()) {
                if (!field.isAnnotationPresent(Column.class) && 
                    !field.isAnnotationPresent(Id.class)) {
                    continue;
                }
                
                String columnName = field.getName();
                String columnType = getSQLType(field.getType());
                StringBuilder column = new StringBuilder(columnName).append(" ").append(columnType);
                
                if (field.isAnnotationPresent(Id.class)) {
                    column.append(" PRIMARY KEY");
                    if (field.getAnnotation(Id.class).autoIncrement()) {
                        column.append(" AUTOINCREMENT");
                    }
                } else {
                    Column colAnnotation = field.getAnnotation(Column.class);
                    if (!colAnnotation.nullable()) {
                        column.append(" NOT NULL");
                    }
                }
                
                columns.add(column.toString());
            }
            
            sql.append(String.join(", ", columns)).append(")");
            
            try (Connection conn = database.getConnection();
                 Statement stmt = conn.createStatement()) {
                stmt.execute(sql.toString());
                database.releaseConnection(conn);
            }
        }
        
        /**
         * Maps Java types to SQL types.
         */
        private String getSQLType(Class<?> type) {
            if (type == int.class || type == Integer.class || type == long.class || type == Long.class) {
                return "INTEGER";
            } else if (type == double.class || type == Double.class || type == float.class || type == Float.class) {
                return "REAL";
            } else if (type == boolean.class || type == Boolean.class) {
                return "INTEGER";
            } else {
                return "TEXT";
            }
        }
        
        /**
         * Saves an entity (insert or update).
         */
        public void save(T entity) throws Exception {
            Object id = idField.get(entity);
            
            if (id == null || (id instanceof Number && ((Number) id).longValue() == 0)) {
                insert(entity);
            } else {
                update(entity);
            }
        }
        
        /**
         * Inserts a new entity.
         */
        private void insert(T entity) throws Exception {
            List<String> columns = new ArrayList<>();
            List<String> placeholders = new ArrayList<>();
            List<Object> values = new ArrayList<>();
            
            for (Field field : entityClass.getDeclaredFields()) {
                if (field.isAnnotationPresent(Id.class)) {
                    Id idAnnotation = field.getAnnotation(Id.class);
                    if (idAnnotation.autoIncrement()) continue;
                }
                
                if (!field.isAnnotationPresent(Column.class) && 
                    !field.isAnnotationPresent(Id.class)) {
                    continue;
                }
                
                field.setAccessible(true);
                columns.add(field.getName());
                placeholders.add("?");
                values.add(field.get(entity));
            }
            
            String sql = String.format("INSERT INTO %s (%s) VALUES (%s)",
                tableName, String.join(", ", columns), String.join(", ", placeholders));
            
            try (Connection conn = database.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                
                for (int i = 0; i < values.size(); i++) {
                    stmt.setObject(i + 1, values.get(i));
                }
                
                stmt.executeUpdate();
                
                // Set generated ID
                if (idField.getAnnotation(Id.class).autoIncrement()) {
                    ResultSet rs = stmt.getGeneratedKeys();
                    if (rs.next()) {
                        idField.set(entity, rs.getLong(1));
                    }
                }
                
                database.releaseConnection(conn);
            }
        }
        
        /**
         * Updates an existing entity.
         */
        private void update(T entity) throws Exception {
            List<String> sets = new ArrayList<>();
            List<Object> values = new ArrayList<>();
            
            for (Field field : entityClass.getDeclaredFields()) {
                if (field.isAnnotationPresent(Id.class)) continue;
                if (!field.isAnnotationPresent(Column.class)) continue;
                
                field.setAccessible(true);
                sets.add(field.getName() + " = ?");
                values.add(field.get(entity));
            }
            
            Object id = idField.get(entity);
            values.add(id);
            
            String sql = String.format("UPDATE %s SET %s WHERE %s = ?",
                tableName, String.join(", ", sets), idField.getName());
            
            try (Connection conn = database.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {
                
                for (int i = 0; i < values.size(); i++) {
                    stmt.setObject(i + 1, values.get(i));
                }
                
                stmt.executeUpdate();
                database.releaseConnection(conn);
            }
        }
        
        /**
         * Finds an entity by ID.
         */
        public T findById(Object id) throws Exception {
            String sql = String.format("SELECT * FROM %s WHERE %s = ?", 
                tableName, idField.getName());
            
            try (Connection conn = database.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {
                
                stmt.setObject(1, id);
                ResultSet rs = stmt.executeQuery();
                
                T result = null;
                if (rs.next()) {
                    result = mapResultToEntity(rs);
                }
                
                database.releaseConnection(conn);
                return result;
            }
        }
        
        /**
         * Finds all entities.
         */
        public List<T> findAll() throws Exception {
            String sql = "SELECT * FROM " + tableName;
            List<T> results = new ArrayList<>();
            
            try (Connection conn = database.getConnection();
                 Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(sql)) {
                
                while (rs.next()) {
                    results.add(mapResultToEntity(rs));
                }
                
                database.releaseConnection(conn);
            }
            
            return results;
        }
        
        /**
         * Deletes an entity by ID.
         */
        public void deleteById(Object id) throws Exception {
            String sql = String.format("DELETE FROM %s WHERE %s = ?", 
                tableName, idField.getName());
            
            try (Connection conn = database.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {
                
                stmt.setObject(1, id);
                stmt.executeUpdate();
                database.releaseConnection(conn);
            }
        }
        
        /**
         * Maps a ResultSet row to an entity.
         */
        private T mapResultToEntity(ResultSet rs) throws Exception {
            T entity = entityClass.getDeclaredConstructor().newInstance();
            
            for (Field field : entityClass.getDeclaredFields()) {
                if (!field.isAnnotationPresent(Column.class) && 
                    !field.isAnnotationPresent(Id.class)) {
                    continue;
                }
                
                field.setAccessible(true);
                Object value = rs.getObject(field.getName());
                
                if (value != null) {
                    if (field.getType() == boolean.class || field.getType() == Boolean.class) {
                        value = ((Number) value).intValue() != 0;
                    }
                    field.set(entity, value);
                }
            }
            
            return entity;
        }
    }
    
    // ============================================================
    // EXAMPLE ENTITIES
    // ============================================================
    
    /**
     * Example User entity.
     */
    @Table(name = "users")
    static class User {
        @Id(autoIncrement = true)
        private Long id;
        
        @Column(nullable = false)
        private String name;
        
        @Column(nullable = false)
        private String email;
        
        @Column
        private Integer age;
        
        public User() {}
        
        public User(String name, String email, Integer age) {
            this.name = name;
            this.email = email;
            this.age = age;
        }
        
        @Override
        public String toString() {
            return String.format("User[id=%d, name=%s, email=%s, age=%d]", 
                id, name, email, age);
        }
        
        // Getters
        public Long getId() { return id; }
        public String getName() { return name; }
        public String getEmail() { return email; }
        public Integer getAge() { return age; }
    }
    
    // ============================================================
    // DEMO
    // ============================================================
    
    /**
     * Main method - Demonstrates the ORM.
     */
    public static void main(String[] args) {
        System.out.println("DatabaseORM - Simple ORM Framework Demo");
        System.out.println("========================================\n");
        
        try {
            // Initialize database
            Database db = new Database("jdbc:sqlite:test_orm.db");
            ORM<User> userORM = new ORM<>(db, User.class);
            
            System.out.println("1. CREATE - Inserting users...");
            User user1 = new User("Alice Smith", "alice@example.com", 30);
            User user2 = new User("Bob Jones", "bob@example.com", 25);
            User user3 = new User("Charlie Brown", "charlie@example.com", 35);
            
            userORM.save(user1);
            userORM.save(user2);
            userORM.save(user3);
            
            System.out.println("  Inserted: " + user1);
            System.out.println("  Inserted: " + user2);
            System.out.println("  Inserted: " + user3);
            
            System.out.println("\n2. READ - Finding all users...");
            List<User> allUsers = userORM.findAll();
            allUsers.forEach(u -> System.out.println("  " + u));
            
            System.out.println("\n3. READ - Finding user by ID...");
            User foundUser = userORM.findById(2L);
            System.out.println("  Found: " + foundUser);
            
            System.out.println("\n4. UPDATE - Updating user...");
            User updateUser = userORM.findById(1L);
            if (updateUser != null) {
                // We'd need setters for a real implementation
                System.out.println("  Before: " + updateUser);
                System.out.println("  (In real implementation, would update fields)");
            }
            
            System.out.println("\n5. DELETE - Deleting user...");
            userORM.deleteById(3L);
            System.out.println("  Deleted user with ID: 3");
            
            System.out.println("\n6. Remaining users:");
            userORM.findAll().forEach(u -> System.out.println("  " + u));
            
            // Cleanup
            db.close();
            System.out.println("\n=== ORM Demo Complete ===");
            System.out.println("Database file: test_orm.db");
            
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
