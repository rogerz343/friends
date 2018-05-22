
/**
 * Simple class representing a person, uniquely identified by `id` and `urlIdType` (together)
 * @author roger
 *
 */
public class Person {
    
    // id should not contain any special characters (e.g. characters reserved for Windows filesystem)
    private final String id;
    private final int urlIdType;
    private final String name;
    private final String baseUrl;	// the url to the Person's main profile page, with no trailing slashes or symbols
    
    // has the form "https://www.facebook.com/john.smith.35", where "john.smith.35" is the id
    private static int CUSTOM_URL_TYPE = 0;
    
    // has the form "https://www.facebook.com/profile.php?id=7777777", where "7777777" is the id
    private static int NUMERIC_TYPE = 1;
    
    /**
     * Constructs a Person using the given name and baseUrl, giving it an id
     * based on the baseUrl.
     * @param name The name of the person (no constraints on String).
     * @param baseUrl The url to the person's facebook profile main page, with no
     * trailing slashes or additional parameters.
     */
    public Person(String name, String baseUrl) {
        // assign the id based on the baseUrl, where the baseUrl has two forms
        // (1) "https://www.facebook.com/profile.php?id=7777777"
        // (2) "https://www.facebook.com/john.smith.35"
        if (baseUrl.contains("/profile.php?id=")) {
            urlIdType = NUMERIC_TYPE;
            
            // "https://www.facebook.com/profile.php?id=".length() = 40
            id = baseUrl.substring(40);
        } else {
            urlIdType = CUSTOM_URL_TYPE;
            
            // "https://www.facebook.com/".length() = 25
            id = baseUrl.substring(25);
        }
        
        this.name = name;
        this.baseUrl = baseUrl;
    }
    
    public String getId() { return id; }
    
    public String getName() { return name; }
    
    public String getBaseUrl() { return baseUrl; }
    
    /**
     * Returns a String that uniquely identifies this person (by combining urlIdType with id)
     * @return A String that uniquely identifies this person (by combining urlIdType with id)
     */
    public String getUniqueKey() {
        return urlIdType + "-" + id;
    }
    
    /**
     * Returns the url of the Friends page of this person.
     * @return The url of the Friends page of this person.
     */
    public String getFriendsPageUrl() {
        if (urlIdType == CUSTOM_URL_TYPE) {
            return baseUrl + "/friends";
        }
        return baseUrl + "&sk=friends";
    }
    
    /**
     * Returns a Person object from its String representation. The String
     * representation must follow the same format as the output of
     * Person#toString().
     * @param personStr The String representation of a Person.
     * @return The Person corresponding to personStr.
     * Note that this method may not work correctly if any of the Person's
     * attributes includes any of the special tokens:
     * "Person [id=", ", name=", ", url=", or "]"
     */
    public static Person fromString(String personStr) {
        int startOfName = 13;
        int endOfName = personStr.indexOf(", baseUrl=");
        String name = personStr.substring(startOfName, endOfName);
        int startOfBaseUrl = endOfName + 10;
        String url = personStr.substring(startOfBaseUrl, personStr.length() - 1);
        return new Person(name, url);
    }
    
    /**
     * Important note: toString() and fromString() are interdependent. Do not change
     * one without considering the other. The string representation here was chosen
     * such that it would satisfy both human readability and compatibility with
     * fromString().
     */
    @Override
    public String toString() {
        return "Person [name=" + name + ", baseUrl=" + baseUrl + "]";
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((id == null) ? 0 : id.hashCode());
        result = prime * result + urlIdType;
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        Person other = (Person) obj;
        if (id == null) {
            if (other.id != null)
                return false;
        } else if (!id.equals(other.id))
            return false;
        if (urlIdType != other.urlIdType)
            return false;
        return true;
    }
    
}
