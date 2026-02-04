# Blog Engine

A full-featured Spring Boot blogging platform with authentication, markdown support, comments system, and admin panel.

## Features

- **User Authentication**: Spring Security with role-based access control
- **Blog Posts**: Full CRUD operations with markdown support
- **Markdown Rendering**: CommonMark library for rich content
- **Comments System**: Threaded comments with moderation
- **Admin Panel**: Manage posts, publish/unpublish, edit content
- **REST API**: RESTful endpoints for programmatic access
- **Thymeleaf Templates**: Server-side rendered views
- **H2 Database**: Embedded database with JPA/Hibernate
- **Responsive Design**: Mobile-friendly CSS

## Technology Stack

- **Spring Boot 3.2.0** - Application framework
- **Spring Security** - Authentication & authorization
- **Spring Data JPA** - Database access
- **H2 Database** - Embedded SQL database
- **Thymeleaf** - Template engine
- **CommonMark 0.21.0** - Markdown parsing
- **Lombok** - Reduce boilerplate code
- **Maven** - Build tool

## Project Structure

```
blog-engine/
├── src/main/java/com/example/blogengine/
│   ├── BlogEngineApplication.java
│   ├── config/
│   │   └── DataInitializer.java
│   ├── controller/
│   │   ├── BlogController.java
│   │   └── BlogRestController.java
│   ├── model/
│   │   ├── User.java
│   │   ├── BlogPost.java
│   │   └── Comment.java
│   ├── repository/
│   │   ├── UserRepository.java
│   │   ├── BlogPostRepository.java
│   │   └── CommentRepository.java
│   ├── security/
│   │   └── SecurityConfig.java
│   └── service/
│       ├── UserService.java
│       ├── BlogPostService.java
│       └── CommentService.java
├── src/main/resources/
│   ├── application.properties
│   ├── templates/
│   │   ├── index.html
│   │   ├── post.html
│   │   ├── login.html
│   │   ├── register.html
│   │   └── admin/
│   │       ├── dashboard.html
│   │       └── post-form.html
│   └── static/css/
│       └── style.css
└── pom.xml
```

## Getting Started

### Prerequisites

- Java 17 or higher
- Maven 3.6+

### Installation

1. Clone or navigate to the project directory:
```bash
cd blog-engine
```

2. Build the project:
```bash
mvn clean install
```

3. Run the application:
```bash
mvn spring-boot:run
```

4. Access the application:
- Web Interface: http://localhost:8080
- H2 Console: http://localhost:8080/h2-console
  - JDBC URL: `jdbc:h2:file:./data/blogdb`
  - Username: `sa`
  - Password: (leave empty)

### Default Users

The application initializes with two users:

**Admin Account:**
- Username: `admin`
- Password: `admin123`
- Roles: ADMIN, USER

**Regular User:**
- Username: `john`
- Password: `password`
- Roles: USER

## Usage

### Creating Posts

1. Login with admin credentials
2. Navigate to Admin Dashboard
3. Click "New Post"
4. Write content in Markdown format
5. Add tags (comma-separated)
6. Save and publish

### Markdown Examples

```markdown
# Heading 1
## Heading 2

**Bold text** and *italic text*

- Bullet point 1
- Bullet point 2

1. Numbered item
2. Another item

`inline code`

```java
public class Example {
    public static void main(String[] args) {
        System.out.println("Hello, World!");
    }
}
```
```

### REST API Endpoints

**Get all published posts:**
```bash
curl http://localhost:8080/api/posts
```

**Get post by slug:**
```bash
curl http://localhost:8080/api/posts/my-first-post
```

**Get posts by tag:**
```bash
curl http://localhost:8080/api/posts/tag/java
```

### Web Routes

- `GET /` - Home page with all published posts
- `GET /posts/{slug}` - View individual post
- `GET /posts/tag/{tag}` - Posts filtered by tag
- `GET /login` - Login page
- `GET /register` - Registration page
- `GET /admin` - Admin dashboard (authenticated)
- `GET /admin/posts/new` - Create new post (authenticated)

## Database

The application uses H2 embedded database stored in `./data/blogdb.mv.db`. The schema is automatically created and updated by Hibernate.

### Entities

- **User**: User accounts with roles
- **BlogPost**: Blog posts with markdown content
- **Comment**: Comments on posts

## Security

- Passwords are encrypted using BCrypt
- CSRF protection enabled for forms
- Role-based access control (ROLE_USER, ROLE_ADMIN)
- Public access to posts and API
- Admin-only access to post management

## Configuration

Edit `src/main/resources/application.properties`:

```properties
# Server port
server.port=8080

# Database location
spring.datasource.url=jdbc:h2:file:./data/blogdb

# Enable/disable H2 console
spring.h2.console.enabled=true

# Hibernate DDL mode
spring.jpa.hibernate.ddl-auto=update
```

## Building for Production

```bash
# Create executable JAR
mvn clean package

# Run the JAR
java -jar target/blog-engine-1.0.0.jar
```

## Development

### Adding Sample Posts

Login as admin and create posts through the web interface, or use the REST API programmatically.

### Customizing Templates

Templates are located in `src/main/resources/templates/`. They use Thymeleaf syntax and can be customized for your needs.

### Styling

CSS is in `src/main/resources/static/css/style.css`. Modify for custom branding.

## License

This project is open source and available for educational purposes.

## Author

Created as a demonstration of Spring Boot best practices and real-world application development.
