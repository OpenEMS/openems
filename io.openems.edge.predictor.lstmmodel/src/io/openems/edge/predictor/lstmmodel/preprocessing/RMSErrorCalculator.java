package io.openems.edge.predictor.lstmmodel.preprocessing;

import java.util.ArrayList;

public class RMSErrorCalculator {
	public static double calculateRMSError(ArrayList<Double> predicted, ArrayList<Double> actual) {
        int size = predicted.size();
        
        if (size != actual.size()) {
            throw new IllegalArgumentException("ArrayLists must have the same size.");
        }
        
        double sumOfSquaredDifferences = 0.0;
        for (int i = 0; i < size; i++) {
            double difference = predicted.get(i) - actual.get(i);
            sumOfSquaredDifferences += difference * difference;
        }
        
        double meanSquaredError = sumOfSquaredDifferences / size;
        double rmsError = Math.sqrt(meanSquaredError);
        
        return rmsError;
    }

}
