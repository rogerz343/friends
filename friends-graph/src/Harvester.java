import java.awt.AWTException;
import java.awt.Color;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
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
import java.util.stream.Collectors;

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
    
    public final int maxNumPeople;
    public final int maxPerPerson;
    public final String downloadsDir;
    public final String outputDir;
    
    private int numDownloaded;
    private Set<Person> finishedPeople;
    private Deque<Person> downloadQueue;
    
    private InterruptibleRobot robot;
    
    private static String LOG_FILE = "harvester.log";
    private static String HARVESTER_STATE_FILE = "harvester.state";
    
    // number of seconds to wait before an operation is considered as failed
    private static int timeout = 180;
    
    private static int SCROLLBAR_X = 1910;
    private static int SCROLLBAR_Y = 972; // 1020 if downloads tab is closed
    private static Color SCROLLBAR_BOTTOM = new Color(192, 192, 192);
    
    private static int EMPTY_SPACE_X = 200;
    private static int EMPTY_SPACE_Y = 300;  
    
    // time to wait for Windows to paste text to and from clipboard, in milliseconds
    // not sure if we actually need this
//    private static int WAIT_TIME_AFTER_CTRL_V = 500;
    
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
        
        Path logFilePath = Paths.get(outputDir, LOG_FILE);
        Files.deleteIfExists(logFilePath);
        Files.createFile(logFilePath);
        
        numDownloaded = 0;
        finishedPeople = new HashSet<>();
        downloadQueue = new ArrayDeque<>();
        
        robot = new InterruptibleRobot();
        robot.setAutoWaitForIdle(true);
        robot.setAutoDelay(200);
        
        isNewHarvester = true;
    }
    
    /**
     * Looks for a file in dir called HARVESTER_STATE_FILE and reads in the
     * state parameters. Constructs a new Harvester based on those parameters.
     * @param dir The directory containing the HARVESTER_STATE_FILE file.
     * @throws IOException 
     */
    public Harvester(String dir) throws IOException {
        Path stateFilePath = Paths.get(dir, HARVESTER_STATE_FILE);
        List<String> lines = Files.readAllLines(stateFilePath);
        
        this.maxNumPeople = Integer.parseInt(lines.get(0));
        this.maxPerPerson = Integer.parseInt(lines.get(1));
        this.downloadsDir = lines.get(2);
        this.outputDir = lines.get(3);
        this.numDownloaded = Integer.parseInt(lines.get(4));
        
        int startLine;
        int currLine = 5;
        
        int numFinished = Integer.parseInt(lines.get(currLine));
        currLine++;
        startLine = currLine;
        this.finishedPeople = new HashSet<>();
        for (currLine = startLine; currLine < startLine + numFinished; currLine++) {
            finishedPeople.add(Person.fromString(lines.get(currLine)));
        }
        
        int numInQueue = Integer.parseInt(lines.get(currLine));
        currLine++;
        this.downloadQueue = new ArrayDeque<>();
        startLine = currLine;
        for (; currLine < startLine + numInQueue; currLine++) {
            downloadQueue.add(Person.fromString(lines.get(currLine)));
        }
        
        isNewHarvester = false;
    }
    
    /**
     * Performs a breadth-first search of your friends network, starting at the person
     * whose profile page is open on chrome (and on screen) when this method is called.
     * Uses the instance variable parameters (those given when this Harvester was constructed).
     * @return true if no error occurred, false otherwise.
     */
    public boolean beginNewHarvest() {
        if (!isNewHarvester) {
            return false;
        }
        
        // first, download the information from the source (usually your own fb page)
        String rootUserHtmlName = harvestSingleFriendsPage();
        
        // retrieve the html file when it is ready (when the download is complete)
        Path rootUserHtmlPath = Paths.get(downloadsDir, rootUserHtmlName).toAbsolutePath();
        if (!waitForDownload(rootUserHtmlPath, timeout)) { return false; }
        
        List<Person> rootUserFriends;
        try {
            rootUserFriends = FriendsHtmlParser.extractFriendsInfo(rootUserHtmlPath.toString(), maxPerPerson, 30);
        } catch (FileNotFoundException e2) {
            e2.printStackTrace();
            return false;
        }
        Person rootUser = rootUserFriends.get(0);
        String outputFile = Paths.get(outputDir, rootUser.getUniqueKey() + ".friends").toAbsolutePath().toString();
        try {
            FriendsFiles.saveToFile(rootUserFriends, outputFile);
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        
        numDownloaded++;
        
        finishedPeople.add(rootUser);
        for (Person p : rootUserFriends) {
            if (!finishedPeople.contains(p)) { downloadQueue.add(p); }
        }
        return harvestAllPages();
    }
    
    // TODO: add error checking
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
        while (numDownloaded < maxNumPeople && !downloadQueue.isEmpty()) {
            Person user = downloadQueue.peek();
            if (finishedPeople.contains(user)) {
                downloadQueue.remove();
                continue;
            }
            
            String userHtmlName = harvestSingleFriendsPage(user);
            
            // retrieve the html file when it is ready
            Path userHtmlPath = Paths.get(downloadsDir, userHtmlName).toAbsolutePath();
            if (!waitForDownload(userHtmlPath, timeout)) {
                // waiting for download timed out; we could continue,
                // but downloads timing out usually means something major is wrong
                // so just return
                return false;
            }
            
            List<Person> userFriends;
            try {
                userFriends = FriendsHtmlParser.extractFriendsInfo(userHtmlPath.toString(), maxPerPerson, 30);
            } catch (FileNotFoundException e1) {
                // could not open this user's profile: just skip this person
                // without adding to finishedPeople
                e1.printStackTrace();
                downloadQueue.remove();
                continue;
            }
            String outputFile = Paths.get(outputDir, user.getUniqueKey() + ".friends").toAbsolutePath().toString();
            try {
                if (!FriendsFiles.saveToFile(userFriends, outputFile)) {
                    System.out.println("harvestAllPages(): "
                            + outputFile
                            + " already exists. Now using existing file.");
                    userFriends = FriendsFiles.loadFromFile(outputFile);
                }
            } catch (IOException e) {
                // could not open this user's profile: just skip this person
                // without adding to finishedPeople
                e.printStackTrace();
                downloadQueue.remove();
                continue;
            }
            
            finishedPeople.add(user);
            int numAdded = 0;
            for (Person p : userFriends) {
                if (!finishedPeople.contains(p)) { downloadQueue.add(p); }
                
                // placed outside of the if statement so nodes will have max degree maxPerPerson
                numAdded++;
                if (numAdded >= maxPerPerson) { break; }
            }
            numDownloaded++;
            
            // if robot was interrupted, don't go on to remove the current user from
            // the queue and don't save the state.
            if (robot.interrupted) {
                return false;
            }
            
            // reaching here means everything went correctly
            
            // remove the person from the queue
            downloadQueue.remove();
            
            // state state every now and then
            if (numDownloaded % 5 == 0) {
                try {
                    saveHarvester();
                } catch (IOException e) {
                    // could not save Harvester state for some reason, just continue
                    // and hopefully it'll save correctly during the next loop
                    e.printStackTrace();
                }
            }
            
            // TODO: delete the .js, .css, etc. source folders associated with the html document
        }
        return true;
    }
    
    /**
     * An alias for the Harvester(String dir) constructor.
     * @param dir The parameter to pass into the Harvester(String dir) constructor.
     * @return A new Harvester constructed using the `dir` parameter.
     * @throws IOException 
     */
    public static Harvester loadHarvester(String dir) throws IOException {
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
        lines.add(Integer.toString(numDownloaded));
        lines.add(Integer.toString(finishedPeople.size()));
        for (Person p : finishedPeople) {
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
        int secondsWaited = 0;  // not the most accurate, but good enough
        
        // probably don't need Files.isReadable() condition, but hey why not
        while ((!Files.exists(filepath) || !Files.isReadable(filepath))
                && secondsWaited < timeout) {
            try {
                Thread.sleep(1000);
                secondsWaited++;
                
                // TODO: remove this; used for debugging
                if (secondsWaited > 12) {
                    System.out.println("Waited " + secondsWaited + " seconds for download.");
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        if (secondsWaited >= timeout) { return false; }
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
     */
    public String harvestSingleFriendsPage() {
        scrollToBottom();
        return fetchHtml();
    }
    
    /**
     * Downloads the html document of a facebook user's Friends page, with all
     * friends loaded.
     * @param person The Person whose Friends page html document we want to harvest.
     * @return The name of the html file that was saved, or null if an error occurred.
     */
    public String harvestSingleFriendsPage(Person person) {
        viewFriendsPage(person);
        scrollToBottom();
        return fetchHtml();
    }
    
    /**
     * Assumes that the current page is a facebook user's main profile page and
     * navigates to the Friends page, given the person.
     * @param person The Person whose Friends page we want to navigate to.
     * @return 0 if no error occurred, 1 otherwise.
     */
    private int viewFriendsPage(Person person) {
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
        StringSelection ss = new StringSelection(urlFriendsPage);
        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(ss, ss);
        
        robot.keyPress(KeyEvent.VK_CONTROL);
        robot.keyPress(KeyEvent.VK_V);
        robot.keyRelease(KeyEvent.VK_V);
        robot.keyRelease(KeyEvent.VK_CONTROL);
        
//        // not sure if we need this
//        try {
//            Thread.sleep(WAIT_TIME_AFTER_CTRL_V);
//        } catch (InterruptedException e) {
//            e.printStackTrace();
//        }
        
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
     * @return 0 if no error occurred. 1 otherwise.
     */
    private int scrollToBottom() {
        robot.mouseMove(0, 0);
        long startTime = System.nanoTime();
        long timeout = 90;      // number of seconds before we consider this method as failing
        boolean done = false;
        while (!done) {
            if ((System.nanoTime() - startTime) / 1000000000 >= timeout) { return 1; }
            robot.keyPress(KeyEvent.VK_PAGE_DOWN);
            robot.keyRelease(KeyEvent.VK_PAGE_DOWN);
            robot.keyPress(KeyEvent.VK_PAGE_DOWN);
            robot.keyRelease(KeyEvent.VK_PAGE_DOWN);
            robot.keyPress(KeyEvent.VK_PAGE_DOWN);
            robot.keyRelease(KeyEvent.VK_PAGE_DOWN);
            try {
                Thread.sleep(700);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            Color sample = robot.getPixelColor(SCROLLBAR_X, SCROLLBAR_Y);
            if (almostEquals(sample, SCROLLBAR_BOTTOM, 10)) {
                // double check
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                sample = robot.getPixelColor(SCROLLBAR_X, SCROLLBAR_Y);
                if (almostEquals(sample, SCROLLBAR_BOTTOM, 10)) {
                    done = true;
                }
            }
            // else, we are either not at the bottom or some other on-screen element
            // got in the way. in any case, just keep going
            
            // TODO: TEMP: JUST FOR PERSONAL TESTING; REMOVE THIS LATER
            if ((System.nanoTime() - startTime) / 1000000000L >= 10) { return 0; }
        }
        return 0;
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
     * in the current working directory. Assumes that chrome is in focus and is on the correct webpage.
     * Assumes that chrome's save "Webpage, complete" saves the dynamically generated html (not the source code).
     * @return The filename of the html file that was downloaded, or null if an error occurred.
     * TODO: temp: there is currently a bug in which the wrong String is returned, perhaps due to
     * the retrieving the incorrect string from the clipboard when assigning htmlFilename
     */
    private String fetchHtml() {
        robot.mouseMove(EMPTY_SPACE_X, EMPTY_SPACE_Y);
        robot.mousePress(InputEvent.BUTTON2_DOWN_MASK);
        robot.mouseRelease(InputEvent.BUTTON2_DOWN_MASK);
        
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
        StringSelection ss = new StringSelection(htmlFilename);
        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(ss, ss);
        
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
}