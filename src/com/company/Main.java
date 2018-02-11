package com.company;
import weka.core.Instances;
import weka.core.converters.ConverterUtils;
import weka.core.converters.ConverterUtils.DataSource;

public class Main {

    public static void main(String[] args) {

        // ARFF File handling
        DataSource source = null;
        try {
            source = new DataSource("vote.arff");
            Instances data = source.getDataSet();
            // setting class attribute if the data format does not provide this information
            // For example, the XRFF format saves the class attribute information as well
            if (data.classIndex() == -1) {
                data.setClassIndex(data.numAttributes() - 1);
            }

            System.out.println(data.toSummaryString());

        } catch (Exception e) {
            e.printStackTrace();
        }

    }
}
