import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * A representation of an undirected graph with nodes of type V. Nodes equality
 * is tested using .equals()
 * @author roger
 *
 * @param <V> The type class of the nodes. Ensuring that the nodes don't mutate is left as
 * an exercise for the user of this class.
 */
public class Graph<V> {
    
    private Map<V, List<V>> adjList = new HashMap<>();
    
    // the list of maximal cliques calculated from getCliques()
    private List<List<V>> maximalCliques = null;
    // the max flow calculated from the latest call to edmondsKarp()
    private int maxFlow = -1;
    
    /**
     * Creates an empty graph (with zero nodes and zero edges)
     */
    public Graph() {}
    
    /**
     * Creates a graph with the nodes in `nodes` and the edges in `edges`
     * @param nodes A list of nodes.
     * @param edges A list of edges, each edge is an array of length 2. The
     * endpoints of the edges to not have to be in `nodes`; they are be created
     * on the fly.
     */
    public Graph(List<V> nodes, List<V[]> edges)  {
        for (V v : nodes) {
            adjList.put(v, new ArrayList<>());
        }
        for (V[] e : edges) {
            if (e.length != 2) {
                throw new IllegalArgumentException("Bad input to Graph constructor");
            }
            if (!adjList.containsKey(e[0])) { adjList.put(e[0], new ArrayList<>()); }
            if (!adjList.containsKey(e[1])) { adjList.put(e[1], new ArrayList<>()); }

            if (e[0].equals(e[1])) { continue; }
            if (!adjList.get(e[0]).contains(e[1])) { adjList.get(e[0]).add(e[1]); }
            if (!adjList.get(e[1]).contains(e[0])) { adjList.get(e[1]).add(e[0]); }
        }
    }
    
    /**
     * Creates a graph with the adjacency list `inputAdjList`. If the adjacency
     * list contains a V that is not a key but in one of the List<V>, then it
     * will automatically be added as a node. Also ensures no duplicate edges.
     * @param inputAdjList The input adjacency list
     */
    public Graph(Map<V, List<V>> inputAdjList) {
        for (V v : inputAdjList.keySet()) {
            adjList.put(v, new ArrayList<>());
            List<V> vNeighbors = inputAdjList.get(v);
            for (V vNeighbor : vNeighbors) {
                if (v.equals(vNeighbor)) { continue; }
                if (!adjList.containsKey(vNeighbor)) {
                    adjList.put(vNeighbor, new ArrayList<>());
                }
                if (!adjList.get(v).contains(vNeighbor)) { adjList.get(v).add(vNeighbor); }
                if (!adjList.get(vNeighbor).contains(v)) { adjList.get(vNeighbor).add(v); }
            }
        }
    }
    
    public int numNodes() {
        return adjList.size();
    }
    
    public int numEdges() {
        int numEdges = 0;
        for (List<V> l : adjList.values()) {
            numEdges += l.size();
        }
        return numEdges / 2;
    }

    public List<V> nodes() {
        return new ArrayList<>(adjList.keySet());
    }
    
    public List<V> neighbors(V u) {
        return new ArrayList<>(adjList.get(u));
    }

    /**
     * Returns a copy of the adjacency lists of this graph.
     * @return a copy of the adjacency lists of this graph.
     */
    public Map<V, List<V>> adjList() {
        Map<V, List<V>> copy = new HashMap<>();
        for (Map.Entry<V, List<V>> e : adjList.entrySet()) {
            List<V> neighbors = new ArrayList<>();
            for (V v : e.getValue()) {
                neighbors.add(v);
            }
            map.put(e.getKey(), neighbors);
        }
        return copy;
    }
    
    /**
     * Returns the length of the shortest path between u and v (the number of edges in the path).
     * @param u A node in the graph.
     * @param v A node in the graph.
     * @return The length of the shortest path between u and v.
     */
    public int distance(V u, V v) {
        return shortestPath(u, v).size() - 1;
    }
    
