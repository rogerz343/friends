import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * A representation of an undirected graph with nodes of type V
 * @author roger
 *
 * @param <V> the type class of the nodes
 */
public class Graph<V> {
    
    private Map<V, List<V>> adjList = new HashMap<>();
    
    // global variables used for cliques algorithm
    private List<List<V>> maximalCliques = null;
    
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
            if (!adjList.get(e[0]).contains(e[1])) { adjList.get(e[0]).add(e[1]); }
            if (!adjList.get(e[1]).contains(e[0])) { adjList.get(e[1]).add(e[0]); }
        }
    }
    
    /**
     * Creates a graph with the adjacency list `inputAdjList`
     * @param inputAdjList The input adjacency list
     */
    public Graph(Map<V, List<V>> inputAdjList) {
        for (V v : inputAdjList.keySet()) {
            adjList.put(v, inputAdjList.get(v));
        }
    }
    
    public int getNumNodes() {
        return adjList.size();
    }
    
    public int getNumEdges() {
        int numEdges = 0;
        for (List<V> l : adjList.values()) {
            numEdges += l.size();
        }
        return numEdges;
    }
    
    /**
     * Finds all maximal cliques in the graph
     * @return A list of maximal cliques in the graph, where each clique is represented
     * as a list of `Person`s.
     * This method uses the Bron-Kerbosch algorithm with vertex ordering and pivoting.
     */
    public List<List<V>> findCliques() {
        maximalCliques = new ArrayList<>();
        BronKerboschVertexOrdering();
        List<List<V>> ans = new ArrayList<>();
        for (List<V> clique : maximalCliques) {
            ans.add(new ArrayList<>(clique));
        }
        return ans;
    }
    
    /**
     * Finds the maximum cliques in the graph `adjList`
     * @param R Parameter for the Bron Kerbosh algorithm
     * @param P Parameter for the Bron Kerbosh algorithm
     * @param X Parameter for the Bron Kerbosh algorithm
     * This method implements the BronKerbosh2 algorithm given at
     * <https://en.wikipedia.org/wiki/Bron%E2%80%93Kerbosch_algorithm>
     */
    private void BronKerboschPivoting(Set<V> R, Set<V> P, Set<V> X) {
        if (P.isEmpty() && X.isEmpty()) {
            maximalCliques.add(new ArrayList<>(R));
            return;
        }
        V pivot;
        if (!P.isEmpty()) {
            pivot = P.iterator().next();
        } else {
            pivot = X.iterator().next();
        }
        Set<V> pivotNeighbors = new HashSet<>(adjList.get(pivot));
        Set<V> PCopy = new HashSet<>(P);
        for (V v : PCopy) {
            if (pivotNeighbors.contains(v)) { continue; }
            
            R.add(v);
            Set<V> vNeighbors = new HashSet<>(adjList.get(v));
            Set<V> PPrime = new HashSet<>(P);
            PPrime.retainAll(vNeighbors);
            Set<V> XPrime = new HashSet<>(X);
            XPrime.retainAll(vNeighbors);
            
            BronKerboschPivoting(R, PPrime, XPrime);
            R.remove(v);
            P.remove(v);
            X.add(v);
        }
    }
    
    /**
     * Finds the maximum cliques in the graph `adjList`
     * @param R Parameter for the Bron Kerbosh algorithm
     * @param P Parameter for the Bron Kerbosh algorithm
     * @param X Parameter for the Bron Kerbosh algorithm
     * This method implements the BronKerbosh3 algorithm given at
     * <https://en.wikipedia.org/wiki/Bron%E2%80%93Kerbosch_algorithm>
     */
    private void BronKerboschVertexOrdering() {
        Set<V> P = new HashSet<>(adjList.keySet());
        Set<V> R = new HashSet<>();
        Set<V> X = new HashSet<>();
        List<NodeIntPair> degeneracyOrdering = new ArrayList<>();
        for (Map.Entry<V, List<V>> e : adjList.entrySet()) {
            degeneracyOrdering.add(new NodeIntPair(e.getKey(), e.getValue().size()));
        }
        degeneracyOrdering.sort((p1, p2) -> p1.val - p2.val);
        for (NodeIntPair p : degeneracyOrdering) {
            V v = p.node;
            
            R.add(v);
            Set<V> vNeighbors = new HashSet<>(adjList.get(v));
            Set<V> PPrime = new HashSet<>(P);
            PPrime.retainAll(vNeighbors);
            Set<V> XPrime = new HashSet<>(X);
            XPrime.retainAll(vNeighbors);
            
            BronKerboschPivoting(R, PPrime, XPrime);
            R.remove(v);
            P.remove(v);
            X.add(v);
        }
    }
    
    private class NodeIntPair {
        public V node;
        public int val;
        public NodeIntPair(V node, int val) {
            this.node = node;
            this.val = val;
        }
    }
}
