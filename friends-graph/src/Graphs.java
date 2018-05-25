
/**
 * Contains functions and operations for {@code Graph}s
 * @author roger
 * 
 */
public class Graphs {

    /**
     * Finds all maximal cliques in the given graph whose size is at least minSize and returns
     * them in descending order of their size.
     * @param minSize the minimum size of a maximal clique that is returned.
     * @return A list of maximal cliques in the graph sorted in descending order by their
     * size, where each clique is represented as a list of nodes.
     * This method uses the Bron-Kerbosch algorithm with vertex ordering and pivoting.
     */
    public static List<List<V>> getMaximalCliques(Graph<V> graph, int minSize) {
        List<List<V>> allMaxCliques = new ArrayList<>();
        BronKerboschVertexOrdering(graph.adjList(), allMaxCliques);
        List<List<V>> ans = new ArrayList<>();
        for (List<V> clique : ans) {
            if (clique.size() >= minSize) {
                ans.add(new ArrayList<>(clique));
            }
        }
        ans.sort((l1, l2) -> l2.size() - l1.size());
        return ans;
    }
    
    /**
     * Finds the maximum cliques in the graph specified by {@code adjList}
     * @param adjList The adjacency list for the graph.
     * @param ans The {@code List} to store the results in.
     * @param R Parameter for the Bron Kerbosch algorithm
     * @param P Parameter for the Bron Kerbosch algorithm
     * @param X Parameter for the Bron Kerbosch algorithm
     * This method implements the BronKerbosch2 algorithm given at
     * <https://en.wikipedia.org/wiki/Bron%E2%80%93Kerbosch_algorithm>
     */
    private static void BronKerboschPivoting(Map<V, List<V>> adjList, List<List<V>> ans,
            Set<V> R, Set<V> P, Set<V> X) {
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
     * Finds the maximum cliques in the graph specified by {@code adjList}
     * @param adjList The adjacency lists for the graph.
     * @param ans The {@code List} to store the results in.
     * This method implements the BronKerbosch3 algorithm given at
     * <https://en.wikipedia.org/wiki/Bron%E2%80%93Kerbosch_algorithm>
     */
    private static void BronKerboschVertexOrdering(Map<V, List<V>> adjList, List<List<V>> ans) {
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
    
    /**
     * Useful for assigning int values to nodes and for assigning weights
     * to edges.
     */
    private class NodeIntPair {
        public V node;
        public int val;
        public NodeIntPair(V node, int val) {
            this.node = node;
            this.val = val;
        }
    }
}