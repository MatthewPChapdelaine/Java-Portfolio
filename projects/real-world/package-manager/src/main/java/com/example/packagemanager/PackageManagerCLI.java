package com.example.packagemanager.cli;

import com.example.packagemanager.graph.GraphVisualizer;
import com.example.packagemanager.graph.LockFileGenerator;
import com.example.packagemanager.model.Dependency;
import com.example.packagemanager.model.DependencyGraph;
import com.example.packagemanager.model.Project;
import com.example.packagemanager.parser.JsonManifestParser;
import com.example.packagemanager.parser.PomParser;
import com.example.packagemanager.repository.MavenRepository;
import com.example.packagemanager.resolver.DependencyResolver;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import java.io.File;
import java.util.List;
import java.util.concurrent.Callable;

@Command(name = "pkgmgr", 
         mixinStandardHelpOptions = true,
         version = "Package Manager 1.0.0",
         description = "Maven-like dependency management tool")
public class PackageManagerCLI implements Callable<Integer> {

    @Option(names = {"-r", "--repository"}, description = "Repository path")
    private String repositoryPath;

    @Command(name = "install", description = "Install dependencies from manifest")
    public Integer install(
            @Parameters(index = "0", description = "Manifest file (pom.xml or manifest.json)")
            File manifestFile) throws Exception {
        
        System.out.println("📦 Package Manager - Install\n");
        
        if (!manifestFile.exists()) {
            System.err.println("Error: Manifest file not found: " + manifestFile);
            return 1;
        }

        MavenRepository repository = repositoryPath != null 
            ? new MavenRepository(repositoryPath)
            : new MavenRepository();

        Project project = parseManifest(manifestFile);
        System.out.println("Project: " + project.getCoordinates());
        System.out.println("Dependencies: " + project.getDependencies().size() + "\n");

        DependencyResolver resolver = new DependencyResolver(repository);
        List<Dependency> resolved = resolver.resolve(project);

        System.out.println("\n✅ Installation complete!");
        System.out.println("Total dependencies resolved: " + resolved.size());
        System.out.println("Repository: " + repository.getRepositoryPath());

        return 0;
    }

    @Command(name = "tree", description = "Display dependency tree")
    public Integer tree(
            @Parameters(index = "0", description = "Manifest file")
            File manifestFile) throws Exception {
        
        System.out.println("🌳 Package Manager - Dependency Tree\n");

        if (!manifestFile.exists()) {
            System.err.println("Error: Manifest file not found: " + manifestFile);
            return 1;
        }

        MavenRepository repository = repositoryPath != null 
            ? new MavenRepository(repositoryPath)
            : new MavenRepository();

        Project project = parseManifest(manifestFile);
        System.out.println("Project: " + project.getCoordinates() + "\n");

        DependencyResolver resolver = new DependencyResolver(repository);
        DependencyGraph graph = resolver.buildDependencyGraph(project);

        GraphVisualizer visualizer = new GraphVisualizer();
        visualizer.printTextTree(graph);

        return 0;
    }

    @Command(name = "graph", description = "Generate dependency graph visualization")
    public Integer graph(
            @Parameters(index = "0", description = "Manifest file")
            File manifestFile,
            @Option(names = {"-o", "--output"}, description = "Output file", defaultValue = "dependency-graph.png")
            File outputFile) throws Exception {
        
        System.out.println("📊 Package Manager - Dependency Graph\n");

        if (!manifestFile.exists()) {
            System.err.println("Error: Manifest file not found: " + manifestFile);
            return 1;
        }

        MavenRepository repository = repositoryPath != null 
            ? new MavenRepository(repositoryPath)
            : new MavenRepository();

        Project project = parseManifest(manifestFile);
        System.out.println("Project: " + project.getCoordinates());

        DependencyResolver resolver = new DependencyResolver(repository);
        DependencyGraph graph = resolver.buildDependencyGraph(project);

        GraphVisualizer visualizer = new GraphVisualizer();
        visualizer.visualize(graph, outputFile);

        return 0;
    }

    @Command(name = "lock", description = "Generate lock file")
    public Integer lock(
            @Parameters(index = "0", description = "Manifest file")
            File manifestFile,
            @Option(names = {"-o", "--output"}, description = "Lock file", defaultValue = "package-lock.json")
            File lockFile) throws Exception {
        
        System.out.println("🔒 Package Manager - Generate Lock File\n");

        if (!manifestFile.exists()) {
            System.err.println("Error: Manifest file not found: " + manifestFile);
            return 1;
        }

        MavenRepository repository = repositoryPath != null 
            ? new MavenRepository(repositoryPath)
            : new MavenRepository();

        Project project = parseManifest(manifestFile);
        System.out.println("Project: " + project.getCoordinates());

        DependencyResolver resolver = new DependencyResolver(repository);
        List<Dependency> resolved = resolver.resolve(project);

        LockFileGenerator generator = new LockFileGenerator();
        generator.generate(resolved, lockFile);

        return 0;
    }

    @Command(name = "info", description = "Display project information")
    public Integer info(
            @Parameters(index = "0", description = "Manifest file")
            File manifestFile) throws Exception {
        
        System.out.println("ℹ️  Package Manager - Project Info\n");

        if (!manifestFile.exists()) {
            System.err.println("Error: Manifest file not found: " + manifestFile);
            return 1;
        }

        Project project = parseManifest(manifestFile);

        System.out.println("Group ID:    " + project.getGroupId());
        System.out.println("Artifact ID: " + project.getArtifactId());
        System.out.println("Version:     " + project.getVersion());
        System.out.println("Packaging:   " + project.getPackaging());
        if (project.getName() != null) {
            System.out.println("Name:        " + project.getName());
        }
        if (project.getDescription() != null) {
            System.out.println("Description: " + project.getDescription());
        }
        System.out.println("\nDirect Dependencies: " + project.getDependencies().size());
        
        for (Dependency dep : project.getDependencies()) {
            System.out.println("  • " + dep.getCoordinates() + " [" + dep.getScope() + "]");
        }

        return 0;
    }

    private Project parseManifest(File file) throws Exception {
        if (file.getName().endsWith(".xml")) {
            PomParser parser = new PomParser();
            return parser.parse(file);
        } else if (file.getName().endsWith(".json")) {
            JsonManifestParser parser = new JsonManifestParser();
            return parser.parse(file);
        } else {
            throw new IllegalArgumentException("Unsupported manifest format: " + file.getName());
        }
    }

    @Override
    public Integer call() {
        System.out.println("Use --help to see available commands");
        return 0;
    }

    public static void main(String[] args) {
        int exitCode = new CommandLine(new PackageManagerCLI()).execute(args);
        System.exit(exitCode);
    }
}
