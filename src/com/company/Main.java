package com.company;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.converters.ConverterUtils.DataSource;
import weka.core.converters.Loader;

import javax.sound.midi.SysexMessage;
import java.util.*;

public class Main {

    // Hard coding stuff for now
    private static double minSup = 0.55;
    private static double minConf = 0.9;
    private static String filePath = "vote.arff";

    private static int numAttributes;
    private static int numInstances;
    private static String[] attributeNames;
    private static List<int[]> itemSetsByIndex;
    private static List<String[]> itemSetsByString;
    private static List<int[]> dataEntriesWithEncodedStringIndices;
    private static HashMap<String,Integer> stringToIntegerEncoded = new HashMap<>();
    private static HashMap<Integer,String> integerToStringEncoded = new HashMap<>();
    private static List <String> tupples = new ArrayList <> ();
    private static DataSource data = null;
    private static Instances instances = null;

    public static void main(String[] args) throws Exception {
        grabData(filePath);

        if (data == null || instances == null) {
            System.out.println("Error gathering data from given file.");
            return;
        }

        AprioriAlgorithm();

//        for (int i = 0; i < dataEntriesWithEncodedStringIndices.size(); i++) {
//            System.out.println(Arrays.toString(dataEntriesWithEncodedStringIndices.get(i)));
//        }
//
        System.out.println();
        for (int j = 0; j < integerToStringEncoded.size(); j++) {
            System.out.println("Index=" + j + " ----> " + integerToStringEncoded.get(j));
        }
    }

    // ARFF File handling
    private static void grabData(String fileName) {
        try {
            data = new DataSource(fileName);
            instances = data.getDataSet();
            // setting class attribute if the data format does not provide this information
            // For example, the XRFF format saves the class attribute information as well
            if (instances.classIndex() == -1) {
                instances.setClassIndex(instances.numAttributes() - 1);
            }

            // Number of attributes
            numAttributes = instances.numAttributes();
            // Number of data entries
            numInstances = instances.numInstances();

            // Gathering array of all attribute names
            attributeNames = new String[numAttributes];
            for (int i = 0; i < numAttributes; i++) {
                attributeNames[i] = instances.attribute(i).name();
            }

            // Creates encoded attribute names (i.e. "Class=democrat" and "Class=republican"
            createEncodedAttributeNames();
            createSizeOneItemSetsByEncodedIndexNumber();

        } catch (Exception e) {
            System.out.println("Unable to convert data.");
            e.printStackTrace();
        }
    }


    private static void AprioriAlgorithm() throws Exception {

        long startTime = System.currentTimeMillis();

        /// Generate of Candidates (C_k) is by joining L_{k-1} with itself

        // C_k: candidate itemset of size k
        // L_k: frequent itemset of size k

        // Create frequent items of size 1, L_1
        // k =2
        // while FrequentItemSet L_{k-1} not empty...
        // Generate candidates from L_{k-1} with minSup
        // for each transaction/instance (t) in database
        // Generate subsets of t that are candidates (C_t)
        // for each candidate c in C_t, c.count++
        // L_k = {c in C_k : c.count >= minSup}
        // k++;
        // return L = U_k L_k

        long endTime = System.currentTimeMillis();
    }

    /**
     * Creating itemsets using their respective indices within the data
     * might be easier to code with. This is up for discussion.
     */
    private static void createSizeOneItemSetsByEncodedIndexNumber() {
        itemSetsByIndex = new ArrayList<int[]>();

        for (int i = 0; i < numAttributes*2; i++) {
            int[] candidate = {i};
            itemSetsByIndex.add(candidate);
        }
    }

    private static void createSizeOneItemSetsByString() {
        itemSetsByString = new ArrayList<String[]>();

        for (int i = 0; i < numAttributes; i++) {
            String[] candidate = {attributeNames[i]};
            itemSetsByString.add(candidate);
        }
    }

    private static void findFrequentItemSets() {

    }

    // Use this to join L_{k-1} with itself to generate C_k
    private static void joinTwoSets() {
        // Assume that items in L_{k-1} are in lexicographic order
        // Let l_1 and l_2 be two itemsets in L_{k-1}
        // They are joinable if:
            // Their first k-2 items match
        // The result of the join is the set of
            // First k-2 items (common to both sets)
            // Last elements of l_1 and l_2

        // Conditions for join:
            // First k-2 items are common
            // l_1[k-1] < l_2[k-1] to avoid duplicates
    }

    private static void createEncodedAttributeNames() throws Exception {
        dataEntriesWithEncodedStringIndices = new ArrayList<>();

        int k = 0;
        for (Instance instance : data.getDataSet()) {
            int[] encodedDataEntry = new int[numAttributes];
            String[] split = instance.toString().split(",");

            for (int i = 0; i < split.length; i++) {
                String encodedString = attributeNames[i] + "=" + split[i];
                if (!stringToIntegerEncoded.containsKey(encodedString)) {
                    integerToStringEncoded.put(k, encodedString);
                    stringToIntegerEncoded.put(encodedString, k);
                    k++;
                }
                encodedDataEntry[i] = stringToIntegerEncoded.get(encodedString);
            }
            dataEntriesWithEncodedStringIndices.add(encodedDataEntry);
        }
    }
}
