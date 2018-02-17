package com.company;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;

/**
 * Created by Jacob on 2/17/2018.
 */
public class AssociationRule {
    private ArrayList<Integer> premise;
    private int premiseCount;
    private ArrayList<Integer> implication;
    private int implicationCount;
    private double confidence;

    public AssociationRule(ArrayList<Integer> premise, int premiseCount,
                           ArrayList<Integer> implication, int implicationCount,
                           double confidence) {
        this.premise = premise;
        this.premiseCount = premiseCount;
        this.implication = implication;
        this.implicationCount = implicationCount;
        this.confidence = confidence;
    }

    public ArrayList<Integer> getPremise() {
        return premise;
    }

    public void setPremise(ArrayList<Integer> premise) {
        this.premise = premise;
    }

    public int getPremiseCount() {
        return premiseCount;
    }

    public void setPremiseCount(int premiseCount) {
        this.premiseCount = premiseCount;
    }

    public ArrayList<Integer> getImplication() {
        return implication;
    }

    public void setImplication(ArrayList<Integer> implication) {
        this.implication = implication;
    }

    public int getImplicationCount() {
        return implicationCount;
    }

    public void setImplicationCount(int implicationCount) {
        this.implicationCount = implicationCount;
    }

    public double getConfidence() {
        return confidence;
    }

    public double getRoundedConfidence() {
        return round(confidence, 2);
    }

    public void setConfidence(double confidence) {
        this.confidence = confidence;
    }

    public static double round(double value, int places) {
        if (places < 0) throw new IllegalArgumentException();

        BigDecimal bd = new BigDecimal(value);
        bd = bd.setScale(places, RoundingMode.HALF_UP);
        return bd.doubleValue();
    }
}
