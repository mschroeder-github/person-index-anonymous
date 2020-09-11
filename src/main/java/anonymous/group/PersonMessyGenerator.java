package anonymous.group;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Set;
import java.util.StringJoiner;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * A messy data generator that produces ground truth for future evaluations.
 *
 * 
 */
public class PersonMessyGenerator {

    //double names
    //firstname and lastname abiguity
    //will be written
    private File messyFile;
    private File nmFile;
    private File personIndexFile;
    private File ambiguityFile;
    private File settingsFile;

    //how many persons should we generate
    private int personIndexSize;

    //how many messy data cells should we generate
    private int messyDataSize;

    //how many persons with double names should we generate
    private int doubleNameSize = 3;

    //what firstname and lastnames to load
    private List<File> firstnameFiles;
    private List<File> lastnameFiles;
    private char nameFileDelimiter = ';';

    //how we randomize
    private Random random;

    private List<String> roles = Arrays.asList("", "Admin", "CEO", "Chief", "Executive", "Developer", "Contact");
    private List<String> notes = Arrays.asList("new", "old", "TODO", "remember", "remove", "send PDF to", "send mail to", "write message to");
    private List<String> delimiters = Arrays.asList("\n", "\n\n", "/", " / ", " - ", "; ", ";");
    private List<String[]> brackets = Arrays.asList(new String[]{"<", ">"}, new String[]{"(", ")"}, new String[]{"[", "]"}, new String[]{"{", "}"}, new String[]{"\"", "\""}, new String[]{"\'", "\'"});

    private boolean changeNameCase = false;

    //ambiguity
    private int lastnameAmbiguityGroupCount = 3;
    private int lastnameAmbiguityGroupSize = 3;
    private int firstnameAmbiguityGroupCount = 3;
    private int firstnameAmbiguityGroupSize = 3;

    private Map<String, List<Person>> firstnameAmbiguity;
    private Map<String, List<Person>> lastnameAmbiguity;

    public final static int onlyLastname = 0;
    public final static int lastnameCommaFirstname = 1;
    public final static int lastnameCommaFirstnameLetter = 2;
    public final static int lastnameSpaceFirstname = 3;
    public final static int lastnameDepartment = 4;
    public final static int departmentNewLineLastnameFirstname = 5;
    public final static int firstnameLastname = 6;
    public final static int lastnameFirstnameEmail = 7;
    public final static int noteRoleLastnameFirstname = 8;
    public final static int onlyFirstname = 9;

    public final static int FirstnameSepMiddlenameLastname = 0;
    public final static int FirstnameMiddlenameLetterLastname = 1;
    public final static int FirstnameLetterMiddlenameLetterLastname = 2;
    public final static int LastnameCommaFirstnameMiddlenameLetter = 3;

    //default = all
    private List<Integer> messyModes = Arrays.asList(onlyLastname,
            onlyFirstname,
            lastnameCommaFirstname,
            lastnameCommaFirstnameLetter,
            lastnameSpaceFirstname,
            lastnameDepartment,
            departmentNewLineLastnameFirstname,
            firstnameLastname,
            lastnameFirstnameEmail,
            noteRoleLastnameFirstname,
            FirstnameSepMiddlenameLastname,
            FirstnameMiddlenameLetterLastname,
            FirstnameLetterMiddlenameLetterLastname,
            LastnameCommaFirstnameMiddlenameLetter
    );

    private int maxPersonsCount = 3;

    //---------------------------------------
    private List<CSVRecord> firstnameRecords;
    private List<CSVRecord> lastnameRecords;

    private List<Person> personIndex;
    private List<int[]> n2mList; //cell 2 person
    private List<MessyData> messyData;
    private List<AmbiguityEntry> ambiguities;

    public PersonMessyGenerator() {

    }

