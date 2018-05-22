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
import java.util.ArrayDeque;
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
    
    private InterruptibleRobot robot;
    
    private static int SCROLLBAR_X = 1910;
    private static int SCROLLBAR_Y = 972; // 1020 if downloads tab is closed
    private static Color SCROLLBAR_NOT_BOTTOM = new Color(241, 241, 241);
    private static Color SCROLLBAR_BOTTOM = new Color(192, 192, 192);
    
    private static int EMPTY_SPACE_X = 200;
    private static int EMPTY_SPACE_Y = 300;  
    
    // time to wait for Windows to copy and paste text to and from clipboard, respectively, in milliseconds
    private static int WAIT_TIME_AFTER_CTRL_C = 2000;
    private static int WAIT_TIME_AFTER_CTRL_V = 500;
    
    public Harvester() throws AWTException {
        robot = new InterruptibleRobot();
        robot.setAutoWaitForIdle(true);
        robot.setAutoDelay(100);
    }
    
    // TODO: add error checking
    /**
     * Performs a breadth-first search of your friends network starting with whoever's profile
     * main page is currently open on the screen (usually your own). For each person, this
     * method downloads their Friends page .html file. Additionally, this method will save
     * files corresponding to a Person and their friends (using FriendsParser.saveToFile)
     * @param maxNumPeople The maximum number of people to download pages from. Cannot be
     * larger than 10000000.
     * @param maxPerPerson The maximum number of friends extracted from each person's friends page.
     * For example, if this is set to 200, then each person will have 200 friends. Note that
     * facebook seems to already sort friends by some measure of "closeness" or "interaction", so
     * this will gather the `maxPerPerson` "best" friends, which is beneficial in some cases.
     * @param downloadsDir The filesystem path to the default chrome download directory
     * @param outputDir The filesystem path to a directory where the output files (lists
     * of a user and his or her friends) will be saved.
     * @return true if no error occurred, false otherwise.
     * 
     */
    public boolean harvestAllPages(int maxNumPeople, int maxPerPerson, String downloadsDir, String outputDir) {
        int timeout = 180;
        
        // first, download the information from the source (usually your own fb page)
        String rootUserHtmlName = harvestSingleFriendsPage();
        
        // retrieve the html file when it is ready (when the download is complete)
        Path rootUserHtmlPath = Paths.get(downloadsDir, rootUserHtmlName).toAbsolutePath();
        if (!waitForDownload(rootUserHtmlPath, timeout)) { return false; }
        
        List<Person> rootUserFriends;
        try {
            rootUserFriends = FriendsParser.extractFriendsInfo(rootUserHtmlPath.toString(), maxPerPerson);
        } catch (FileNotFoundException e2) {
            e2.printStackTrace();
            return false;
        }
        Person rootUser = rootUserFriends.get(0);
        String outputFile = Paths.get(outputDir, rootUser.id).toAbsolutePath().toString();
        try {
            FriendsParser.saveToFile(rootUserFriends, outputFile);
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        
        Set<Person> finishedPeople = new HashSet<>();
        finishedPeople.add(rootUser);
        Deque<Person> downloadQueue = new ArrayDeque<>();
        for (Person p : rootUserFriends) {
            if (!finishedPeople.contains(p)) { downloadQueue.add(p); }
        }
        int numDownloaded = 1;
        while (numDownloaded < maxNumPeople && !downloadQueue.isEmpty()) {
            Person user = downloadQueue.remove();
            if (finishedPeople.contains(user)) { continue; }
            
            String userHtmlName = harvestSingleFriendsPage(user.url);
            
            // retrieve the html file when it is ready
            Path userHtmlPath = Paths.get(downloadsDir, userHtmlName).toAbsolutePath();
            if (!waitForDownload(userHtmlPath, timeout)) { return false; }
            
            // TODO: TEMP: remove this. figure out how to actually wait for a file to completely download
            // and be ready to use
            try {
                Thread.sleep(15 * 1000);
            } catch (InterruptedException e1) {
                e1.printStackTrace();
            }
            
            List<Person> userFriends;
            try {
                userFriends = FriendsParser.extractFriendsInfo(userHtmlPath.toString(), maxPerPerson);
            } catch (FileNotFoundException e1) {
                // could not open this user's profile: just skip this person
                e1.printStackTrace();
                continue;
            }
            outputFile = Paths.get(outputDir, user.id).toAbsolutePath().toString();
            try {
                FriendsParser.saveToFile(userFriends, outputFile);
            } catch (IOException e) {
                e.printStackTrace();
                return false;
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
        }
        return true;
    }
    
    /**
     * Blocks the program until the file specified by filepath exists.
     * @param filepath The path to the file that we want to exist.
     * @param timeout The number of seconds that the program will wait for
     * before timing out and returning an error.
     * @return true if the file is found, false otherwise.
     */
    private boolean waitForDownload(Path filepath, int timeout) {
        int secondsWaited = 0;  // not the most accurate, but good enough
        
        // probably don't need Files.isReadable() condition, but hey why not
        while (!Files.exists(filepath)
                && Files.isReadable(filepath)
                && secondsWaited < timeout) {
            try {
                Thread.sleep(1000);
                secondsWaited++;
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        if (secondsWaited >= timeout) { return false; }
        return true;
    }
    
    // TODO: add error checking
    /**
     * Downloads the html document of a facebook user's "Friends" page. Has several
     * assumptions in order to operate correctly:
     * - the display is a 1920 x 1080, running windows with 100% scaling
     * - the windows taskbar is at the default height
     * - chrome is maximized on the screen
     * - chrome's bookmark bar is visible
     * - chrome's zoom level is default (100%)
     * - chrome's downloads bar (at the bottom of the page) is OPEN
     * - chrome's developer tools panel is CLOSED
     * - the current page is the main page of a facebook profile
     * @return The name of the html file that was saved, or null if an error occurred.
     */
    public String harvestSingleFriendsPage() {
        viewFriendsPage();        
        scrollToBottom();
        return fetchHtml();
    }
    
    /**
     * Same as harvestSingleFriendsPage(), but you can pass in a url to the person's
     * Friends page.
     * @param url The url to the person's Friends page.
     * @return The name of the html file that was saved, or null if an error occurred.
     */
    public String harvestSingleFriendsPage(String url) {
        viewFriendsPage(url);
        scrollToBottom();
        return fetchHtml();
    }
    
    /**
     * Assumes that the current page is a facebook user's profile page and
     * navigagtes to the friends page, given its url.
     * @param url The url of the Friends page of the desired person
     * @return 0 if no error occurred, 1 otherwise.
     */
    private int viewFriendsPage(String url) {
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
        String urlFriendsPage = FriendsParser.getFriendsPageUrl(url);
        StringSelection ss = new StringSelection(urlFriendsPage);
        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(ss, ss);
        
        robot.keyPress(KeyEvent.VK_CONTROL);
        robot.keyPress(KeyEvent.VK_V);
        robot.keyRelease(KeyEvent.VK_V);
        robot.keyRelease(KeyEvent.VK_CONTROL);
        
        try {
            Thread.sleep(WAIT_TIME_AFTER_CTRL_V);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        
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
     * Assumes that the current page is the main page for a specific person
     * and navigates to friends page
     * @return 0 if no error occurred. 1 if could not locate friends page button.
     * 2 if other error occurred.
     */
    private int viewFriendsPage() {
        // make sure window is in focus
        robot.mouseMove(EMPTY_SPACE_X, EMPTY_SPACE_Y);
        robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
        robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
        
        // copy the current chrome address bar
        robot.keyPress(KeyEvent.VK_CONTROL);
        robot.keyPress(KeyEvent.VK_L);
        robot.keyRelease(KeyEvent.VK_L);
        robot.keyRelease(KeyEvent.VK_CONTROL);
        robot.keyPress(KeyEvent.VK_CONTROL);
        robot.keyPress(KeyEvent.VK_C);
        robot.keyRelease(KeyEvent.VK_C);
        robot.keyRelease(KeyEvent.VK_CONTROL);
        try {
            Thread.sleep(WAIT_TIME_AFTER_CTRL_C);
        } catch (InterruptedException e) {
            e.printStackTrace();
            return 2;
        }
        
        String url = null;
        try {
            url = (String) Toolkit.getDefaultToolkit()
                    .getSystemClipboard().getData(DataFlavor.stringFlavor);
        } catch (HeadlessException | UnsupportedFlavorException | IOException e) {
            e.printStackTrace();
            return 2;
        }
        if (url == null) { return 2; }
        
        String baseUrl = FriendsParser.getBaseUrl(url);
        String friendsPageUrl = FriendsParser.getFriendsPageUrl(baseUrl);
        
        StringSelection ss = new StringSelection(friendsPageUrl);
        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(ss, ss);
        
        robot.keyPress(KeyEvent.VK_CONTROL);
        robot.keyPress(KeyEvent.VK_L);
        robot.keyRelease(KeyEvent.VK_L);
        robot.keyRelease(KeyEvent.VK_CONTROL);
        
        robot.keyPress(KeyEvent.VK_CONTROL);
        robot.keyPress(KeyEvent.VK_V);
        robot.keyRelease(KeyEvent.VK_V);
        robot.keyRelease(KeyEvent.VK_CONTROL);
        
        try {
            Thread.sleep(WAIT_TIME_AFTER_CTRL_V);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        
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
            if ((System.nanoTime() - startTime) / 1000000000 >= 30) { return 0; }
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
     * @return The filename of the html file that was downloaded, or null if an error occured.
     */
    private String fetchHtml() {
         
        robot.mouseMove(EMPTY_SPACE_X, EMPTY_SPACE_Y);
        robot.mousePress(InputEvent.BUTTON2_DOWN_MASK);
        robot.mouseRelease(InputEvent.BUTTON2_DOWN_MASK);
        
        robot.keyPress(KeyEvent.VK_CONTROL);
        robot.keyPress(KeyEvent.VK_S);
        robot.keyRelease(KeyEvent.VK_S);
        robot.keyRelease(KeyEvent.VK_CONTROL);
        
        // change the name of the file just in case, to avoid overriding message
        
        try {
			Thread.sleep(100);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
        robot.keyPress(KeyEvent.VK_HOME);
        robot.keyRelease(KeyEvent.VK_HOME);
        typeRandomDigit();
        typeRandomDigit();
        typeRandomDigit();
        typeRandomDigit();
        typeRandomDigit();
        
        // get the file name
        robot.keyPress(KeyEvent.VK_CONTROL);
        robot.keyPress(KeyEvent.VK_A);
        robot.keyRelease(KeyEvent.VK_A);
        robot.keyRelease(KeyEvent.VK_CONTROL);
        robot.keyPress(KeyEvent.VK_CONTROL);
        robot.keyPress(KeyEvent.VK_C);
        robot.keyRelease(KeyEvent.VK_C);
        robot.keyRelease(KeyEvent.VK_CONTROL);
        try {
            Thread.sleep(WAIT_TIME_AFTER_CTRL_C);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        
        String htmlFilename;
        try {
            htmlFilename = (String) Toolkit.getDefaultToolkit()
                    .getSystemClipboard().getData(DataFlavor.stringFlavor);
        } catch (HeadlessException | UnsupportedFlavorException | IOException e) {
            e.printStackTrace();
            return null;
        }
        
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
     * Types a digit at random.
     * @return The digit that was typed.
     */
    private char typeRandomDigit() {
        int random = (int) (Math.random() * 10);
        int code = KeyEvent.VK_0;
        if (random == 0) { code = KeyEvent.VK_0; }
        else if (random == 1) { code = KeyEvent.VK_1; }
        else if (random == 2) { code = KeyEvent.VK_2; }
        else if (random == 3) { code = KeyEvent.VK_3; }
        else if (random == 4) { code = KeyEvent.VK_4; }
        else if (random == 5) { code = KeyEvent.VK_5; }
        else if (random == 6) { code = KeyEvent.VK_6; }
        else if (random == 7) { code = KeyEvent.VK_7; }
        else if (random == 8) { code = KeyEvent.VK_8; }
        else if (random == 9) { code = KeyEvent.VK_9; }
        else { System.out.println("Math is broken"); }
        robot.keyPress(code);
        robot.keyRelease(code);
        return (char) (random + '0');
    }
}