package anonymous.group;

import java.io.File;

/**
 * Constants used in the other classes.
 * 
 */
public class Constants {
    
    /**
     * Person name finder model.
     * See http://opennlp.sourceforge.net/models-1.5/
     */
    public static final String enNerPersonBin = "en-ner-person.bin";

    /**
     * Name of the ground truth folder.
     */
    public static final File groundTruthFolder = new File("ground-truth-folder");

    /**
     * File name of the generated messy data.
     */
    public static final String messyPersonFilename = "messy-persons.csv";
    
    /**
     * File name of the generated messy data in token form using JSON.
     */
    public static final String messyPersonFilenameJson = messyPersonFilename + ".json";
    
    /**
     * File name of the person index.
     */
    public static String personIndexFilename = "person-index.csv";
    
    /**
     * File name of the file containing the relation between text and person.
     */
    public static String nmFilename = "nm.csv";
    
    /**
     * File name of the file containing ambiguity entries.
     */
    public static String ambiguityFilename = "ambiguity.csv";
    
}
