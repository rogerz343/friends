
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
