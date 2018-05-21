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
    	String path = "D:\\Robin Zhang\\Downloads\\32829Roger Zhang.html";
    	List<Person> list = FriendsParser.extractFriendsInfo(path);
    	for (Person p : list) {
    		System.out.println(p.toString());
    	}
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
