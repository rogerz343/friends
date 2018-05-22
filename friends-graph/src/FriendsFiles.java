import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Used for file I/O relating to "Friends" files.
 * @author roger
 *
 */
public class FriendsFiles {
    /**
     * Saves a list of people to the file specified by filepath. The resulting file will
     * have each Person object on its own line, with the first Person representing
     * the "owner" and the following Person's representing the owner's friends. This method
     * does nothing if the file already exists and throws an exception if any other error
     * occurs.
     * @param people The list of people to save. The first Person in the list represents the "owner", and the
     * rest of the Person's are the "owner"'s friends.
     * @param filepath The path to the file to save to.
     * @return false if the file already exists; true otherwise.
     * @throws IllegalArgumentException if `people` is empty.
     * @throws IOException
     */
    public static boolean saveToFile(List<Person> people, String filepath) throws IOException {
        if (people.isEmpty()) { throw new IllegalArgumentException(); }
        Path path = Paths.get(filepath);
        
        try {
            path = Files.createFile(path);
        } catch (FileAlreadyExistsException e) {
            return false;
        }
        
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
}
