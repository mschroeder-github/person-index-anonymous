package anonymous.group;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.StringJoiner;
import static java.util.stream.Collectors.toSet;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.io.FileUtils;
import org.json.JSONObject;

/**
 * Evaluates results given a ground truth.
 * 
 */
public class Evaluator {

    private List<File> groundTruthFolders;

    public Evaluator() {

    }

    public class EvaluationResult {

        File groundTruth;
        File miningResult;

        double precision;
        double recall;
        double fscore;

        double precisionCellAvg;
        double recallCellAvg;
        double fscoreCell;
        
        double precisionAmb;
        double recallAmb;
        double fscoreAmb;
        
        JSONObject settingsObj;
    }

    public List<EvaluationResult> evalutate() throws Exception {
        List<EvaluationResult> results = new ArrayList<>();

        for (File groundTruthFolder : groundTruthFolders) {

            List<File> miningResultFolders = Arrays.asList(groundTruthFolder.listFiles(f -> f.isDirectory()));
            miningResultFolders.sort((a, b) -> {
                int comp = Integer.compare(a.getName().length(), b.getName().length());
                if(comp == 0) {
                    return a.getName().compareTo(b.getName());
                }
                return comp;
            });
            
            for (File miningResultFolder : miningResultFolders) {

                EvaluationResult result = new EvaluationResult();
                
                //settings
                File settings = new File(groundTruthFolder, "settings.json");
                result.settingsObj = new JSONObject(FileUtils.readFileToString(settings, StandardCharsets.UTF_8));
                
                
                System.out.println("compare " + groundTruthFolder.getPath() + " with " + miningResultFolder.getPath());

                List<Person> gt = loadPerson(new File(groundTruthFolder, Constants.personIndexFilename));
                List<Person> mr = loadPerson(new File(miningResultFolder, Constants.personIndexFilename));

                List<int[]> gtNM = loadNM(new File(groundTruthFolder, Constants.nmFilename));
                List<int[]> mrNM = loadNM(new File(miningResultFolder, Constants.nmFilename));
                
                List<AmbiguityEntry> gtAmbEntries = loadAmbiguityEntries(new File(groundTruthFolder, Constants.ambiguityFilename));
                List<AmbiguityEntry> mrAmbEntries = loadAmbiguityEntries(new File(miningResultFolder, Constants.ambiguityFilename));
                
                //we have to skip the n:m entries that are ambiguous == true
                int gtNMCount = gtNM.size();
                gtNM.removeIf(array -> array[2] == 1);
                int nmAmbCount = gtNMCount - gtNM.size();
                System.out.println(nmAmbCount + " n:m ambiguities removed");
                
                mrNM.removeIf(array -> array.length > 2 && array[2] == 1);

                Set<Person> gtSet = new HashSet<>(gt);
                Set<Person> mrSet = new HashSet<>(mr);

                System.out.println(gtSet.size() + " ground truth persons vs " + mrSet.size() + " mining result persons");

                List<Person[]> correctMatches = intersectPersons(gtSet, mrSet);
                Set<Person> correctPersons = correctMatches.stream().map(array -> array[1]).collect(toSet());
                Set<Person> incorrectPersons = subtract(mrSet, correctPersons);

                //precision = relevant intersect retrieved / retrieved
                double precision = correctMatches.size() / (double) mrSet.size();

                //recall = relevant intersect retrieved / relevant
                double recall = correctMatches.size() / (double) gtSet.size();

                double fscore = 2 * ((precision * recall) / (precision + recall));

                System.out.println("correct: " + correctMatches.size() + " / " + gtSet.size());
                System.out.println("incorrect: " + incorrectPersons.size() + " / " + mrSet.size());

                System.out.println("precision: " + precision);
                System.out.println("recall: " + recall);
                System.out.println("f-score: " + fscore);

                System.out.println("all:");
                for (Person gtP : gt) {
                    System.out.println("  " + gtP);
                }

                System.out.println("correct guessed by mining result:");
                for (Person[] match : correctMatches) {
                    System.out.println("  " + match[0] + " -> " + match[1]);
                }

                System.out.println("incorrect guessed by mining result:");
                for (Person ip : incorrectPersons) {
                    System.out.println("  " + ip);
                }

                double precisionCellAvg = 0;
                double recallCellAvg = 0;

                Map<Integer, Integer> gt2mrPersonId = new HashMap<>();
                
                System.out.println();
                System.out.println("n to m relation check for each correct person:");
                //check n:m for the correct found ones
                for (Person[] match : correctMatches) {
                    gt2mrPersonId.put(match[0].getId(), match[1].getId());
                    
                    //check if person was found in all cells
                    
                    Set<Integer> gtCells = gtNM.stream().filter(e -> e[1] == match[0].getId()).map(e -> e[0]).collect(toSet());
                    Set<Integer> mrCells = mrNM.stream().filter(e -> e[1] == match[1].getId()).map(e -> e[0]).collect(toSet());

                    Set<Integer> correctCells = intersection(gtCells, mrCells);

                    //relevant intersect retrieved / retrieved
                    double precisionCell = correctCells.size() / (double) mrCells.size();

                    //relevant intersect retrieved / relevant
                    double recallCell = correctCells.size() / (double) gtCells.size();

                    if (Double.isNaN(precisionCell)) {
                        precisionCell = 0.0;
                    }
                    if (Double.isNaN(recallCell)) {
                        recallCell = 0.0;
                    }

                    System.out.println("\t" + match[1] + " correct=" + correctCells.size() + "/" + mrCells.size() + " precision=" + precisionCell + " recall=" + recallCell);

                    precisionCellAvg += precisionCell;
                    recallCellAvg += recallCell;
                }

                precisionCellAvg /= correctMatches.size();
                recallCellAvg /= correctMatches.size();

                double fscoreCell = 2 * ((precisionCellAvg * recallCellAvg) / (precisionCellAvg + recallCellAvg));

                System.out.println("precision avg (n:m): " + precisionCellAvg);
                System.out.println("recall avg (n:m): " + recallCellAvg);

                
                result.groundTruth = groundTruthFolder;
                result.miningResult = miningResultFolder;
                result.precision = precision;
                result.recall = recall;
                result.fscore = fscore;

                result.precisionCellAvg = precisionCellAvg;
                result.recallCellAvg = recallCellAvg;
                result.fscoreCell = fscoreCell;

                
                
                
                System.out.println();
                System.out.println("ambiguity check:");
                List<AmbiguityEntry[]> ambiguityMatches = intersectAmbiguity(gtAmbEntries, mrAmbEntries);
                int gtAmbCount = 0;
                int mrAmbCount = 0;
                int correctAmbMatchCount = 0;
                for(AmbiguityEntry ae : gtAmbEntries) {
                    gtAmbCount += ae.toSet().size();
                }
                for(AmbiguityEntry ae : mrAmbEntries) {
                    mrAmbCount += ae.toSet().size();
                }
                for(AmbiguityEntry[] ambiguityMatch : ambiguityMatches) {
                    Set<Integer> setGt2Mr = new HashSet<>();
                    for(Integer i : ambiguityMatch[0].toSet()) {
                        setGt2Mr.add(gt2mrPersonId.get(i));
                    }
                    
                    Set<Integer> set = intersection(setGt2Mr, ambiguityMatch[1].toSet());
                    correctAmbMatchCount += set.size();
                }
                
                //precision = relevant intersect retrieved / retrieved
                double precisionAmb = correctAmbMatchCount / (double) mrAmbCount;

                //recall = relevant intersect retrieved / relevant
                double recallAmb = correctAmbMatchCount / (double) gtAmbCount;

                double fscoreAmb = 2 * ((precisionAmb * recallAmb) / (precisionAmb + recallAmb));
                
                System.out.println(correctAmbMatchCount + "/" + gtAmbCount + " correct ambiguity matches");
                System.out.println("Amb Precision: " + precisionAmb);
                System.out.println("Amb Recall: " + recallAmb);
                System.out.println("Amb fscore: " + fscoreAmb);
                
                result.precisionAmb = precisionAmb;
                result.recallAmb = recallAmb;
                result.fscoreAmb = fscoreAmb;
                
                results.add(result);
                
                System.out.println();
                System.out.println("====================================================================");
                System.out.println("====================================================================");
                System.out.println();
                
            } //mining result folder

        } //for each ground truth
        
        return results;
    }

