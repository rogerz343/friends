package graph;
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
 * Contains functions and operations for {@link Graph}s to supplement the ones in {@link Graph}.
 * @author roger
 * 
 */
public class Graphs {
	
	/**
     * Returns a list of all of the nodes that are exactly distance d away from source in the
     * given graph.
     * @param g The graph.
     * @param source A node in the graph.
     * @return A list of all of the nodes that are exactly distance d away from the source.
     */
    public static <V> List<V> distanceDFrom(Graph<V> g, V source, int d) {
        Set<V> discovered = new HashSet<>();
        Deque<V> queue = new ArrayDeque<>();
        discovered.add(source);
        queue.add(source);
        int size = queue.size();
        int currDist = 0;
        while (!queue.isEmpty() && currDist < d) {
            for (int i = 0; i < size; i++) {
                V v = queue.remove();
                for (V w : g.adjList.get(v)) {
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
     * Returns the length of the shortest path between u and v (the number of edges in the path)
     * in the given graph.
     * @param g The graph.
     * @param u A node in the graph.
     * @param v A node in the graph.
     * @return The length of the shortest path between u and v.
     */
    public static <V> int distance(Graph<V> g, V u, V v) {
        return shortestPath(g, u, v).size() - 1;
    }
    
    /**
     * Returns the sequence of distinct nodes on a shortest path from u to v in the given graph,
     * including the nodes u and v.
     * @param g The graph.
     * @param u A node in the graph.
     * @param v A node in the graph.
     * @return The sequence of distinct nodes on a shortest path from u to v, including the
     * nodes u and v. Returns null if no such path exists.
     */
    public static <V> List<V> shortestPath(Graph<V> g, V u, V v) {
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
                for (V b : g.adjList.get(a)) {
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
     * Returns a list of nodes that are exactly distance 2 away from u in the given graph.
     * The list is sorted in descending order of the number of length-2
     * paths to u. In the context of a social network, this would return
     * a list of people who you are not friends with but have mutual friends with.
     * @param g The graph.
     * @param u The node in consideration
     * @param minMutualFriends The minimum number of mutual friends of u and a node
     * v in order for v to be added to the returned List.
     * @return A list of nodes sorted in descending order by the number of
     * length-2 paths from those nodes to u (e.g. number of mutual
     * friends they have with u).
     */
    public static <V> List<V> suggestedFriendsFor(Graph<V> g, V u, int minMutualFriends) {
        Set<V> friends = new HashSet<>(g.adjList.get(u));
        List<V> candidates = distanceDFrom(g, u, 2);
        List<NodeIntPair<V>> candidatesFiltered = new ArrayList<>();
        for (V v : candidates) {
            List<V> mutualFriends = mutualFriends(g, u, v);
            if (!friends.contains(v) && mutualFriends.size() >= minMutualFriends) {
                candidatesFiltered.add(new NodeIntPair<>(v, mutualFriends.size()));
            }
        }
        candidatesFiltered.sort((p1, p2) -> p2.val - p1.val);
        return candidatesFiltered.stream()
                .map(p -> p.node)
                .collect(Collectors.toCollection(ArrayList<V>::new));
    }
    
    /**
     * Returns a list of nodes in the graph that are neighbors with both u and v.
     * @param g The graph.
     * @param u A node in the graph.
     * @param w A node in the graph.
     * @return A list of nodes in the graph that are neighbors with both u and v.
     */
    public static <V> List<V> mutualFriends(Graph<V> g, V u, V w) {
        Set<V> uNeighbors = new HashSet<>();
        for (V uNeighbor : g.adjList.get(u)) {
            uNeighbors.add(uNeighbor);
        }
        List<V> result = new ArrayList<>();
        for (V wNeighbor : g.adjList.get(w)) {
            if (uNeighbors.contains(wNeighbor)) {
                result.add(wNeighbor);
            }
        }
        return result;
    }
	
	/**
     * Finds the maximum flow from the source to the sink in the given graph, assuming that all
     * edges have capacity 1.
     * @param g The undirected graph.
     * @param source The source node.
     * @param sink The sink node.
     * @return The value of the maximum flow from the source to the sink.
     */
    public static <V> int maxFlow(Graph<V> g, V source, V sink) {
    	Map<V, List<FlowEdge<V>>> residual = new HashMap<>();
        return edmondsKarp(g.adjList, residual, source, sink);
    }


    public static <V> int highestDensitySubgraph(V s, V t) {
        // temporarily add a source and sink
        // TODO: finish this
    	return 0;
    }

    /**
     * Finds the minimum s-t cut of the flow network.
     * @param g The graph.
     * @param s A node in the graph.
     * @param t A node in the graph (distinct from t).
     * @return The nodes for each of the two parts of the minimum cut (partition) of the graph.
     * The returned {@code List} has size 2 and each part of the partition is a {@code List} of
     * nodes stored in the 0th and 1st indices of the returned {@code List}, respectively.
     * indices of the returned {@code List}
     */
    public static <V> List<List<V>> minSTCut(Graph<V> g, V s, V t) {
        Map<V, List<FlowEdge<V>>> residual = new HashMap<>();
        edmondsKarp(g.adjList, residual, s, t);
        Set<V> discovered = new HashSet<>();
        discovered.add(s);
        Deque<V> queue = new ArrayDeque<>();
        queue.add(s);
        while (!queue.isEmpty()) {
            V curr = queue.remove();
            for (FlowEdge<V> fe : residual.get(curr)) {
                if (fe.capacity - fe.flow > 0) {
                    discovered.add(fe.node2);
                    queue.add(fe.node2);
                }
            }
        }
        List<V> partition1 = new ArrayList<>(discovered);
        List<V> partition2 = new ArrayList<>();
        for (V v : g.nodes()) {
            if (!discovered.contains(v)) {
                partition2.add(v);
            }
        }
        List<List<V>> ans = new ArrayList<>();
        ans.add(partition1);
        ans.add(partition2);
        return ans;
    }
    
    /**
     * Performs the Edmonds-Karp variation of Ford-Fulkerson method using adjacency lists and
     * returns the resulting max flow. The final residual network obtained is stored in
     * {@code residual}.
     * @param adjList The adjacency list for the graph.
     * @param flowGraph The data structure to store the final residual network in.
     * @param source The source node.
     * @param sink The sink node.
     * @return The maximum flow from {@code source} to {@code sink}.
     */
    private static <V> int edmondsKarp(Map<V, List<V>> adjList, Map<V, List<FlowEdge<V>>> flowGraph, V source, V sink) {
    	flowGraph.clear();
    	
        // create a copy of the adjacency list but with values for each edge's capacity and flow
        for (Map.Entry<V, List<V>> e : adjList.entrySet()) {
            List<FlowEdge<V>> edges = new ArrayList<>();
            for (V node2 : e.getValue()) {
                edges.add(new FlowEdge<>(e.getKey(), node2, 1, 0));
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
                for (FlowEdge<V> fe : flowGraph.get(curr)) {
                    if (parents.get(fe.node2) == null && !fe.node2.equals(source)
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
                List<FlowEdge<V>> pathEdges = new ArrayList<>();
                V curr = sink;
                while (parents.get(curr) != null) {
                    V parent = parents.get(curr);
                    for (FlowEdge<V> childEdge : flowGraph.get(parent)) {
                        if (childEdge.node2.equals(curr)) {
                            pathFlow = Math.min(pathFlow, childEdge.capacity - childEdge.flow);
                            pathEdges.add(childEdge);
                            break;
                        }
                    }
                    curr = parent;
                }
                for (FlowEdge<V> pathEdge : pathEdges) {
                    pathEdge.flow += pathFlow;
                    for (FlowEdge<V> reversedFe : flowGraph.get(pathEdge.node2)) {
                        if (reversedFe.node2.equals(pathEdge.node1)) {
                            reversedFe.flow -= pathFlow;
                        }
                    }
                    flow += pathFlow;
                }
            } else {
                return flow;
            }
        }
    }
    
    /**
     * Finds all maximal cliques with size at least {@code minSize} in the given graph
     * containing the specified node.
     * @param g The graph.
     * @param v The node that is included in all returned maximal cliques.
     * @param minSize The minimum size of a maximal clique that is returned. This will be set
     * to 3 if the given value is less than 3.
     * @return A list of maximal cliques in the graph that contain the specified node, sorted
     * in descending order by their size, where each clique is represented as a list of nodes.
     * This method uses the Bron-Kerbosch algorithm and pivoting.
     */
    public static <V> List<List<V>> maximalCliquesContaining(Graph<V> g, V v, int minSize) {
    	minSize = Math.max(minSize, 3);
    	
    	// create a copy of the subgraph induced on v and its neighbors
    	Map<V, List<V>> subgraph = new HashMap<>();
    	subgraph.put(v, new ArrayList<>());
    	List<V> curr = subgraph.get(v);
    	for (V neighbor : g.adjList.get(v)) {
    		subgraph.put(neighbor, new ArrayList<>());
    		curr.add(neighbor);
    		subgraph.get(neighbor).add(v);
    	}
    	for (V a : subgraph.get(v)) {
    		for (V b : g.adjList.get(a)) {
    			if (subgraph.containsKey(b)) {
	    			if (!subgraph.get(a).contains(b)) { subgraph.get(a).add(b); }
	    			if (!subgraph.get(b).contains(a)) { subgraph.get(b).add(a); }
    			}
    		}
    	}
    	
    	// TESTING SECTION
    	
    	int numEdges = 0;
        for (List<V> l : subgraph.values()) {
            numEdges += l.size();
        }
        numEdges /= 2;
    	System.out.println("Induced subgraph nodes: " + subgraph.size());
    	System.out.println("Induced subgraph edges: " + numEdges);
    	
    	// END OF TESTING SECTION
    	
    	List<List<V>> maxCliques = new ArrayList<>();
    	BronKerboschVertexOrdering(subgraph, maxCliques);
    	List<List<V>> ans = new ArrayList<>();
    	for (List<V> clique : maxCliques) {
            if (clique.size() >= minSize) {
                ans.add(new ArrayList<>(clique));
            }
        }
    	maxCliques.sort((l1, l2) -> l2.size() - l1.size());
    	return maxCliques;
    }

    /**
     * Finds all maximal cliques in the given graph whose size is at least minSize and returns
     * them in descending order of their size. Note: this method may take a very long time to
     * run. If possible, use {@link maximalCliquesContaining} instead.
     * @param minSize The minimum size of a maximal clique that is returned. This will be set
     * to 3 if the given value is less than 3.
     * @return A list of maximal cliques in the graph sorted in descending order by their
     * size, where each clique is represented as a list of nodes.
     * This method uses the Bron-Kerbosch algorithm with vertex ordering and pivoting.
     */
    public static <V> List<List<V>> allMaximalCliques(Graph<V> graph, int minSize) {
    	minSize = Math.max(minSize, 3);
        
    	List<List<V>> allMaxCliques = new ArrayList<>();
        BronKerboschVertexOrdering(graph.adjList, allMaxCliques);
        List<List<V>> ans = new ArrayList<>();
        for (List<V> clique : allMaxCliques) {
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
    private static <V> void BronKerboschPivoting(Map<V, List<V>> adjList, List<List<V>> ans,
            Set<V> R, Set<V> P, Set<V> X) {
        if (P.isEmpty() && X.isEmpty()) {
            ans.add(new ArrayList<>(R));
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
            
            BronKerboschPivoting(adjList, ans, R, PPrime, XPrime);
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
    private static <V> void BronKerboschVertexOrdering(Map<V, List<V>> adjList, List<List<V>> ans) {
        Set<V> P = new HashSet<>(adjList.keySet());
        Set<V> R = new HashSet<>();
        Set<V> X = new HashSet<>();
        List<NodeIntPair<V>> degeneracyOrdering = new ArrayList<>();
        for (Map.Entry<V, List<V>> e : adjList.entrySet()) {
            degeneracyOrdering.add(new NodeIntPair<>(e.getKey(), e.getValue().size()));
        }
        degeneracyOrdering.sort((p1, p2) -> p1.val - p2.val);
        for (NodeIntPair<V> p : degeneracyOrdering) {
            V v = p.node;
            
            R.add(v);
            Set<V> vNeighbors = new HashSet<>(adjList.get(v));
            Set<V> PPrime = new HashSet<>(P);
            PPrime.retainAll(vNeighbors);
            Set<V> XPrime = new HashSet<>(X);
            XPrime.retainAll(vNeighbors);
            
            BronKerboschPivoting(adjList, ans, R, PPrime, XPrime);
            R.remove(v);
            P.remove(v);
            X.add(v);
        }
    }
    
    /**
     * Represents a directed edge from {@code node1} to {@code node2} in a graph with flows.
     * @param <V> The type of node in the graph.
     */
    private static class FlowEdge<V> {
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
    
    /**
     * Useful for assigning int values to nodes in a Graph.
     * to edges.
     * @author roger
     * @param <V> The type of the nodes of the graph.
     */
    private static class NodeIntPair<V> {
        public V node;
        public int val;
        public NodeIntPair(V node, int val) {
            this.node = node;
            this.val = val;
        }
    }
}