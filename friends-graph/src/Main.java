import java.awt.AWTException;
import java.awt.Robot;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * friends
 * @author roger
 * Program used to fetch and analyze information about a user's facebook friends. See the
 * readme.txt for more details.
 */
public class Main {
    // TODO: add a GUI
    public static void main(String[] args) throws AWTException {
//        harvest();
    	System.out.println("running...");
    	String path = "C:\\Users\\roger\\Downloads\\27747Christopher Ye.html";
    	List<Person> list = FriendsParser.extractFriendsInfo(path);
//    	for (Person p : list) {
//    		System.out.print(p.toString());
//    	}
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

    // TODO: There is currently a bug with JDK 8 Robot class. Particularly, on 
    // Windows 10 when the display is scaled to a value other than default (125%
    // on most laptops, 100% on most desktops).
    private static void testRobot() throws AWTException {
        Robot r = new Robot();
        r.setAutoWaitForIdle(true);
        r.setAutoDelay(100);
        r.mouseMove(735, 360);
    }
}