    public void generate() throws Exception {
        System.out.println("read first names");
        firstnameRecords = new ArrayList<>();
        for (File f : firstnameFiles) {
            CSVParser p = CSVFormat.DEFAULT.withDelimiter(nameFileDelimiter).parse(new FileReader(f));
            firstnameRecords.addAll(p.getRecords());
            p.close();
        }

        System.out.println("read last names");
        lastnameRecords = new ArrayList<>();
        for (File f : lastnameFiles) {
            CSVParser p = CSVFormat.DEFAULT.withDelimiter(nameFileDelimiter).parse(new FileReader(f));
            lastnameRecords.addAll(p.getRecords());
            p.close();
        }

        System.out.println("generate " + personIndexSize + " persons for index");
        personIndex = new ArrayList<>();

        int personId = 0;

        System.out.println("generate ambigous persons first: lastname=" + lastnameAmbiguityGroupCount + "*" + lastnameAmbiguityGroupSize + ", firstname=" + firstnameAmbiguityGroupCount + "*" + firstnameAmbiguityGroupSize);
        firstnameAmbiguity = new HashMap<>();
        lastnameAmbiguity = new HashMap<>();
        for (int i = 0; i < lastnameAmbiguityGroupCount; i++) {
            CSVRecord lastnameRec = randomlySelect(lastnameRecords);
            lastnameRecords.remove(lastnameRec);

            String lastname = Utils.toProperCase(lastnameRec.get(0));

            List<Person> l = lastnameAmbiguity.computeIfAbsent(lastname, k -> new ArrayList<>());

            for (int j = 0; j < lastnameAmbiguityGroupSize; j++) {
                CSVRecord firstnameRec = randomlySelect(firstnameRecords);
                firstnameRecords.remove(firstnameRec);

                String firstname = Utils.toProperCase(firstnameRec.get(0));

                Person person = new Person(personId++, firstname, lastname);
                personIndex.add(person);

                l.add(person);
            }
        }
        for (int i = 0; i < firstnameAmbiguityGroupCount; i++) {
            CSVRecord firstnameRec = randomlySelect(firstnameRecords);
            firstnameRecords.remove(firstnameRec);

            String firstname = Utils.toProperCase(firstnameRec.get(0));

            List<Person> l = firstnameAmbiguity.computeIfAbsent(firstname, k -> new ArrayList<>());

            for (int j = 0; j < firstnameAmbiguityGroupSize; j++) {
                CSVRecord lastnameRec = randomlySelect(lastnameRecords);
                lastnameRecords.remove(lastnameRec);

                String lastname = Utils.toProperCase(lastnameRec.get(0));

                Person person = new Person(personId++, firstname, lastname);
                personIndex.add(person);

                l.add(person);
            }
        }
        for (Entry<String, List<Person>> entry : lastnameAmbiguity.entrySet()) {
            for (Person a : entry.getValue()) {
                for (Person b : entry.getValue()) {
                    if (a != b) {
                        a.getLastnameAmbiguousWith().add(b);
                    }
                }
            }
        }
        for (Entry<String, List<Person>> entry : firstnameAmbiguity.entrySet()) {
            for (Person a : entry.getValue()) {
                for (Person b : entry.getValue()) {
                    if (a != b) {
                        a.getFirstnameAmbiguousWith().add(b);
                    }
                }
            }
        }

        int doubleNameGenerated = 0;
        System.out.println("generate rest of unambigous persons with " + doubleNameSize + " double names");
        for (int i = personIndex.size(); i < personIndexSize; i++) {
            CSVRecord firstnameRec = randomlySelect(firstnameRecords);
            CSVRecord lastnameRec = randomlySelect(lastnameRecords);

            //unambigous
            firstnameRecords.remove(firstnameRec);
            lastnameRecords.remove(lastnameRec);

            String firstname = firstnameRec.get(0);
            String lastname = lastnameRec.get(0);

            Person person = new Person(i, Utils.toProperCase(firstname), Utils.toProperCase(lastname));
            personIndex.add(person);

            if (doubleNameGenerated < doubleNameSize) {
                CSVRecord middlenameRec = randomlySelect(firstnameRecords);
                firstnameRecords.remove(middlenameRec);

                person.setMiddlename(Utils.toProperCase(middlenameRec.get(0)));

                doubleNameGenerated++;
            }
        }

        //write messy data
        System.out.println("generate " + messyDataSize + " messy data and n:m list");
        messyData = new ArrayList<>();
        n2mList = new ArrayList<>();
        ambiguities = new ArrayList<>();
        int messyModeIndex = 0;
        for (int i = 0; i < messyDataSize; i++) {

            //what persons should be mentioned in this cell
            List<Person> persons = selectPersons();

            //how is the content of the cell
            MessyData data = generateMessyData(persons, messyModeIndex);

            Set<Integer> ambiguousPersonIds = new HashSet<>();

            if (!data.reason2ambiguity.isEmpty()) {
                AmbiguityEntry entry = new AmbiguityEntry();

                for (Entry<String, List<Person>> r2a : data.reason2ambiguity.entrySet()) {
                    entry.cell = i;
                    entry.reason = r2a.getKey();
                    entry.persons = new ArrayList<>();
                    entry.persons.addAll(r2a.getValue());

                    ambiguousPersonIds.add(entry.persons.get(0).getId());
                }

                ambiguities.add(entry);
            }

            messyModeIndex += persons.size();

            messyData.add(data);

            //save that in cell i the person with id is mentioned
            for (Person person : persons) {
                n2mList.add(new int[]{i, person.getId(), ambiguousPersonIds.contains(person.getId()) ? 1 : 0});
            }
        }

        int fullMentionedCount = 0;
        int onlyFirstnameCount = 0;
        int onlyLastnameCount = 0;
        int neverMentionedCount = 0;
        int middleNameCount = 0;
        for (Person person : personIndex) {
            if (person.isFirstnameMentioned() && person.isLastnameMentioned()) {
                fullMentionedCount++;
            } else if (person.isFirstnameMentioned()) {
                onlyFirstnameCount++;
            } else if (person.isLastnameMentioned()) {
                onlyLastnameCount++;
            } else {
                neverMentionedCount++;
            }

            if (person.hasMiddlename() && person.isMiddlenameMentioned()) {
                middleNameCount++;
            }
        }
        System.out.println(fullMentionedCount + "/" + personIndex.size() + " fully mentioned");
        System.out.println(onlyFirstnameCount + "/" + personIndex.size() + " only first name mentioned");
        System.out.println(onlyLastnameCount + "/" + personIndex.size() + " only last name mentioned");
        System.out.println(neverMentionedCount + "/" + personIndex.size() + " never mentioned");
        System.out.println(middleNameCount + "/" + doubleNameSize + " middle name mentioned");

        //write person index
        System.out.println("write person index to " + personIndexFile.getPath());
        CSVPrinter personIndexWriter = CSVFormat.DEFAULT.print(new FileWriter(personIndexFile));
        personIndexWriter.printRecord("id", "firstname", "middlename", "lastname", "firstname mentioned", "middlename mentioned", "lastname mentioned");
        for (Person person : personIndex) {
            personIndexWriter.printRecord(person.getId(),
                    person.getFirstname(), person.getMiddlename(), person.getLastname(),
                    person.isFirstnameMentioned(), person.isMiddlenameMentioned(), person.isLastnameMentioned()
            );
        }
        personIndexWriter.close();

        //write messy data
        System.out.println("write messy data to " + messyFile.getPath());
        CSVPrinter messyDataWriter = CSVFormat.DEFAULT.print(new FileWriter(messyFile));
        messyDataWriter.printRecord("id", "data");
        for (int i = 0; i < messyData.size(); i++) {
            messyDataWriter.printRecord(i, messyData.get(i).plain);
        }
        messyDataWriter.close();

        //write messy data tokens
        FileWriter fw = new FileWriter(new File(messyFile.getParentFile(), messyFile.getName() + ".json"));
        for (int i = 0; i < messyData.size(); i++) {
            fw.write(new JSONArray(messyData.get(i).tokens).toString());
            fw.write("\n");
        }
        fw.close();

        //write n to m list
        System.out.println("write n:m list to " + nmFile.getPath());
        CSVPrinter nmWriter = CSVFormat.DEFAULT.print(new FileWriter(nmFile));
        nmWriter.printRecord("cell", "person", "ambiguous");
        for (int[] entry : n2mList) {
            nmWriter.printRecord(entry[0], entry[1], entry[2] == 1);
        }
        nmWriter.close();

        //write ambiguities
        System.out.println("write " + ambiguities.size() + " ambiguities to " + ambiguityFile.getPath());
        CSVPrinter ambWriter = CSVFormat.DEFAULT.print(new FileWriter(ambiguityFile));
        ambWriter.printRecord("cell", "reason", "persons", "correct person");
        for (AmbiguityEntry entry : ambiguities) {
            StringJoiner sj = new StringJoiner(",");
            entry.persons.forEach(p -> sj.add(String.valueOf(p.getId())));
            ambWriter.printRecord(entry.cell, entry.reason, sj.toString(), entry.persons.get(0).getId());
        }
        ambWriter.close();

        if(settingsFile != null) {
            System.out.println("write settings");
            
            JSONObject settings = new JSONObject();
            settings.put("personIndexSize", personIndexSize);
            settings.put("messyDataSize", messyDataSize);
            settings.put("doubleNameSize", doubleNameSize);
            settings.put("maxPersonsCount", maxPersonsCount);
            settings.put("lastnameAmbiguityGroupCount", lastnameAmbiguityGroupCount);
            settings.put("lastnameAmbiguityGroupSize", lastnameAmbiguityGroupSize);
            settings.put("firstnameAmbiguityGroupCount", firstnameAmbiguityGroupCount);
            settings.put("firstnameAmbiguityGroupSize", firstnameAmbiguityGroupSize);
            
            FileUtils.writeStringToFile(settingsFile, settings.toString(2), StandardCharsets.UTF_8);
        }
        
        System.out.println("done");
    }

