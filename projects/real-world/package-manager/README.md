# Package Manager

A Maven-like dependency management tool with advanced features including dependency resolution, lock file generation, and graph visualization.

## Features

- **POM.xml Parser**: Parse standard Maven POM files
- **JSON Manifest**: Alternative JSON-based manifest format
- **Dependency Resolution**: Smart dependency resolution with conflict handling
- **Transitive Dependencies**: Automatic resolution of transitive dependencies
- **Lock File Generation**: Create reproducible dependency lock files
- **Dependency Graph**: Visual dependency graph generation (PNG)
- **Dependency Tree**: Text-based dependency tree display
- **Local Repository**: Simulated Maven .m2 repository
- **CLI Interface**: Full-featured command-line interface with picocli
- **Version Conflict Resolution**: Automatic version conflict handling

## Technology Stack

- **Java 17** - Programming language
- **Picocli 4.7.5** - CLI framework
- **Gson 2.10.1** - JSON processing
- **dom4j 2.1.4** - XML parsing
- **Graphviz-Java 0.18.1** - Graph visualization
- **Lombok** - Reduce boilerplate
- **Maven** - Build tool

## Project Structure

```
package-manager/
├── src/main/java/com/example/packagemanager/
│   ├── PackageManagerCLI.java
│   ├── cli/
│   ├── model/
│   │   ├── Dependency.java
│   │   ├── Project.java
│   │   ├── LockFile.java
│   │   └── DependencyGraph.java
│   ├── parser/
│   │   ├── PomParser.java
│   │   └── JsonManifestParser.java
│   ├── repository/
│   │   └── MavenRepository.java
│   ├── resolver/
│   │   └── DependencyResolver.java
│   └── graph/
│       ├── GraphVisualizer.java
│       └── LockFileGenerator.java
├── examples/
│   ├── sample-pom.xml
│   └── sample-manifest.json
└── pom.xml
```

## Getting Started

### Prerequisites

- Java 17 or higher
- Maven 3.6+

### Build

```bash
cd package-manager
mvn clean package
```

This creates two JARs:
- `target/package-manager-1.0.0.jar` - Regular JAR
- `target/package-manager-1.0.0-jar-with-dependencies.jar` - Standalone executable JAR

### Installation (Optional)

Create an alias for easy access:

```bash
alias pkgmgr='java -jar /path/to/package-manager-1.0.0-jar-with-dependencies.jar'
```

## Usage

### Basic Commands

**Display help:**
```bash
java -jar target/package-manager-1.0.0-jar-with-dependencies.jar --help
```

**Install dependencies:**
```bash
java -jar target/package-manager-1.0.0-jar-with-dependencies.jar install examples/sample-pom.xml
```

**Display dependency tree:**
```bash
java -jar target/package-manager-1.0.0-jar-with-dependencies.jar tree examples/sample-pom.xml
```

**Generate dependency graph:**
```bash
java -jar target/package-manager-1.0.0-jar-with-dependencies.jar graph examples/sample-pom.xml -o deps.png
```

**Generate lock file:**
```bash
java -jar target/package-manager-1.0.0-jar-with-dependencies.jar lock examples/sample-pom.xml -o package-lock.json
```

**Show project info:**
```bash
java -jar target/package-manager-1.0.0-jar-with-dependencies.jar info examples/sample-pom.xml
```

### Command Reference

#### `install`
Installs all dependencies from a manifest file.

```bash
pkgmgr install <manifest-file> [options]

Options:
  -r, --repository <path>   Custom repository path
```

#### `tree`
Displays the dependency tree in text format.

```bash
pkgmgr tree <manifest-file>
```

#### `graph`
Generates a visual dependency graph as a PNG image.

```bash
pkgmgr graph <manifest-file> [options]

Options:
  -o, --output <file>   Output file (default: dependency-graph.png)
```

#### `lock`
Generates a lock file for reproducible builds.

```bash
pkgmgr lock <manifest-file> [options]

Options:
  -o, --output <file>   Lock file path (default: package-lock.json)
```

#### `info`
Displays project information and direct dependencies.

```bash
pkgmgr info <manifest-file>
```

## Manifest Formats

### POM.xml Format (Maven)

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0">
    <modelVersion>4.0.0</modelVersion>
    
    <groupId>com.example</groupId>
    <artifactId>my-app</artifactId>
    <version>1.0.0</version>
    <packaging>jar</packaging>
    
    <dependencies>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
            <version>3.2.0</version>
        </dependency>
    </dependencies>
