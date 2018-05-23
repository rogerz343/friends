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
 * Note: when you are logged into facebook, it appears that there are two forms of url's for
 * profiles: one specified by a custom url (such as "https://www.facebook.com/john.smith.75")
 * and one specified by a profile id (such as "https://www.facebook.com/profile.php?id=7777777").
 * These two cases are considered in many of the methods below.
 */
public class FriendsHtmlParser {
    
    // tags that help to locate the source url of the html file
    // the following String and char immediately precede and succeed (respectively) the html source url
    private static String PRECEDES_SOURCE_URL = ")";
    private static char SUCCEEDS_SOURCE_URL = ' ';
    
    // tags that help to locate the current user's (owner of html file) name
    // the following tag indicates that we are coming up towards the user's name
    private static String SPAN_A_TAG =
            "<span class=\"_2t_q\" id=\"fb-timeline-cover-name\" data-testid=\"profile_name_in_profile_page\">"
            + "<a class=\"_2nlw _2nlv\" href=\"";
    // the following String and char immediately precede and succeed (respectively) the name of the current user
    private static String PRECEDES_OWNER_NAME = "\">";
    private static char SUCCEEDS_OWNER_NAME = '<';
    
    // tags that indicate that we are about to read a friend's information
    // IMPORTANT: DIV_TAG2 will not appear if the profile block corresponds to yourself (whoever is logged in)
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
     * Given an html file of a person's facebook Friends page, returns a list of that
     * person's friends, where the first element in the list is the input person him/herself.
     * This method will try to open the file every second for maxReadAttempts seconds before
     * throwing a FileNotFoundException.
     * @param filepath The path to the person's facebook friends html file.
     * @param maxToExtract The maximum number of friends to extract. Facebook seems
     * to already sort friends by some notion of interaction, so the most "important"
     * friends will be extracted first.
     * @return The maximum number of attempts that this method will make to open the file
     * (at one attempt per second).
     * @return A list of the input profile's friends (up to `maxToExtract`). Returns null if an error occurred.
     * Assumes that html document and people's names are well-formed (i.e. names don't
     * contain strange characters such as "<", ">", "\"", and the general form of the
     * document is: ... owner id ... owner name ... friend block ... friend block ... ... ... EOF
     * @throws FileNotFoundException If filepath could not be opened
     * 
     */
    public static List<Person> extractFriendsInfo(String filepath, int maxToExtract, int maxReadAttempts)
            throws FileNotFoundException {
        File file = new File(filepath);
        
        int numReadAttempts = 0;
        BufferedReader br;
        while (true) {
            try {
                br = new BufferedReader(new FileReader(file));
                break;
            } catch (FileNotFoundException e) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e1) {
                    e1.printStackTrace();
                }
            }
            numReadAttempts++;
            if (numReadAttempts > maxReadAttempts) {
                throw new FileNotFoundException("Could not open file to read after "
                        + maxReadAttempts + " attempts (1 attempt per second)");
            }
            
