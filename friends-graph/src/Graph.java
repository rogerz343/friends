import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A representation of an undirected graph with nodes of type V. Nodes equality
 * is tested using .equals(). This graph disallows self-loops and parallel edges.
 * @author roger
 *
 * @param <V> The type class of the nodes. Ensuring that the nodes don't mutate is left as
 * an exercise for the user of this class.
 */
public class Graph<V> {
    
    Map<V, List<V>> adjList = new HashMap<>();
    
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
            copy.put(e.getKey(), new ArrayList<>(e.getValue()));
        }
        return copy;
    }
    
    /**
     * Adds the node to the graph, if it doesn't already exist.
     * @param v The node to add.
     * @return true if a new node was added, false otherwise (including the case where the
     * node already existed).
     */
    public boolean addNode(V v) {
    	if (adjList.containsKey(v)) { return false; }
    	adjList.put(v, new ArrayList<>());
    	return true;
    }
    
    /**
     * Adds the (undirected) edge {u, v} to the graph. If either of u or v does not exist in the
     * graph, then it is added.
     * @param u A node.
     * @param v A node, different from u.
     * @return true if insertion was successful, false otherwise (including the case where the
     * edge already existed).
     */
    public boolean addEdge(V u, V v) {
    	if (u.equals(v)) { return false; }
    	
    	boolean edgeAdded = false;
    	if (!adjList.containsKey(u)) { adjList.put(u, new ArrayList<>()); }
    	if (!adjList.containsKey(v)) { adjList.put(v, new ArrayList<>()); }
    	
    	if (!adjList.get(u).contains(v)) {
    		adjList.get(u).add(v);
    		edgeAdded = true;
    	}
    	if (!adjList.get(v).contains(u)) {
    		adjList.get(v).add(u);
    		edgeAdded = true;
    	}
    	return edgeAdded;
    }
        
    /**
     * Removes the specified node and all edges incident on it, if it exists.
     * @param u The node to remove.
     */
    public void removeNode(V u) {
    	if (!adjList.containsKey(u)) { return; }
    	List<V> neighbors = adjList.get(u);
    	adjList.remove(u);
    	for (V v : neighbors) {
    		adjList.get(v).remove(u);
    	}
    }
        
    /**
     * Removes the specified edge, if it exists.
     * @param u A node in the graph.
     * @param v A node in the graph.
     */
    public void removeEdge(V u, V v) {
    	adjList.get(u).remove(v);
    	adjList.get(v).remove(u);
    }
    
    public long nodesProcessedTopLevel = 0;
    
    /**
     * Returns the length of the shortest path between u and v (the number of edges in the path).
     * Identical to {@code Graphs.distance(this, u, v)}
     * @param u A node in the graph.
     * @param v A node in the graph.
     * @return The length of the shortest path between u and v.
     */

    public int distance(V u, V v) {
        return Graphs.distance(this, u, v);
    }
}
