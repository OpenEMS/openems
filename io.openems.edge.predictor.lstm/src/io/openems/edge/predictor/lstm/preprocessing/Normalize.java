package io.openems.edge.predictor.lstm.preprocessing;

public class Normalize {
	public double[][] standData;
	
	public Normalize(double data1[][]) {

		standData = new double [data1.length][data1[0].length];
		for(int i=0; i< data1.length; i++) {
			standData[i]=standardize(data1[i]);
		        // Calculate mean and standard deviation
			}
		
		
	}
	
	
	
	 public  double[] standardize(double[] inputData) {
	        // Calculate mean and standard deviation
	        double sum = 0.0;
	        for (double x : inputData) {
	            sum += x;
	        }
	        double mean = sum / inputData.length;

	        double sumSquaredDeviations = 0.0;
	        for (double x : inputData) {
	            sumSquaredDeviations += Math.pow(x - mean, 2);
	        }
	        double variance = sumSquaredDeviations / (inputData.length);
	        double stdDeviation = Math.sqrt(variance);

	        // Standardize the data using Z-score
	        double[] standardizedData = new double[inputData.length];
	        for (int i = 0; i < inputData.length; i++) {
	            standardizedData[i] = (inputData[i] - mean) / stdDeviation;
	        }

	        return standardizedData;
	    }

}