</project>
```

### JSON Manifest Format

```json
{
  "groupId": "com.example",
  "artifactId": "my-app",
  "version": "1.0.0",
  "packaging": "jar",
  "dependencies": [
    {
      "groupId": "org.springframework.boot",
      "artifactId": "spring-boot-starter-web",
      "version": "3.2.0",
      "scope": "compile"
    }
  ]
}
```

## Features in Detail

### Dependency Resolution

The resolver handles:
- Transitive dependency resolution
- Version conflict detection and resolution
- Dependency exclusions
- Optional dependencies
- Scope management (compile, test, provided, runtime)

**Resolution Strategy:**
1. Parse manifest file
2. Resolve direct dependencies
3. Recursively resolve transitive dependencies
4. Handle version conflicts (highest version wins)
5. Apply exclusions
6. Build final dependency list

### Lock File

Lock files ensure reproducible builds by recording exact resolved versions:

```json
{
  "version": "1.0",
  "dependencies": {
    "org.springframework:spring-web": {
      "groupId": "org.springframework",
      "artifactId": "spring-web",
      "version": "6.1.0",
      "resolved": "org.springframework:spring-web:6.1.0",
      "checksum": "a1b2c3d4e5f6g7h8"
    }
  }
}
```

### Dependency Graph

Generates visual graphs showing:
- Direct dependencies
- Transitive dependencies
- Dependency relationships
- Depth levels (color-coded)

The graph is saved as a PNG file that can be viewed in any image viewer.

### Repository Structure

Dependencies are stored in a Maven-compatible structure:

```
~/.m2/pkg-manager-repo/
└── com/
    └── example/
        └── mylib/
            └── 1.0.0/
                └── mylib-1.0.0.jar
```

## Examples

### Example 1: Install Dependencies

```bash
# Create a simple POM file
cat > pom.xml << 'EOF'
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0">
    <modelVersion>4.0.0</modelVersion>
    <groupId>com.example</groupId>
    <artifactId>test-app</artifactId>
    <version>1.0.0</version>
    <dependencies>
        <dependency>
            <groupId>org.junit.jupiter</groupId>
            <artifactId>junit-jupiter</artifactId>
            <version>5.10.0</version>
        </dependency>
    </dependencies>
</project>
EOF

# Install dependencies
java -jar package-manager.jar install pom.xml
```

### Example 2: View Dependency Tree

```bash
java -jar package-manager.jar tree examples/sample-pom.xml
```

Output:
```
=== Dependency Tree ===

├─ spring-boot-starter-web:3.2.0 [compile]
   ├─ spring-web:6.1.0 [compile]
   ├─ spring-webmvc:6.1.0 [compile]
   └─ jackson-databind:2.15.0 [compile]
├─ junit-jupiter:5.10.0 [test]
   ├─ junit-jupiter-api:5.10.0 [test]
   └─ junit-jupiter-engine:5.10.0 [test]
```

### Example 3: Generate Graph

```bash
java -jar package-manager.jar graph examples/sample-pom.xml -o my-deps.png
```

### Example 4: Create Lock File

```bash
java -jar package-manager.jar lock examples/sample-pom.xml
cat package-lock.json
```

## Development

### Running Tests

```bash
mvn test
```

### Building from Source

```bash
mvn clean package
```

### Creating Standalone JAR

The standalone JAR includes all dependencies:

```bash
mvn clean package
# Use: target/package-manager-1.0.0-jar-with-dependencies.jar
```

## Advanced Usage

### Custom Repository Path

```bash
java -jar package-manager.jar install pom.xml -r /custom/repo/path
```

### Programmatic API

```java
MavenRepository repo = new MavenRepository();
PomParser parser = new PomParser();
Project project = parser.parse(new File("pom.xml"));

DependencyResolver resolver = new DependencyResolver(repo);
List<Dependency> resolved = resolver.resolve(project);

LockFileGenerator generator = new LockFileGenerator();
generator.generate(resolved, new File("package-lock.json"));
```

## Troubleshooting

**Graphviz not found:**
- Install Graphviz: `sudo apt-get install graphviz` (Linux)
- Or download from: https://graphviz.org/download/

**Memory issues with large graphs:**
```bash
java -Xmx2g -jar package-manager.jar graph pom.xml
```

**Version conflicts:**
The tool automatically resolves conflicts by choosing the highest version. Check the output for warnings.

## Limitations

- Simulated repository (not connected to Maven Central)
- Sample transitive dependencies (not complete)
- No network download functionality
- Simplified conflict resolution

These limitations make it perfect for learning and demonstration purposes.

## Future Enhancements

- Real Maven Central integration
- Parallel dependency resolution
- Dependency caching
- Plugin system
- Build lifecycle integration
- Multi-module project support

## License

This project is open source and available for educational purposes.

## Author

Created as a demonstration of advanced Java programming concepts including dependency management, graph algorithms, and CLI design.