    public List<Person> selectPersons() {
        List<Person> persons = new ArrayList<>();
        int personSize = 1 + (maxPersonsCount > 0 ? random.nextInt(maxPersonsCount) : 0);
        for (int i = 0; i < personSize; i++) {
            Person p = randomlySelect(personIndex);
            if (!persons.contains(p)) {
                persons.add(p);
            }
        }
        return persons;
    }

    public MessyData generateMessyData(List<Person> persons, int messyModeIndex) {
        MessyData md = new MessyData();

        //shuffle persons
        Collections.shuffle(persons, random);

        List<List<String>> personTokens = new ArrayList<>();
        List<String> personData = new ArrayList<>();
        for (Person person : persons) {

            String fn = person.getFirstname();
            String ln = person.getLastname();
            String mn = person.getMiddlename();

            if (changeNameCase) {
                fn = changeCase(random, fn);
                ln = changeCase(random, ln);
            }

            String data = "";
            List<String> tokens = new ArrayList<>();

            if (person.hasMiddlename()) {

                int middleNameMode = messyModeIndex % 4;

                //force that middle name is mentioned at least once
                if (!person.isMiddlenameMentioned()) {
                    middleNameMode = 0;
                }

                switch (middleNameMode) {
                    case FirstnameSepMiddlenameLastname: {
                        person.setFirstnameMentioned(true);
                        person.setMiddlenameMentioned(true);
                        person.setLastnameMentioned(true);

                        String letter = randomlySelect(Arrays.asList(" ", "-", "_"));

                        data = fn + letter + mn + " " + ln;
                        tokens.add(fn);

                        if (!letter.trim().isEmpty()) {
                            tokens.add(letter);
                        }

                        tokens.add(mn);
                        tokens.add(ln);
                        break;
                    }
                    case FirstnameMiddlenameLetterLastname: {
                        person.setFirstnameMentioned(true);
                        person.setLastnameMentioned(true);

                        String letter = randomlySelect(Arrays.asList("", "."));

                        data = fn + " " + mn.charAt(0) + letter + " " + ln;
                        tokens.add(fn);
                        tokens.add("" + mn.charAt(0));
                        if (!letter.isEmpty()) {
                            tokens.add(letter);
                        }
                        tokens.add(ln);
                        break;
                    }
                    case FirstnameLetterMiddlenameLetterLastname: {
                        person.setLastnameMentioned(true);

                        String letter = randomlySelect(Arrays.asList("", "."));

                        data = fn.charAt(0) + letter + " " + mn.charAt(0) + letter + " " + ln;
                        tokens.add("" + fn.charAt(0));
                        if (!letter.isEmpty()) {
                            tokens.add(letter);
                        }
                        tokens.add("" + mn.charAt(0));
                        if (!letter.isEmpty()) {
                            tokens.add(letter);
                        }
                        tokens.add(ln);
                        break;
                    }
                    case LastnameCommaFirstnameMiddlenameLetter: {
                        person.setLastnameMentioned(true);

                        String letter = randomlySelect(Arrays.asList("", "."));

                        data = ln + ", " + fn.charAt(0) + letter + " " + mn.charAt(0) + letter;
                        tokens.add(ln);
                        tokens.add(",");
                        tokens.add("" + fn.charAt(0));
                        if (!letter.isEmpty()) {
                            tokens.add(letter);
                        }

                        tokens.add("" + mn.charAt(0));
                        if (!letter.isEmpty()) {
                            tokens.add(letter);
                        }

                        break;
                    }
                }
            } else {

                int messyMode = messyModes.get(messyModeIndex % messyModes.size());
                switch (messyMode) {
                    //<Nachname>
                    case onlyLastname: {
                        person.setLastnameMentioned(true);
                        data = ln;
                        tokens.add(ln);

                        if (person.hasLastnameAmbiguousWith()) {
                            List<Person> personList = new ArrayList<>();
                            personList.add(person);
                            personList.addAll(person.getLastnameAmbiguousWith());
                            md.reason2ambiguity.put(ln, personList);
                        }
                        break;
                    }
                    case onlyFirstname: {
                        person.setFirstnameMentioned(true);
                        data = fn;
                        tokens.add(fn);

                        if (person.hasFirstnameAmbiguousWith()) {
                            List<Person> personList = new ArrayList<>();
                            personList.add(person);
                            personList.addAll(person.getFirstnameAmbiguousWith());
                            md.reason2ambiguity.put(fn, personList);
                        }
                        break;
                    }
                    //<Nachname>, <Vorname>
                    case lastnameCommaFirstname: {
                        person.setFirstnameMentioned(true);
                        person.setLastnameMentioned(true);
                        data = ln + ", " + fn;
                        tokens.add(ln);
                        tokens.add(",");
                        tokens.add(fn);
                        break;
                    }
                    //<Nachname>, <Vorname-Anfangsbuchstabe>.
                    case lastnameCommaFirstnameLetter: {
                        person.setLastnameMentioned(true);
                        data = ln + ", " + fn.charAt(0) + ".";
                        tokens.add(ln);
                        tokens.add(",");
                        tokens.add(fn.charAt(0) + ".");
                        break;
                    }
                    //<Nachname> <Vorname>
                    case lastnameSpaceFirstname: {
                        person.setFirstnameMentioned(true);
                        person.setLastnameMentioned(true);
                        data = ln + " " + fn;
                        tokens.add(ln);
                        tokens.add(fn);
                        break;
                    }
                    //<Nachname> <Org-Einheit>
                    case lastnameDepartment: {
                        person.setLastnameMentioned(true);
                        String rd = randomDepartment();
                        data = ln + " " + rd;
                        tokens.add(ln);
                        tokens.add(rd);
                        break;
                    }
                    //<Org-Einheit>\n<Nachname> <Vorname> 
                    case departmentNewLineLastnameFirstname: {
                        person.setFirstnameMentioned(true);
                        person.setLastnameMentioned(true);
                        String rd = randomDepartment();
                        data = rd + "\n"
                                + ln + " " + fn;
                        tokens.add(rd);
                        tokens.add("\n");
                        tokens.add(ln);
                        tokens.add(fn);
                        break;
                    }
                    //<Vorname> <Nachname>
                    case firstnameLastname: {
                        person.setFirstnameMentioned(true);
                        person.setLastnameMentioned(true);
                        data = fn + " " + ln;
                        tokens.add(fn);
                        tokens.add(ln);
                        break;
                    }
                    //<Nachname>, <Vorname> '<' <mail> '>'
                    case lastnameFirstnameEmail: {
                        person.setFirstnameMentioned(true);
                        person.setLastnameMentioned(true);

                        String mail = ln.toLowerCase() + "@" + RandomStringUtils.randomAlphabetic(5).toLowerCase() + "." + RandomStringUtils.randomAlphabetic(2).toLowerCase();
                        data = ln + ", " + fn
                                + " <" + mail + ">";

                        tokens.add(ln);
                        tokens.add(",");
                        tokens.add(fn);
                        tokens.add("<");
                        tokens.add(mail);
                        tokens.add(">");

                        break;
                    }
                    //'neuer ReV' <Nachname> <Vorname>
                    case noteRoleLastnameFirstname: {
                        person.setFirstnameMentioned(true);
                        person.setLastnameMentioned(true);
                        String note = randomlySelect(notes);
                        String role = randomlySelect(roles);
                        data = note + " " + role + " " + ln + " " + fn;
                        tokens.add(note);
                        tokens.add(role);
                        tokens.add(ln);
                        tokens.add(fn);
                        break;
                    }
                }

            }

            personData.add(data);
            personTokens.add(tokens);
            messyModeIndex++;

            //if all messy modes are used shuffel them to have other sequences
            if (messyModeIndex > 0 && messyModeIndex % messyModes.size() == 0) {
                Collections.shuffle(messyModes, random);
            }
        }

        //joins
        /*
        * <Nachname>/(\n)?<Nachname>
        * <Nachname> (<Name>)
        * <Nachname> <Vorname>\n(<Nachname> <Vorname>)
         */
        String delim = randomlySelect(delimiters);
        StringJoiner sj = new StringJoiner(delim);
        List<String> allTokens = new ArrayList<>();
        for (int i = 0; i < personData.size(); i++) {
            String pd = personData.get(i);
            List<String> tokens = personTokens.get(i);

            //use only brackets if more than one person
            boolean withBrackets = random.nextInt(2) == 0;
            if (withBrackets && personData.size() > 1) {
                String[] bracket = randomlySelect(brackets);
                pd = bracket[0] + pd + bracket[1];

                tokens.add(0, bracket[0]);
                tokens.add(bracket[1]);
            }

            allTokens.addAll(tokens);
            if (i != personData.size() - 1) {
                allTokens.add(delim.trim());
            }
            sj.add(pd);
        }

        md.plain = sj.toString();
        md.tokens = allTokens;
        return md;
    }

