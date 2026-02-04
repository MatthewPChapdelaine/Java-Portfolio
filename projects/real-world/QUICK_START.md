# Quick Start Guide

## 🚀 Get Started in 5 Minutes

### Prerequisites Check
```bash
java -version   # Should be 17+
mvn -version    # Should be 3.6+
```

---

## 1️⃣ Blog Engine (Spring Boot Web App)

### Start the Application
```bash
cd blog-engine
mvn spring-boot:run
```

### Access & Test
- **URL:** http://localhost:8080
- **Admin Login:** 
  - Username: `admin`
  - Password: `admin123`
- **Regular User:**
  - Username: `john`
  - Password: `password`

### What to Try
1. ✅ Login with admin account
2. ✅ Create a new blog post (Admin → New Post)
3. ✅ Use markdown in content (e.g., `# Heading`, `**bold**`)
4. ✅ Publish the post
5. ✅ View on homepage
6. ✅ Add comments to posts
7. ✅ Try REST API: `curl http://localhost:8080/api/posts`

---

## 2️⃣ Chat Application (WebSocket)

### Start the Application
```bash
cd chat-application
mvn spring-boot:run
```

### Access & Test
- **URL:** http://localhost:8081

### What to Try
1. ✅ Open home page and see available rooms
2. ✅ Join "general" room
3. ✅ Enter username (e.g., "Alice")
4. ✅ Send messages
5. ✅ Open in another browser/tab with different username
6. ✅ See real-time messages
7. ✅ Watch typing indicators
8. ✅ Create new room
9. ✅ View online users in sidebar

---

## 3️⃣ Package Manager (CLI Tool)

### Build the Tool
```bash
cd package-manager
mvn clean package
```

### Quick Commands

**Show help:**
```bash
java -jar target/package-manager-1.0.0-jar-with-dependencies.jar --help
```

**Install dependencies:**
```bash
java -jar target/package-manager-1.0.0-jar-with-dependencies.jar install examples/sample-pom.xml
```

**View dependency tree:**
```bash
java -jar target/package-manager-1.0.0-jar-with-dependencies.jar tree examples/sample-pom.xml
```

**Generate graph (requires Graphviz):**
```bash
java -jar target/package-manager-1.0.0-jar-with-dependencies.jar graph examples/sample-pom.xml
# Opens: dependency-graph.png
```

**Create lock file:**
```bash
java -jar target/package-manager-1.0.0-jar-with-dependencies.jar lock examples/sample-pom.xml
# Creates: package-lock.json
```

**Project info:**
```bash
java -jar target/package-manager-1.0.0-jar-with-dependencies.jar info examples/sample-manifest.json
```

---

## 📊 Database Access (H2 Console)

### Blog Engine Database
- URL: http://localhost:8080/h2-console
- JDBC URL: `jdbc:h2:file:./data/blogdb`
- Username: `sa`
- Password: (leave empty)

### Chat Application Database
- URL: http://localhost:8081/h2-console
- JDBC URL: `jdbc:h2:file:./data/chatdb`
- Username: `sa`
- Password: (leave empty)

---

## 🛠️ Troubleshooting

### Port Already in Use
Change port in `src/main/resources/application.properties`:
```properties
server.port=8082
```

### Maven Build Fails
```bash
mvn clean install -DskipTests
```

### Database Locked
```bash
rm -rf data/
```

---

## 📚 Learning Path

**Beginner** → Start with Blog Engine  
**Intermediate** → Move to Chat Application  
**Advanced** → Explore Package Manager  

---

Happy Coding! 🎉
