/**
 * APRIORI ALGORITHM - JAVA IMPLEMENTATION
 *
 * AUTHOR: Jacob Warner
 *
 * @se <a href="https://github.com/JacobWarner/Apriori-Java-Implementation">GitHub Repository</a>
 */

package com.company;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartUtilities;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.axis.NumberTickUnit;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.category.DefaultCategoryDataset;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.converters.ConverterUtils.DataSource;

import java.awt.*;
import java.io.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class Main {

    // Parameters set with default values that can be overwritten through command line arguments
    private static double minSup = 0.55;
    private static double minConf = 0.9;
    private static int numRulesToPrint = 10;
    private static String inputFilePath = "vote.arff";
    private static String outputFilePath = "result.txt";
    private static String testRunTime = "n";

    // Data gathered from input file
    private static int numAttributes;
    private static int numInstances;
    private static String[] attributeNames;
    private static ArrayList<ArrayList<Integer>> encodedInstances;
    private static HashMap<String,Integer> stringToIntegerEncoded = new HashMap<>();
    private static HashMap<Integer,String> integerToStringEncoded = new HashMap<>();
    private static DataSource data = null;
    private static Instances instances = null;

    private static BufferedWriter writer = null;

    public static void main(String[] args) throws Exception {

        // Handling input arguments
        for (int i = 0; i < args.length; i++) {
            switch(i){
                case 0:
                    inputFilePath = args[0];
                    break;
                case 1:
                    minSup = Double.parseDouble(args[1]);
                    if (minSup < 0.0 || minSup > 1.0) {
                        System.out.println("Invalid minimum support. Using default of 0.55");
                        minSup = 0.55;
                    }
                    break;
                case 2:
                    minConf = Double.parseDouble(args[2]);
                    if (minConf < 0.0 || minConf > 1.0) {
                        System.out.println("Invalid minimum confidence. Using default of 0.9");
                        minConf = 0.9;
                    }
                    break;
                case 3:
                    numRulesToPrint = Integer.parseInt(args[3]);
                    if (numRulesToPrint < 0) {
                        System.out.println("Invalid number of rules to print. Using default of 10");
                    }
                    break;
                case 4:
                    outputFilePath = args[4];
                    break;
                case 5:
                    testRunTime = args[5];
                    if (!testRunTime.equalsIgnoreCase("y") && !testRunTime.equalsIgnoreCase("n")) {
                        System.out.println("Invalid input for runtime testing. Using default answer (n).");
                    }
                    break;
            }
        }

        grabData(inputFilePath);

        if (data == null || instances == null) {
            System.out.println("Error gathering data from given file. Exiting.");
            return;
        }

        writer = new BufferedWriter(new FileWriter(outputFilePath, true));
        writer.append("");
        writer.newLine();

        try {
            ConcurrentHashMap<ArrayList<Integer>, Integer> frequentItemSets = AprioriAlgorithm();
            ArrayList<AssociationRule> rules = ruleGeneration(frequentItemSets);

            sortRules(rules);
            printAllRules(rules);

            if (testRunTime.equalsIgnoreCase("y")) {
                writer.newLine();
                writer.newLine();
                writer.append("End of main program. Now testing runtime with different supports, 0.1 to 1.0");
                writer.newLine();
                testRuntimeOfProgram();
            }
        } finally {
            writer.newLine();
            writer.append("");
            if (writer != null) {
                writer.close();
            }

            System.out.println("Program complete.");
            System.out.println("Output file: " + outputFilePath);
        }
    }


    /**
     * Given a file path, it'll attempt to grab the necessary data from it.
     * {@link DataSource} allows the reading of files other than ARFF, but it must be appropriately formatted
     *
     * @param fileName - the file path of the data you want to read
     */
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
            System.out.println("Unable to convert data from file. Exiting.");
            System.out.println(e.getLocalizedMessage());
        }
    }


    /**
     * Apriori Algorithm
     *
     * @return (Key,Value) pairs of frequent itemsets and their respective frequencies in the data
     * @throws IOException - throws IOException if {@link BufferedWriter} isn't functional
     */
    private static ConcurrentHashMap<ArrayList<Integer>, Integer> AprioriAlgorithm() throws IOException {
        writer.append("=======");
        writer.newLine();
        writer.append("Apriori");
        writer.newLine();
        writer.append("=======");
        writer.newLine();

        int numSupportedInstances = (int)(minSup*numInstances);

        writer.newLine();
        writer.append("Input file: ").append(inputFilePath);
        writer.newLine();
        writer.append("Number of Instances: ").append(String.valueOf(numInstances));
        writer.newLine();
        writer.append("Number of Attributes: ").append(String.valueOf(numAttributes));
        writer.newLine();
        writer.append("Minimum support: ").append(String.valueOf(minSup)).append(" (").append(String.valueOf(numSupportedInstances)).append(" instances)");
        writer.newLine();
        writer.append("Minimum metric <confidence>: ").append(String.valueOf(minConf));
        writer.newLine();
        writer.append("Generated sets of large itemsets:");


        // ALGORITHM HERE

        int k = 2;
        ConcurrentHashMap<ArrayList<Integer>, Integer> frequentItemSets = new ConcurrentHashMap<>();
        ArrayList<ArrayList<Integer>> itemSetsOfSizeOne = createSizeOneItemSetsByEncodedIndexNumber();
        ArrayList<ArrayList<Integer>> currentFrequentItemSets = createItemSetsWithSupport(itemSetsOfSizeOne, frequentItemSets);
        ArrayList<ArrayList<Integer>> currentCandidateItemSets = new ArrayList<>();

        while (currentFrequentItemSets.size() > 0) {
            printFrequentItemSets(currentFrequentItemSets.size(), (k-1));
            currentCandidateItemSets = createCandidates(currentFrequentItemSets, k);
            currentFrequentItemSets = createItemSetsWithSupport(currentCandidateItemSets, frequentItemSets);
            k++;
        }

        return frequentItemSets;
    }


    /**
     * Creates itemsets with given minimum support
     *
     * @param items - sets of itemsets
     * @param frequentItemSets - frequent itemsets and their respective frequencies within the data
     * @return
     */
    private static ArrayList<ArrayList<Integer>> createItemSetsWithSupport(ArrayList<ArrayList<Integer>> items, ConcurrentHashMap<ArrayList<Integer>, Integer> frequentItemSets){
        ArrayList<ArrayList<Integer>> itemSet = new ArrayList<>();
        HashMap<ArrayList<Integer>, Integer> tempFrequent = new HashMap<>();

        for (ArrayList<Integer> item : items) {
            for (ArrayList<Integer> instance : encodedInstances) {
                if (instance.containsAll(item)) {
                    frequentItemSets.put(item, (frequentItemSets.getOrDefault(item, 0) + 1));
                    tempFrequent.put(item, (tempFrequent.getOrDefault(item, 0) + 1));
                }
            }
        }

        for (ArrayList<Integer> key : tempFrequent.keySet()){
            double support = ((double)tempFrequent.get(key))/((double)numInstances);

            if (support >= minSup) {
                itemSet.add(key);
            }
        }

        return itemSet;
    }


    /**
     * Creating candidates from the current frequent itemsets
     *
     * @param itemSet - sets of current frequent itemsets
     * @param k - num of items within the generated candidates itemsets
     * @return sets of candidate itemsets
     */
    private static ArrayList<ArrayList<Integer>> createCandidates(ArrayList<ArrayList<Integer>> itemSet, int k) {
        ArrayList<ArrayList<Integer>> candidateSet = new ArrayList<>();

        for (int i = 0; i < itemSet.size() - 1; i++) {
            for (int j = i; j < itemSet.size(); j++) {
                if (canTwoListsCombine(itemSet.get(i), itemSet.get(j), k)) {
                    ArrayList<Integer> combinedItemSet = union(itemSet.get(i), itemSet.get(j));

                    if (combinedItemSet.size() == k) {
                        candidateSet.add(combinedItemSet);
                    }
                }
            }
        }

        return candidateSet;
    }


    /**
     * Creating itemsets using their respective indices within the encoded data
     *
     * @return sets of 1-itemsets [ a set for each attribute and their labels (i.e. y/n) ]
     */
    private static ArrayList<ArrayList<Integer>> createSizeOneItemSetsByEncodedIndexNumber() {
        ArrayList<ArrayList<Integer>> sizeOneItemSets = new ArrayList<>();

        int numEncodedAttributes = stringToIntegerEncoded.keySet().size();
        for (int i = 0; i < numEncodedAttributes; i++) {
            ArrayList<Integer> candidate = new ArrayList<>(1);
            candidate.add(i);
            sizeOneItemSets.add(candidate);
        }

        return sizeOneItemSets;
    }


    /**
     * Encodes the attribute names for easier manipulation later. Follows the same format as Weka.
     *
     * @throws Exception - throws an Exception if {@link DataSource} isn't functional
     */
    private static void createEncodedAttributeNames() throws Exception {
        encodedInstances = new ArrayList<>();

        int k = 0;
        for (Instance instance : data.getDataSet()) {
            ArrayList<Integer> encodedDataEntry = new ArrayList<>(numAttributes);
            String[] split = instance.toString().split(",");

            for (int i = 0; i < split.length; i++) {
                String encodedString = attributeNames[i] + "=" + split[i];
                if (!stringToIntegerEncoded.containsKey(encodedString)) {
                    integerToStringEncoded.put(k, encodedString);
                    stringToIntegerEncoded.put(encodedString, k);
                    k++;
                }
                encodedDataEntry.add(i, stringToIntegerEncoded.get(encodedString));
            }
            encodedInstances.add(encodedDataEntry);
        }
    }


    /**
     * Checks to see if two itemsets can form a union.
     * It's necessary for creating candidate itemsets.
     *
     * @param list1 - an itemset of size k-1
     * @param list2 - another itemset of size k-1
     * @param k - the size of the combined itemset
     * @return true if they can combine, false otherwise
     */
    private static boolean canTwoListsCombine(ArrayList<Integer> list1, ArrayList<Integer> list2, int k) {
       boolean canTheyCombine = true;

       if (list1.size() != list2.size() || list1.get(k-2) >= list2.get(k-2)) {
           return false;
       }

       for (int i = 0; i < k-2; i++) {
           if (!list1.get(i).equals(list2.get(i))) {
               canTheyCombine = false;
           }
       }

       return canTheyCombine;
    }


    /**
     * Creates a union between two {@link ArrayList}
     *
     * @param list1 - an array list
     * @param list2 - another array list
     * @param <T> - type for array list
     * @return - the array list create by the union of list1 and list2
     */
    private static <T> ArrayList<T> union(ArrayList<T> list1, ArrayList<T> list2) {
        Set<T> set = new HashSet<T>();

        set.addAll(list1);
        set.addAll(list2);

        return new ArrayList<T>(set);
    }

    /**
     * Sorts a list of Association Rules first by confidence, then support, then frequencies
     *
     * @param rules - a list of {@link AssociationRule}s
     * @return a sorted list of Association Rules
     */
    private static ArrayList<AssociationRule> sortRules(ArrayList<AssociationRule> rules) {

        // Sort first by support (follows Weka)
        rules.sort(new Comparator<AssociationRule>() {
            @Override
            public int compare(AssociationRule o1, AssociationRule o2) {
                double supportDiff = o2.getSupport() - o1.getSupport();

                return (supportDiff > 0 ? 1 : supportDiff < 0 ? -1 : 0);
            }
        });

        // Then sort by confidence, support, and frequencies
        rules.sort(new Comparator<AssociationRule>() {
            @Override
            public int compare(AssociationRule o1, AssociationRule o2) {
                double confDiff = o2.getConfidence() - o1.getConfidence();
                if (confDiff > 0) {
                    return 1;
                } else if (confDiff < 0) {
                    return -1;
                } else {

                    double supportDiff = o2.getSupport() - o1.getSupport();

                    if (supportDiff > 0) {
                        return 1;
                    } else if (supportDiff < 0) {
                        return -1;
                    } else {

                        int implicationCountDiff = o2.getImplicationCount() - o1.getImplicationCount();

                        if (implicationCountDiff > 0) {
                            return 1;
                        } else if (implicationCountDiff < 0) {
                            return -1;
                        } else {

                            int premiseCountDiff = o2.getPremiseCount() - o1.getPremiseCount();

                            if (premiseCountDiff > 0) {
                                return 1;
                            } else if (premiseCountDiff < 0) {
                                return -1;
                            }

                            return 0;
                        }
                    }
                }
            }
        });

        return rules;
    }

    /**
     * Generates the rules given frequent itemsets
     *
     * @param frequentItemSets - frequent itemsets with their respective frequencies within the data
     * @return a list of {@link AssociationRule}s, which contain the premise, implication, their individual frequencies, and the confidence of the rule
     */
    private static ArrayList<AssociationRule> ruleGeneration(ConcurrentHashMap<ArrayList<Integer>, Integer> frequentItemSets) {
        ArrayList<AssociationRule> rules = new ArrayList<>();
        for (ArrayList<Integer> item : frequentItemSets.keySet()) {
            Set<Integer> set = new HashSet<>(item);

            for (Set<Integer> s : powerSet(set)) {
                if (s.isEmpty() || s.size() == 0 || s == null) continue;
                Set<Integer> implied = new HashSet<>(item);
                implied.removeAll(s);

                if (implied.size() > 0) {

                    ArrayList<Integer> premise = new ArrayList<>(s);

                    int premiseCount = 0;

                    // Extra check due to NullPointerExceptions
                    if (!frequentItemSets.containsKey(premise)) {
                        for (ArrayList<Integer> instance : encodedInstances) {
                            if (instance.containsAll(premise)) {
                                frequentItemSets.put(premise, (frequentItemSets.getOrDefault(item, 0) + 1));
                            }
                        }
                    }

                    premiseCount = frequentItemSets.get(premise);
                    int impliedCount = frequentItemSets.get(item);

                    double itemSupport = ((double)impliedCount / (double)numInstances);
                    double subsetSupport = ((double)premiseCount / (double)numInstances);

                    // Recalculate support if needed :(
                    if (itemSupport < minSup) {
                        double frequency = 0.0;
                        double frequencyP = 0.0;
                        for (ArrayList<Integer> instance : encodedInstances) {
                            if (instance.containsAll(item)) {
                                frequency++;
                            }
                            if (instance.containsAll(premise)) {
                                frequencyP++;
                            }
                        }

                        premiseCount = (int)frequencyP;
                        impliedCount = (int)frequency;

                        itemSupport = frequency/ (double)numInstances;
                        subsetSupport = frequencyP / (double)numInstances;
                    }
                    if (itemSupport < minSup) continue;

                    double confidence = (itemSupport / subsetSupport);

                    if (confidence >= minConf) {
                        ArrayList<Integer> implication = new ArrayList<>(implied);
                        AssociationRule newRule = new AssociationRule(premise, premiseCount, implication, impliedCount, confidence, itemSupport);
                        if (!rules.contains(newRule)) {
                            rules.add(newRule);
                        }
                    }
                }
            }
        }

        return rules;
    }


    /**
     * Generates the powerset of a set.
     * NOTE: It generates an empty set, but we check against that in the method, ruleGeneration
     *
     * @param originalSet - the original set
     * @param <T> - the set's type
     * @return - a set of sets (the powerset of the original set)
     */
    private static <T> Set<Set<T>> powerSet(Set<T> originalSet) {
        Set<Set<T>> sets = new HashSet<Set<T>>();
        if (originalSet.isEmpty()) {
            sets.add(new HashSet<T>());
            return sets;
        }

        List<T> list = new ArrayList<T>(originalSet);
        T head = list.get(0);
        Set<T> rest = new HashSet<T>(list.subList(1, list.size()));
        for (Set<T> set : powerSet(rest)) {
            Set<T> newSet = new HashSet<T>();
            newSet.add(head);
            newSet.addAll(set);
            sets.add(newSet);
            sets.add(set);
        }

        return sets;
    }


    /**
     * Used to format the rules as Weka does
     *
     * @param rule - an {@link AssociationRule}
     * @return - the given rule formatted into a String
     */
    private static String ruleToString(AssociationRule rule) {
        StringBuilder sb = new StringBuilder();

        ArrayList<Integer> leftSide = rule.getPremise();
        for (Integer i : leftSide) {
            String association = integerToStringEncoded.get(i) + " ";
            sb.append(association);
        }

        sb.append(rule.getPremiseCount());
        sb.append(" ==> ");

        ArrayList<Integer> rightSide = rule.getImplication();
        for (Integer i : rightSide) {
            String association = integerToStringEncoded.get(i) + " ";
            sb.append(association);
        }

        sb.append(rule.getImplicationCount());
        sb.append("    <conf:(");
        sb.append(rule.getRoundedConfidence());
        sb.append(")>");
        sb.append("    <sup:(");
        sb.append(rule.getRoundedSupport());
        sb.append(")>");

        return sb.toString();
    }


    /**
     * Prints all rules just as Weka does
     *
     * @param rules - a list of {@link AssociationRule}s
     * @throws IOException - throws an IOException if {@link BufferedWriter} isn't functional
     */
    private static void printAllRules(ArrayList<AssociationRule> rules) throws IOException {
        writer.newLine();

        if (rules.size() == 0) {
            writer.append("No rules found!");
            writer.newLine();
        } else {
            writer.append("There were ").append(String.valueOf(rules.size())).append(" rules found.");
            writer.newLine();
            writer.append("Best rules found:");
            writer.newLine();

            int ruleNum = 1;
            for (AssociationRule rule : rules) {
                writer.newLine();
                writer.append("\t").append(String.valueOf(ruleNum)).append(". ").append(ruleToString(rule));
                ruleNum++;
                if (ruleNum > numRulesToPrint) break;
            }
        }
    }


    /**
     * Prints frequent itemsets as Weka does
     *
     * @param frequentItemNum - number of frequent itemsets of k-size
     * @param k - the size of the frequent itemsets
     * @throws IOException - throws an IOException if {@link BufferedWriter} isn't functional
     */
    private static void printFrequentItemSets(int frequentItemNum, int k) throws IOException {
        writer.newLine();
        writer.append("Size of set of large itemsets L(").append(String.valueOf(k)).append("): ").append(String.valueOf(frequentItemNum));
    }


    /**
     * Tests the runtime of the Apriori Algorithm with support 0.1 through 1.0 (incrementing by 0.1)
     * NOTE: Includes runtime of rule generation, as it adds a bit of time in the lower supports.
     *
     * @throws Exception - throws Exception if {@link BufferedWriter} or {@link DataSource} are not functional
     */
    private static void testRuntimeOfProgram() throws Exception {
        HashMap<Double, Double> algorithmRunTime = new HashMap<>();
        HashMap<Double, Double> ruleGenerationRunTime = new HashMap<>();

        double maxSupport = 1.0;
        double minSupport = 0.1;
        long startTime = 0;
        long endTime = 0;
        double timeInSeconds = 0;

        while (minSupport <= maxSupport) {
            minSup = minSupport;

            // Tracking runtime of the Apriori algorithm
            startTime = System.nanoTime();
            ConcurrentHashMap<ArrayList<Integer>, Integer> frequentItemSets = AprioriAlgorithmWithoutPrint();
            endTime = System.nanoTime();
            timeInSeconds = ((double) (endTime-startTime)) / 1E9;
            algorithmRunTime.put(minSup, timeInSeconds);

            // Tracking runtime of rule generation
            startTime = System.nanoTime();
            ArrayList<AssociationRule> rules = ruleGeneration(frequentItemSets);
            endTime = System.nanoTime();
            timeInSeconds = ((double) (endTime-startTime)) / 1E9;
            ruleGenerationRunTime.put(minSupport, timeInSeconds);

            minSupport += 0.1;
        }

        writer.newLine();
        writer.append("==================");
        writer.newLine();
        writer.append("RUNTIME RESULTS");
        writer.newLine();
        writer.append("==================");
        writer.newLine();

        String temp = "";
        for (double sup = 0.1; sup <= maxSupport;) {
            writer.newLine();
            temp = String.format("Minimum support: %.1f\n", sup);
            writer.append(temp);
            writer.newLine();
            temp = String.format("\t Apriori Algorithm: %.6f seconds\n", algorithmRunTime.get(sup));
            writer.append(temp);
            writer.newLine();
            temp = String.format("\t Rule Generation: %.6f seconds\n", ruleGenerationRunTime.get(sup));
            writer.append(temp);
            writer.newLine();
            sup = sup + 0.1;
        }

        createRuntimeChart(algorithmRunTime, ruleGenerationRunTime);
    }


    /**
     * Apriori Algorithm used for testing (does not print to a text file)
     *
     * @throws Exception - throws Exception if {@link DataSource} is not functional
     */
    private static ConcurrentHashMap<ArrayList<Integer>, Integer> AprioriAlgorithmWithoutPrint() throws Exception {
        ConcurrentHashMap<ArrayList<Integer>, Integer> frequentItemSets = new ConcurrentHashMap<>();

        int k = 2;
        ArrayList<ArrayList<Integer>> itemSetsOfSizeOne = createSizeOneItemSetsByEncodedIndexNumber();
        ArrayList<ArrayList<Integer>> currentFrequentItemSets = createItemSetsWithSupport(itemSetsOfSizeOne, frequentItemSets);
        ArrayList<ArrayList<Integer>> currentCandidateItemSets;

        while (currentFrequentItemSets.size() > 0) {
            currentCandidateItemSets = createCandidates(currentFrequentItemSets, k);
            currentFrequentItemSets = createItemSetsWithSupport(currentCandidateItemSets, frequentItemSets);
            k++;
        }

        return frequentItemSets;
    }

    /**
     * Creates a runtime analysis chart with the support on the x-axis and runtime (in seconds) on the y-axis
     * NOTE: Run-time also includes Rule Generation time. If you want just the Apriori Algorithm, feel free to change the commented lines within the method.
     *
     * @param algorithmRunTime - (key,value) pairs of support with their respective run-times
     * @throws IOException - throws an IOException if {@link BufferedWriter} isn't functional
     */
    private static void createRuntimeChart(HashMap<Double, Double> algorithmRunTime, HashMap<Double, Double> ruleGenerationRunTime) throws IOException {
        DefaultCategoryDataset lineChartData = new DefaultCategoryDataset();

        for (double sup = 0.1; sup <= 1.0;) {

            // Rule Generation and Apriori Algorithm run-times
            lineChartData.addValue((algorithmRunTime.get(sup) + ruleGenerationRunTime.get(sup)), "Time", String.format("%.2f", sup));

            // Aprioro Algorithm run-times only
            // lineChartData.addValue(algorithmRunTime.get(sup), "Time", String.format("%.2f", sup));
            sup = sup + 0.1;
        }

        JFreeChart lineChartObject = ChartFactory.createLineChart(
                "Apriori Algorithm Analysis","Support",
                "Runtime (seconds)",
                lineChartData, PlotOrientation.VERTICAL,
                false,false,false);

        CategoryPlot plot = lineChartObject.getCategoryPlot();
        NumberAxis range = (NumberAxis)plot.getRangeAxis();
        range.setTickUnit(new NumberTickUnit(0.5));
        range.setLowerBound(0.0);

        lineChartObject.setBorderStroke(new BasicStroke(0.5f));

        int width = 960;    /* Width of the image */
        int height = 720;   /* Height of the image */
        File lineChart = new File( "RuntimeLineChart.jpeg" );
        ChartUtilities.saveChartAsJPEG(lineChart ,lineChartObject, width ,height);
    }
}
