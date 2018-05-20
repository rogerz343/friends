import java.awt.AWTException;
import java.awt.Color;
import java.awt.Robot;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.io.IOException;

/**
 * 
 * @author roger
 * Assumptions made before executing code:
 * - facebook banner, chrome console, chrome scrollbar colors are correct
 * - the display is a 1920 x 1080, running windows with 100% scaling
 * - the windows taskbar is at the default height
 * - chrome is maximized on the screen
 * - chrome's bookmark bar is open
 * - chrome's zoom level is default (100%)
 * - chrome's downloads bar (at the bottom of the page) is closed
 */
public class Harvester {
    
    private Robot robot;
    private static int FB_BANNER_X = 1840;
    private static int FB_BANNER_Y = 100;
    private static Color FB_BANNER_COLOR = new Color(66, 103, 178);
    private static Color CONSOLE_COLOR = new Color(243, 243, 243);
    
    private static int FRIENDS_X = 735;
    private static int FRIENDS_Y_COVER = 460;
    private static int FRIENDS_Y_NO_COVER = 360;
    private static Color FRIENDS_BUTTON_COLOR = new Color(255, 255, 255);
    
    private static int SCROLLBAR_X = 1910;
    private static int SCROLLBAR_Y = 1020;
    private static Color SCROLLBAR_NOT_BOTTOM = new Color(241, 241, 241);
    private static Color SCROLLBAR_BOTTOM = new Color(193, 193, 193);
    
    private static int ELEMENTS_TAB_X = 1460;
    private static int ELEMENTS_TAB_Y = 100;
    private static int HTML_HEADER_X = 1390;
    private static int HTML_HEADER_Y = 143;
    private static int EDIT_AS_HTML_X = 1410;
    private static int EDIT_AS_HTML_Y = 183;
    
    
    public Harvester() throws AWTException {
        robot = new Robot();
        robot.setAutoWaitForIdle(true);
        robot.setAutoDelay(100);
    }
    
    // TODO: add error checking
    public int harvest() {
        closeConsole();
        viewFriendsPage();
        scrollToBottom();
        String html = fetchHtmlString();
    }
    
    /**
     * Closes the chrome console, if it is open.
     * @return 0 if no error occurred. 1 otherwise.
     */
    private int closeConsole() {
        robot.mouseMove(0, 0);
        Color sample = robot.getPixelColor(FB_BANNER_X, FB_BANNER_Y);
        if (sample.equals(FB_BANNER_COLOR)) {
            return 0;
        }
        if (sample.equals(CONSOLE_COLOR)) {
            robot.keyPress(KeyEvent.VK_F12);
            robot.keyRelease(KeyEvent.VK_F12);
            sample = robot.getPixelColor(FB_BANNER_X, FB_BANNER_Y);
            if (sample.equals(FB_BANNER_COLOR)) {
                return 0;
            }
        }
        return 1;
    }
    
    /**
     * Opens the chrome console, if it is closed.
     * @return 0 if no error occurred. 1 otherwise.
     */
    private int openConsole() {
        robot.mouseMove(0, 0);
        Color sample = robot.getPixelColor(FB_BANNER_X, FB_BANNER_Y);
        if (sample.equals(CONSOLE_COLOR)) {
            return 0;
        }
        if (sample.equals(FB_BANNER_COLOR)) {
            robot.keyPress(KeyEvent.VK_F12);
            robot.keyRelease(KeyEvent.VK_F12);
            sample = robot.getPixelColor(FB_BANNER_X, FB_BANNER_Y);
            if (sample.equals(CONSOLE_COLOR)) {
                return 0;
            }
        }
        return 1;
    }
    
    /**
     * Assumes that the current page is the main page for a specific person
     * and navigates to friends page
     * @return 0 if no error occurred. 1 if could not locate friends page button.
     */
    private int viewFriendsPage() {
        robot.mouseMove(0, 0);
        Color sample1 = robot.getPixelColor(FRIENDS_X, FRIENDS_Y_NO_COVER);
        Color sample2 = robot.getPixelColor(FRIENDS_X, FRIENDS_Y_NO_COVER - 10);
        Color sample3 = robot.getPixelColor(FRIENDS_X, FRIENDS_Y_NO_COVER + 10);
        if (sample1.equals(FRIENDS_BUTTON_COLOR)
                && sample2.equals(FRIENDS_BUTTON_COLOR)
                && sample3.equals(FRIENDS_BUTTON_COLOR)) {
            robot.mouseMove(FRIENDS_X, FRIENDS_Y_NO_COVER);
        } else {
            sample1 = robot.getPixelColor(FRIENDS_X, FRIENDS_Y_COVER);
            sample2 = robot.getPixelColor(FRIENDS_X, FRIENDS_Y_COVER - 10);
            sample3 = robot.getPixelColor(FRIENDS_X, FRIENDS_Y_COVER + 10);
            if (sample1.equals(FRIENDS_BUTTON_COLOR)
                    && sample2.equals(FRIENDS_BUTTON_COLOR)
                    && sample3.equals(FRIENDS_BUTTON_COLOR)) {
                robot.mouseMove(FRIENDS_X, FRIENDS_Y_NO_COVER);
            } else {
                return 1;
            }
        }
        robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
        robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return 0;
    }
    
    /**
     * Scrolls the page to the bottom.
     * @return 0 if no error occurred. 1 otherwise.
     */
    private int scrollToBottom() {
        closeConsole();
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
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            Color sample = robot.getPixelColor(SCROLLBAR_X, SCROLLBAR_Y);
            if (sample.equals(SCROLLBAR_BOTTOM)) {
                // double check
                try {
                    Thread.sleep(3000);
                } catch (InterruptedException e) {
                    // TODO Auto-generated catch block
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
     * Returns the dynamically generated html from the current web page. Assumes that the
     * chrome is in focus and on the correct webpage.
     * @return The html string if it worked. undefined behavior if an error occured.
     */
    private String fetchHtmlString() {
        openConsole();
        robot.mouseMove(ELEMENTS_TAB_X, ELEMENTS_TAB_Y);
        robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
        robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
        
        robot.mouseMove(HTML_HEADER_X, HTML_HEADER_Y);
        robot.mousePress(InputEvent.BUTTON2_DOWN_MASK);
        robot.mouseRelease(InputEvent.BUTTON2_DOWN_MASK);
        
        robot.mouseMove(EDIT_AS_HTML_X, EDIT_AS_HTML_Y);
        robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
        robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
        
        robot.keyPress(KeyEvent.VK_CONTROL);
        robot.keyPress(KeyEvent.VK_A);
        robot.keyRelease(KeyEvent.VK_A);
        robot.keyRelease(KeyEvent.VK_CONTROL);
        
        // TOOD: make this not slow af
        try {
            Thread.sleep(20000);
        } catch (InterruptedException e1) {
            e1.printStackTrace();
        }
        
        robot.keyPress(KeyEvent.VK_CONTROL);
        robot.keyPress(KeyEvent.VK_C);
        robot.keyRelease(KeyEvent.VK_C);
        robot.keyRelease(KeyEvent.VK_CONTROL);
        
        Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        String html = null;
        try {
            html = (String) clipboard.getData(DataFlavor.stringFlavor);
        } catch (UnsupportedFlavorException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return html;
    }
}