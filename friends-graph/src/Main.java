import java.awt.AWTException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * friends
 * @author roger
 * Program used to fetch and analyze information about a user's facebook friends. See the
 * readme.txt for more details.
 */
public class Main {
    
    static String DOWNLOADS_DIR = "D:\\Robin Zhang\\Downloads\\";
    static String OUTPUT_DIR = "D:\\Robin Zhang\\Desktop\\large borg desktop\\save\\";
    
    // TODO: add a GUI
    public static void main(String[] args) throws AWTException, IOException {
        long startTime = System.nanoTime();
        System.out.println("Program started at: " + LocalDateTime.now());
        
        harvestAll();
        // saveGraphInfo("cliques.txt");
        
        long endTime = System.nanoTime();
        System.out.println("Program ran for " + ((endTime - startTime) / 1000000000) + " seconds.");
    }
    
    public static void saveGraphInfo(String filename) throws IOException {
        Graph<Person> graph = loadIntoGraph(OUTPUT_DIR);
        System.out.println("Number of nodes: " + graph.getNumNodes());
        System.out.println("Number of edges: " + graph.getNumEdges());
        List<List<Person>> cliques = graph.getMaximalCliques(3);
        
        Path path = Paths.get(DOWNLOADS_DIR, filename);
        path = Files.createFile(path);
        
        List<String> lines =
                cliques.stream()
                .map(c -> c.toString())
                .collect(Collectors.toCollection(ArrayList<String>::new));
        Files.write(path, lines);
    }
    
    public static Graph<Person> loadIntoGraph(String dirpath) {
        List<List<Person>> adjLists = FriendsFiles.loadAllInDirectory(OUTPUT_DIR, false);
        Map<Person, List<Person>> adjList = new HashMap<>();
        for (List<Person> l : adjLists) {
            Person p = l.get(0);
            l.remove(0);
            adjList.put(p, l);
        }
        return new Graph<Person>(adjList);
    }

    public static boolean harvestAll() throws IOException {
        // give the user some time to set up the facebook page correctly
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        Harvester h;
        try {
            h = new Harvester(450, 2000, DOWNLOADS_DIR, OUTPUT_DIR);
        } catch (AWTException e) {
            e.printStackTrace();
            System.out.println("Could not create Harvester");
            return false;
        }
        return h.beginNewHarvest();
    }
}
