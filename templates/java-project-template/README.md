# Java Project Template

A basic Java project structure with Maven.

## Structure

```
java-project-template/
├── src/
│   ├── main/java/com/example/
│   │   └── Main.java        # Main application
│   └── test/java/com/example/
│       └── MainTest.java    # Unit tests
├── pom.xml                  # Maven configuration
└── README.md               # This file
```

## Setup

```bash
# Install dependencies
mvn install
```

## Usage

```bash
# Compile
mvn compile

# Run
mvn exec:java -Dexec.mainClass="com.example.Main"
```

## Testing

```bash
mvn test
```

## Development

1. Install dependencies with `mvn install`
2. Make your changes in `src/main/java/`
3. Write tests in `src/test/java/`
4. Run tests with `mvn test`
5. Build with `mvn package`
