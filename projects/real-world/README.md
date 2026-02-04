# Real-World Java Projects

This directory contains three complete, production-ready Java projects demonstrating real-world application development patterns and best practices.

## Projects Overview

### 1. Blog Engine 📝
**Location:** `blog-engine/`

A full-featured Spring Boot blogging platform with authentication, markdown support, and admin panel.

**Key Features:**
- Spring Security authentication
- Markdown rendering with CommonMark
- Comments system
- Admin dashboard
- REST API
- H2 database with JPA
- Thymeleaf templates

**Tech Stack:** Spring Boot, Spring Security, Spring Data JPA, Thymeleaf, H2, CommonMark

**Quick Start:**
```bash
cd blog-engine
mvn spring-boot:run
# Access: http://localhost:8080
# Login: admin/admin123
```

---

### 2. Chat Application 💬
**Location:** `chat-application/`

Real-time WebSocket chat application with multiple rooms, private messaging, and user presence tracking.

**Key Features:**
- WebSocket/STOMP messaging
- Multiple chat rooms
- Private messaging
- User presence tracking
- Typing indicators
- Message persistence
- SockJS fallback

**Tech Stack:** Spring Boot, Spring WebSocket, STOMP, SockJS, Spring Data JPA, H2

**Quick Start:**
```bash
cd chat-application
mvn spring-boot:run
# Access: http://localhost:8081
```

---

### 3. Package Manager 📦
**Location:** `package-manager/`

Maven-like dependency management tool with resolution algorithms, lock files, and graph visualization.

**Key Features:**
- POM.xml & JSON manifest parsing
- Dependency resolution algorithm
- Transitive dependency handling
- Lock file generation
- Dependency graph visualization
- CLI with picocli
- Maven-compatible repository

**Tech Stack:** Java 17, Picocli, Gson, dom4j, Graphviz-Java

**Quick Start:**
```bash
cd package-manager
mvn clean package
java -jar target/package-manager-1.0.0-jar-with-dependencies.jar install examples/sample-pom.xml
```

---

## Building All Projects

### Prerequisites
- Java 17 or higher
- Maven 3.6+

### Build Commands

**Build individual project:**
```bash
cd <project-directory>
mvn clean install
```

**Build all projects:**
```bash
# From real-world/ directory
for dir in blog-engine chat-application package-manager; do
    echo "Building $dir..."
    (cd $dir && mvn clean install)
done
```

## Project Comparison

| Feature | Blog Engine | Chat App | Package Manager |
|---------|------------|----------|-----------------|
| Web UI | ✅ Thymeleaf | ✅ HTML/JS | ❌ CLI only |
| Database | ✅ H2/JPA | ✅ H2/JPA | ❌ File-based |
| Real-time | ❌ | ✅ WebSocket | ❌ |
| REST API | ✅ | ❌ | ❌ |
| Authentication | ✅ Spring Security | ❌ | ❌ |
| CLI | ❌ | ❌ | ✅ Picocli |

## Learning Objectives

### Blog Engine
- Spring Boot application structure
- Spring Security configuration
- JPA relationships and queries
- Template rendering with Thymeleaf
- REST API design
- Markdown processing

### Chat Application
- WebSocket/STOMP protocol
- Real-time bidirectional communication
- Message broker configuration
- Session management
- SockJS fallback handling

### Package Manager
- CLI application design
- Graph algorithms
- Dependency resolution
- XML/JSON parsing
- File system operations
- Visualization with Graphviz

## Common Patterns

All projects demonstrate:
- ✅ Proper Java package structure
- ✅ Maven project management
- ✅ Lombok for reducing boilerplate
- ✅ Comprehensive README documentation
- ✅ Clean architecture (models, services, repositories)
- ✅ Error handling
- ✅ Sample data initialization

## Testing

Each project can be tested independently:

```bash
# Run tests for a project
cd <project-directory>
mvn test
```

## Deployment

### Blog Engine & Chat Application

**Create executable JAR:**
```bash
mvn clean package
java -jar target/<artifact-name>.jar
```

**Docker deployment (example):**
```bash
# Build
mvn clean package
docker build -t blog-engine .

# Run
docker run -p 8080:8080 blog-engine
```

### Package Manager

**Create standalone CLI tool:**
```bash
mvn clean package
# Use: target/package-manager-1.0.0-jar-with-dependencies.jar

# Create alias
alias pkgmgr='java -jar $(pwd)/target/package-manager-1.0.0-jar-with-dependencies.jar'
```

## Port Configuration

- **Blog Engine:** Port 8080 (configurable in `application.properties`)
- **Chat Application:** Port 8081 (configurable in `application.properties`)
- **Package Manager:** No port (CLI application)

## Database Access

Both web applications use H2 in-memory database:

- **H2 Console URL:** `http://localhost:<port>/h2-console`
- **JDBC URL:** `jdbc:h2:file:./data/<dbname>`
- **Username:** `sa`
- **Password:** (empty)

## IDE Setup

### IntelliJ IDEA
1. File → Open → Select project directory
2. Maven projects will auto-import
3. Enable annotation processing for Lombok
4. Run main application class

### Eclipse
1. File → Import → Existing Maven Projects
2. Install Lombok plugin
3. Run as Spring Boot App (web apps) or Java Application

### VS Code
1. Install Java Extension Pack
2. Install Spring Boot Extension Pack
3. Open project folder
4. Maven tasks auto-detected

## Troubleshooting

**Port already in use:**
```bash
# Change port in src/main/resources/application.properties
server.port=8082
```

**Lombok not working:**
```bash
# Rebuild with annotation processing enabled
mvn clean install -DskipTests
```

**Database locked:**
```bash
# Delete database files
rm -rf data/
```

**Graphviz not found (Package Manager):**
```bash
# Ubuntu/Debian
sudo apt-get install graphviz

# macOS
brew install graphviz
```

## Contributing

These projects are educational examples. Feel free to:
- Use as learning resources
- Modify for your needs
- Extend with new features
- Use as project templates

## License

All projects are open source and available for educational purposes.

## Additional Resources

- [Spring Boot Documentation](https://spring.io/projects/spring-boot)
- [Spring WebSocket Guide](https://spring.io/guides/gs/messaging-stomp-websocket/)
- [Maven Documentation](https://maven.apache.org/guides/)
- [Picocli User Manual](https://picocli.info/)

## Author

Created as a comprehensive demonstration of Java application development patterns, best practices, and real-world project structures.

---

**Need help?** Check individual project README files for detailed documentation and usage examples.
