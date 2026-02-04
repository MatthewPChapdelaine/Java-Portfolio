package com.example.packagemanager.resolver;

import com.example.packagemanager.model.Dependency;
import com.example.packagemanager.model.DependencyGraph;
import com.example.packagemanager.model.Project;
import com.example.packagemanager.repository.MavenRepository;

import java.util.*;

public class DependencyResolver {
    private final MavenRepository repository;
    private final Map<String, Dependency> resolvedDependencies;
    private final Set<String> visited;

    public DependencyResolver(MavenRepository repository) {
        this.repository = repository;
        this.resolvedDependencies = new HashMap<>();
        this.visited = new HashSet<>();
    }

    public List<Dependency> resolve(Project project) {
        resolvedDependencies.clear();
        visited.clear();

        System.out.println("Resolving dependencies for " + project.getCoordinates());
        
        for (Dependency dependency : project.getDependencies()) {
            resolveDependency(dependency, 0);
        }

        List<Dependency> result = new ArrayList<>(resolvedDependencies.values());
        System.out.println("Resolved " + result.size() + " dependencies");
        return result;
    }

    public DependencyGraph buildDependencyGraph(Project project) {
        DependencyGraph graph = new DependencyGraph();
        visited.clear();

        for (Dependency dependency : project.getDependencies()) {
            buildGraph(dependency, null, graph, 0);
        }

        return graph;
    }

    private void resolveDependency(Dependency dependency, int depth) {
        String key = dependency.getKey();

        if (visited.contains(key)) {
            return;
        }

        visited.add(key);
        printIndent(depth);
        System.out.println("├─ Resolving " + dependency.getCoordinates());

        Optional<Project> resolvedProject = repository.resolve(dependency);
        
        if (resolvedProject.isPresent()) {
            handleVersionConflict(dependency);
            resolvedDependencies.put(key, dependency);

            for (Dependency transitiveDep : resolvedProject.get().getDependencies()) {
                if (!isExcluded(transitiveDep, dependency)) {
                    resolveDependency(transitiveDep, depth + 1);
                }
            }
        } else {
            printIndent(depth);
            System.out.println("   ⚠ Could not resolve " + dependency.getCoordinates());
        }
    }

    private void buildGraph(Dependency dependency, String parentKey, 
                           DependencyGraph graph, int depth) {
        String key = dependency.getKey();
        graph.addNode(dependency, depth);

        if (parentKey != null) {
            graph.addEdge(parentKey, key);
        }

        if (visited.contains(key)) {
            return;
        }

        visited.add(key);

        Optional<Project> resolvedProject = repository.resolve(dependency);
        if (resolvedProject.isPresent()) {
            for (Dependency transitiveDep : resolvedProject.get().getDependencies()) {
                if (!isExcluded(transitiveDep, dependency)) {
                    buildGraph(transitiveDep, key, graph, depth + 1);
                }
            }
        }
    }

    private void handleVersionConflict(Dependency newDependency) {
        String key = newDependency.getKey();
        if (resolvedDependencies.containsKey(key)) {
            Dependency existing = resolvedDependencies.get(key);
            String selectedVersion = selectVersion(existing.getVersion(), newDependency.getVersion());
            
            if (!selectedVersion.equals(existing.getVersion())) {
                System.out.println("   ⚠ Version conflict: " + key);
                System.out.println("     Choosing " + selectedVersion + " over " + existing.getVersion());
                newDependency.setVersion(selectedVersion);
                resolvedDependencies.put(key, newDependency);
            }
        }
    }

    private String selectVersion(String v1, String v2) {
        String[] parts1 = v1.split("\\.");
        String[] parts2 = v2.split("\\.");
        
        for (int i = 0; i < Math.min(parts1.length, parts2.length); i++) {
            try {
                int num1 = Integer.parseInt(parts1[i]);
                int num2 = Integer.parseInt(parts2[i]);
                if (num1 > num2) return v1;
                if (num2 > num1) return v2;
            } catch (NumberFormatException e) {
                return v1.compareTo(v2) > 0 ? v1 : v2;
            }
        }
        
        return parts1.length >= parts2.length ? v1 : v2;
    }

    private boolean isExcluded(Dependency dependency, Dependency parent) {
        if (parent.getExclusions() == null) return false;
        
        for (Dependency.Exclusion exclusion : parent.getExclusions()) {
            if (exclusion.getGroupId().equals(dependency.getGroupId()) &&
                exclusion.getArtifactId().equals(dependency.getArtifactId())) {
                return true;
            }
        }
        return false;
    }

    private void printIndent(int depth) {
        for (int i = 0; i < depth; i++) {
            System.out.print("   ");
        }
    }
}
