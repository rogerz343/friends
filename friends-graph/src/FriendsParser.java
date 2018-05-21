import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;

/**
 * Contains methods that extract relevant information from an html file.
 * @author roger
 *
 */
public class FriendsParser {
    
    // tags that help to locate the current user's (owner of html file) id
    // the following String and char immediately precede and succeed (respectively) the id of the current user
    private static String PRECEDES_OWNER_ID = "https://www.facebook.com/";
    private static char SUCCEEDS_OWNER_ID = '/';
    
    // tags that indicate that we are about to read a friend's information
    private static String LI_TAG = "<li class=\"_698\">";
    private static String DIV_TAG1 = "<div class=\"clearfix _5qo4\" data-testid=\"friend_list_item\">";
    private static String DIV_TAG2 = "<div class=\"uiProfileBlockContent\">";
    
    // the following String and char immediately precede and succeed (respectively) the URL of the friend's profile
    private static String PRECEDES_URL = "<a href=\"";
    private static char SUCCEEDS_URL = '\"';
    
    // the following String and char immediately precede and succeed (respectively) the name of the friend
    private static String PRECEDES_NAME = ">";
    private static char SUCCEEDS_NAME = '<';
    
    /**
     * Returns a list of the input person's friends, where the first element in the list is
     * the input person him/herself.
     * @param filepath The path to the person's facebook friends html page.
     * @return A list of the input profile's friends. Returns null if an error occurred.
     * Assumes that html document and people's names are well-formed (i.e. names don't
     * contain strange characters such as "<", ">", "\"".
     * Currently, this method is conservative: it will return null if almost any error
     * (even minor ones) occur
     */
    public static List<Person> extractFriendsInfo(String filepath) {
        File file = new File(filepath);
        
        // get the user's name from the filename.
        // the user's filename should have the following format (without quotes or brackets):
        // "#####[name].html"
        String filename = file.getName();
        String ownerName = filename.substring(5, filename.length() - 5);
        
        BufferedReader br;
        try {
            br = new BufferedReader(new FileReader(file));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return null;
        }
        
        // add the html's "owner"'s name to the results
        List<Person> result = new ArrayList<>();
        if (!findString(br, PRECEDES_OWNER_ID)) { return null; }
        String ownerId = readUntil(br, SUCCEEDS_OWNER_ID);
        if (ownerId == null) { return null; }
        String ownerUrl = "https://www.facebook.com/" + ownerId;
        result.add(new Person(ownerId, ownerName, ownerUrl));
        
        // add the rest of the friends
        boolean success = true;
        while (!isEOF(br)) {
            success = findString(br, LI_TAG);
            if (!success && !isEOF(br)) { return null; }
            success = findString(br, DIV_TAG1);
            if (!success && !isEOF(br)) { return null; }
            success = findString(br, DIV_TAG2);
            if (!success && !isEOF(br)) { return null; }
            success = findString(br, PRECEDES_URL);
            if (!success && !isEOF(br)) { return null; }
            
            String friendUrl = readUntil(br, SUCCEEDS_URL);
            if (friendUrl == null) { return null; }
            friendUrl = getBaseUrl(friendUrl);
            String[] friendUrlComponents = friendUrl.split("/");
            String friendId = friendUrlComponents[friendUrlComponents.length - 1];
            
            success = findString(br, PRECEDES_NAME);
            if (!success && !isEOF(br)) { return null; }
            
            String friendName = readUntil(br, SUCCEEDS_NAME);
            if (friendName == null) { return null; }
            result.add(new Person(friendId, friendName, friendUrl));
        }
        
        try {
            br.close();
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
        
        return result;
    }
    
    /**
     * Given the url to a facebook profile main page, removes any trailing slashes,
     * symbols, or parameters.
     * @param url The url to a facebook profile page
     * @return The url to the given facebook profile main page, with no trailing
     * slashes, symbols, or parameters.
     * Example:
     * Turns "https://www.facebook.com/john.smith.35?fref=pb&hc_location=friends_tab"
     * to "https://www.facebook.com/john.smith.35"
     * but does NOT work if not on main page, ex:
     * "https://www.facebook.com/john.smith.35/friends?lst=1000017..."
     * will become "https://www.facebook.com/john.smith.35/friends"
     */
    public static String getBaseUrl(String url) {
        return url.split("[\\?#]")[0];
    }
    
    /**
     * Checks whether the given Reader has reached EOF without advancing file pointer
     * @param r The Reader
     * @return true if the reader has reached EOF, false otherwise (including when an error occurred)
     */
    private static boolean isEOF(Reader r) {
        try {
            r.mark(2);
            char c = (char) r.read();
            r.reset();
            return c == -1;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * Advances the Reader's file pointer to the character after the last character
     * in the first instance of `s` in the Reader if `s` is found. Advances the file pointer
     * to the end of file if `s` is not found.
     * @param r The Reader
     * @param s The String to find in the Reader
     * @return true if the String s was found, false otherwise or an error occurred.
     */
    private static boolean findString(Reader r, String s) {
        try {
            r.mark(2);
            char c = (char) r.read();
            r.reset();
            while (c != -1) {
                r.mark(s.length() + 1);
                for (int i = 0; i < s.length(); i++) {
                    c = (char) r.read();
                    if (c == -1) { return false; }
                    if (c != s.charAt(i)) { break; }
                    if (i == s.length() - 1) { return true; }
                }
                r.reset();
                c = (char) r.read();
            }
            return false;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * Reads characters from `r` until the `end` is found and returns the String that
     * was built (excluding `end`). Also advances the file pointer to the character after
     * `end`.
     * @param r The Reader
     * @param end The char to read until.
     * @return The substring from the current file pointer up until (but excluding) `end`.
     * Returns null if an error occurred (ex: end of file reached or IOException)
     */
    private static String readUntil(Reader r, char end) {
        try {
            StringBuilder sb = new StringBuilder();
            char c = (char) r.read();
            while (c != -1 && c != end) {
                sb.append(c);
                c = (char) r.read();
            }
            if (c == -1) { return null; }
            return sb.toString();
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
}
