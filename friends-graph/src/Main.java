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
    	String DOWNLOADS_DIR = "D:\\Robin Zhang\\Downloads\\";
    	String OUTPUT_DIR = "D:\\Robin Zhang\\Desktop\\savev2\\";
    	try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    	Harvester h = new Harvester();
    	h.harvestAllPages(2000, 250, DOWNLOADS_DIR, OUTPUT_DIR);
    }

    public static void harvest() throws AWTException {
        // give the user some time to set up the facebook page correctly
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        Harvester h = new Harvester();
        h.harvestSingleFriendsPage();
    }
}
