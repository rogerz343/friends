import java.awt.AWTException;
import java.awt.Robot;

/**
 * friends
 * @author roger
 * Program used to fetch and analyze information about a user's facebook friends. See the
 * readme.txt for more details.
 */
public class Main {
    // TODO: add a GUI
    public static void main(String[] args) throws AWTException {
        Robot r = new Robot();
        r.setAutoWaitForIdle(true);
        r.setAutoDelay(100);
        r.mouseMove(735, 360);
        //harvest();
    }
    
    public static void harvest() throws AWTException {
        // give the user some time to set up the facebook page correctly
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        Harvester h = new Harvester();
        h.harvest();
    }
}
