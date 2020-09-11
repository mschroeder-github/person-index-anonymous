package anonymous.group;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * A data class representing a person having first name, last name and optionally middle name.
 * 
 */
public class Person {
    private int id;
    private String firstname;
    private String middlename;
    private String lastname;
    
    private boolean firstnameMentioned;
    private boolean middlenameMentioned;
    private boolean lastnameMentioned;
    
    private List<Person> firstnameAmbiguousWith;
    private List<Person> lastnameAmbiguousWith;

    public Person() {
        firstnameAmbiguousWith = new ArrayList<>();
        lastnameAmbiguousWith = new ArrayList<>();
    }

    public Person(int id, String firstname, String lastname) {
        this();
        this.id = id;
        this.firstname = firstname;
        this.lastname = lastname;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getFirstname() {
        return firstname;
    }
    
    public void setFirstname(String firstname) {
        this.firstname = firstname;
    }

    public String getLastname() {
        return lastname;
    }

    public void setLastname(String lastname) {
        this.lastname = lastname;
    }

    public String getMiddlename() {
        return middlename;
    }

    public void setMiddlename(String middlename) {
        this.middlename = middlename;
    }
    
    public boolean hasMiddlename() {
        return middlename != null;
    }
    
    public boolean isFirstnameMentioned() {
        return firstnameMentioned;
    }

    public void setFirstnameMentioned(boolean firstnameMentioned) {
        this.firstnameMentioned = firstnameMentioned;
    }

    public boolean isMiddlenameMentioned() {
        return middlenameMentioned;
    }

    public void setMiddlenameMentioned(boolean middlenameMentioned) {
        this.middlenameMentioned = middlenameMentioned;
    }
    
    public boolean isLastnameMentioned() {
        return lastnameMentioned;
    }

    public void setLastnameMentioned(boolean lastnameMentioned) {
        this.lastnameMentioned = lastnameMentioned;
    }

    public boolean hasFirstnameAmbiguousWith() {
        return !firstnameAmbiguousWith.isEmpty();
    }
    
    public boolean hasLastnameAmbiguousWith() {
        return !lastnameAmbiguousWith.isEmpty();
    }

    public List<Person> getFirstnameAmbiguousWith() {
        return firstnameAmbiguousWith;
    }

    public List<Person> getLastnameAmbiguousWith() {
        return lastnameAmbiguousWith;
    }
    
    public void swap() {
        String tmp = firstname;
        firstname = lastname;
        lastname = tmp;
    }
    
    @Override
    public int hashCode() {
        int hash = 3;
        hash = 17 * hash + Objects.hashCode(this.firstname);
        hash = 17 * hash + Objects.hashCode(this.lastname);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final Person other = (Person) obj;
        if (!Objects.equals(this.firstname, other.firstname)) {
            return false;
        }
        if (!Objects.equals(this.lastname, other.lastname)) {
            return false;
        }
        return true;
    }
    
    @Override
    public String toString() {
        return "Person{" + "id=" + id + ", firstname=" + firstname + ", middlename=" + middlename + ", lastname=" + lastname + '}';
    }
}
