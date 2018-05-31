import java.awt.AWTException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

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
    
    // PARAMETERS FOR THIS PROGRAM
    static String DOWNLOADS_DIR = 
    static String OUTPUT_DIR = 
    static int MAX_PAGES_TO_DOWNLOAD = 450;
    static int MAX_PER_PERSON = Integer.MAX_VALUE;
    
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
    
    /**
     * Call this function to compute various functions abou the graph. See {@link Graph} and
     * {@link Graphs} to see what you can do with them.
     * @throws IOException
     */
    public static void saveGraphInfo() throws IOException {
        Graph<Person> graph = loadIntoGraph(OUTPUT_DIR);
        System.out.println("Number of nodes: " + graph.numNodes());
        System.out.println("Number of edges: " + graph.numEdges());
        
        // SUGGESTED FRIENDS (uncomment below to use)
        
//        List<Person> suggested = Graphs.suggestedFriendsFor(graph, ROOT_PERSON, 20);
//        Path path = Paths.get(DOWNLOADS_DIR, "r_suggested.txt");
//        Files.write(path, suggested.toString().getBytes(),
//                StandardOpenOption.CREATE,
//                StandardOpenOption.TRUNCATE_EXISTING,
//                StandardOpenOption.WRITE);
        
        // MUTUAL FRIENDS (uncomment below to use)
        
//        List<Person> mf = Graphs.mutualFriends(graph, ROOT_PERSON, TEST_PERSON);
//        Path path = Paths.get(DOWNLOADS_DIR, "r_t_mutual.txt");
//        Files.write(path, mf.toString().getBytes(),
//                StandardOpenOption.CREATE,
//                StandardOpenOption.TRUNCATE_EXISTING,
//                StandardOpenOption.WRITE);
        
        // CLIQUES (uncomment below to use)
        
        List<List<Person>> cliques = Graphs.maximalCliquesContaining(graph, ROOT_PERSON, 3);
        System.out.println("num cliques size >= 3: " + cliques.size());
        
        // filter out cliques that are very similar to cliques we already have
        List<List<Person>> filtered = new ArrayList<>();
        filtered.add(cliques.get(0));
        for (int i = 1; i < cliques.size(); i++) {
            List<Person> candidate = cliques.get(i);
            Set<Person> candidatePeople = new HashSet<>();
            for (Person p : candidate) {
                candidatePeople.add(p);
            }
            boolean distinct = true;
            for (int j = 0; j < filtered.size() && distinct; j++) {
                List<Person> c = filtered.get(j);
                int numSame = 0;
                for (Person p : c) {
                    if (candidatePeople.contains(p)) {
                        numSame++;
                    }
                }
                if (((double) numSame) / candidate.size() > 0.55) {
                    distinct = false;
                }
            }
            if (distinct) {
                filtered.add(candidate);
            }
        }
        cliques = filtered;
        
        // make the file not too large
        List<List<Person>> top10 = new ArrayList<>();
        for (int i = 0; i < 50; i++) {
            top10.add(cliques.get(i));
        }
        
        List<String> lines =
                top10.stream()
                .map(c -> c.toString())
                .collect(Collectors.toCollection(ArrayList<String>::new));
        Path path = Paths.get(DOWNLOADS_DIR, "r_cliquesv3.txt");
        Files.write(path, lines);
    }
    
    /**
     * Call this function with a directory to load all .friends files from that directory
     * and put the data into a graph.
     * @param dirpath The path to the directory containing the .friends files.
     * @return The resulting friends graph.
     */
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

    /**
     * Call this function to start downloading/gathering info from fb friends pages.
     * To setup, make sure your own friends page is open on chrome.
     * @return true if no error or interrupt occurred, false otherwise.
     * @throws IOException
     */
    public static boolean runHarvestAll() throws IOException {
        // give the user some time to set up the facebook page correctly
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        Harvester h;
        try {
            h = new Harvester(MAX_PAGES_TO_DOWNLOAD, MAX_PER_PERSON, DOWNLOADS_DIR, OUTPUT_DIR);
        } catch (AWTException e) {
            e.printStackTrace();
            System.out.println("Could not create Harvester");
            return false;
        }
        return h.beginNewHarvest();
    }
    
    /**
     * Call this function to resume the process of downloading pages which was initiated
     * by {@code runHarvestAll()} or a previous call to {@code resumeHarvest()}.
     * @return
     * @throws IOException
     */
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
