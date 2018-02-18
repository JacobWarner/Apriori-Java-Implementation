package com.company;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.converters.ConverterUtils.DataSource;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class Main {

    // Hard coding stuff for now
    private static double minSup = 0.55;
    private static double minConf = 0.9;
    private static String filePath = "vote.arff";

    private static int numAttributes;
    private static int numInstances;
    private static String[] attributeNames;
    private static ArrayList<ArrayList<Integer>> encodedInstances;

    private static HashMap<String,Integer> stringToIntegerEncoded = new HashMap<>();
    private static HashMap<Integer,String> integerToStringEncoded = new HashMap<>();

    private static DataSource data = null;
    private static Instances instances = null;

    public static void main(String[] args) throws Exception {
        grabData(filePath);

        if (data == null || instances == null) {
            System.out.println("Error gathering data from given file.");
            return;
        }

        ConcurrentHashMap<ArrayList<Integer>, Integer> frequentItemSets = AprioriAlgorithm();
        ArrayList<AssociationRule> rules = ruleGeneration(frequentItemSets);

        rules.sort(new Comparator<AssociationRule>() {
            @Override
            public int compare(AssociationRule o1, AssociationRule o2) {
                double confDiff = o2.getConfidence() - o1.getConfidence();
                return ((confDiff > 0) ? 1 : (confDiff < 0) ? -1 : 0);
            }
        });

        printAllRules(rules);

        System.out.println("End of program. Now testing runtime with different supports, 0.1 to 1.0, and a fixed confidence of 0.9\n");

        // testRuntimeOfProgram();
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

    private static ConcurrentHashMap<ArrayList<Integer>, Integer> AprioriAlgorithm() throws Exception {
        System.out.println("Apriori");
        System.out.println("=======");

        int numSupportedInstances = (int)(minSup*numInstances);
        System.out.println("\nMinimum support: " + minSup + " (" + numSupportedInstances + " instances)");
        System.out.println("Minimum metric <confidence>: " + minConf);
        System.out.println("\nGenerated sets of large itemsets:");

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
     * Creating itemsets using their respective indices within the data
     * might be easier to code with. This is up for discussion.
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

    private static <T> ArrayList<T> union(ArrayList<T> list1, ArrayList<T> list2) {
        Set<T> set = new HashSet<T>();

        set.addAll(list1);
        set.addAll(list2);

        return new ArrayList<T>(set);
    }

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
                    if (!frequentItemSets.containsKey(premise)) {
                        for (ArrayList<Integer> instance : encodedInstances) {
                            if (instance.containsAll(premise)) {
                                frequentItemSets.put(premise, (frequentItemSets.getOrDefault(item, 0) + 1));
                            }
                        }
                    }

                    premiseCount = frequentItemSets.get(premise);
                    int impliedCount = frequentItemSets.get(item);

                    double itemSupport = ((double)impliedCount / numInstances);
                    double subsetSupport = ((double)premiseCount / numInstances);

                    double confidence = (itemSupport / subsetSupport);

                    if (confidence >= minConf) {
                        ArrayList<Integer> implication = new ArrayList<>(implied);
                        AssociationRule newRule = new AssociationRule(premise, premiseCount, implication, impliedCount, confidence);
                        if (!rules.contains(newRule)) {
                            rules.add(newRule);
                        }
                    }
                }
            }
        }

        return rules;
    }

    // This generates the empty set, but we don't use it later on
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

        return sb.toString();
    }

    private static void printAllRules(ArrayList<AssociationRule> rules) {
        System.out.print("\nBest rules found:\n\n");

        int ruleNum = 1;
        for (AssociationRule rule : rules) {
            System.out.println("\t" + ruleNum + ". " + ruleToString(rule));
            ruleNum++;
        }
    }

    private static void printFrequentItemSets(int frequentItemNum, int k) {
        System.out.println("\nSize of set of large itemsets L(" + k + "): " + frequentItemNum);
    }

    private static void testRuntimeOfProgram() throws Exception {
        grabData(filePath);

        if (data == null || instances == null) {
            System.out.println("Error gathering data from given file.");
            return;
        }

        HashMap<Double, Double> algorithmRunTime = new HashMap<>();
        double maxSupport = 1.0;
        double minSupport = 0.1;
        long startTime = 0;
        long endTime = 0;
        double timeInSeconds = 0;

        while (minSupport <= maxSupport) {
            minSup = minSupport;
            startTime = System.nanoTime();
            AprioriAlgorithmWithoutPrint();
            endTime = System.nanoTime();
            timeInSeconds = ((double) (endTime-startTime)) / 1E9;
            algorithmRunTime.put(minSup, timeInSeconds);
            minSupport += 0.1;
        }

        for (double sup = 0.1; sup <= maxSupport;) {
            System.out.printf("Minimum support: %.1f\n", sup);
            System.out.printf("\t Apriori Algorithm: %.6f seconds\n", algorithmRunTime.get(sup));
            sup = sup + 0.1;
        }
    }

    private static void AprioriAlgorithmWithoutPrint() throws Exception {
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
    }
}
