import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

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
     * Saves a list of people to the file specified by filepath. Does nothing if the file already exists.
     * The resulting file will have each Person object on its own line, with the first Person representing
     * the "owner" and the following Person's representing the owner's friends.
     * @param people The list of people to save. The first Person in the list represents the "owner", and the
     * rest of the Person's are the "owner"'s friends.
     * @param filepath The path to the file to save to.
     * @return true if no error occurred, false otherwise.
     * @throws IOException 
     */
    public static boolean saveToFile(List<Person> people, String filepath) throws IOException {
        if (people.isEmpty()) { return false; }
        Path path = Paths.get(filepath);
        path = Files.createFile(path);
        List<String> lines =
                people.stream()
                .map(p -> p.toString())
                .collect(Collectors.toCollection(ArrayList<String>::new));
        Files.write(path, lines);
        return true;
    }
    
    /**
     * Loads a list of people from the file specified by filepath. The first person in the
     * resulting list is the "owner", while the rest of the people are the owner's friends.
     * @param filepath The path to the file to load the Person's from.
     * @return A list of Person's, where the owner is the first person in the list, and the
     * rest of the people are his/her friends.
     * @throws IOException
     */
    public static List<Person> loadFromFile(String filepath) throws IOException {
        Path path = Paths.get(filepath);
        List<String> lines = Files.readAllLines(path);
        return lines.stream()
                .map(s -> Person.fromString(s))
                .collect(Collectors.toCollection(ArrayList<Person>::new));
    }
    
    /**
     * Returns a list of the input person's friends, where the first element in the list is
     * the input person him/herself.
     * @param filepath The path to the person's facebook friends html file.
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
        	// logic on error checking: if the tag isn't found, then either we hit the EOF
        	// (which is fine) or something actually went wrong (not fine, so return null)
            success = findString(br, LI_TAG);
            if (!success && !isEOF(br)) { return null; }
            success = findString(br, DIV_TAG1);
            if (!success && !isEOF(br)) { return null; }
            
            // DIV_TAG2 will not appear if the profile block corresponds to the user who is
            // logged in to facebook himself/herself. Thus, just skip this block.
            success = findString(br, DIV_TAG2);
            if (!success && !isEOF(br)) { continue; }
            success = findString(br, PRECEDES_URL);
            if (!success && !isEOF(br)) { return null; }
            
            // must return here if EOF (otherwise, not EOF means we are currently reading
            // a valid friend "block"
            if (isEOF(br)) { break; }
            
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
     * Given the url to a facebook profile main page, this method removes any trailing slashes,
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
