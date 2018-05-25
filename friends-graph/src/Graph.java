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
    
    // global variable used for cliques algorithm
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
    
    public int getNumNodes() {
        return adjList.size();
    }
    
    public int getNumEdges() {
        int numEdges = 0;
        for (List<V> l : adjList.values()) {
            numEdges += l.size();
        }
        return numEdges / 2;
    }
    
    public List<V> getNeighbors(V u) {
        return new ArrayList<>(adjList.get(u));
    }
    
    /**
     * Returns the length of the shortest path between u and v.
     * @param u A node in the graph.
     * @param v A node in the graph.
     * @return The length of the shortest path between u and v.
     */
    public int getDistance(V u, V v) {
        return getShortestPath(u, v).size() - 1;
    }
    
    /**
     * Returns the sequence of nodes on a shortest path from u to v, including the nodes u and v.
     * @param u A node in the graph.
     * @param v A node in the graph.
     * @return
     */
    public List<V> getShortestPath(V u, V v) {
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
        List<V> result = new ArrayList<>();
        if (parents.get(v) == null) { return result; }
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
    public List<V> getSuggestedFriends(V u, int minMutualFriends) {
        List<V> candidates = getDistanceDNodes(u, 2);
        List<NodeIntPair> candidatesFiltered = new ArrayList<>();
        for (V v : candidates) {
            List<V> mutualFriends = getMutualFriends(u, v);
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
    public List<V> getMutualFriends(V u, V w) {
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
    public List<V> getDistanceDNodes(V source, int d) {
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
     * Finds the maximum flow from the given source node to the given sink node. This method
     * implements the Edmonds-Karp variation of Ford-Fulkerson method.
     * @param source The source node.
     * @param sink The sink node.
     * @return The value of the maximum flow from source to sink.
     */
    public int maxFlow(V source, V sink) {
        // first create a copy of the adjacency lists to restore to later
        Map<V, List<V>> adjListBackup = new HashMap<>();
        for (Map.Entry<V, List<V>> e : adjList.entrySet()) {
            adjListBackup.put(e.getKey(), new ArrayList<>(e.getValue()));
        }
        int flow = 0;
        
        // TODO: finish this
        return 0;
    }
        
    /**
     * Finds all maximal cliques in the graph whose size is at least minSize and returns
     * them in descending order of their size.
     * @param minSize the minimum size of a maximal clique that is returned.
     * @return A list of maximal cliques in the graph sorted in descending order by their
     * size, where each clique is represented as a list of `Person`s.
     * This method uses the Bron-Kerbosch algorithm with vertex ordering and pivoting.
     */
    public List<List<V>> getMaximalCliques(int minSize) {
        maximalCliques = new ArrayList<>();
        BronKerboschVertexOrdering();
        List<List<V>> ans = new ArrayList<>();
        for (List<V> clique : maximalCliques) {
            if (clique.size() >= minSize) {
                ans.add(new ArrayList<>(clique));
            }
        }
        ans.sort((l1, l2) -> l2.size() - l1.size());
        return ans;
    }
    
    public long numCalls = 0;
    
    /**
     * Finds the maximum cliques in the graph `adjList`
     * @param R Parameter for the Bron Kerbosch algorithm
     * @param P Parameter for the Bron Kerbosch algorithm
     * @param X Parameter for the Bron Kerbosch algorithm
     * This method implements the BronKerbosch2 algorithm given at
     * <https://en.wikipedia.org/wiki/Bron%E2%80%93Kerbosch_algorithm>
     */
    private void BronKerboschPivoting(Set<V> R, Set<V> P, Set<V> X) {
        numCalls++;
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
    
    public long nodesProcessedTopLevel = 0;
    
    /**
     * Finds the maximum cliques in the graph `adjList`
     * @param R Parameter for the Bron Kerbosch algorithm
     * @param P Parameter for the Bron Kerbosch algorithm
     * @param X Parameter for the Bron Kerbosch algorithm
     * This method implements the BronKerbosch3 algorithm given at
     * <https://en.wikipedia.org/wiki/Bron%E2%80%93Kerbosch_algorithm>
     */
    private void BronKerboschVertexOrdering() {
        numCalls++;
        Set<V> P = new HashSet<>(adjList.keySet());
        Set<V> R = new HashSet<>();
        Set<V> X = new HashSet<>();
        List<NodeIntPair> degeneracyOrdering = new ArrayList<>();
        for (Map.Entry<V, List<V>> e : adjList.entrySet()) {
            degeneracyOrdering.add(new NodeIntPair(e.getKey(), e.getValue().size()));
        }
        degeneracyOrdering.sort((p1, p2) -> p1.val - p2.val);
        long startTime = System.nanoTime();
        for (NodeIntPair p : degeneracyOrdering) {
            if (nodesProcessedTopLevel % 1000 == 0) {
                System.out.println("nodes processed: " + nodesProcessedTopLevel);
                System.out.println("Time for the last 1000 nodes: " + ((System.nanoTime() - startTime) / 1000000000) + " seconds.");
            }
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
            nodesProcessedTopLevel++;
        }
        
        System.out.println("nodes processed: " + nodesProcessedTopLevel);
        System.out.println("Time for the last 1000 nodes: " + ((System.nanoTime() - startTime) / 1000000000) + " seconds.");
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
