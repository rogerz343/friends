import java.awt.AWTException;
import java.awt.Color;
import java.awt.HeadlessException;
import java.awt.Toolkit;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * A class used to download html documents corresponding to a facebook user's "Friends" page,
 * with all of the html elements loaded. The class simply uses Java's Robot class to automate
 * the task and can be terminated by moving the mouse/cursor manually.
 * @author roger
 */
public class Harvester {
    
    // indicates whether or not this Harvester instance was created from a
    // previous save file or brand new
    private boolean isNewHarvester;
    
    // the user who was logged in during data collection, who is also the person
    // whose facebook Friends page this Harvester started on
    private Person rootPerson = null;
    
    public final int maxNumPeople;
    public final int maxPerPerson;
    public final String downloadsDir;
    public final String outputDir;
    
    private int numDownloaded;
    private Set<Person> finishedPeople;
    private Deque<Person> skippedPeople;    // skipped due to some error
    
    // inQueuePeople and downloadQueue have the same people, but we use
    // both to get a Deque with O(1) membership testing
    private Set<Person> inQueuePeople;
    private Deque<Person> downloadQueue;
    
    private IRWrapper robot;
    private final String logFilePath;
    
    private static String LOG_FILE = "harvester.log";
    private static String HARVESTER_STATE_FILE = "harvester.state";
    
    // number of seconds to wait before an operation is considered as failed
    private static int timeout = 180;
    
    private static int SCROLLBAR_X = 1910;
    private static int SCROLLBAR_Y = 972; // 1020 if downloads tab is closed
    private static Color SCROLLBAR_BOTTOM = new Color(192, 192, 192);
    
    private static int EMPTY_SPACE_X = 200;
    private static int EMPTY_SPACE_Y = 300;  
    
    /**
     * Constructs a brand new Harvester with the given parameters
     * @param maxNumPeople The maximum number of people to download pages from.
     * @param maxPerPerson The maximum number of friends extracted from each person's friends
     * page, ordered by however facebook orders friendship.
     * @param downloadsDir The filesystem path to the default chrome download directory.
     * @param outputDir The filesystem path to a directory where all of the output files that
     * result from this class's methods will be saved.
     * @throws AWTException
     * @throws IOException 
     */
    public Harvester(int maxNumPeople, int maxPerPerson,
            String downloadsDir, String outputDir) throws AWTException, IOException {
        this.maxNumPeople = maxNumPeople;
        this.maxPerPerson = maxPerPerson;
        this.downloadsDir = downloadsDir;
        this.outputDir = outputDir;
        
        logFilePath = Paths.get(outputDir, LOG_FILE).toString();
        Files.deleteIfExists(Paths.get(logFilePath));
        Files.createFile(Paths.get(logFilePath));
        
        numDownloaded = 0;
        finishedPeople = new HashSet<>();
        skippedPeople = new ArrayDeque<>();
        inQueuePeople = new HashSet<>();
        downloadQueue = new ArrayDeque<>();
        
        robot = new IRWrapper(true, 150);
        
        isNewHarvester = true;
        FriendsFiles.writeLog(logFilePath, "Initialized new Harvester.");
    }
    
