package com.company;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;

/**
 * Created by Jacob Warner on 2/17/2018.
 */
public class AssociationRule {
    private ArrayList<Integer> premise;
    private int premiseCount;
    private ArrayList<Integer> implication;
    private int implicationCount;
    private double confidence;
    private double support;

    /**
     * An Association Rule generated as part of the Apriori Algorithm
     *
     * @param premise          - the itemset on the left of the rule (premise)
     * @param premiseCount     - the premise itemset's frequency within the given data
     * @param implication      - the itemset on the right of the rule (implied)
     * @param implicationCount - the implied itemset's frequency within the given data
     * @param confidence       - the confidence of the rule
     * @param support          - the support of the rule
     */
    public AssociationRule(ArrayList<Integer> premise, int premiseCount,
                           ArrayList<Integer> implication, int implicationCount,
                           double confidence, double support) {
        this.premise = premise;
        this.premiseCount = premiseCount;
        this.implication = implication;
        this.implicationCount = implicationCount;
        this.confidence = confidence;
        this.support = support;
    }

    public ArrayList<Integer> getPremise() {
        return premise;
    }

    public int getPremiseCount() {
        return premiseCount;
    }

    public ArrayList<Integer> getImplication() {
        return implication;
    }

    public int getImplicationCount() {
        return implicationCount;
    }

    public double getConfidence() {
        return confidence;
    }

    public double getRoundedConfidence() {
        return round(confidence, 2);
    }

    public double getRoundedSupport() {
        return round(support, 2);
    }

    public static double round(double value, int places) {
        if (places < 0) throw new IllegalArgumentException();

        BigDecimal bd = new BigDecimal(value);
        bd = bd.setScale(places, RoundingMode.HALF_UP);
        return bd.doubleValue();
    }

    public double getSupport() {
        return support;
    }
}
