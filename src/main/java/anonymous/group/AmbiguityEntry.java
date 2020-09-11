package anonymous.group;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.StringJoiner;

/**
 * A data class representing an ambiguity entry.
 * It is used by the generator, approach and evaluator. 
 * 
 */
public class AmbiguityEntry {
    
    /*package*/ int cell;
    /*package*/ String reason;
    /*package*/ List<Person> persons;
    
    public AmbiguityEntry() {
        persons = new ArrayList<>();
    }

    public int getCell() {
        return cell;
    }

    public void setCell(int cell) {
        this.cell = cell;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public List<Person> getPersons() {
        return persons;
    }

    public void setPersons(List<Person> persons) {
        this.persons = persons;
    }
    
    public String toListString() {
        StringJoiner sj = new StringJoiner(",");
        persons.forEach(p -> sj.add("" + p.getId()));
        return sj.toString();
    }
    
    public Set<Integer> toSet() {
        Set<Integer> s = new HashSet<>();
        persons.forEach(p -> s.add(p.getId()));
        return s;
    }

}