    /**
     * Looks for a file in dir called HARVESTER_STATE_FILE and reads in the
     * state parameters. Constructs a new Harvester based on those parameters.
     * @param dir The directory containing the HARVESTER_STATE_FILE file.
     * @throws IOException 
     * @throws AWTException 
     */
    public Harvester(String dir) throws IOException, AWTException {
        Path stateFilePath = Paths.get(dir, HARVESTER_STATE_FILE);
        List<String> lines = Files.readAllLines(stateFilePath);
        
        this.maxNumPeople = Integer.parseInt(lines.get(0));
        this.maxPerPerson = Integer.parseInt(lines.get(1));
        this.downloadsDir = lines.get(2);
        this.outputDir = lines.get(3);
        this.rootPerson = Person.fromString(lines.get(4));
        logFilePath = Paths.get(outputDir, LOG_FILE).toString();
        
        this.numDownloaded = Integer.parseInt(lines.get(5));
        
        int startLine;
        int currLine = 6;
        
        int numFinished = Integer.parseInt(lines.get(currLine));
        currLine++;
        startLine = currLine;
        this.finishedPeople = new HashSet<>();
        for (; currLine < startLine + numFinished; currLine++) {
            finishedPeople.add(Person.fromString(lines.get(currLine)));
        }
        
        int numSkipped = Integer.parseInt(lines.get(currLine));
        currLine++;
        this.skippedPeople = new ArrayDeque<>();
        startLine = currLine;
        for (; currLine < startLine + numSkipped; currLine++) {
            skippedPeople.add(Person.fromString(lines.get(currLine)));
        }
        
        int numInQueue = Integer.parseInt(lines.get(currLine));
        currLine++;
        this.inQueuePeople = new HashSet<>();
        this.downloadQueue = new ArrayDeque<>();
        startLine = currLine;
        for (; currLine < startLine + numInQueue; currLine++) {
            Person p = Person.fromString(lines.get(currLine));
            inQueuePeople.add(p);
            downloadQueue.add(p);
        }
        
        robot = new IRWrapper(true, 150);
        
        isNewHarvester = false;
        FriendsFiles.writeLog(logFilePath, "Loaded Harvester from \"" + dir + "\".");
    }
    