    public String toLatex(List<EvaluationResult> results) {
        StringBuilder sb = new StringBuilder();

        //EvaluationResult firstResult = results.get(0);
        //int mn = firstResult.settingsObj.getInt("doubleNameSize");
        //int amb = firstResult.settingsObj.getInt("lastnameAmbiguityGroupCount");
        
        sb.append("\\begin{tabular}{|c|r|r|r|r|r|| r|r|r|| r|r|r|| r|r|r|}\n");
        sb.append("\\hline\n");
        StringJoiner header1 = new StringJoiner(" & ");
        header1.add("Nr.");
        header1.add("$|P|$");
        header1.add("$|T|$");
        header1.add("Max");
        header1.add("MN");
        header1.add("Amb");
        //header1.add("Algorithm");
        
        header1.add("$prec_P$");
        header1.add("$recall_P$");
        header1.add("$fscore_P$");
        
        header1.add("$prec_R$");
        header1.add("$recall_R$");
        header1.add("$fscore_R$");
        
        header1.add("$prec_A$");
        header1.add("$recall_A$");
        header1.add("$fscore_A$");
        sb.append(header1.toString() + " \\\\\n");
        sb.append("\\hline\n");
        sb.append("\\hline\n");

        String format = "%.2f";

        int nr = 1;
        for (EvaluationResult result : results) {

            StringJoiner sj = new StringJoiner(" & ");

            sj.add(String.format("%d", nr));
            sj.add(String.format("%d", result.settingsObj.getInt("personIndexSize")));
            sj.add(String.format("%d", result.settingsObj.getInt("messyDataSize")));
            sj.add(String.format("%d", result.settingsObj.getInt("maxPersonsCount")));
            sj.add(String.format("%d", result.settingsObj.getInt("doubleNameSize")));
            sj.add(String.format("%d", result.settingsObj.getInt("lastnameAmbiguityGroupCount")));
            //sj.add(String.format("%s", result.miningResult.getName()));

            sj.add(String.format(Locale.US, format, result.precision));
            sj.add(String.format(Locale.US, format, result.recall));
            sj.add(String.format(Locale.US, format, result.fscore));

            sj.add(String.format(Locale.US, format, result.precisionCellAvg));
            sj.add(String.format(Locale.US, format, result.recallCellAvg));
            sj.add(String.format(Locale.US, format, result.fscoreCell));
            
            sj.add(String.format(Locale.US, format, result.precisionAmb));
            sj.add(String.format(Locale.US, format, result.recallAmb));
            sj.add(String.format(Locale.US, format, result.fscoreAmb));

            sb.append(sj.toString() + " \\\\\n");
            sb.append("\\hline\n");
            nr++;
        }
        
        sb.append("\\end{tabular}\n");

        return sb.toString();
    }

