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

        } catch (Exception e) {
            System.out.println("Unable to convert data.");
            e.printStackTrace();
        }
    }


    private static void AprioriAlgorithm() throws Exception {
        // Generate of Candidates (C_k) is by joining L_{k-1} with itself

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

        long startTime = System.currentTimeMillis();
        createSizeOneItemSetsByEncodedIndexNumber();
        int itemsetNumber=1; //the current itemset being looked at
        int nbFrequentSets=0;

        while (itemSetsByIndex.size()>0) {

            calculateFrequentItemsets();

            if(itemSetsByIndex.size()!=0)
            {
                nbFrequentSets+=itemSetsByIndex.size();
                System.out.println("Found "+itemSetsByIndex.size()+" frequent itemsets of size " + itemsetNumber + " (with support "+(minSup*100)+"%)");
                createNewItemsetsFromPreviousOnes();
            }

            itemsetNumber++;
        }
        long endTime = System.currentTimeMillis();
        System.out.println("Execution time is: "+((double)(endTime-startTime)/1000) + " seconds.");
        System.out.println("Found "+nbFrequentSets+ " frequents sets for support "+(minSup*100)+"% (absolute "+Math.round(numInstances*minSup)+")");
        System.out.println("Done");

        Lister();
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

    private static void createNewItemsetsFromPreviousOnes()
    {
        // by construction, all existing itemsets have the same size
        int currentSizeOfItemsets = itemSetsByIndex.get(0).length;

        HashMap<String, int[]> tempCandidates = new HashMap<String, int[]>(); //temporary candidates

        // compare each pair of itemsets of size n-1
        for(int i=0; i<itemSetsByIndex.size(); i++)
        {
            for(int j=i+1; j<itemSetsByIndex.size(); j++)
            {
                int[] X = itemSetsByIndex.get(i);
                int[] Y = itemSetsByIndex.get(j);

                assert (X.length==Y.length);

                //make a string of the first n-2 tokens of the strings
                int [] newCand = new int[currentSizeOfItemsets+1];
                for(int s=0; s<newCand.length-1; s++) {
                    newCand[s] = X[s];
                }

                int ndifferent = 0;
                // then we find the missing value
                for(int s1=0; s1<Y.length; s1++)
                {
                    boolean found = false;
                    // is Y[s1] in X?
                    for(int s2=0; s2<X.length; s2++) {
                        if (X[s2]==Y[s1]) {
                            found = true;
                            break;
                        }
                    }
                    if (!found){ // Y[s1] is not in X
                        ndifferent++;
                        // we put the missing value at the end of newCand
                        newCand[newCand.length -1] = Y[s1];
                    }

                }

                // we have to find at least 1 different, otherwise it means that we have two times the same set in the existing candidates
                assert(ndifferent>0);


                if (ndifferent==1) {
                    // HashMap does not have the correct "equals" for int[] :-(
                    // I have to create the hash myself using a String :-(
                    // I use Arrays.toString to reuse equals and hashcode of String
                    Arrays.sort(newCand);
                    tempCandidates.put(Arrays.toString(newCand),newCand);
                }
            }
        }

        //set the new itemsets
        itemSetsByIndex = new ArrayList<int[]>(tempCandidates.values());
    }

    /** put "true" in trans[i] if the integer i is in line */
    private static void line2booleanArray(int[] array, boolean[] trans) {
        Arrays.fill(trans, false);
        //put the contents of that line into the transaction array
        for (int i = 0; i < array.length; i++) {
            int parsedVal = array[i];
            trans[parsedVal]=true; //if it is not a 0, assign the value to true
        }
    }

    private static void calculateFrequentItemsets() throws Exception {
        List<int[]> frequentCandidates = new ArrayList<int[]>(); //the frequent candidates for the current itemset

        boolean match; //whether the transaction has all the items in an itemset
        int count[] = new int[itemSetsByIndex.size()]; //the number of successful matches, initialized by zeros

        boolean[] trans = new boolean[integerToStringEncoded.size()];

        // for each transaction
        int k = 0;
        while (k < dataEntriesWithEncodedStringIndices.size()) {

            // boolean[] trans = extractEncoding1(data_in.readLine());
            line2booleanArray(dataEntriesWithEncodedStringIndices.get(k++), trans);

            // check each candidate
            for (int c = 0; c < itemSetsByIndex.size(); c++) {
                match = true; // reset match to false
                // tokenize the candidate so that we know what items need to be
                // present for a match
                int[] cand = itemSetsByIndex.get(c);
                //int[] cand = candidatesOptimized[c];
                // check each item in the itemset to see if it is present in the
                // transaction
                for (int xx : cand) {
                    if (trans[xx] == false) {
                        match = false;
                        break;
                    }
                }
                if (match) { // if at this point it is a match, increase the count
                    count[c]++;
                    //log(Arrays.toString(cand)+" is contained in trans "+i+" ("+line+")");
                }
            }

        }

        for (int i = 0; i < itemSetsByIndex.size(); i++) {
            // if the count% is larger than the minSup%, add to the candidate to
            // the frequent candidates
            if ((count[i] / (double) (numInstances)) >= minSup) {
                foundFrequentItemSet(itemSetsByIndex.get(i),count[i]);
                frequentCandidates.add(itemSetsByIndex.get(i));
            }
            //else log("-- Remove candidate: "+ Arrays.toString(candidates.get(i)) + "  is: "+ ((count[i] / (double) numTransactions)));
        }

        //new candidates are only the frequent candidates
        itemSetsByIndex = frequentCandidates;
    }

    /** triggers actions if a frequent item set has been found  */
    private static void foundFrequentItemSet(int[] itemset, int support) {
        String New = Arrays.toString(itemset);
        String New1 = New.substring(0, New.length() - 1) + ", " + support + "]";
        tupples.add(New1);
        System.out.println(Arrays.toString(itemset) + "  ("+ ((support / (double) numInstances))+" "+support+")");
    }

    public static void Lister() {
        int b = tupples.size();
        if (b == 0) {
            System.exit(0);
        }
        int i,
                j,
                k = 0,
                m = 0;
        String newb = tupples.get(b - 1);
        int a = ((newb.substring(1, newb.length() - 1).split(", ")).length);
        int[][] lol = new int[b][a - 1];
        int[] lols = new int[b];
        for (i = 0; i < b; i++) {
            newb = tupples.get(i);
            String[] poop = newb.substring(1, newb.length() - 1).split(", ");
            for (j = 0; j < poop.length - 1; j++) {
                lol[i][j] = Integer.parseInt(poop[j]);
            }
            lols[i] = Integer.parseInt(poop[j]);
            if ((j + 1) == a && k == 0) {
                k = i;
            }
            poop = null;
        }
        System.out.println("\nAssociation Rules: When Minimum Confidence=" + minConf * 100 + "%");
        for (i = k; i < b; i++) {
            for (j = 0; j < k; j++) {
                m += assoc_print(lol[i], lol[j], lols[i], lols[j]);
            }
        }
        if (m == 0) {
            System.out.println("No association rules passed the minimum confidence of " + minConf * 100 + "%");
        }
    }

    public static int assoc_print(int[] a, int[] b, int a1, int b1) {
        String win = "(",
                lose = "(";
        int i,
                j,
                k = 0;
        int[] loss = new int[a.length];
        for (i = 0; i < b.length && b[i] != 0; i++) {
            k = 1;
            win = win + b[i] + ",";
            for (j = 0; j < a.length; j++) {
                if (b[i] == a[j]) {
                    k = 0;
                    loss[j] = 1;
                }
            }
        }
        win = win.substring(0, win.length() - 1) + ")";
        for (i = 0; i < a.length; i++) {
            if (loss[i] == 0) {
                lose = lose + a[i] + ",";
            }
        }
        lose = lose.substring(0, lose.length() - 1) + ")";
        if (k == 0) {
            double Lol = (double) a1 / b1;
            if (Lol > minConf) {
                System.out.printf("%s ==> %s :	%.2f%c \n", win, lose, Lol * 100, 37);
                return 1;
            }
        }
        return 0;
    }
}
