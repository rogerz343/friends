import java.io.IOException;
import java.nio.file.DirectoryIteratorException;
import java.nio.file.DirectoryStream;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Used for file I/O for files related to this program.
 * @author roger
 *
 */
public class FriendsFiles {
    
    /**
     * Writes the specified message to the specified file with a timestamp. Exceptions
     * thrown by Files.write are ignored.
     * @param logFilePath The path to the file to write to.
     * @param message The message to append to the file.
     * @throws IOException
     */
    public static void writeLog(String logFilePath, String message) {
        String logMessage = "[" + LocalDateTime.now() + "] " + message + "\n";
        try {
            Files.write(Paths.get(logFilePath), logMessage.getBytes(),
                    StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.APPEND);
        } catch (IOException e) {
            System.err.println("err: writeLog() failed.");
            e.printStackTrace();
        }
        
        // TODO: remove. for debugging only
        // System.out.println(logMessage);
    }
    
    /**
     * Loads all Friends files in a directory (not recursively). If an error occurs in reading
     * some file, then that file is skipped.
     * @param dirpath The path to the directory to load Friends files from.
     * @param readAllFiles Set to true if you want to read all files in the directory,
     * and false if you only want to read files that have a ".friends" extension.
     * @return A list in which each element is a list of `Person`s, such that
     * in the list of `Person`s, every Person other than the first one is a
     * friend of the first Person in the list. Returns null if dirpath
     * is not a directory.
     */
    public static List<List<Person>> loadAllInDirectory(String dirpath, boolean readAllFiles) {
        Path directory = Paths.get(dirpath);
        if (!Files.isDirectory(directory)) { return null; }
        
        List<List<Person>> result = new ArrayList<>();
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(directory)) {
            for (Path file : stream) {
                if (!Files.isRegularFile(file)) { continue; }
                if (!readAllFiles && !file.toString().endsWith(".friends")) { continue; }
                result.add(loadFromFile(file.toString()));
            }
        } catch (IOException | DirectoryIteratorException x) {
            System.err.println(x);
        }
        return result;
    }
    
    
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
