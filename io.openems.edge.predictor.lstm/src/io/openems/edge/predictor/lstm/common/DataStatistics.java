package io.openems.edge.predictor.lstm.common;

import java.util.ArrayList;

public class DataStatistics {

	/**
	 * Get the mean of the array.
	 * 
	 * @param data the data
	 * @return mean value
	 */
	public static double getMean(ArrayList<Double> data) {

		double sum = 0.0;
		for (double x : data) {
			sum += x;
		}
		double mean = sum / data.size();
		return mean;
	}

	/**
	 * Get the Standard deviation.
	 * 
	 * @param data the data
	 * @return stdDeviation standard deviation
	 */
	public static double getStandardDeviation(ArrayList<Double> data) {
		double mean = getMean(data);

		double sumSquaredDeviations = 0.0;
		for (double x : data) {
			sumSquaredDeviations += Math.pow(x - mean, 2);
		}

		double variance = sumSquaredDeviations / (data.size());
		double stdDeviation = Math.sqrt(variance);
		return stdDeviation;
	}

}
