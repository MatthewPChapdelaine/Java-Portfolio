import java.util.*;

/**
 * GraphAlgorithms - Implementation of common graph algorithms.
 * 
 * Features:
 * - Dijkstra's shortest path algorithm
 * - Breadth-First Search (BFS)
 * - Depth-First Search (DFS)
 * - Topological sort
 * - Cycle detection (directed and undirected)
 * - Connected components
 * - Comprehensive test cases
 * 
 * Compile: javac GraphAlgorithms.java
 * Run: java GraphAlgorithms
 * 
 * @author Advanced Java Learning
 * @version 1.0
 */
public class GraphAlgorithms {
    
    /**
     * Edge representation with weight.
     */
    static class Edge {
        int to;
        int weight;
        
        public Edge(int to, int weight) {
            this.to = to;
            this.weight = weight;
        }
        
        @Override
        public String toString() {
            return "-> " + to + " (w=" + weight + ")";
        }
    }
    
    /**
     * Graph representation using adjacency list.
     */
    static class Graph {
        private final int vertices;
        private final List<List<Edge>> adjacencyList;
        private final boolean directed;
        
        /**
         * Constructs a graph with given number of vertices.
         * 
         * @param vertices Number of vertices
         * @param directed Whether the graph is directed
         */
        public Graph(int vertices, boolean directed) {
            this.vertices = vertices;
            this.directed = directed;
            this.adjacencyList = new ArrayList<>(vertices);
            
            for (int i = 0; i < vertices; i++) {
                adjacencyList.add(new ArrayList<>());
            }
        }
        
        /**
         * Adds an edge to the graph.
         * 
         * @param from Source vertex
         * @param to Destination vertex
         * @param weight Edge weight
         */
        public void addEdge(int from, int to, int weight) {
            adjacencyList.get(from).add(new Edge(to, weight));
            if (!directed) {
                adjacencyList.get(to).add(new Edge(from, weight));
            }
        }
        
        public void addEdge(int from, int to) {
            addEdge(from, to, 1);
        }
        
        public int getVertices() {
            return vertices;
        }
        
        public List<Edge> getNeighbors(int vertex) {
            return adjacencyList.get(vertex);
        }
        
        public boolean isDirected() {
            return directed;
        }
        
        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("Graph (").append(directed ? "directed" : "undirected")
              .append(", ").append(vertices).append(" vertices):\n");
            
            for (int i = 0; i < vertices; i++) {
                sb.append("  ").append(i).append(": ");
                for (Edge edge : adjacencyList.get(i)) {
                    sb.append(edge).append(" ");
                }
                sb.append("\n");
            }
            