    private String randomDepartment() {
        String dep = RandomStringUtils.randomAlphabetic(2).toUpperCase();

        int size = random.nextInt(5);

        for (int i = 0; i < size; i++) {
            dep += "-" + RandomStringUtils.randomAlphabetic(1).toUpperCase();
        }

        return dep;
    }

    private class MessyData {

        private String plain;
        private List<String> tokens;
        private Map<String, List<Person>> reason2ambiguity;

        public MessyData() {
            reason2ambiguity = new HashMap<>();
        }
    }

    //==========================================================================
    //helper
    private <T> T randomlySelect(List<T> list) {
        return list.get(random.nextInt(list.size()));
    }

    private boolean randomlyDecide(float current, float max) {
        float rndFloat = random.nextFloat() * max;
        //the more current value reaches max the more propable it is
        return current > rndFloat;
    }

    private String changeCase(Random random, String name) {
        switch (random.nextInt(3)) {
            case 0:
                return name.toLowerCase();
            case 1:
                return name.toUpperCase();
            case 2:
                return name;
        }
        return name;
    }

    //==========================================================================
    //setter for settings
    public void setMessyFile(File messyFile) {
        this.messyFile = messyFile;
    }

    public void setNMFile(File nmFile) {
        this.nmFile = nmFile;
    }

