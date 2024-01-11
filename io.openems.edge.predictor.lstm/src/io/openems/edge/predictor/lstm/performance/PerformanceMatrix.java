package io.openems.edge.predictor.lstm.performance;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.math3.distribution.TDistribution;
import org.apache.commons.math3.stat.StatUtils;

import io.openems.edge.predictor.lstm.common.DataStatistics;

public class PerformanceMatrix {
	private ArrayList<Double> target = new ArrayList<Double>();
	private ArrayList<Double> predicted = new ArrayList<Double>();
	private double allowedError = 0.0;

	public PerformanceMatrix(ArrayList<Double> tar, ArrayList<Double> predict, double allowedErr) {
		this.target = tar;
		this.predicted = predict;
		 this.allowedError =  allowedErr;

	}

	/**
	 * Calculate the Mean Absolute Error (MAE) between two lists of values. This
	 * method computes the Mean Absolute Error (MAE) between the target values and
	 * the predicted values. MAE is a measure of the average absolute difference
	 * between corresponding elements in the two lists.
	 *
	 * @param target    The list of actual or target values.
	 * @param predicted The list of predicted values.
	 * @return The Mean Absolute Error (MAE) between the two lists.
	 * @throws IllegalArgumentException if the input lists do not have the same
	 *                                  size.
	 */

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

	/**
	 * Calculate the Root Mean Squared Error (RMSE) between two lists of values.
	 * This method computes the Root Mean Squared Error (RMSE) between the target
	 * values and the predicted values. RMSE is a measure of the square root of the
	 * average squared differences between corresponding elements in the two lists.
	 *
	 * @param target    The list of actual or target values.
	 * @param predicted The list of predicted values.
	 * @return The Root Mean Squared Error (RMSE) between the two lists.
	 * @throws IllegalArgumentException if the input lists do not have the same
	 *                                  size.
	 */
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

	/**
	 * Calculate the Mean Squared Error (MSE) between two lists of values.
	 * This method computes the Mean Squared Error (MSE) between the target values
	 * and the predicted values. MSE is a measure of the average squared differences
	 * between corresponding elements in the two lists.
	 *
	 * @param target    The list of actual or target values.
	 * @param predicted The list of predicted values.
	 * @return The Mean Squared Error (MSE) between the two lists.
	 * @throws IllegalArgumentException if the input lists do not have the same
	 *                                  size.
	 */

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

	/**
	 * Calculate the accuracy of predictions within a specified percentage
	 * tolerance. This method measures the accuracy of predictions by comparing the
	 * predicted values to the target values within a specified percentage
	 * tolerance. It returns the proportion of predictions that fall within the
	 * allowed percentage difference.
	 *
	 * @param target            The list of actual or target values.
	 * @param predicted         The list of predicted values.
	 * @param allowedPercentage The allowed percentage tolerance for accuracy
	 *                          comparison (e.g., 0.05 for 5%).
	 * @return The accuracy as a proportion of predictions within the specified
	 *         percentage tolerance.
	 */

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

	/**
	 * Calculate the Mean Absolute Percentage Error (MAPE) between two lists of
	 * values. This method computes the Mean Absolute Percentage Error (MAPE)
	 * between the target values and the predicted values. MAPE is a measure of the
	 * average absolute percentage difference between corresponding elements in the
	 * two lists.
	 *
	 * @param target    The list of actual or target values.
	 * @param predicted The list of predicted values.
	 * @return The Mean Absolute Percentage Error (MAPE) between the two lists as a
	 *         percentage.
	 * @throws IllegalArgumentException if the input lists do not have the same
	 *                                  size.
	 */

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

	/**
	 * Calculate the two-tailed p-value for the paired t-test between target and
	 * predicted data. This method performs a paired t-test between the provided
	 * target and predicted data to determine the two-tailed p-value. The p-value
	 * measures the probability that the observed differences between the two
	 * datasets occurred by chance. A low p-value indicates a significant
	 * difference.
	 *
	 * @param target    The list of actual or target values.
	 * @param predicted The list of predicted values.
	 * @return The two-tailed p-value for the paired t-test.
	 * @throws IllegalArgumentException if the input lists do not have the same
	 *                                  size.
	 */

	public static double pvalue(ArrayList<Double> target, ArrayList<Double> predicted) {
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

	/**
	 * Generate and display a performance report. This method generates a
	 * performance report that includes various statistical measures and accuracy
	 * metrics for the provided target and predicted data. It prints out information
	 * about the mean, standard deviation, mean absolute error, root mean squared
	 * error, mean squared error, mean absolute percentage error, and accuracy
	 * within a specified error margin.
	 */

	public void statusReport() {
		System.out.println("");

		System.out.println("..................Performance Report.............................");
		
		System.out.println("Size of actual data = " + this.target.size());
		System.out.println("Size  of prediction data = " + this.predicted.size());

		System.out.println("average of actual data = " + DataStatistics.getMean(this.target));
		System.out.println("average of prediction data = " + DataStatistics.getMean(this.predicted));

		System.out.println("Stander deviation of actual data = " + DataStatistics.getStanderDeviation(this.target));
		System.out
				.println("Stander deviation of predicted data = " + DataStatistics.getStanderDeviation(this.predicted));

		System.out.println("Mean abslute error = " + meanAbsluteError(this.target, this.predicted)
				+ " (average absolute difference between the predicted values and the actual values)");
		System.out.println("RMS error = " + rmsError(this.target, this.predicted) + " (square root of the MSE)");
		System.out.println("Mean squared error  = " + meanSquaredError(this.target, this.predicted)
				+ " (average of the squared differences between predicted values and actual values)");
		System.out.println("Mean abslute percentage error  = " + meanAbslutePercentage(this.target, this.predicted)
				+ "(measures the average percentage difference between predicted and actual values)");
		System.out.println("accuracy for " + this.allowedError * 100 + " % error margine = "
				+ accuracy(this.target, this.predicted, this.allowedError) * 100 + " %");
		// System.out.println("pvalue =" + pvalue(target,predicted));

		System.out.println("");
	}
}
