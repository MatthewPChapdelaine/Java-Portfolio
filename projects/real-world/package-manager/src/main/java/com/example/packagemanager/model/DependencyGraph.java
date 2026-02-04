package com.example.packagemanager.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DependencyGraph {
    private Map<String, Node> nodes;
    private Map<String, Set<String>> edges;

    public DependencyGraph() {
        this.nodes = new HashMap<>();
        this.edges = new HashMap<>();
    }

    public void addNode(Dependency dependency, int depth) {
        String key = dependency.getKey();
        if (!nodes.containsKey(key)) {
            nodes.put(key, new Node(dependency, depth));
        }
    }

    public void addEdge(String from, String to) {
        edges.computeIfAbsent(from, k -> new HashSet<>()).add(to);
    }

    public Node getNode(String key) {
        return nodes.get(key);
    }

    public Set<String> getChildren(String key) {
        return edges.getOrDefault(key, Collections.emptySet());
    }

    public List<Dependency> getAllDependencies() {
        List<Dependency> result = new ArrayList<>();
        for (Node node : nodes.values()) {
            result.add(node.getDependency());
        }
        return result;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Node {
        private Dependency dependency;
        private int depth;
    }
}
