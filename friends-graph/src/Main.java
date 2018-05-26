import java.awt.AWTException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import friends.FriendsFiles;
import friends.Harvester;
import friends.Person;
import graph.Graph;
import graph.Graphs;

/**
 * friends
 * @author roger
 * Program used to fetch and analyze information about a user's facebook friends. See the
 * readme.txt for more details.
 */
public class Main {
    
    // FOR TESTING (personal information about others should be removed for github)
    static Person ROOT_PERSON = 
    static Person TEST_PERSON = 
    
    static String DOWNLOADS_DIR = 
    static String OUTPUT_DIR = 
    
    // TODO: add a GUI
    public static void main(String[] args) throws AWTException, IOException {
        long startTime = System.nanoTime();
        System.out.println("Program started at: " + LocalDateTime.now());
        
        // runHarvestAll();
        // resumeHarvest();
        saveGraphInfo();
        
        long endTime = System.nanoTime();
        System.out.println("Program ran for " + ((endTime - startTime) / 1000000000) + " seconds.");
    }
    
    public static void saveGraphInfo() throws IOException {
        Graph<Person> graph = loadIntoGraph(OUTPUT_DIR);
        System.out.println("Number of nodes: " + graph.numNodes());
        System.out.println("Number of edges: " + graph.numEdges());
        

        
        // SUGGESTED FRIENDS
        
        List<Person> suggested = Graphs.suggestedFriendsFor(graph, ROOT_PERSON, 20);
        Path path = Paths.get(DOWNLOADS_DIR, "r_suggested.txt");
        Files.write(path, suggested.toString().getBytes(),
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING,
                StandardOpenOption.WRITE);
        
        // MUTUAL FRIENDS
        
//        List<Person> mf = Graphs.mutualFriends(graph, ROOT_PERSON, TEST_PERSON);
//        Path path = Paths.get(DOWNLOADS_DIR, "r_t_mutual.txt");
//        Files.write(path, mf.toString().getBytes(),
//                StandardOpenOption.CREATE,
//                StandardOpenOption.TRUNCATE_EXISTING,
//                StandardOpenOption.WRITE);
        
        // CLIQUES
        
//        List<List<Person>> cliques = Graphs.maximalCliquesContaining(graph, ROOT_PERSON, 3);
//        System.out.println("num cliques size >= 3: " + cliques.size());
//        
//        // make the file not too large
//        List<List<Person>> top10 = new ArrayList<>();
//        for (int i = 0; i < 10; i++) {
//            top10.add(cliques.get(i));
//        }
//        
//        List<String> lines =
//                top10.stream()
//                .map(c -> c.toString())
//                .collect(Collectors.toCollection(ArrayList<String>::new));
//        Path path = Paths.get(DOWNLOADS_DIR, filename);
//        Files.write(path, lines);
    }
    
    public static Graph<Person> loadIntoGraph(String dirpath) {
        List<List<Person>> adjLists = FriendsFiles.loadAllInDirectory(OUTPUT_DIR, false);
        Map<Person, List<Person>> adjList = new HashMap<>();
        for (List<Person> l : adjLists) {
            Person p = l.get(0);
            l.remove(0);
            adjList.put(p, l);
        }
        System.out.println("Number of complete-info nodes: " + adjList.size());
        return new Graph<Person>(adjList);
    }

    public static boolean runHarvestAll() throws IOException {
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
    
    public static boolean resumeHarvest() throws IOException {
        // give the user some time to set up the facebook page correctly
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        Harvester h;
        try {
            h = new Harvester(OUTPUT_DIR);
        } catch (AWTException e) {
            e.printStackTrace();
            System.out.println("Could not create Harvester");
            return false;
        }
        return h.harvestAllPages();
    }
}