    public void setPersonIndexFile(File personIndexFile) {
        this.personIndexFile = personIndexFile;
    }

    public void setAmbiguityFile(File ambiguityFile) {
        this.ambiguityFile = ambiguityFile;
    }

    public void setSettingsFile(File settingsFile) {
        this.settingsFile = settingsFile;
    }
    
    public void setFirstnameFiles(List<File> firstnameFiles) {
        this.firstnameFiles = firstnameFiles;
    }

    public void setLastnameFiles(List<File> lastnameFiles) {
        this.lastnameFiles = lastnameFiles;
    }

    public void setRandom(Random random) {
        this.random = random;
    }

    public void setPersonIndexSize(int personIndexSize) {
        this.personIndexSize = personIndexSize;
    }

    public void setMessyDataSize(int messyDataSize) {
        this.messyDataSize = messyDataSize;
    }

    public void setMessyModes(List<Integer> messyModes) {
        this.messyModes = messyModes;
    }

    public void setMaxPersonsCount(int maxPersonsCount) {
        this.maxPersonsCount = maxPersonsCount;
    }

    public void setChangeNameCase(boolean changeNameCase) {
        this.changeNameCase = changeNameCase;
    }

    public void setLastnameAmbiguityGroupCount(int lastnameAmbiguityGroupCount) {
        this.lastnameAmbiguityGroupCount = lastnameAmbiguityGroupCount;
    }

