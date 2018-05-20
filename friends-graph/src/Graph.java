import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A representation of an undirected graph with nodes of type V
 * @author roger
 *
 * @param <V> the type class of the nodes
 */
public class Graph<V> {
    
    private Map<V, List<V>> adjList;
    
    /**
     * Creates an empty graph (with zero nodes and zero edges)
     */
    public Graph() {
        adjList = new HashMap<>();
    }
    
    /**
     * Creates a graph with the nodes in `nodes` and the edges in `edges`
     * @param nodes A list of nodes.
     * @param edges A list of edges, each edge is an array of length 2. The
     * endpoints of the edges to not have to be in `nodes`; they are be created
     * on the fly.
     * @throws Exception 
     */
    public Graph(List<V> nodes, List<V[]> edges) throws Exception {
        adjList = new HashMap<>();
        for (V v : nodes) {
            adjList.put(v, new ArrayList<>());
        }
        for (V[] e : edges) {
            if (e.length != 2) {
                throw new Exception("Bad input to Graph constructor.");
            }
            if (!adjList.containsKey(e[0])) { adjList.put(e[0], new ArrayList<>()); }
            if (!adjList.containsKey(e[1])) { adjList.put(e[1], new ArrayList<>()); }
            if (!adjList.get(e[0]).contains(e[1])) { adjList.get(e[0]).add(e[1]); }
            if (!adjList.get(e[1]).contains(e[0])) { adjList.get(e[1]).add(e[0]); }
        }
    }
    
}