    /**
     * Returns the sequence of distinct nodes on a shortest path from u to v, including the
     * nodes u and v.
     * @param u A node in the graph.
     * @param v A node in the graph.
     * @return The sequence of distinct nodes on a shortest path from u to v, including the
     * nodes u and v. Returns null if no such path exists.
     */
    public List<V> shortestPath(V u, V v) {
        if (u.equals(v)) {
            List<V> result = new ArrayList<>();
            result.add(u);
            return result;
        }
        Map<V, V> parents = new HashMap<>();
        Set<V> discovered = new HashSet<>();
        Deque<V> queue = new ArrayDeque<>();
        discovered.add(u);
        queue.add(u);
        while (!queue.isEmpty()) {
            int size = queue.size();
            for (int i = 0; i < size; i++) {
                V a = queue.remove();
                for (V b : adjList.get(a)) {
                    if (!discovered.contains(b)) {
                        parents.put(b, a);
                        discovered.add(b);
                        queue.add(b);
                        
                        if (b.equals(v)) {
                            break;
                        }
                    }
                }
            }
        }
        if (parents.get(v) == null) { return null; }
        List<V> result = new ArrayList<>();
        V curr = v;
        while (parents.get(curr) != null) {
            result.add(curr);
            curr = parents.get(curr);
        }
        Collections.reverse(result);
        return result;
    }
    
    /**
     * Returns a list of nodes that are exactly distance 2 away from
     * u. The list is sorted in descending order of the number of length-2
     * paths to u. In the context of a social network, this would return
     * a list of people who you are not friends with but have mutual friends with.
     * @param u The node in consideration
     * @param minMutualFriends The minimum number of mutual friends of u and a node
     * v in order for v to be added to the returned List.
     * @return A list of nodes sorted in descending order by the number of
     * length-2 paths from those nodes to u (e.g. number of mutual
     * friends they have with u).
     */
    public List<V> suggestedFriendsFor(V u, int minMutualFriends) {
        List<V> candidates = distanceDFrome(u, 2);
        List<NodeIntPair> candidatesFiltered = new ArrayList<>();
        for (V v : candidates) {
            List<V> mutualFriends = mutualFriendsOf(u, v);
            if (mutualFriends.size() >= minMutualFriends) {
                candidatesFiltered.add(new NodeIntPair(v, mutualFriends.size()));
            }
        }
        candidatesFiltered.sort((p1, p2) -> p2.val - p1.val);
        return candidatesFiltered.stream()
                .map(p -> p.node)
                .collect(Collectors.toCollection(ArrayList<V>::new));
    }
    
    /**
     * Returns a list of nodes that are neighbors with both u and v.
     * @param u A node in the graph.
     * @param w A node in the graph.
     * @return
     */
    public List<V> mutualFriendsOf(V u, V w) {
        Set<V> uNeighbors = new HashSet<>();
        for (V uNeighbor : adjList.get(u)) {
            uNeighbors.add(uNeighbor);
        }
        List<V> result = new ArrayList<>();
        for (V wNeighbor : adjList.get(w)) {
            if (uNeighbors.contains(wNeighbor)) {
                result.add(wNeighbor);
            }
        }
        return result;
    }
    
    /**
     * Returns a list of all of the nodes that are exactly distance d away from source.
     * @param source A node in the graph.
     * @return A list of all of the nodes that are exactly distance d away from the source.
     */
    public List<V> distanceDFrom(V source, int d) {
        Set<V> discovered = new HashSet<>();
        Deque<V> queue = new ArrayDeque<>();
        discovered.add(source);
        queue.add(source);
        int size = queue.size();
        int currDist = 0;
        while (!queue.isEmpty() && currDist < d) {
            for (int i = 0; i < size; i++) {
                V v = queue.remove();
                for (V w : adjList.get(v)) {
                    if (!discovered.contains(w)) {
                        queue.add(w);
                    }
                }
            }
            currDist++;
        }
        if (currDist == d) {
            return new ArrayList<>(queue);
        }
        return new ArrayList<>();
    }

    /**
     * Finds the maximum flow from the source to the sink, assuming that all edges have capacity 1.
     * @param source The source node.
     * @param sink The sink node.
     * @return The value of the maximum flow from the source to the sink.
     */
    public int maxFlow(V source, V sink) {
        if (flow == -1) {
            edmondsKarp(source, sink);
        }
        return maxFlow;
    }


    public int highestDensitySubgraph(V s, V t) {
        // temporarily add a source and sink
        
    }

