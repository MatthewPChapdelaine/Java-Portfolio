# Chat Application

A real-time WebSocket-based chat application built with Spring Boot, STOMP, and SockJS. Features multiple chat rooms, private messaging, user presence tracking, and message persistence.

## Features

- **Real-Time Messaging**: WebSocket/STOMP protocol for instant message delivery
- **Multiple Chat Rooms**: Create and join different chat rooms
- **Private Messaging**: Send direct messages to specific users
- **User Presence**: Track online users in real-time
- **Typing Indicators**: See when other users are typing
- **Message Persistence**: All messages saved to database
- **User Sessions**: Automatic session management
- **Responsive Design**: Works on desktop and mobile devices
- **SockJS Fallback**: Automatic fallback for browsers without WebSocket support

## Technology Stack

- **Spring Boot 3.2.0** - Application framework
- **Spring WebSocket** - WebSocket support
- **STOMP** - Messaging protocol
- **SockJS** - WebSocket fallback
- **Spring Data JPA** - Database access
- **H2 Database** - Embedded database
- **Thymeleaf** - Server-side templates
- **Maven** - Build tool

## Project Structure

```
chat-application/
├── src/main/java/com/example/chatapp/
│   ├── ChatApplication.java
│   ├── config/
│   │   ├── WebSocketConfig.java
│   │   └── DataInitializer.java
│   ├── controller/
│   │   ├── ChatController.java
│   │   └── WebController.java
│   ├── model/
│   │   ├── User.java
│   │   ├── ChatRoom.java
│   │   ├── Message.java
│   │   └── ChatMessage.java
│   ├── repository/
│   │   ├── UserRepository.java
│   │   ├── ChatRoomRepository.java
│   │   └── MessageRepository.java
│   └── service/
│       ├── UserService.java
│       ├── ChatRoomService.java
│       └── MessageService.java
├── src/main/resources/
│   ├── application.properties
│   ├── templates/
│   │   ├── index.html
│   │   └── chat.html
│   └── static/
│       ├── css/style.css
│       └── js/chat.js
└── pom.xml
```

## Getting Started

### Prerequisites

- Java 17 or higher
- Maven 3.6+

### Installation

1. Navigate to the project directory:
```bash
cd chat-application
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
- Web Interface: http://localhost:8081
- H2 Console: http://localhost:8081/h2-console
  - JDBC URL: `jdbc:h2:file:./data/chatdb`
  - Username: `sa`
  - Password: (leave empty)

### Default Rooms

The application initializes with three default rooms:
- **general** - General discussion
- **random** - Random topics
- **tech** - Technology discussions

## Usage

### Joining a Chat Room

1. Open http://localhost:8081
2. Click on a room to join
3. Enter your username and display name
4. Start chatting!

### Creating a New Room

1. Click "New Room" button on the home page
2. Enter room name, description, and your username
3. The room will be created and available to all users

### Private Messaging

Private messaging is supported through the WebSocket API. Users can send direct messages to specific users by username.

### WebSocket Endpoints

**STOMP Destinations:**

- `/app/chat.sendMessage/{roomName}` - Send message to room
- `/app/chat.addUser/{roomName}` - Join room
- `/app/chat.private` - Send private message
- `/app/chat.typing/{roomName}` - Typing indicator

**Subscribe Destinations:**

- `/topic/{roomName}` - Room messages
- `/topic/{roomName}/typing` - Typing notifications
- `/user/queue/messages` - Private messages

### Message Types

- **CHAT** - Regular chat message
- **JOIN** - User joined notification
- **LEAVE** - User left notification
- **TYPING** - Typing indicator

## API Example

### JavaScript Client

```javascript
// Connect to WebSocket
const socket = new SockJS('/ws');
const stompClient = Stomp.over(socket);

stompClient.connect({}, function() {
    // Subscribe to room
    stompClient.subscribe('/topic/general', function(message) {
        const msg = JSON.parse(message.body);
        console.log('Received:', msg);
    });

    // Send message
    stompClient.send('/app/chat.sendMessage/general', {}, JSON.stringify({
        sender: 'username',
        content: 'Hello, World!',
        type: 'CHAT'
    }));
});
```

## Database Schema

### Users Table
- `id` - Primary key
- `username` - Unique username
- `display_name` - Display name
- `avatar_color` - Avatar color code
- `online` - Online status
- `created_at` - Registration timestamp
- `last_seen_at` - Last activity timestamp

### Chat Rooms Table
- `id` - Primary key
- `name` - Unique room name
- `description` - Room description
- `is_private` - Private room flag
- `created_at` - Creation timestamp
- `created_by` - Creator user ID

### Messages Table
- `id` - Primary key
- `room_id` - Room reference (nullable for private messages)
- `sender_id` - Sender user ID
- `recipient_id` - Recipient user ID (for private messages)
- `content` - Message content
- `type` - Message type (CHAT, JOIN, LEAVE, TYPING)
- `timestamp` - Message timestamp

## Configuration

Edit `src/main/resources/application.properties`:

```properties
# Server port
server.port=8081

# Database location
spring.datasource.url=jdbc:h2:file:./data/chatdb

# Enable/disable H2 console
spring.h2.console.enabled=true
```

## Features in Detail

### Real-Time Communication

Uses WebSocket protocol with STOMP messaging for bidirectional, real-time communication between clients and server.

### Message Persistence

All messages are persisted to the H2 database, allowing users to view message history when joining a room.

### User Presence

Tracks online users and updates the user list in real-time as users join and leave rooms.

### Typing Indicators

Shows when other users are typing in the current room with a timeout mechanism.

### Avatar Colors

Each user is assigned a unique color for their avatar, making it easy to distinguish between users.

## Building for Production

```bash
# Create executable JAR
mvn clean package

# Run the JAR
java -jar target/chat-application-1.0.0.jar
```

## Development

### Testing WebSocket Connection

You can test the WebSocket connection using browser developer tools:

```javascript
const socket = new SockJS('http://localhost:8081/ws');
const client = Stomp.over(socket);
client.connect({}, () => console.log('Connected!'));
```

### Adding New Features

1. **Add new message types** - Extend `Message.MessageType` enum
2. **Custom room features** - Modify `ChatRoom` entity and service
3. **User authentication** - Integrate Spring Security
4. **File sharing** - Add file upload endpoints

## Troubleshooting

**WebSocket connection fails:**
- Check firewall settings
- Ensure port 8081 is not blocked
- Verify SockJS fallback is working

**Messages not persisting:**
- Check H2 database connection
- Verify JPA configuration
- Check application logs for errors

## License

This project is open source and available for educational purposes.

## Author

Created as a demonstration of real-time web applications with Spring Boot and WebSocket technology.
