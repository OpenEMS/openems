package io.openems.edge.predictor.lstm.common;

import java.util.ArrayList;

public class DataStatistics {

	/**
	 * Calculates the mean (average) of a list of double values. This method
	 * computes the mean of the provided list of double values by summing all the
	 * elements and dividing the sum by the total number of elements in the list.
	 *
	 * @param data An ArrayList of double values from which to calculate the mean.
	 * @return The mean of the provided data as a double value.
	 * @throws ArithmeticException if the input list is empty (division by zero).
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
	 * Calculates the standard deviation of a list of double values. This method
	 * computes the standard deviation of the provided list of double values.
	 * Standard deviation measures the amount of variation or dispersion in the
	 * data. It is calculated as the square root of the variance, which is the
	 * average of the squared differences between each data point and the mean.
	 *
	 * @param data An ArrayList of double values for which to calculate the standard
	 *             deviation.
	 * @return The standard deviation of the provided data as a double value.
	 * @throws IllegalArgumentException if the input list is empty.
	 */

	public static double getStanderDeviation(ArrayList<Double> data) {
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
