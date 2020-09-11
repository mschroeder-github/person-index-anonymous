package anonymous.group;

import static anonymous.group.PersonMessyGenerator.departmentNewLineLastnameFirstname;
import static anonymous.group.PersonMessyGenerator.firstnameLastname;
import static anonymous.group.PersonMessyGenerator.lastnameCommaFirstname;
import static anonymous.group.PersonMessyGenerator.lastnameCommaFirstnameLetter;
import static anonymous.group.PersonMessyGenerator.lastnameDepartment;
import static anonymous.group.PersonMessyGenerator.lastnameFirstnameEmail;
import static anonymous.group.PersonMessyGenerator.lastnameSpaceFirstname;
import static anonymous.group.PersonMessyGenerator.noteRoleLastnameFirstname;
import static anonymous.group.PersonMessyGenerator.onlyFirstname;
import static anonymous.group.PersonMessyGenerator.onlyLastname;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import opennlp.tools.namefind.NameFinderME;
import opennlp.tools.namefind.TokenNameFinderModel;
import org.apache.commons.io.FileUtils;

/**
 * To run ground truth generation, baseline approach and evaluation in one pipeline.
 * 
 */
public class Pipeline {

    private File outputFolder;
    
    private boolean tryAllFeatures = false;

    public Pipeline(File outputFolder) {
        this.outputFolder = outputFolder;
        FileUtils.deleteQuietly(outputFolder);
        outputFolder.mkdirs();
    }

    private File generateGroundTruth(int i, GroundTruthSetting setting) throws Exception {
        File resultFolder = new File(outputFolder, "ground-truth-" + i);
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

        gen.setRandom(new Random(setting.randomSeed));
        gen.setPersonIndexSize(setting.personIndexSize);
        gen.setMessyDataSize(setting.messyDataSize);
        gen.setMaxPersonsCount(setting.maxPersonCounts);
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

        gen.setLastnameAmbiguityGroupCount(setting.degreeOfAmbiguity);
        gen.setLastnameAmbiguityGroupSize(setting.degreeOfAmbiguity);
        gen.setFirstnameAmbiguityGroupCount(setting.degreeOfAmbiguity);
        gen.setFirstnameAmbiguityGroupSize(setting.degreeOfAmbiguity);

        gen.setDoubleNameSize(setting.doubleNameSize);

        gen.generate();

        return resultFolder;
    }

    private void applyBaselineApproach(List<File> groundTruthFolders) throws IOException {

        //NOTE: download 'en-ner-person.bin' at http://opennlp.sourceforge.net/models-1.5/
        TokenNameFinderModel model;
        try (InputStream modelIn = new FileInputStream(Constants.enNerPersonBin)) {
            model = new TokenNameFinderModel(modelIn);
        }

        NameFinderME nameFinder = new NameFinderME(model);

        if(tryAllFeatures) {
            for (boolean[] b : Utils.bool(3)) {
                BaselineApproach.run(groundTruthFolders, nameFinder, b[0], b[1], b[2]);
            }
        } else {
            BaselineApproach.run(groundTruthFolders, nameFinder, true, false, false);
        }
    }

    private List<Evaluator.EvaluationResult> evaluateResults(List<File> groundTruthFolders) throws Exception {
        Evaluator evaluator = new Evaluator();
        evaluator.setGroundTruthFolders(groundTruthFolders);

        List<Evaluator.EvaluationResult> results = evaluator.evalutate();

        String latex = evaluator.toLatex(results);

        System.out.println();
        System.out.println();
        System.out.println(latex);

        return results;
    }

    
    public List<Evaluator.EvaluationResult> run(List<GroundTruthSetting> settings) throws Exception {
        List<File> groundTruthFolders = new ArrayList<>();

        for (int i = 0; i < settings.size(); i++) {
            GroundTruthSetting setting = settings.get(i);
            groundTruthFolders.add(generateGroundTruth(i, setting));
        }

        applyBaselineApproach(groundTruthFolders);

        return evaluateResults(groundTruthFolders);
    }

