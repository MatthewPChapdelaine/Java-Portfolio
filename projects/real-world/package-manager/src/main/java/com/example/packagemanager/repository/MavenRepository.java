package com.example.packagemanager.repository;

import com.example.packagemanager.model.Dependency;
import com.example.packagemanager.model.Project;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public class MavenRepository {
    private final Path repositoryPath;
    private final Map<String, Project> cachedProjects;

    public MavenRepository(String repositoryPath) {
        this.repositoryPath = Paths.get(repositoryPath);
        this.cachedProjects = new HashMap<>();
        ensureRepositoryExists();
    }

    public MavenRepository() {
        String userHome = System.getProperty("user.home");
        this.repositoryPath = Paths.get(userHome, ".m2", "pkg-manager-repo");
        this.cachedProjects = new HashMap<>();
        ensureRepositoryExists();
    }

    private void ensureRepositoryExists() {
        try {
            Files.createDirectories(repositoryPath);
        } catch (IOException e) {
            throw new RuntimeException("Failed to create repository directory", e);
        }
    }

    public Optional<Project> resolve(Dependency dependency) {
        String key = dependency.getKey();
        
        if (cachedProjects.containsKey(key)) {
            return Optional.of(cachedProjects.get(key));
        }

        Path artifactPath = getArtifactPath(dependency);
        if (Files.exists(artifactPath)) {
            Project project = new Project(
                dependency.getGroupId(),
                dependency.getArtifactId(),
                dependency.getVersion()
            );
            
            project.setDependencies(resolveDependencies(dependency));
            cachedProjects.put(key, project);
            return Optional.of(project);
        }

        return simulateRemoteResolve(dependency);
    }

    private Optional<Project> simulateRemoteResolve(Dependency dependency) {
        Project project = new Project(
            dependency.getGroupId(),
            dependency.getArtifactId(),
            dependency.getVersion()
        );

        List<Dependency> transitiveDeps = simulateTransitiveDependencies(dependency);
        project.setDependencies(transitiveDeps);

        cachedProjects.put(dependency.getKey(), project);
        return Optional.of(project);
    }

    private List<Dependency> simulateTransitiveDependencies(Dependency dependency) {
        List<Dependency> deps = new ArrayList<>();
        
        if (dependency.getArtifactId().contains("spring-boot-starter-web")) {
            deps.add(new Dependency("org.springframework", "spring-web", "6.1.0"));
            deps.add(new Dependency("org.springframework", "spring-webmvc", "6.1.0"));
            deps.add(new Dependency("com.fasterxml.jackson.core", "jackson-databind", "2.15.0"));
        } else if (dependency.getArtifactId().contains("junit")) {
            deps.add(new Dependency("org.junit.jupiter", "junit-jupiter-api", "5.10.0"));
            deps.add(new Dependency("org.junit.jupiter", "junit-jupiter-engine", "5.10.0"));
        } else if (dependency.getArtifactId().contains("slf4j")) {
            deps.add(new Dependency("org.slf4j", "slf4j-api", "2.0.9"));
        }

        return deps;
    }

    private List<Dependency> resolveDependencies(Dependency dependency) {
        return new ArrayList<>();
    }

    public void install(Dependency dependency, byte[] artifactData) throws IOException {
        Path artifactPath = getArtifactPath(dependency);
        Files.createDirectories(artifactPath.getParent());
        Files.write(artifactPath, artifactData);
    }

    public boolean exists(Dependency dependency) {
        return Files.exists(getArtifactPath(dependency));
    }

    private Path getArtifactPath(Dependency dependency) {
        String groupPath = dependency.getGroupId().replace('.', '/');
        return repositoryPath.resolve(groupPath)
                .resolve(dependency.getArtifactId())
                .resolve(dependency.getVersion())
                .resolve(dependency.getArtifactId() + "-" + dependency.getVersion() + ".jar");
    }

    public Path getRepositoryPath() {
        return repositoryPath;
    }
}
