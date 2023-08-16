package io.openems.edge.predictor.lstmmodel.preprocessing;

public class ReverseNormalize {
	
	
	
	public static  double revStandardize(double data[],double zvalue) {
		//compute mean from orginal data
		double sum = 0.0;
        for (double x : data) {
            sum += x;
        }
        double mean = sum / data.length;
        
        // compute standard deviation 
        
        double sumSquaredDeviations = 0.0;
        for (double x : data) {
            sumSquaredDeviations += Math.pow(x - mean, 2);
        }
        double variance = sumSquaredDeviations / (data.length);
        double stdDeviation = Math.sqrt(variance);
 
        
		double revVal =(zvalue*stdDeviation)+mean;
		return revVal;
		
	}
	
	
}


