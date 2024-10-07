package io.openems.edge.predictor.lstmmodel.performance;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.math3.distribution.TDistribution;
import org.apache.commons.math3.stat.StatUtils;

import io.openems.edge.predictor.lstmmodel.common.DataStatistics;

public class PerformanceMatrix {
	private ArrayList<Double> target = new ArrayList<Double>();
	private ArrayList<Double> predicted = new ArrayList<Double>();
	private double allowedError = 0.0;

	public PerformanceMatrix(ArrayList<Double> tar, ArrayList<Double> predict, double allowedErr) {
		this.target = tar;
		this.predicted = predict;
		this.allowedError = allowedErr;

	}

	/**
	 * Calculates the mean absolute error between the target and predicted values.
	 * Mean absolute error (MAE) is a metric that measures the average absolute
	 * difference between corresponding elements of two lists.
	 *
	 * @param target    The list of target values.
	 * @param predicted The list of predicted values.
	 * @return The mean absolute error between the target and predicted values.
	 * @throws IllegalArgumentException If the input lists have different sizes.
	 */
	public static double meanAbsoluteError(ArrayList<Double> target, ArrayList<Double> predicted) {

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
	 * Calculates the Root Mean Square (RMS) error between the target and predicted
	 * values. RMS error is a measure of the average magnitude of the differences
	 * between corresponding elements of two lists.
	 *
	 * @param target    The list of target values.
	 * @param predicted The list of predicted values.
	 * @return The root mean square error between the target and predicted values.
	 * @throws IllegalArgumentException If the input lists have different sizes.
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
	 * Calculate the RmsError of two arrays.
	 * 
	 * @param target    double array of target
	 * @param predicted double array of predicted
	 * @return rms Error
	 */
	public static double rmsError(double[] target, double[] predicted) {
		if (predicted.length != target.length) {
			throw new IllegalArgumentException("Input lists must have the same size");
		}

		double sumSquaredError = 0.0;
		for (int i = 0; i < predicted.length; i++) {
			double error = predicted[i] - target[i];
			sumSquaredError += error * error;
		}

		double meanSquaredError = sumSquaredError / predicted.length;
		return Math.sqrt(meanSquaredError);
	}

	/**
	 * Calculates the Mean Squared Error (MSE) between the target and predicted
	 * values. MSE is a measure of the average squared differences between
	 * corresponding elements of two lists.
	 *
	 * @param target    The list of target values.
	 * @param predicted The list of predicted values.
	 * @return The mean squared error between the target and predicted values.
	 * @throws IllegalArgumentException If the input lists have different sizes.
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
	 * Calculates the accuracy between the target and predicted values within a
	 * specified allowed percentage difference.
	 *
	 * @param target            The list of target values.
	 * @param predicted         The list of predicted values.
	 * @param allowedPercentage The maximum allowed percentage difference for
	 *                          accuracy.
	 * @return The accuracy between the target and predicted values.
	 */
	public static double accuracy(ArrayList<Double> target, ArrayList<Double> predicted, double allowedPercentage) {
		double count = 0;

		for (int i = 0; i < predicted.size(); i++) {
			double diff = Math.abs(predicted.get(i) - target.get(i)) //
					/ Math.max(predicted.get(i), target.get(i));
			if (diff <= allowedPercentage) {
				count++;
			}
		}
		return (double) count / predicted.size();
	}

	/**
	 * Calculate the Accuracy of the predicted compared to target.
	 * 
	 * @param target            double array of target
	 * @param predicted         double array of predicted
	 * @param allowedPercentage allowed percentage error
	 * @return accuracy
	 */
	public static double accuracy(double[] target, double[] predicted, double allowedPercentage) {
		double count = 0;

		for (int i = 0; i < predicted.length; i++) {
			double diff = Math.abs(predicted[i] - target[i]) //
					/ Math.max(predicted[i], target[i]);
			if (diff <= allowedPercentage) {
				count++;
			}
		}
		return (double) count / predicted.length;
	}

	/**
	 * Calculates the Mean Absolute Percentage Error (MAPE) between the target and
	 * predicted values. MAPE is a measure of the average percentage difference
	 * between corresponding elements of two lists.
	 *
	 * @param target    The list of target values.
	 * @param predicted The list of predicted values.
	 * @return The mean absolute percentage error between the target and predicted
	 *         values.
	 * @throws IllegalArgumentException If the input lists have different sizes.
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
	 * Calculates the two-tailed p-value using the t-statistic for the differences
	 * between predicted and actual values.
	 *
	 * @param target    The list of target values.
	 * @param predicted The list of predicted values.
	 * @return The two-tailed p-value for the differences between predicted and
	 *         actual values.
	 * @throws IllegalArgumentException If the input lists have different sizes.
	 */
	public static double pvalue(ArrayList<Double> target, ArrayList<Double> predicted) {
		if (predicted.size() != target.size()) {
			throw new IllegalArgumentException("Input lists must have the same size.");
		}

		List<Double> differences = new ArrayList<>();
		for (int i = 0; i < predicted.size(); i++) {
			differences.add(predicted.get(i) - target.get(i));
		}

		double[] differencesArray = differences.stream()//
				.mapToDouble(Double::doubleValue).toArray();
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
	 * Generates and prints a performance report containing various statistical
	 * metrics and error measures between the actual and predicted data. The report
	 * includes average, standard deviation, mean absolute error, RMS error, mean
	 * squared error, mean absolute percentage error, and accuracy with a specified
	 * error margin. Note: This method assumes that the necessary statistical
	 * methods (e.g., meanAbsoluteError, rmsError, meanSquaredError,
	 * meanAbslutePercentage, accuracy) are implemented in the same class. The
	 * p-value calculation is not included in the report by default.
	 */
	public void statusReport() {
		System.out.println("\n.................. Performance Report .............................");

		// Calculate and display statistics for actual data
		double averageActual = DataStatistics.getMean(this.target);
		double stdDevActual = DataStatistics.getStandardDeviation(this.target);
		System.out.println("Average of actual data = " + averageActual);
		System.out.println("Standard deviation of actual data = " + stdDevActual);

		// Calculate and display statistics for predicted data
		double averagePredicted = DataStatistics.getMean(this.predicted);
		double stdDevPredicted = DataStatistics.getStandardDeviation(this.predicted);
		System.out.println("Average of prediction data = " + averagePredicted);
		System.out.println("Standard deviation of predicted data = " + stdDevPredicted);

		// Display various error metrics
		System.out.println("Mean absolute error = " + meanAbsoluteError(this.target, this.predicted)
				+ " (average absolute difference between predicted and actual values)");
		System.out.println("RMS error = " + rmsError(this.target, this.predicted) + " (square root of the MSE)");
		System.out.println("Mean squared error = " + meanSquaredError(this.target, this.predicted)
				+ " (average of the squared differences between predicted and actual values)");
		System.out.println("Mean absolute percentage error = " + meanAbslutePercentage(this.target, this.predicted)
				+ " (measures the average percentage difference between predicted and actual values)");

		// Display accuracy with the specified error margin
		double accuracyPercentage = accuracy(this.target, this.predicted, this.allowedError) * 100;
		System.out.println("Accuracy for " + this.allowedError * 100 + "% error margin = " + accuracyPercentage + "%");

		// System.out.println("P-value = " + pvalue(target, predicted));

		System.out.println("");
	}

}