    //find all matching pairs (0 = gt, 1 = mr)
    private List<Person[]> intersectPersons(Set<Person> gt, Set<Person> mr) {
        List<Person[]> l = new ArrayList<>();
        for (Person gtp : gt) {
            for (Person mrp : mr) {
                if (gtp.getFirstname().equals(mrp.getFirstname()) && 
                    gtp.getLastname().equals(mrp.getLastname())
                    ) {
                    
                    if(gtp.hasMiddlename() && mrp.hasMiddlename()) {
                        
                        if(gtp.getMiddlename().equals(mrp.getMiddlename())) {
                            l.add(new Person[]{gtp, mrp});
                        }
                        
                    } else {
                        l.add(new Person[]{gtp, mrp});
                    }
                }
            }
        }
        return l;
    }

    private List<AmbiguityEntry[]> intersectAmbiguity(List<AmbiguityEntry> gt, List<AmbiguityEntry> mr) {
        List<AmbiguityEntry[]> l = new ArrayList<>();
        for(AmbiguityEntry gta : gt) {
            for(AmbiguityEntry mra : mr) {
                if(gta.cell == mra.cell && gta.reason.equals(mra.reason)) {
                    l.add(new AmbiguityEntry[]{ gta, mra });
                    break;
                }
            }
        }
        return l;
    }
    