            // TODO: remove this; for debugging only
            // TODO: we are getting stuck in this loop; it seems that sometimes,
            // the robot doesn't actually download the file; in the last test run,
            // this robot failed to download the html of jacquelines page, but
            // then how did we get past Harvester#waitForDownload()???
            // TODO: investigation to be continued tomorrow...
            if (numReadAttempts > 12) {
                System.out.println("extractFriendsInfo(): waited " + numReadAttempts + " seconds to open file: " + filepath);
            }
        }
        
        List<Person> result = new ArrayList<>();
        
        // get the owner user's information and add it to the results
        if (!findString(br, PRECEDES_SOURCE_URL)) { return null; }
        String sourceUrl = readUntil(br, SUCCEEDS_SOURCE_URL);
        if (sourceUrl == null) { return null; }
        
        // note that this section has slightly different functionality from getBaseUrl()
        // sourceUrl has two forms:
        // (1) https://www.facebook.com/profile.php?id=7777777&sk=friends
        // (2) https://www.facebook.com/john.smith.35/friends?...
        String ownerBaseUrl;
        if (sourceUrl.contains("/profile.php?id=")) {
            ownerBaseUrl = sourceUrl.split("&")[0];
        } else {
            // first, change form from
            // "https://www.facebook.com/john.smith.35/friends?..." to
            // "https://www.facebook.com/john.smith.35/friends"
            String sourceUrlStripped = sourceUrl.split("[\\?#]")[0];
            int indexAfterBaseUrl = sourceUrlStripped.length() - 8;
            ownerBaseUrl = sourceUrlStripped.substring(0, indexAfterBaseUrl);
        }
                
        if (!findString(br, SPAN_A_TAG)) { return null; }
        if (!findString(br, PRECEDES_OWNER_NAME)) { return null; }
        String ownerName = readUntil(br, SUCCEEDS_OWNER_NAME);
        if (ownerName == null) { return null; }
                
        result.add(new Person(ownerName, ownerBaseUrl));
        
        // TODO: remove; for debugging only
        int numLoops = 0;
                
        // add the rest of the friends
        boolean success;
        while (!isEOF(br)) {
            // TODO: for debugging; remove this
            numLoops++;
            if (numLoops > 200) {
                System.out.println("extractFriendsInfo(): stuck in !EOF loop.");
            }
            
            // logic on error checking: if the tag isn't found, then either we hit the EOF
            // (which is fine) or something actually went wrong (not fine, so return null)
            success = findString(br, LI_TAG);
            if (!success && !isEOF(br)) { return null; }
            success = findString(br, DIV_TAG1);
            if (!success && !isEOF(br)) { return null; }
            
            success = findString(br, DIV_TAG2);
            if (!success && !isEOF(br)) {
                // DIV_TAG2 will not appear if the profile block corresponds to the user who is
                // logged in to facebook himself/herself. Thus, ...
                // TODO: currently just assumes whoever is logged in myself (roger zhang)
                result.add(new Person("Roger Zhang", "https://www.facebook.com/roger.zhang.5"));
                continue;
            }
            success = findString(br, PRECEDES_URL);
            if (!success && !isEOF(br)) { return null; }
            
            // must return here if EOF (otherwise, not EOF means we are currently reading
            // a valid friend "block"
            if (isEOF(br)) { break; }
            
            String friendUrl = readUntil(br, SUCCEEDS_URL);
            if (friendUrl == null) { return null; }
            String friendBaseUrl = getBaseUrl(friendUrl);
            
            // special case: when account is deactivated, just skip this person
            if (friendBaseUrl == null) { continue; }
                        
            success = findString(br, PRECEDES_NAME);
            if (!success && !isEOF(br)) { return null; }
            String friendName = readUntil(br, SUCCEEDS_NAME);
            if (friendName == null) { return null; }
            result.add(new Person(friendName, friendBaseUrl));
            
            if (result.size() >= maxToExtract + 1) { return result; }
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
     * Given the url to a facebook profile main page, this method removes trailing slashes,
     * symbols, and other additional parameters.
     * @param url The url to a facebook profile page.
     * @return The url to the given facebook profile main page, with no trailing slashes,
     * symbols, or other parameters. If the user's account is deactivated, then
     * returns null.
     * For example, "https://www.facebook.com/john.smith.35?fref=pb&hc_location=friends_tab"
     * becomes "https://www.facebook.com/john.smith.35"
     * and "https://www.facebook.com/profile.php?id=7777777?fref=pb&hc_location=friends_tab"
     * becomes "https://www.facebook.com/profile.php?id=7777777".
     * This method gives incorrect results if the given url is not the main profile page. For
     * example, "https://www.facebook.com/john.smith.35/friends?lst=1000017..."
     * incorrectly becomes "https://www.facebook.com/john.smith.35/friends"
     */
    public static String getBaseUrl(String url) {
        // check for case when user's account is deactivated
        // the url will have the form: "https://www.facebook.com/[logged in user id]/friends#",
        // so we can identify it by the presence of a "/friends"
        if (url.contains("/friends")) {
            return null;
        }
        
        // check for case when user doesn't have custom url
        if (url.substring(0, Math.min(url.length(), 40)).contains("/profile.php?id=")) {
            return url.split("&")[0];
        }
        
        // user has custom url. the url has the form: "https://facebook.com/[custom_url]?[...]"
        // where '?' represents either '?' or '#' and "[...]" represents trailing characters
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
            int c = r.read();
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
            int c = r.read();
            r.reset();
            while (c != -1) {
                r.mark(s.length() + 1);
                for (int i = 0; i < s.length(); i++) {
                    c = r.read();
                    if (c == -1) { return false; }
                    if (c != s.charAt(i)) { break; }
                    if (i == s.length() - 1) { return true; }
                }
                r.reset();
                c = r.read();
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
            int c = r.read();
            while (c != -1 && c != end) {
                sb.append((char) c);
                c = r.read();
            }
            if (c == -1) { return null; }
            return sb.toString();
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
}