    /**
     * Performs a breadth-first search of your friends network, starting at the person
     * whose profile page is open on chrome (and on screen) when this method is called.
     * Uses the instance variable parameters (those given when this Harvester was constructed).
     * @return true if no error occurred, false otherwise.
     */
    public boolean beginNewHarvest() {
        if (!isNewHarvester) {
            FriendsFiles.writeLog(logFilePath,
                    "beginNewHarvest(): err: attempted to call on a non-new Harvester");
            return false;
        }
        
        FriendsFiles.writeLog(logFilePath, "beginNewHarvest(): starting.");
        
        // first, download the information from the source (usually your own fb page)
        String rootUserHtmlName;
        try {
            rootUserHtmlName = harvestSingleFriendsPage();
        } catch (RobotInterruptedException e1) {
            FriendsFiles.writeLog(logFilePath, "beginNewHarvest(): robot interrupted. exiting.");
            e1.printStackTrace();
            return false;
        }
        
        // retrieve the html file when it is ready (when the download is complete)
        Path rootUserHtmlPath = Paths.get(downloadsDir, rootUserHtmlName).toAbsolutePath();
        if (!waitForDownload(rootUserHtmlPath, timeout)) { return false; }
        
        List<Person> rootUserFriends;
        try {
            rootUserFriends = FriendsHtmlParser.extractFriendsInfo(rootUserHtmlPath.toString(),
                    maxPerPerson, 30, null);
        } catch (FileNotFoundException e2) {
            e2.printStackTrace();
            return false;
        }
        Person rootUser = rootUserFriends.get(0);
        String outputFile = Paths.get(outputDir, rootUser.getUniqueKey() + ".friends")
                .toAbsolutePath()
                .toString();
        try {
            FriendsFiles.saveToFile(rootUserFriends, outputFile);
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        
        numDownloaded++;
        
        finishedPeople.add(rootUser);
        for (Person p : rootUserFriends) {
            if (!finishedPeople.contains(p) && !inQueuePeople.contains(p)) {
                inQueuePeople.add(p);
                downloadQueue.add(p);
            }
        }
        
        FriendsFiles.writeLog(logFilePath, "beginNewHarvest(): retrieved root Person info. done.");
        
        return harvestAllPages();
    }
    
    /**
     * Performs a breadth-first search of your friends network, with the parameters specified
     * by the instance variables. A facebook profile page should be open on the screen when
     * this method is called. For each Person in `downloadQueue`, this method downloads that
     * person's Friends page (.html file). Then, this method will save a file (corresponding
     * to that Person and his/her friends) to `outputDir`. The process is repeated for all of
     * this person's friends. The state of this Harvester is periodically saved as a file in
     * `outputDir` so that it the data processing can be interrupted and resume later on.
     * @return true if no error occurred, false otherwise.
     */
    public boolean harvestAllPages() {
        FriendsFiles.writeLog(logFilePath, "harvestAllPages(): starting.");
        int numLoops = 0;
        while (numDownloaded < maxNumPeople && !downloadQueue.isEmpty()) {
            // state state every now and then
            if (numLoops % 5 == 0) {
                try {
                    FriendsFiles.writeLog(logFilePath,
                            "harvestAllPages(): saving Harvester state.");
                    saveHarvester();
                } catch (IOException e) {
                    // could not save Harvester state for some reason, just continue
                    // and hopefully it'll save correctly during the next loop
                    FriendsFiles.writeLog(logFilePath, "harvestAllPages(): IOException thrown "
                            + "by saveHarvester(). Moving on without saving.");
                    e.printStackTrace();
                }
            }
            numLoops++;
            
            // get information about user
            Person user = downloadQueue.peek();
            
            // used for human-readable log file messages
            String userSummary = "[" + user.getName() + " (" + user.getBaseUrl() + ")]";
            
            FriendsFiles.writeLog(logFilePath, "harvestAllPages(): retrieving info for "
                    + userSummary + ".");
            
            if (finishedPeople.contains(user)) {
                FriendsFiles.writeLog(logFilePath, "harvestAllPages(): already previously "
                        + "retrieved info for " + userSummary + ". Skipping.");
                inQueuePeople.remove(user);
                downloadQueue.remove();
                continue;
            }
            
            String userHtmlName;
            try {
                userHtmlName = harvestSingleFriendsPage(user);
            } catch (RobotInterruptedException e1) {
                FriendsFiles.writeLog(logFilePath, "harvestAllPages(): robot interrupted. exiting.");
                e1.printStackTrace();
                return false;
            }
            
            // retrieve the html file when it is ready
            Path userHtmlPath = Paths.get(downloadsDir, userHtmlName).toAbsolutePath();
            if (!waitForDownload(userHtmlPath, timeout)) {
                FriendsFiles.writeLog(logFilePath, "harvestAllPages(): waitForDownload() "
                        + "timed out for user " + userSummary + ". Skipping.");
                skippedPeople.add(user);
                inQueuePeople.remove(user);
                downloadQueue.remove();
                continue;
            }
            
            List<Person> userFriends;
            try {
                userFriends = FriendsHtmlParser.extractFriendsInfo(userHtmlPath.toString(),
                        maxPerPerson, 30, rootPerson);
            } catch (FileNotFoundException e) {
                // could not open this user's profile: just skip this person
                // without adding to finishedPeople
                e.printStackTrace();
                FriendsFiles.writeLog(logFilePath, "harvestAllPages(): could not open .html file "
                        + "for user " + userSummary + ". Skipping.");
                skippedPeople.add(user);
                inQueuePeople.remove(user);
                downloadQueue.remove();
                continue;
            }
            String outputFile = Paths.get(outputDir, user.getUniqueKey() + ".friends")
                    .toAbsolutePath()
                    .toString();
            try {
                if (!FriendsFiles.saveToFile(userFriends, outputFile)) {
                    FriendsFiles.writeLog(logFilePath,
                            "harvestAllPages(): " + outputFile
                            + " already exists. Now using existing file.");
                    userFriends = FriendsFiles.loadFromFile(outputFile);
                }
            } catch (IOException e) {
                // could not open this user's profile: just skip this person
                // without adding to finishedPeople
                e.printStackTrace();
                FriendsFiles.writeLog(logFilePath, "harvestAllPages(): could not load "
                        + ".friends file for user " + userSummary + ". Skipping.");
                skippedPeople.add(user);
                inQueuePeople.remove(user);
                downloadQueue.remove();
                continue;
            }
            
            finishedPeople.add(user);
            int numAdded = 0;
            for (Person p : userFriends) {
                if (!finishedPeople.contains(p) && !inQueuePeople.contains(p)) {
                    inQueuePeople.add(p);
                    downloadQueue.add(p);
                }
                
                // placed outside of previous if statement to ensure max degree maxPerPerson
                numAdded++;
                if (numAdded >= maxPerPerson) { break; }
            }
            numDownloaded++;
            
            // reaching here means robot was not interrupted and everything went correctly
            
            FriendsFiles.writeLog(logFilePath, "harvestAllPages(): successfully retrieved.");
            inQueuePeople.remove(user);
            downloadQueue.remove();
            
            // TODO: delete the .js, .css, etc. source folders associated with the html document
        }
        if (downloadQueue.isEmpty()) {
            FriendsFiles.writeLog(logFilePath, "harvestAllPages(): finished because "
                    + "downloadQueue is empty.");
        } else {
            FriendsFiles.writeLog(logFilePath, "harvestAllPages(): finished because "
                    + "maxNumPeople have been retrieved.");
        }
        return true;
    }
    
    /**
     * An alias for the Harvester(String dir) constructor.
     * @param dir The parameter to pass into the Harvester(String dir) constructor.
     * @return A new Harvester constructed using the `dir` parameter.
     * @throws IOException 
     * @throws AWTException 
     */
    public static Harvester loadHarvester(String dir) throws IOException, AWTException {
        return new Harvester(dir);
    }
    
    /**
     * Saves all of the necessary parameters of this Harvester to a file that
     * can be read in by the Harvester(String dir) constructor
     * @throws IOException
     */
    private void saveHarvester() throws IOException {
        Path harvestProgressPath = Paths.get(outputDir, HARVESTER_STATE_FILE);
                
        List<String> lines = new ArrayList<>();
        lines.add(Integer.toString(maxNumPeople));
        lines.add(Integer.toString(maxPerPerson));
        lines.add(downloadsDir);
        lines.add(outputDir);
        lines.add(rootPerson.toString());
        
        lines.add(Integer.toString(numDownloaded));
        lines.add(Integer.toString(finishedPeople.size()));
        
        for (Person p : finishedPeople) {
            lines.add(p.toString());
        }
        lines.add(Integer.toString(skippedPeople.size()));
        for (Person p : skippedPeople) {
            lines.add(p.toString());
        }
        lines.add(Integer.toString(downloadQueue.size()));
        for (Person p : downloadQueue) {
            lines.add(p.toString());
        }
        
        Files.write(harvestProgressPath, lines,
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING,
                StandardOpenOption.WRITE);
    }
    
    /**
     * Blocks the program until the file specified by filepath exists.
     * @param filepath The path to the file that we want to exist.
     * @param timeout The number of seconds that the program will wait for
     * before timing out and returning an error.
     * @return true if the file is found, false otherwise.
     */
    private static boolean waitForDownload(Path filepath, int timeout) {
        long startTime = System.nanoTime();
        
        // probably don't need Files.isReadable() condition, but hey why not
        while ((System.nanoTime() - startTime) / 1000000000L < timeout &&
                !Files.exists(filepath)) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        if (!Files.exists(filepath)) { return false; }
        return true;
    }
    
    // TODO: add error checking
    /**
     * Downloads the html document of a facebook user's Friends page, with all
     * friends loaded, given that the current window is a Friends page.
     * This method has several assumptions in order to operate correctly:
     * - the current page is the Friends page of some facebook user
     * - the display is a 1920 x 1080, running windows with 100% scaling
     * - the windows taskbar is at the default height
     * - chrome is maximized on the screen
     * - chrome's bookmark bar is visible
     * - chrome's zoom level is default (100%)
     * - chrome's downloads bar (at the bottom of the page) is OPEN
     * - chrome's developer tools panel is CLOSED
     * @return The name of the html file that was saved, or null if an error occurred.
     * @throws RobotInterruptedException 
     */
    public String harvestSingleFriendsPage() throws RobotInterruptedException {
        scrollToBottom(300);
        return fetchHtml();
    }
    
    /**
     * Downloads the html document of a facebook user's Friends page, with all
     * friends loaded.
     * @param person The Person whose Friends page html document we want to harvest.
     * @return The name of the html file that was saved, or null if an error occurred.
     * @throws RobotInterruptedException 
     */
    public String harvestSingleFriendsPage(Person person) throws RobotInterruptedException {
        viewFriendsPage(person);
        scrollToBottom(300);
        return fetchHtml();
    }
    
    /**
     * Assumes that the current page is a facebook user's main profile page and
     * navigates to the Friends page, given the person.
     * @param person The Person whose Friends page we want to navigate to.
     * @return 0 if no error occurred, 1 otherwise.
     * @throws RobotInterruptedException 
     */
    private int viewFriendsPage(Person person) throws RobotInterruptedException {
        // make sure window is in focus
        robot.mouseMove(EMPTY_SPACE_X, EMPTY_SPACE_Y);
        robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
        robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
        
        // edit chrome's address bar
        robot.keyPress(KeyEvent.VK_CONTROL);
        robot.keyPress(KeyEvent.VK_L);
        robot.keyRelease(KeyEvent.VK_L);
        robot.keyRelease(KeyEvent.VK_CONTROL);
        robot.keyPress(KeyEvent.VK_BACK_SPACE);
        robot.keyRelease(KeyEvent.VK_BACK_SPACE);
        
        // put next person's page into chrome address
        String urlFriendsPage = person.getFriendsPageUrl();
        blockingCopy(urlFriendsPage);
        
        robot.keyPress(KeyEvent.VK_CONTROL);
        robot.keyPress(KeyEvent.VK_V);
        robot.keyRelease(KeyEvent.VK_V);
        robot.keyRelease(KeyEvent.VK_CONTROL);
        
        robot.keyPress(KeyEvent.VK_ENTER);
        robot.keyRelease(KeyEvent.VK_ENTER);
        
        // give the page some time to load
        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return 0;
    }
    
    /**
     * Scrolls the page to the bottom. May occasionally not make it all the way
     * to the bottom (ex: in a dynamically loading webpage that takes too long to load
     * new data)
     * @param timeout The maximum number of seconds that this method is allowed to
     * be active for before returning. As a (very approximate) reference, it takes about
     * 100 seconds to scroll down a friends page that contains 1200 friends.
     * @return true if no error (including timeout) occurred, false otherwise.
     * @throws RobotInterruptedException 
     */
    private boolean scrollToBottom(long timeout) throws RobotInterruptedException {        
        long startTime = System.nanoTime();
        while ((System.nanoTime() - startTime) / 1000000000 < timeout) {
            robot.keyPress(KeyEvent.VK_PAGE_DOWN);
            robot.keyRelease(KeyEvent.VK_PAGE_DOWN);
            robot.keyPress(KeyEvent.VK_PAGE_DOWN);
            robot.keyRelease(KeyEvent.VK_PAGE_DOWN);
            robot.keyPress(KeyEvent.VK_PAGE_DOWN);
            robot.keyRelease(KeyEvent.VK_PAGE_DOWN);
            try {
                Thread.sleep(600);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            Color sample = robot.getPixelColor(SCROLLBAR_X, SCROLLBAR_Y);
            if (almostEquals(sample, SCROLLBAR_BOTTOM, 10)) {
                // double check
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                sample = robot.getPixelColor(SCROLLBAR_X, SCROLLBAR_Y);
                if (almostEquals(sample, SCROLLBAR_BOTTOM, 10)) {
                    // triple check
                    try {
                        Thread.sleep(3000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    sample = robot.getPixelColor(SCROLLBAR_X, SCROLLBAR_Y);
                    if (almostEquals(sample, SCROLLBAR_BOTTOM, 10)) {
                        return true;
                    }
                }
            }
            // else, we are either not at the bottom or some other on-screen element
            // got in the way. in any case, just keep going
        }
        return false;
    }
    
    /**
     * Tests whether two colors are the same within the given margin.
     * @param c1 The first color.
     * @param c2 The second color.
     * @param margin The maximum value that each rgb component (on a scale of 0-255)
     * can differ by to be considered equal (close enough)
     * @return true if each of the rgb color channels (on a scale of 0-255) of c1 and
     * c2 differ by at most margin.
     */
    private boolean almostEquals(Color c1, Color c2, int margin) {
        return Math.abs(c1.getRed() - c2.getRed()) <= margin
                && Math.abs(c1.getGreen() - c2.getGreen()) <= margin
                && Math.abs(c1.getBlue() - c2.getBlue()) <= margin;
    }
    
    /**
     * Saves the dynamically generated html file from the current web page as an html file
     * in the current working directory. Assumes that chrome is in focus and is on the correct
     * webpage. Also assumes that chrome's save "Webpage, complete" saves the dynamically generated
     * html (not the source code).
     * @return The filename of the html file that was downloaded, or null if an error occurred.
     * @throws RobotInterruptedException 
     */
    private String fetchHtml() throws RobotInterruptedException {
        robot.mouseMove(EMPTY_SPACE_X, EMPTY_SPACE_Y);
        robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
        robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
        
        robot.keyPress(KeyEvent.VK_CONTROL);
        robot.keyPress(KeyEvent.VK_S);
        robot.keyRelease(KeyEvent.VK_S);
        robot.keyRelease(KeyEvent.VK_CONTROL);
        
        // kind of hack-y: set the filename to be a random number such that the
        // program is practically never going to assign the html files of two different
        // people to be the same name
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        robot.keyPress(KeyEvent.VK_BACK_SPACE);
        robot.keyRelease(KeyEvent.VK_BACK_SPACE);
        String htmlFilename = "";
        for (int i = 0; i < 20; i++) {
            char randomChar = (char) ((int) (Math.random() * 26) + 'a');
            htmlFilename += randomChar;
        }
        htmlFilename += ".html";
        
        // copy and paste htmlFilename into chrome's save dialog
        blockingCopy(htmlFilename);
        
        robot.keyPress(KeyEvent.VK_CONTROL);
        robot.keyPress(KeyEvent.VK_V);
        robot.keyRelease(KeyEvent.VK_V);
        robot.keyRelease(KeyEvent.VK_CONTROL);
        
        // save the complete webpage (in order to save the dynamically generated html document)
        robot.keyPress(KeyEvent.VK_TAB);
        robot.keyRelease(KeyEvent.VK_TAB);
        robot.keyPress(KeyEvent.VK_DOWN);
        robot.keyRelease(KeyEvent.VK_DOWN);
        robot.keyPress(KeyEvent.VK_DOWN);
        robot.keyRelease(KeyEvent.VK_DOWN);
        robot.keyPress(KeyEvent.VK_ENTER);
        robot.keyRelease(KeyEvent.VK_ENTER);
        robot.keyPress(KeyEvent.VK_ENTER);
        robot.keyRelease(KeyEvent.VK_ENTER);
        return htmlFilename;
    }
    
    /**
     * Copies the given String to the clipboard, only returning after the String has completed
     * copying.
     * @param s The string to copy to system clipboard.
     * @return true if no error occurred, false otherwise.
     */
    private boolean blockingCopy(String s) {
        StringSelection ss = new StringSelection(s);
        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(ss, ss);
        
        String clipboardStr;
        try {
            clipboardStr = (String) Toolkit.getDefaultToolkit()
                    .getSystemClipboard().getData(DataFlavor.stringFlavor);
        } catch (HeadlessException | UnsupportedFlavorException | IOException e) {
            e.printStackTrace();
            return false;
        }
        while (!clipboardStr.equals(s)) {
            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(ss, ss);
            try {
                clipboardStr = (String) Toolkit.getDefaultToolkit()
                        .getSystemClipboard().getData(DataFlavor.stringFlavor);
            } catch (HeadlessException | UnsupportedFlavorException | IOException e) {
                e.printStackTrace();
                return false;
            }
        }
        return true;
    }
}