    private List<Person> loadPerson(File file) throws IOException {
        List<Person> list = new ArrayList<>();

        CSVParser p = CSVFormat.DEFAULT.parse(new FileReader(file));
        for (CSVRecord rec : p.getRecords()) {
            if (rec.getRecordNumber() == 1) {
                continue;
            }

            Person person = new Person(Integer.parseInt(rec.get(0)), rec.get(1), rec.get(3));
            if(!rec.get(2).trim().isEmpty()) {
                person.setMiddlename(rec.get(2));
            }
            list.add(person);
        }
        p.close();

        return list;
    }

    private List<int[]> loadNM(File file) throws IOException {
        List<int[]> list = new ArrayList<>();

        CSVParser p = CSVFormat.DEFAULT.parse(new FileReader(file));
        for (CSVRecord rec : p.getRecords()) {
            if (rec.getRecordNumber() == 1) {
                continue;
            }

            if(rec.size() > 2) { 
                list.add(new int[]{Integer.parseInt(rec.get(0)), Integer.parseInt(rec.get(1)), rec.get(2).equals("true") ? 1 : 0});
            } else {
                list.add(new int[]{Integer.parseInt(rec.get(0)), Integer.parseInt(rec.get(1))});
            }
        }
        p.close();

        return list;
    }
    
    private List<AmbiguityEntry> loadAmbiguityEntries(File file) throws IOException {
        List<AmbiguityEntry> l = new ArrayList<>();
        CSVParser p = CSVFormat.DEFAULT.parse(new FileReader(file));
        for (CSVRecord rec : p.getRecords()) {
            if (rec.getRecordNumber() == 1) {
                continue;
            }

            AmbiguityEntry entry = new AmbiguityEntry();
            
            entry.cell = Integer.parseInt(rec.get(0));
            entry.reason = rec.get(1);
            
            for(String seg : rec.get(2).split(",")) {
                Person person = new Person(Integer.parseInt(seg), "", "");
                entry.persons.add(person);
            }
            
            l.add(entry);
        }
        p.close();
        return l;
    }

    public void setGroundTruthFolders(List<File> groundTruthFolders) {
        this.groundTruthFolders = groundTruthFolders;
    }

    //==========================================================================
    //set helper
    
    public static <T> Set<T> intersection(Set<T> a, Set<T> b) {
        a = new HashSet<>(a);
        a.retainAll(b);
        return a;
    }

    public static <T> Set<T> subtract(Set<T> a, Set<T> b) {
        a = new HashSet<>(a);
        a.removeAll(b);
        return a;
    }

    public static <T> Set<T> union(Set<T> a, Set<T> b) {
        a = new HashSet<>(a);
        a.addAll(b);
        return a;
    }
    
    //==========================================================================
    
    public static void main(String[] args) throws Exception {
        Evaluator evaluator = new Evaluator();

        evaluator.setGroundTruthFolders(Arrays.asList(
                Constants.groundTruthFolder
        ));

        List<EvaluationResult> results = evaluator.evalutate();

        String latex = evaluator.toLatex(results);

        System.out.println();
        System.out.println();
        System.out.println(latex);
    }
}