    /**
     * Finds the minimum s-t cut of the flow network
     * @param s A node in the graph.
     * @param t A node in the graph (distinct from t).
     * @return The nodes for each of the two parts of the minimum cut (partition) of the graph.
     * Each part is a {@code List} of nodes and the two {@code List}s are in the 0th and 1st
     * indices of the returned array.
     */
    public List<V>[] minCutPartition(V s, V t) {
        Map<V, List<FlowEdge>> residual = edmondsKarp(s, t);
        Set<V> discovered = new HashSet<>();
        discovered.add(s);
        Deque<V> queue = new ArrayDeque<>();
        queue.add(s);
        while (!queue.isEmpty()) {
            V curr = queue.remove();
            for (FlowEdge fe : residual.get(curr)) {
                if (fe.capacity - fe.flow > 0) {
                    discovered.add(fe.node2);
                    queue.add(fe.node2);
                }
            }
        }
        List<V> partition1 = new ArrayList<>(discovered);
        List<V> partition2 = new ArrayList<>();
        for (V v : nodes()) {
            if (!discovered.contains(v)) {
                partition2.add(v);
            }
        }
        return new List<V>[]{ partition1, partition2 };
    }
    
    /**
     * Performs the Edmonds-Karp variation of Ford-Fulkerson method using adjacency lists and
     * returns the residual flow network.
     * @param source The source node.
     * @param sink The sink node.
     * @return The final residual flow network after running the Edmonds-Karp algorithm.
     */
    private Map<V, List<FlowEdge>> edmondsKarp(V source, V sink) {
        // create a copy of the adjacency list but with values for each edge's capacity and flow
        Map<V, List<FlowEdge>> flowGraph = new HashMap<>();
        for (Map.Entry<V, List<V>> e : adjList.entrySet()) {
            List<FlowEdge> edges = new ArrayList<>();
            for (V node2 : e.getValue()) {
                edges.add(new FlowEdge(e.getKey(), node2, 1, 0));
            }
            flowGraph.put(e.getKey(), edges);
        }

        int flow = 0;
        
        while (true) {
            // find shortest augmenting path (note that this is different from shortestPath())
            Map<V, V> parents = new HashMap<>();
            Deque<V> queue = new ArrayDeque<>();
            queue.add(source);
            while (!queue.isEmpty() && parents.get(sink) == null) {
                V curr = queue.remove();
                for (FlowEdge fe : flowGraph.get(curr)) {
                    if (parents.get(fe.node2) == null && !node2.equals(source)
                            && fe.capacity > fe.flow) {
                        parents.put(fe.node2, curr);
                        queue.add(fe.node2);
                    }
                    if (fe.node2 == sink) {
                        break;
                    }
                }
            }
            
            // augment flow with the new augmenting path, if it exists
            if (parents.get(sink) != null) {
                int pathFlow = Integer.MAX_VALUE;
                List<FlowEdge> pathEdges = new ArrayList<>();
                V curr = sink;
                while (parents.get(curr) != null) {
                    V parent = parents.get(curr);
                    for (V childEdge : flowGraph.get(parent)) {
                        if (childEdge.node2.equals(curr)) {
                            pathFlow = Math.min(pathFlow, childEdge.capacity - childEdge.flow);
                            pathEdges.add(childEdge);
                            break;
                        }
                    }
                    curr = parent;
                }
                for (FlowEdge pathEdge : pathEdges) {
                    pathEdge.flow += pathFlow;
                    for (FlowEdge reversedFe : flowGraph.get(pathEdge.node2)) {
                        if (reversedFe.node2.equals(pathEdge.node1)) {
                            reversedFe.flow -= pathFlow;
                        }
                    }
                    flow += pathFlow;
                }
            } else {
                this.flow = flow;
                return flowGraph;
            }
        }
    }

    /**
     * Represents a directed edge from {@code node1} to {@code node2} in a graph with flows.
     */
    private class FlowEdge {
        public V node1;
        public V node2;
        public int capacity;
        public int flow;
        public FlowEdge(V node1, V node2, int capacity, int flow) {
            this.node1 = node1;
            this.node2 = node2;
            this.capacity = capacity;
            this.flow = flow;
        }
    }
}
