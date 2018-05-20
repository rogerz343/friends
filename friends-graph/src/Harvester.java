import java.awt.AWTException;
import java.awt.Color;
import java.awt.MouseInfo;
import java.awt.Point;
import java.awt.Robot;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;

/**
 * A class used to download html documents corresponding to a facebook user's "Friends" page,
 * with all of the html elements loaded.
 * @author roger
 * Assumptions made before executing code:
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
public class Harvester {
    
    private InterruptibleRobot robot;
    
    private static int FRIENDS_X = 735; // 842; // 735;
    private static int FRIENDS_Y_COVER = 460;
    private static int FRIENDS_Y_NO_COVER = 360;
    private static Color FRIENDS_BUTTON_COLOR = new Color(255, 255, 255);
    
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
     * Downloads the html document of a facebook user's "Friends" page. See class documentation
     * for more detail.
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
     * 2 if other error occured.
     */
    private int viewFriendsPage() {
        if (robot.interrupted) { return 2; } 
        robot.mouseMove(0, 0);
        Color sample = robot.getPixelColor(FRIENDS_X, FRIENDS_Y_NO_COVER);
        if (false) {
//        if (sample.equals(FRIENDS_BUTTON_COLOR)) {
//            robot.mouseMove(FRIENDS_X, FRIENDS_Y_NO_COVER);
        } else {
            sample = robot.getPixelColor(FRIENDS_X, FRIENDS_Y_COVER);
            if (sample.equals(FRIENDS_BUTTON_COLOR)) {
                robot.mouseMove(FRIENDS_X, FRIENDS_Y_NO_COVER);
            } else {
                return 1;
            }
        }
        robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
        robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
        return 0;
    }
    
    /**
     * Scrolls the page to the bottom.
     * @return 0 if no error occurred. 1 otherwise.
     */
    private int scrollToBottom() {
        robot.mouseMove(0, 0);
        boolean done = false;
        while (!done) {
            if (robot.interrupted) { return 1; }
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
        if (robot.interrupted) { return 1; } 
        robot.mouseMove(EMPTY_SPACE_X, EMPTY_SPACE_Y);
        robot.mousePress(InputEvent.BUTTON2_DOWN_MASK);
        robot.mouseRelease(InputEvent.BUTTON2_DOWN_MASK);
        
        if (robot.interrupted) { return 1; }
        robot.keyPress(KeyEvent.VK_CONTROL);
        robot.keyPress(KeyEvent.VK_S);
        robot.keyRelease(KeyEvent.VK_S);
        robot.keyRelease(KeyEvent.VK_CONTROL);
        
        // change the name of the file just in case, to avoid overriding message
        if (robot.interrupted) { return 1; }
        robot.keyPress(KeyEvent.VK_HOME);
        robot.keyRelease(KeyEvent.VK_HOME);
        typeRandomDigit();
        typeRandomDigit();
        typeRandomDigit();
        typeRandomDigit();
        typeRandomDigit();
        
        if (robot.interrupted) { return 1; }
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
    
    /**
     * A java Robot that monitors whether it has been interrupted by user input.
     * @author roger
     *
     */
    private class InterruptibleRobot extends Robot {
        
        private int lastXMove;
        private int lastYMove;
        public boolean interrupted;

        public InterruptibleRobot() throws AWTException {
            super();
            lastXMove = -1;
            lastYMove = -1;
            interrupted = false;
        }
        
        public void mouseMove(int x, int y) {
            checkInterrupt();
            super.mouseMove(x, y);
            lastXMove = x;
            lastYMove = y;
        }
        
        public void keyPress(int keycode) {
            checkInterrupt();
            super.keyPress(keycode);
        }
        
        /**
         * Checks if the user (or any other program) has interrupted the robot by checking
         * if the mouse cursor is still in the same position that it was last mouseMove'ed to.
         * If interrupted, the instance variable `interrupted` is set to true.
         */
        private void checkInterrupt() {
            if (lastXMove == -1 || lastYMove == -1) { return; }
            Point curr = MouseInfo.getPointerInfo().getLocation();
            interrupted = curr.x != lastXMove || curr.y != lastYMove;
        }
        
    }
}