    public void setLastnameAmbiguityGroupSize(int lastnameAmbiguityGroupSize) {
        this.lastnameAmbiguityGroupSize = lastnameAmbiguityGroupSize;
    }

    public void setFirstnameAmbiguityGroupCount(int firstnameAmbiguityGroupCount) {
        this.firstnameAmbiguityGroupCount = firstnameAmbiguityGroupCount;
    }

    public void setFirstnameAmbiguityGroupSize(int firstnameAmbiguityGroupSize) {
        this.firstnameAmbiguityGroupSize = firstnameAmbiguityGroupSize;
    }

    public void setDoubleNameSize(int doubleNameSize) {
        this.doubleNameSize = doubleNameSize;
    }

    //==========================================================================
    public static void main(String[] args) throws Exception {

        List<PersonMessyGenerator> gens = Arrays.asList(
                mode()
        );

        for (PersonMessyGenerator gen : gens) {
            gen.generate();
            System.out.println("============");
        }
    }

    private static PersonMessyGenerator mode() {
        File resultFolder = Constants.groundTruthFolder;
        resultFolder.mkdirs();

        PersonMessyGenerator gen = new PersonMessyGenerator();
        gen.setMessyFile(new File(resultFolder, "messy-persons.csv"));
        gen.setNMFile(new File(resultFolder, "nm.csv"));
        gen.setPersonIndexFile(new File(resultFolder, "person-index.csv"));
        gen.setAmbiguityFile(new File(resultFolder, "ambiguity.csv"));
        gen.setSettingsFile(new File(resultFolder, "settings.json"));

        gen.setFirstnameFiles(Arrays.asList(
                new File("names/common-firstname-america-single.csv")
        ));
        gen.setLastnameFiles(Arrays.asList(
                new File("names/common-lastname-us.csv")
        ));

        gen.setRandom(new Random(1));
        gen.setPersonIndexSize(100);
        gen.setMessyDataSize(300);
        gen.setMaxPersonsCount(4);
        gen.setMessyModes(Arrays.asList(
                onlyLastname,
                onlyFirstname,
                lastnameCommaFirstname,
                lastnameCommaFirstnameLetter,
                lastnameSpaceFirstname,
                lastnameDepartment,
                departmentNewLineLastnameFirstname,
                firstnameLastname,
                lastnameFirstnameEmail,
                noteRoleLastnameFirstname
        ));
        gen.setChangeNameCase(false);

        gen.setLastnameAmbiguityGroupCount(3);
        gen.setLastnameAmbiguityGroupSize(3);
        gen.setFirstnameAmbiguityGroupCount(3);
        gen.setFirstnameAmbiguityGroupSize(3);

        gen.setDoubleNameSize(5);

        return gen;
    }

}
