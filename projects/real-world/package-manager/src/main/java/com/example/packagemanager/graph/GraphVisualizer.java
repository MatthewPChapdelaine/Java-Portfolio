package com.example.packagemanager.graph;

import com.example.packagemanager.model.Dependency;
import com.example.packagemanager.model.DependencyGraph;
import guru.nidi.graphviz.attribute.Color;
import guru.nidi.graphviz.attribute.Label;
import guru.nidi.graphviz.attribute.Shape;
import guru.nidi.graphviz.attribute.Style;
import guru.nidi.graphviz.engine.Format;
import guru.nidi.graphviz.engine.Graphviz;
import guru.nidi.graphviz.model.MutableGraph;
import guru.nidi.graphviz.model.MutableNode;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static guru.nidi.graphviz.model.Factory.*;

public class GraphVisualizer {

    public void visualize(DependencyGraph graph, File outputFile) throws IOException {
        MutableGraph g = mutGraph("dependency_graph").setDirected(true);
        Map<String, MutableNode> nodeMap = new HashMap<>();

        for (Map.Entry<String, DependencyGraph.Node> entry : graph.getNodes().entrySet()) {
            String key = entry.getKey();
            DependencyGraph.Node node = entry.getValue();
            Dependency dep = node.getDependency();

            MutableNode graphNode = mutNode(key)
                    .add(Label.of(dep.getArtifactId() + "\n" + dep.getVersion()))
                    .add(Shape.BOX)
                    .add(Style.FILLED)
                    .add(getColorForDepth(node.getDepth()));

            nodeMap.put(key, graphNode);
            g.add(graphNode);
        }

        for (Map.Entry<String, java.util.Set<String>> entry : graph.getEdges().entrySet()) {
            String fromKey = entry.getKey();
            MutableNode fromNode = nodeMap.get(fromKey);

            for (String toKey : entry.getValue()) {
                MutableNode toNode = nodeMap.get(toKey);
                if (toNode != null) {
                    fromNode.addLink(toNode);
                }
            }
        }

        Graphviz.fromGraph(g)
                .width(1200)
                .render(Format.PNG)
                .toFile(outputFile);

        System.out.println("Dependency graph saved to: " + outputFile.getAbsolutePath());
    }

    public void printTextTree(DependencyGraph graph) {
        System.out.println("\n=== Dependency Tree ===\n");
        
        Map<String, Boolean> printed = new HashMap<>();
        for (Map.Entry<String, DependencyGraph.Node> entry : graph.getNodes().entrySet()) {
            if (entry.getValue().getDepth() == 0) {
                printNode(entry.getKey(), graph, printed, 0, true);
            }
        }
    }

    private void printNode(String key, DependencyGraph graph, 
                          Map<String, Boolean> printed, int depth, boolean isLast) {
        if (printed.containsKey(key) && depth > 0) {
            printIndent(depth, isLast);
            System.out.println("↻ " + key + " (circular/duplicate)");
            return;
        }

        printed.put(key, true);
        DependencyGraph.Node node = graph.getNode(key);
        
        if (node == null) return;

        printIndent(depth, isLast);
        Dependency dep = node.getDependency();
        System.out.println("├─ " + dep.getArtifactId() + ":" + dep.getVersion() + 
                          " [" + dep.getScope() + "]");

        java.util.Set<String> children = graph.getChildren(key);
        if (children != null && !children.isEmpty()) {
            java.util.List<String> childList = new java.util.ArrayList<>(children);
            for (int i = 0; i < childList.size(); i++) {
                printNode(childList.get(i), graph, printed, depth + 1, i == childList.size() - 1);
            }
        }
    }

    private void printIndent(int depth, boolean isLast) {
        for (int i = 0; i < depth; i++) {
            System.out.print("   ");
        }
    }

    private Color getColorForDepth(int depth) {
        switch (depth % 5) {
            case 0: return Color.LIGHTBLUE;
            case 1: return Color.LIGHTGREEN;
            case 2: return Color.LIGHTYELLOW;
            case 3: return Color.LIGHTPINK;
            case 4: return Color.LIGHTCYAN;
            default: return Color.WHITE;
        }
    }
}
