package anonymous.group;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import opennlp.tools.namefind.NameFinderME;
import opennlp.tools.namefind.TokenNameFinderModel;
import opennlp.tools.util.Span;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.output.FileWriterWithEncoding;
import org.json.JSONArray;

/**
 * A baseline approach based on OpenNLP to solve the person index challenge.
 * 
 */
public class BaselineApproach {

    public static void run(List<File> groundTruthFolders, NameFinderME nameFinder, boolean useFirstnameGazetteer, boolean clearAdaptiveData, boolean useProps) throws IOException {
        for (File groundTruthFolder : groundTruthFolders) {

            Set<Person> persons = new HashSet<>();
            int personIndex = 0;

            //read input data
            File f = new File(groundTruthFolder, Constants.messyPersonFilenameJson);
            for (String line : FileUtils.readLines(f, StandardCharsets.UTF_8)) {

                //read tokens and use name finder
                JSONArray array = new JSONArray(line);
                String[] tokens = array.toList().stream().toArray(i -> new String[i]);
                List<String> tokenList = Arrays.asList(tokens);
                Span[] spans = nameFinder.find(tokens);
                double[] probs = nameFinder.probs(spans);

                //System.out.println(line);
                //System.out.println(Arrays.toString(tokens));
                //System.out.println(Arrays.toString(spans));
                //System.out.println("=========================");
                //each span is a detected person
                for (int i = 0; i < spans.length; i++) {
                    Span span = spans[i];
                    double prob = probs[i];

                    //the tokens of the detected persons
                    List<String> personTokens = tokenList.subList(span.getStart(), span.getEnd());

                    //propability could be used
                    if (useProps && prob <= 0.5) {
                        //System.out.println("\t" + personTokens + " " + prob);
                        continue;
                    }

                    //size is 1 (first or lastname), 2 (first and last name) or 3 (with middle name)
                    if (personTokens.size() < 1 || personTokens.size() > 3) {
                        continue;
                    }
                    
                    //System.out.println(personTokens);

                    //create a person based on the tokens
                    Person person = new Person();
                    //could be wrong to assume last name
                    if (personTokens.size() == 1) {
                        person.setLastname(Utils.toProperCase(personTokens.get(0)));
                    } else if (personTokens.size() == 2) {
                        person.setLastname(Utils.toProperCase(personTokens.get(0)));
                        person.setFirstname(Utils.toProperCase(personTokens.get(1)));
                    } else if (personTokens.size() == 3) {
                        person.setFirstname(Utils.toProperCase(personTokens.get(0)));
                        person.setMiddlename(Utils.toProperCase(personTokens.get(1)));
                        person.setLastname(Utils.toProperCase(personTokens.get(2)));
                    }

                    //only unique persons
                    if (!persons.contains(person)) {
                        persons.add(person);
                        person.setId(personIndex++);
                    }
                }

                //Clear Adaptive Data (CAD) in name finder
                if (clearAdaptiveData) {
                    nameFinder.clearAdaptiveData();
                }
            }

            System.out.println(persons.size() + " raw persons found with name finder");

            //System.out.println(persons.size() + " persons");
            //use last name to cluster persons
            //Map<String, List<Person>> name2persons = new HashMap<>();
            //for (Person person : persons) {
                //System.out.println("\t" + person);
            //    name2persons.computeIfAbsent(person.getLastname(), p -> new ArrayList<>()).add(person);
            //}

            //find best person name pair with regex [A-Z][a-z]+
            String pattern = "[A-Z][a-z]+";

            Set<String> firstnameGazetter = new HashSet<>(FileUtils.readLines(new File("names/common-firstname-america-single.csv"), StandardCharsets.UTF_8));

            //find only persons that have clean names
            Set<Person> cleanPersons = new HashSet<>();
            //for (Entry<String, List<Person>> personEntry : name2persons.entrySet()) {
            for (Person p : persons) {
                boolean firstnameOK = p.getFirstname() != null && p.getFirstname().matches(pattern);
                boolean lastnameOK =  p.getLastname() != null && p.getLastname().matches(pattern);

                if (firstnameOK && lastnameOK) {

                    //firstname correction hack
                    if (useFirstnameGazetteer) {
                        if (firstnameGazetter.contains(p.getLastname())) {
                            p.swap();
                        }
                    }

                    cleanPersons.add(p);
                }
            }
            //}

            System.out.println(cleanPersons.size() + " clean persons");

            //output folder based on settings
            //String name = "OpenNLP" + (useFirstnameGazetteer ? "-FNG" : "") + (clearAdaptiveData ? "-CAD" : "") + (useProps ? "-PROP" : "");
            String name = "bl" + (useFirstnameGazetteer ? "G" : "") + (clearAdaptiveData ? "C" : "") + (useProps ? "P" : "");
            
            File miningResultFolder = new File(groundTruthFolder, name);
            miningResultFolder.mkdir();

            File fout = new File(miningResultFolder, Constants.personIndexFilename);
            System.out.println("write " + cleanPersons.size() + " persons to " + fout.getPath());
            CSVPrinter w = CSVFormat.DEFAULT.print(new FileWriterWithEncoding(fout, StandardCharsets.UTF_8));
            w.printRecord("id", "firstname", "middlename", "lastname");
            int id = 0;
            for (Person person : cleanPersons) {
                person.setId(id);
                w.printRecord(id++, person.getFirstname(), person.getMiddlename(), person.getLastname());
            }
            w.close();

            //create n:m list
            //for all persons if a token equals last name matches we create a n:m entry 
            List<int[]> nm = new ArrayList<>();
            List<AmbiguityEntry> ambiguities = new ArrayList<>();
            
            int lineIndex = 0;
            for (String line : FileUtils.readLines(f, StandardCharsets.UTF_8)) {
                JSONArray array = new JSONArray(line);
                String[] tokens = array.toList().stream().toArray(i -> new String[i]);
                
                for (String token : tokens) {
                    
                    List<Person> foundPersons = new ArrayList<>();

                    for (Person person : cleanPersons) {
                        if (token.equals(person.getLastname()) || token.equals(person.getFirstname())) {
                            if(!foundPersons.contains(person)) {
                                foundPersons.add(person);
                            }
                        }
                    }
                    
                    if(foundPersons.size() == 1) {
                        nm.add(new int[]{lineIndex, foundPersons.get(0).getId()});
                    } else if(foundPersons.size() > 1) {
                        //if the person is ambiguous to other persons write an entry in the ambiguity table
                        AmbiguityEntry ambEntry = new AmbiguityEntry();
                        ambEntry.cell = lineIndex;
                        ambEntry.reason = token;
                        ambEntry.persons = foundPersons;
                        ambiguities.add(ambEntry);
                    }
                }
                
                lineIndex++;
            }

            System.out.println(nm.size() + " n:m entries created");

            //write n:m list
            File nmFile = new File(miningResultFolder, Constants.nmFilename);
            System.out.println("write " + nm.size() + " n:m entries to " + nmFile.getPath());
            w = CSVFormat.DEFAULT.print(new FileWriterWithEncoding(nmFile, StandardCharsets.UTF_8));
            w.printRecord("cell", "person");
            for (int[] entry : nm) {
                w.printRecord(entry[0], entry[1]);
            }
            w.close();

            //write ambiguities list
            File ambiguitiesFile = new File(miningResultFolder, Constants.ambiguityFilename);
            System.out.println("write " + ambiguities.size() + " ambiguous entries to " + ambiguitiesFile.getPath());
            w = CSVFormat.DEFAULT.print(new FileWriterWithEncoding(ambiguitiesFile, StandardCharsets.UTF_8));
            w.printRecord("cell", "reason", "persons");
            for (AmbiguityEntry entry : ambiguities) {
                w.printRecord(entry.cell, entry.reason, entry.toListString());
            }
            w.close();
        }
    }
    
    public static void main(String[] args) throws IOException {
        
        //NOTE: download 'en-ner-person.bin' at http://opennlp.sourceforge.net/models-1.5/
        TokenNameFinderModel model;
        try (InputStream modelIn = new FileInputStream(Constants.enNerPersonBin)) {
            model = new TokenNameFinderModel(modelIn);
        }

        NameFinderME nameFinder = new NameFinderME(model);
        
        //mail to Chief Morgan (Wilson), [remove Baker, Robert]
        String[] text = new String[] { "mail", "to", "Chief", "Morgan", "(", "Wilson", ")",",", "[", "remove",  "Baker", ",",  "Robert", "]" };
        for(Span span : nameFinder.find(text)) {
            for(int i = span.getStart(); i < span.getEnd(); i++) {
                System.out.print(text[i] + " " + nameFinder.probs()[i]);
            }
            
            System.out.println();
        }

        List<File> groundTruthFolders = Arrays.asList(
                Constants.groundTruthFolder
        );

        for (boolean[] b : Utils.bool(3)) {
            System.out.println("run with " + Arrays.toString(b));
            run(groundTruthFolders, nameFinder, b[0], b[1], b[2]);
            
            System.out.println();
        }
    }
    
}
