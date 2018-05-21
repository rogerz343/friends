
/**
 * Simple class representing a person, uniquely identified by the id
 * @author roger
 *
 */
public class Person {
    
    public final String id;
    public final String name;
    public final String url;	// the url to the Person's main profile page, with no trailing slashes or symbols
    
    public Person(String id, String name, String url) {
        this.id = id;
        this.name = name;
        this.url = url;
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
        int endOfId = personStr.indexOf(", name=");
        int endOfName = personStr.indexOf(", url=");
        String id = personStr.substring(11, endOfId);
        String name = personStr.substring(endOfId + 7, endOfName);
        String url = personStr.substring(endOfName + 6, personStr.length() - 1);
        return new Person(id, name, url);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((id == null) ? 0 : id.hashCode());
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
        return true;
    }

    @Override
    public String toString() {
        return "Person [id=" + id + ", name=" + name + ", url=" + url + "]";
    }
    
}