            return sb.toString();
        }
    }
    
    // ============================================================
    // DIJKSTRA'S ALGORITHM - Shortest Path
    // ============================================================
    
    /**
     * Result of Dijkstra's algorithm.
     */
    static class DijkstraResult {
        int[] distances;
        int[] previous;
        
        public DijkstraResult(int[] distances, int[] previous) {
            this.distances = distances;
            this.previous = previous;
        }
        
        /**
         * Gets the shortest path to a destination.
         * 
         * @param dest Destination vertex
         * @return List of vertices in path
         */
        public List<Integer> getPath(int dest) {
            List<Integer> path = new ArrayList<>();
            if (distances[dest] == Integer.MAX_VALUE) {
                return path; // No path exists
            }
            
            for (int at = dest; at != -1; at = previous[at]) {
                path.add(at);
            }
            Collections.reverse(path);
            return path;
        }
    }
    
    /**
     * Implements Dijkstra's shortest path algorithm.
     * 
     * @param graph The graph
     * @param source Source vertex
     * @return DijkstraResult containing distances and paths
     */
    public static DijkstraResult dijkstra(Graph graph, int source) {
        int n = graph.getVertices();
        int[] dist = new int[n];
        int[] prev = new int[n];
        boolean[] visited = new boolean[n];
        
        Arrays.fill(dist, Integer.MAX_VALUE);
        Arrays.fill(prev, -1);
        dist[source] = 0;
        
        PriorityQueue<int[]> pq = new PriorityQueue<>(Comparator.comparingInt(a -> a[1]));
        pq.offer(new int[]{source, 0});
        
        while (!pq.isEmpty()) {
            int[] current = pq.poll();
            int u = current[0];
            
            if (visited[u]) continue;
            visited[u] = true;
            
            for (Edge edge : graph.getNeighbors(u)) {
                int v = edge.to;
                int weight = edge.weight;
                
                if (!visited[v] && dist[u] != Integer.MAX_VALUE && 
                    dist[u] + weight < dist[v]) {
                    dist[v] = dist[u] + weight;
                    prev[v] = u;
                    pq.offer(new int[]{v, dist[v]});
                }
            }
        }
        
        return new DijkstraResult(dist, prev);
    }
    
    // ============================================================
    // BREADTH-FIRST SEARCH (BFS)
    // ============================================================
    
    /**
     * Performs BFS traversal starting from given vertex.
     * 
     * @param graph The graph
     * @param start Starting vertex
     * @return List of vertices in BFS order
     */
    public static List<Integer> bfs(Graph graph, int start) {
        List<Integer> result = new ArrayList<>();
        boolean[] visited = new boolean[graph.getVertices()];
        Queue<Integer> queue = new LinkedList<>();
        
        visited[start] = true;
        queue.offer(start);
        
        while (!queue.isEmpty()) {
            int vertex = queue.poll();
            result.add(vertex);
            
            for (Edge edge : graph.getNeighbors(vertex)) {
                if (!visited[edge.to]) {
                    visited[edge.to] = true;
                    queue.offer(edge.to);
                }
            }
        }
        
        return result;
    }
    
    /**
     * Finds shortest path in unweighted graph using BFS.
     * 
     * @param graph The graph
     * @param start Start vertex
     * @param end End vertex
     * @return List of vertices in path, or empty if no path exists
     */
    public static List<Integer> bfsShortestPath(Graph graph, int start, int end) {
        int n = graph.getVertices();
        boolean[] visited = new boolean[n];
        int[] parent = new int[n];
        Arrays.fill(parent, -1);
        
        Queue<Integer> queue = new LinkedList<>();
        visited[start] = true;
        queue.offer(start);
        
        while (!queue.isEmpty()) {
            int vertex = queue.poll();
            
            if (vertex == end) {
                // Reconstruct path
                List<Integer> path = new ArrayList<>();
                for (int at = end; at != -1; at = parent[at]) {
                    path.add(at);
                }
                Collections.reverse(path);
                return path;
            }
            
            for (Edge edge : graph.getNeighbors(vertex)) {
                if (!visited[edge.to]) {
                    visited[edge.to] = true;
                    parent[edge.to] = vertex;
                    queue.offer(edge.to);
                }
            }
        }
        
        return new ArrayList<>(); // No path found
    }
    
    // ============================================================
    // DEPTH-FIRST SEARCH (DFS)
    // ============================================================
    
    /**
     * Performs DFS traversal starting from given vertex.
     * 
     * @param graph The graph
     * @param start Starting vertex
     * @return List of vertices in DFS order
     */
    public static List<Integer> dfs(Graph graph, int start) {
        List<Integer> result = new ArrayList<>();
        boolean[] visited = new boolean[graph.getVertices()];
        dfsHelper(graph, start, visited, result);
        return result;
    }
    
    private static void dfsHelper(Graph graph, int vertex, boolean[] visited, List<Integer> result) {
        visited[vertex] = true;
        result.add(vertex);
        
        for (Edge edge : graph.getNeighbors(vertex)) {
            if (!visited[edge.to]) {
                dfsHelper(graph, edge.to, visited, result);
            }
        }
    }
    
    // ============================================================
    // TOPOLOGICAL SORT
    // ============================================================
    
    /**
     * Performs topological sort on a DAG using DFS.
     * 
     * @param graph The graph (must be directed acyclic)
     * @return List of vertices in topological order, or null if cycle detected
     */
    public static List<Integer> topologicalSort(Graph graph) {
        if (!graph.isDirected()) {
            throw new IllegalArgumentException("Graph must be directed");
        }
        
        int n = graph.getVertices();
        boolean[] visited = new boolean[n];
        boolean[] recursionStack = new boolean[n];
        Stack<Integer> stack = new Stack<>();
        
        for (int i = 0; i < n; i++) {
            if (!visited[i]) {
                if (topologicalSortHelper(graph, i, visited, recursionStack, stack)) {
                    return null; // Cycle detected
                }
            }
        }
        
        List<Integer> result = new ArrayList<>();
        while (!stack.isEmpty()) {
            result.add(stack.pop());
        }
        
        return result;
    }
    
    private static boolean topologicalSortHelper(Graph graph, int vertex, 
                                                 boolean[] visited, boolean[] recursionStack, 
                                                 Stack<Integer> stack) {
        visited[vertex] = true;
        recursionStack[vertex] = true;
        
        for (Edge edge : graph.getNeighbors(vertex)) {
            if (!visited[edge.to]) {
                if (topologicalSortHelper(graph, edge.to, visited, recursionStack, stack)) {
                    return true; // Cycle detected
                }
            } else if (recursionStack[edge.to]) {
                return true; // Back edge found - cycle detected
            }
        }
        
        recursionStack[vertex] = false;
        stack.push(vertex);
        return false;
    }
    
    // ============================================================
    // CYCLE DETECTION
    // ============================================================
    
    /**
     * Detects if graph contains a cycle (directed graph).
     * 
     * @param graph The directed graph
     * @return true if cycle exists
     */
    public static boolean hasCycleDirected(Graph graph) {
        if (!graph.isDirected()) {
            throw new IllegalArgumentException("Use hasCycleUndirected for undirected graphs");
        }
        
        int n = graph.getVertices();
        boolean[] visited = new boolean[n];
        boolean[] recursionStack = new boolean[n];
        
        for (int i = 0; i < n; i++) {
            if (!visited[i]) {
                if (hasCycleDirectedHelper(graph, i, visited, recursionStack)) {
                    return true;
                }
            }
        }
        
        return false;
    }
    
    private static boolean hasCycleDirectedHelper(Graph graph, int vertex, 
                                                   boolean[] visited, boolean[] recursionStack) {
        visited[vertex] = true;
        recursionStack[vertex] = true;
        
        for (Edge edge : graph.getNeighbors(vertex)) {
            if (!visited[edge.to]) {
                if (hasCycleDirectedHelper(graph, edge.to, visited, recursionStack)) {
                    return true;
                }
            } else if (recursionStack[edge.to]) {
                return true;
            }
        }
        
        recursionStack[vertex] = false;
        return false;
    }
    
    /**
     * Detects if graph contains a cycle (undirected graph).
     * 
     * @param graph The undirected graph
     * @return true if cycle exists
     */
    public static boolean hasCycleUndirected(Graph graph) {
        if (graph.isDirected()) {
            throw new IllegalArgumentException("Use hasCycleDirected for directed graphs");
        }
        
        int n = graph.getVertices();
        boolean[] visited = new boolean[n];
        
        for (int i = 0; i < n; i++) {
            if (!visited[i]) {
                if (hasCycleUndirectedHelper(graph, i, -1, visited)) {
                    return true;
                }
            }
        }
        
        return false;
    }
    
    private static boolean hasCycleUndirectedHelper(Graph graph, int vertex, 
                                                     int parent, boolean[] visited) {
        visited[vertex] = true;
        
        for (Edge edge : graph.getNeighbors(vertex)) {
            if (!visited[edge.to]) {
                if (hasCycleUndirectedHelper(graph, edge.to, vertex, visited)) {
                    return true;
                }
            } else if (edge.to != parent) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * Finds connected components in an undirected graph.
     * 
     * @param graph The undirected graph
     * @return List of connected components
     */
    public static List<List<Integer>> connectedComponents(Graph graph) {
        if (graph.isDirected()) {
            throw new IllegalArgumentException("Graph must be undirected");
        }
        
        List<List<Integer>> components = new ArrayList<>();
        boolean[] visited = new boolean[graph.getVertices()];
        
        for (int i = 0; i < graph.getVertices(); i++) {
            if (!visited[i]) {
                List<Integer> component = new ArrayList<>();
                dfsHelper(graph, i, visited, component);
                components.add(component);
            }
        }
        
        return components;
    }
    
    // ============================================================
    // TEST CASES
    // ============================================================
    
    private static void testDijkstra() {
        System.out.println("\n=== DIJKSTRA'S ALGORITHM TEST ===");
        Graph graph = new Graph(5, true);
        graph.addEdge(0, 1, 4);
        graph.addEdge(0, 2, 1);
        graph.addEdge(2, 1, 2);
        graph.addEdge(1, 3, 1);
        graph.addEdge(2, 3, 5);
        graph.addEdge(3, 4, 3);
        
        System.out.println(graph);
        
        DijkstraResult result = dijkstra(graph, 0);
        System.out.println("Shortest distances from vertex 0:");
        for (int i = 0; i < result.distances.length; i++) {
            System.out.printf("  To %d: %d, Path: %s\n", 
                i, result.distances[i], result.getPath(i));
        }
    }
    
    private static void testBFS() {
        System.out.println("\n=== BFS TEST ===");
        Graph graph = new Graph(7, false);
        graph.addEdge(0, 1);
        graph.addEdge(0, 2);
        graph.addEdge(1, 3);
        graph.addEdge(1, 4);
        graph.addEdge(2, 5);
        graph.addEdge(2, 6);
        
        List<Integer> bfsOrder = bfs(graph, 0);
        System.out.println("BFS from vertex 0: " + bfsOrder);
        
        List<Integer> path = bfsShortestPath(graph, 0, 6);
        System.out.println("Shortest path from 0 to 6: " + path);
    }
    
    private static void testDFS() {
        System.out.println("\n=== DFS TEST ===");
        Graph graph = new Graph(7, false);
        graph.addEdge(0, 1);
        graph.addEdge(0, 2);
        graph.addEdge(1, 3);
        graph.addEdge(1, 4);
        graph.addEdge(2, 5);
        graph.addEdge(2, 6);
        
        List<Integer> dfsOrder = dfs(graph, 0);
        System.out.println("DFS from vertex 0: " + dfsOrder);
    }
    
    private static void testTopologicalSort() {
        System.out.println("\n=== TOPOLOGICAL SORT TEST ===");
        Graph dag = new Graph(6, true);
        dag.addEdge(5, 2);
        dag.addEdge(5, 0);
        dag.addEdge(4, 0);
        dag.addEdge(4, 1);
        dag.addEdge(2, 3);
        dag.addEdge(3, 1);
        
        System.out.println(dag);
        List<Integer> topoOrder = topologicalSort(dag);
        System.out.println("Topological order: " + topoOrder);
        
        // Test with cycle
        Graph withCycle = new Graph(3, true);
        withCycle.addEdge(0, 1);
        withCycle.addEdge(1, 2);
        withCycle.addEdge(2, 0);
        
        System.out.println("\nGraph with cycle:");
        List<Integer> result = topologicalSort(withCycle);
        System.out.println("Topological sort result: " + 
            (result == null ? "null (cycle detected)" : result.toString()));
    }
    
    private static void testCycleDetection() {
        System.out.println("\n=== CYCLE DETECTION TEST ===");
        
        // Directed graph with cycle
        Graph directedWithCycle = new Graph(3, true);
        directedWithCycle.addEdge(0, 1);
        directedWithCycle.addEdge(1, 2);
        directedWithCycle.addEdge(2, 0);
        System.out.println("Directed graph with cycle: " + hasCycleDirected(directedWithCycle));
        
        // Directed graph without cycle
        Graph directedNoCycle = new Graph(3, true);
        directedNoCycle.addEdge(0, 1);
        directedNoCycle.addEdge(1, 2);
        System.out.println("Directed graph without cycle: " + hasCycleDirected(directedNoCycle));
        
        // Undirected graph with cycle
        Graph undirectedWithCycle = new Graph(3, false);
        undirectedWithCycle.addEdge(0, 1);
        undirectedWithCycle.addEdge(1, 2);
        undirectedWithCycle.addEdge(2, 0);
        System.out.println("Undirected graph with cycle: " + hasCycleUndirected(undirectedWithCycle));
        
        // Undirected graph without cycle (tree)
        Graph tree = new Graph(3, false);
        tree.addEdge(0, 1);
        tree.addEdge(1, 2);
        System.out.println("Undirected graph without cycle: " + hasCycleUndirected(tree));
    }
    
    private static void testConnectedComponents() {
        System.out.println("\n=== CONNECTED COMPONENTS TEST ===");
        Graph graph = new Graph(8, false);
        graph.addEdge(0, 1);
        graph.addEdge(1, 2);
        graph.addEdge(3, 4);
        graph.addEdge(5, 6);
        graph.addEdge(6, 7);
        
        List<List<Integer>> components = connectedComponents(graph);
        System.out.println("Number of components: " + components.size());
        for (int i = 0; i < components.size(); i++) {
            System.out.println("  Component " + (i + 1) + ": " + components.get(i));
        }
    }
    
    /**
     * Main method - Runs all test cases.
     */
    public static void main(String[] args) {
        System.out.println("Graph Algorithms - Comprehensive Test Suite");
        System.out.println("===========================================");
        
        try {
            testDijkstra();
            testBFS();
            testDFS();
            testTopologicalSort();
            testCycleDetection();
            testConnectedComponents();
            
            System.out.println("\n=== ALL TESTS COMPLETED SUCCESSFULLY ===");
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
