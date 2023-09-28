package io.openems.edge.predictor.lstm.performance;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.math3.distribution.TDistribution;
import org.apache.commons.math3.stat.StatUtils;

import io.openems.edge.predictor.lstm.common.DataStatistics;

public class PerformanceMatrix {
	ArrayList<Double> target = new ArrayList<Double>();
	ArrayList<Double> predicted= new ArrayList<Double>();
	double allowedError = 0.0;
	
	public  PerformanceMatrix(ArrayList<Double> tar, ArrayList<Double> predict, double allowedErr) {
		target = tar; 
		predicted= predict;
		 allowedError = allowedErr;
		
		

	}

	public static double meanAbsluteError(ArrayList<Double> target, ArrayList<Double> predicted) {

		if (predicted.size() != target.size()) {
			throw new IllegalArgumentException("Input lists must have the same size");
		}

		double sumError = 0.0;
		for (int i = 0; i < predicted.size(); i++) {
			double error = Math.abs(predicted.get(i) - target.get(i));
			sumError += error;
		}

		return sumError / predicted.size();

	}

	public static double rmsError(ArrayList<Double> target, ArrayList<Double> predicted) {
		if (predicted.size() != target.size()) {
			throw new IllegalArgumentException("Input lists must have the same size");
		}

		double sumSquaredError = 0.0;
		for (int i = 0; i < predicted.size(); i++) {
			double error = predicted.get(i) - target.get(i);
			sumSquaredError += error * error;
		}

		double meanSquaredError = sumSquaredError / predicted.size();
		return Math.sqrt(meanSquaredError);
	}

	public static double meanSquaredError(ArrayList<Double> target, ArrayList<Double> predicted) {
		if (predicted.size() != target.size()) {
			throw new IllegalArgumentException("Input lists must have the same size");
		}

		double sumSquaredError = 0.0;
		for (int i = 0; i < predicted.size(); i++) {
			double error = predicted.get(i) - target.get(i);
			sumSquaredError += error * error;
		}

		return sumSquaredError / predicted.size();
	}

	public static double accuracy(ArrayList<Double> target, ArrayList<Double> predicted, double allowedPercentage) {
		double count = 0;

		for (int i = 0; i < predicted.size(); i++) {
			double diff = Math.abs(predicted.get(i) - target.get(i)) / Math.max(predicted.get(i), target.get(i));
			if (diff <= allowedPercentage) {
				count++;
			}
		}

		return (double) count / predicted.size();

	}

	public static double meanAbslutePercentage(ArrayList<Double> target, ArrayList<Double> predicted) {
		if (predicted.size() != target.size()) {
			throw new IllegalArgumentException("Input lists must have the same size");
		}

		double sumPercentageError = 0.0;
		for (int i = 0; i < predicted.size(); i++) {
			double absoluteError = Math.abs(predicted.get(i) - target.get(i));
			double percentageError = absoluteError / target.get(i) * 100.0;
			sumPercentageError += percentageError;
		}

		return sumPercentageError / predicted.size();
	}
	public static double pvalue(ArrayList<Double>target,ArrayList<Double>predicted)
	{
		 // Check if the input lists have the same size
        if (predicted.size() != target.size()) {
            throw new IllegalArgumentException("Input lists must have the same size.");
        }

        // Calculate the differences between predicted and actual values
        List<Double> differences = new ArrayList<>();
        for (int i = 0; i < predicted.size(); i++) {
            differences.add(predicted.get(i) - target.get(i));
        }

        // Calculate the mean and standard deviation of the differences
        double[] differencesArray = differences.stream().mapToDouble(Double::doubleValue).toArray();
        double mean = StatUtils.mean(differencesArray);
        double stdDev = Math.sqrt(StatUtils.variance(differencesArray));

        // Calculate the t-statistic
        double tStat = mean / (stdDev / Math.sqrt(predicted.size()));

        // Degrees of freedom
        int degreesOfFreedom = predicted.size() - 1;

        // Create a T-distribution with the appropriate degrees of freedom
        TDistribution tDistribution = new TDistribution(degreesOfFreedom);

        // Calculate the two-tailed p-value
        double pValue = 2 * (1.0 - tDistribution.cumulativeProbability(Math.abs(tStat)));

        return pValue;
	}
	
	public  void statusReport() {
       System.out.println("");
		
		System.out.println("..................Performance Report.............................");
		
		System.out.println("average of actual data = " + DataStatistics.getMean(target) );
		System.out.println("average of prediction data = " + DataStatistics.getMean(predicted));

		System.out.println("Stander deviation of actual data = " + DataStatistics.getSTD(target));
		System.out.println("Stander deviation of predicted data = " + DataStatistics.getSTD(predicted));

		System.out.println("Mean abslute error = " + meanAbsluteError(target, predicted)+ " (average absolute difference between the predicted values and the actual values)");
		System.out.println("RMS error = " + rmsError(target, predicted) +" (square root of the MSE)");
		System.out.println("Mean squared error  = " + meanSquaredError(target, predicted) + " (average of the squared differences between predicted values and actual values)");
		System.out.println("Mean abslute percentage error  = " + meanAbslutePercentage(target, predicted) +"(measures the average percentage difference between predicted and actual values)");
		System.out.println("accuracy for " + allowedError * 100 + " % error margine = "
				+ accuracy(target, predicted, allowedError)*100+" %");
		//System.out.println("pvalue =" + pvalue(target,predicted));
		
		System.out.println("");
	}
}