    public void examineParameters(List<Evaluator.EvaluationResult> results) {

        MinAvgMaxSdDouble bl = new MinAvgMaxSdDouble();
        MinAvgMaxSdDouble blC = new MinAvgMaxSdDouble();
        MinAvgMaxSdDouble blG = new MinAvgMaxSdDouble();
        MinAvgMaxSdDouble blP = new MinAvgMaxSdDouble();

        for (Evaluator.EvaluationResult result : results) {
            //no parameter set
            if (result.miningResult.getName().equals("bl")) {
                bl.add(result.fscore);
            }
            //clear adaptive data
            if (result.miningResult.getName().equals("blC")) {
                blC.add(result.fscore);
            }
            //gazetteer
            if (result.miningResult.getName().equals("blG")) {
                blG.add(result.fscore);
            }
            //propability
            if (result.miningResult.getName().equals("blP")) {
                blP.add(result.fscore);
            }
        }

        System.out.println("blAvg: " + bl.toStringAvgSDLatex(3));
        System.out.println("blAvgC (clear adaptive data): " + blC.toStringAvgSDLatex(3));
        System.out.println("blAvgG (gazetteer): " + blG.toStringAvgSDLatex(3));
        System.out.println("blAvgP (probablity): " + blP.toStringAvgSDLatex(3));
        
        
        /*
        double blAvg = calculateAverage(bl);
        double blAvgC = calculateAverage(blC);
        double blAvgG = calculateAverage(blG);
        double blAvgP = calculateAverage(blP);
        
        System.out.println("blAvg: " + blAvg);
        System.out.println("blAvgC (clear adaptive data): " + blAvgC);
        System.out.println("blAvgG (gazetteer): " + blAvgG);
        System.out.println("blAvgP (probablity): " + blAvgP);
        
        System.out.println("bl:");
        for(double d : bl) {
            System.out.println("\t" + d);
        }
        System.out.println("blC:");
        for(double d : blC) {
            System.out.println("\t" + d);
        }
        System.out.println("blG:");
        for(double d : blG) {
            System.out.println("\t" + d);
        }
        System.out.println("blP:");
        for(double d : blP) {
            System.out.println("\t" + d);
        }
        */
        
    }

    /**
     * Settings for ground truth generation.
     */
    public static class GroundTruthSetting {

        int randomSeed;
        int personIndexSize;
        int messyDataSize;
        int maxPersonCounts;
        int degreeOfAmbiguity;
        int doubleNameSize;

        public GroundTruthSetting() {
            randomSeed = 1;
            personIndexSize = 100;
            messyDataSize = 300;
            maxPersonCounts = 4;
            degreeOfAmbiguity = 3;
            doubleNameSize = 5;
        }

        public GroundTruthSetting(int randomSeed, int personIndexSize, int messyDataSize, int maxPersonCounts, int degreeOfAmbiguity, int doubleNameSize) {
            this.randomSeed = randomSeed;
            this.personIndexSize = personIndexSize;
            this.messyDataSize = messyDataSize;
            this.maxPersonCounts = maxPersonCounts;
            this.degreeOfAmbiguity = degreeOfAmbiguity;
            this.doubleNameSize = doubleNameSize;
        }

        public static List<GroundTruthSetting> generate(int personIndexSizeMax, int groupSizeMax) {
            List<GroundTruthSetting> settings = new ArrayList<>();

            for (int personIndexSize = 10; personIndexSize <= personIndexSizeMax; personIndexSize += 10) {
                int messyDataSize = personIndexSize * 15;

                GroundTruthSetting setting = new GroundTruthSetting();
                setting.randomSeed = new Random().nextInt();
                setting.personIndexSize = personIndexSize;
                setting.messyDataSize = messyDataSize;

                setting.degreeOfAmbiguity = 0;
                setting.doubleNameSize = 0;

                for (int groupSize = 0; groupSize <= groupSizeMax; groupSize += 2) {
                    setting.maxPersonCounts = groupSize;

                    settings.add(setting);
                }
            }

            return settings;
        }

    }

    public static void main(String[] args) throws Exception {
        gts();
        //examineParameters();
    }
    
    //evaluates on various ground truth data
    public static void gts() throws Exception {
        int rnd = 1;
        
        
        Pipeline pipeline = new Pipeline(new File("pipeline-output"));
        pipeline.run(Arrays.asList(
                                          //P  data  G  amb MN
                new GroundTruthSetting(rnd, 1, 10,    0, 0, 0),
                new GroundTruthSetting(rnd, 1, 200,   0, 0, 0)
                
                ,new GroundTruthSetting(rnd, 20, 200, 0, 0, 0)
                ,new GroundTruthSetting(rnd, 20, 200, 10, 0, 0)
                ,new GroundTruthSetting(rnd, 20, 200, 10, 0, 4)
                ,new GroundTruthSetting(rnd, 20, 200, 10, 2, 4)
                ,new GroundTruthSetting(rnd, 20, 200, 10, 3, 4)
                ,new GroundTruthSetting(rnd, 40, 300, 10, 3, 4)
        ));
    }
    
    //evaluates baseline approach parameters
    public static void examineParameters() throws Exception {
        Pipeline pipeline = new Pipeline(new File("pipeline-output"));

        List<GroundTruthSetting> settings = GroundTruthSetting.generate(100, 10);
        System.out.println(settings.size());

        List<Evaluator.EvaluationResult> l = pipeline.run(settings);
        pipeline.examineParameters(l);
    }

}
