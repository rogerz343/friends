import java.awt.AWTException;
import java.awt.Color;
import java.awt.HeadlessException;
import java.awt.Toolkit;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.io.IOException;

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
    private static Color SCROLLBAR_BOTTOM = new Color(193, 193, 193);
    
    private static int EMPTY_SPACE_X = 200;
    private static int EMPTY_SPACE_Y = 300;    
    
    public Harvester() throws AWTException {
        robot = new InterruptibleRobot();
        robot.setAutoWaitForIdle(true);
        robot.setAutoDelay(100);
    }
    
    // TODO: add error checking
    /**
     * Downloads the html document of a facebook user's "Friends" page. Has several
     * assumptions in order to operate correctly:
     * - FRIENDS_BUTTON_COLOR is correct
     * - the display is a 1920 x 1080, running windows with 100% scaling
     * - the windows taskbar is at the default height
     * - chrome is maximized on the screen
     * - chrome's bookmark bar is open
     * - chrome's zoom level is default (100%)
     * - chrome's downloads bar (at the bottom of the page) is OPEN
     * - chrome's developer tools panel is CLOSED
     * - the current page is the main page of a facebook profile, scrolled all the way to the top
     * - the facebook chat panel (on the right hand side) is open, default size
     */
    public void harvest() {
        viewFriendsPage();
        // give the page some time to load
        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        
        scrollToBottom();
        fetchHtml();
    }
    
    /**
     * Assumes that the current page is the main page for a specific person
     * and navigates to friends page
     * @return 0 if no error occurred. 1 if could not locate friends page button.
     * 2 if other error occurred.
     */
    private int viewFriendsPage() {
        if (robot.interrupted) { return 2; }
        robot.mouseMove(EMPTY_SPACE_X, EMPTY_SPACE_Y);
        robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
        robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
        
        robot.keyPress(KeyEvent.VK_CONTROL);
        robot.keyPress(KeyEvent.VK_L);
        robot.keyRelease(KeyEvent.VK_L);
        robot.keyRelease(KeyEvent.VK_CONTROL);
        
        robot.keyPress(KeyEvent.VK_CONTROL);
        robot.keyPress(KeyEvent.VK_C);
        robot.keyRelease(KeyEvent.VK_C);
        robot.keyRelease(KeyEvent.VK_CONTROL);
        try {
            Thread.sleep(200);
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
        
        // turns "https://www.facebook.com/john.smith.35?fref=pb&hc_location=friends_tab"
        // to "https://www.facebook.com/john.smith.35"
        // Note: does NOT work if not on main page:
        //   ex: "https://www.facebook.com/john.smith.35/friends?lst=1000017..."
        //   will become "https://www.facebook.com/john.smith.35/friends"
        String baseUrlNoSlash = url.split("[\\?#]")[0];
        String friendsPageUrl = baseUrlNoSlash + "/friends";
        
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
        
        robot.keyPress(KeyEvent.VK_ENTER);
        robot.keyRelease(KeyEvent.VK_ENTER);
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
        boolean done = false;
        while (!done) {
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
            if (sample.equals(SCROLLBAR_BOTTOM)) {
                // double check
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                sample = robot.getPixelColor(SCROLLBAR_X, SCROLLBAR_Y);
                if (sample.equals(SCROLLBAR_BOTTOM)) {
                    done = true;
                }
            } else if (!sample.equals(SCROLLBAR_NOT_BOTTOM)) {
                return 1;
            }
        }
        return 0;
    }
    
    /**
     * Saves the dynamically generated html file from the current web page as an html file
     * in the current working directory. Assumes that chrome is in focus and is on the correct webpage.
     * Assumes that chrome's save "Webpage, complete" saves the dynamically generated html (not the source code).
     * @return 0 if no error occurred. 1 otherwise.
     */
    private int fetchHtml() {
         
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
        return 0;
    }
    
    /**
     * Types a digit at random.
     */
    private void typeRandomDigit() {
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
    }
}